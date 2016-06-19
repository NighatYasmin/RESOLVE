/**
 * SyntacticSugarConverter.java
 * ---------------------------------
 * Copyright (c) 2016
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.rsrg.parsing.utilities;

import edu.clemson.cs.rsrg.absyn.ResolveConceptualElement;
import edu.clemson.cs.rsrg.absyn.clauses.AffectsClause;
import edu.clemson.cs.rsrg.absyn.clauses.AssertionClause;
import edu.clemson.cs.rsrg.absyn.declarations.facilitydecl.FacilityDec;
import edu.clemson.cs.rsrg.absyn.declarations.operationdecl.ProcedureDec;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.ParameterVarDec;
import edu.clemson.cs.rsrg.absyn.expressions.programexpr.*;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.VarDec;
import edu.clemson.cs.rsrg.absyn.items.programitems.IfConditionItem;
import edu.clemson.cs.rsrg.absyn.rawtypes.NameTy;
import edu.clemson.cs.rsrg.absyn.rawtypes.Ty;
import edu.clemson.cs.rsrg.absyn.statements.*;
import edu.clemson.cs.rsrg.errorhandling.exception.MiscErrorException;
import edu.clemson.cs.rsrg.parsing.data.Location;
import edu.clemson.cs.rsrg.parsing.data.PosSymbol;
import edu.clemson.cs.rsrg.treewalk.TreeWalkerVisitor;
import java.util.*;

/**
 * <p>This class performs the various different syntactic sugar conversions
 * using part of the RESOLVE abstract syntax tree. This visitor logic is
 * implemented as a {@link TreeWalkerVisitor}.</p>
 *
 * @author Yu-Shan Sun
 * @version 1.0
 */
public class SyntacticSugarConverter extends TreeWalkerVisitor {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /**
     * <p>This map provides a mapping between the newly declared array name types
     * to the types of elements in the array.</p>
     */
    private final Map<NameTy, NameTy> myArrayNameTyToInnerTyMap;

    /**
     * <p>Once we are done walking the tree, the top most node will create
     * the new element.</p>
     */
    private ResolveConceptualElement myFinalProcessedElement;

    /**
     * <p>Since we don't have symbol table, we really don't know if
     * we are generating a new object with the same name. In order to avoid
     * problems, all of our objects will have a name that starts with "_" and
     * end the current new element counter. This number increases by 1 each
     * time we create a new element.</p>
     */
    private int myNewElementCounter;

    /**
     * <p>This stores the new statements created by current statement
     * we are visiting.</p>
     */
    private NewStatementsContainer myNewStatementsContainer;

    /**
     * <p>This stores the {@link ParameterVarDec}, {@link FacilityDec} and
     * {@link VarDec} obtained from the parent node.</p>
     */
    private ParentNodeElementsContainer myParentNodeElementsContainer;

    /**
     * <p>This is a map from the original {@link ResolveConceptualElement} to
     * the replacing {@link ResolveConceptualElement}.</p>
     */
    private final Map<ResolveConceptualElement, ResolveConceptualElement> myReplacingElementsMap;

    /**
     * <p>Once we are done walking an element that could have a syntactic
     * sugar conversion, we add that element into the top-most collector.</p>
     *
     * <p>When we are done walking a {@link ResolveConceptualElement} that
     * can contain a list of {@link Statement}s, we build a new instance
     * of the object using the elements in the collector.</p>
     */
    private Stack<ResolveConceptualElementCollector> myResolveElementCollectorStack;

    // ===========================================================
    // Constructors
    // ===========================================================

    public SyntacticSugarConverter(Map<NameTy, NameTy> arrayNameTyToInnerTyMap,
            int newElementCounter) {
        myArrayNameTyToInnerTyMap = arrayNameTyToInnerTyMap;
        myFinalProcessedElement = null;
        myNewElementCounter = newElementCounter;
        myReplacingElementsMap = new HashMap<>();
        myResolveElementCollectorStack = new Stack<>();
    }

    // ===========================================================
    // Visitor Methods
    // ===========================================================

    // -----------------------------------------------------------
    // Parent Nodes
    // -----------------------------------------------------------

    /**
     * <p>This should be the top-most node that we start with
     * when processing syntactic sugar conversions inside a
     * {@link ProcedureDec}.</p>
     *
     * @param e Current {@link ProcedureDec} we are visiting.
     */
    @Override
    public void preProcedureDec(ProcedureDec e) {
        // Store the params, facility and variable declarations
        myParentNodeElementsContainer =
                new ParentNodeElementsContainer(e.getParameters(), e
                        .getFacilities(), e.getVariables());

        // Create a new collector
        myResolveElementCollectorStack
                .push(new ResolveConceptualElementCollector(e));
    }

    /**
     * <p>This should be the last element we walk. All syntactic
     * sugar should have been performed and we can safely create
     * the new {@link ProcedureDec} to be returned.</p>
     *
     * @param e Current {@link ProcedureDec} we are visiting.
     */
    @Override
    public void postProcedureDec(ProcedureDec e) {
        // Return type (if any)
        Ty returnTy = null;
        if (e.getReturnTy() != null) {
            returnTy = e.getReturnTy().clone();
        }

        // Affects clause (if any)
        AffectsClause affectsClause = null;
        if (e.getAffectedVars() != null) {
            affectsClause = e.getAffectedVars().clone();
        }

        // Decreasing clause (if any)
        AssertionClause decreasingClause = null;
        boolean recursiveFlag = false;
        if (e.getDecreasing() != null) {
            decreasingClause = e.getDecreasing().clone();
            recursiveFlag = true;
        }

        // Build the new ProcedureDec
        ResolveConceptualElementCollector collector =
                myResolveElementCollectorStack.pop();
        myFinalProcessedElement =
                new ProcedureDec(e.getName().clone(),
                        myParentNodeElementsContainer.parameterVarDecs,
                        returnTy, affectsClause, decreasingClause,
                        myParentNodeElementsContainer.facilityDecs,
                        myParentNodeElementsContainer.varDecs, collector.stmts,
                        recursiveFlag);

        // Just in case
        myParentNodeElementsContainer = null;
    }

    // -----------------------------------------------------------
    // Statement Nodes
    // -----------------------------------------------------------

    /**
     * <p>This statement could have syntactic sugar conversions, so
     * we will need to have a way to store the new {@link Statement}s that get
     * generated.</p>
     *
     * @param e Current {@link CallStmt} we are visiting.
     */
    @Override
    public void preCallStmt(CallStmt e) {
        myNewStatementsContainer = new NewStatementsContainer();
    }

    @Override
    public void postCallStmt(CallStmt e) {
        myNewStatementsContainer = null;
    }

    /**
     * <p>This statement doesn't need to do any syntactic sugar conversions,
     * therefore we create a new {@link ConfirmStmt} and add it to the top-most
     * {@link ResolveConceptualElementCollector} instance.</p>
     *
     * @param e Current {@link ConfirmStmt} we are visiting.
     */
    @Override
    public void postConfirmStmt(ConfirmStmt e) {
        addToInnerMostCollector(e.clone());
    }

    /**
     * <p>This statement could have syntactic sugar conversions, so
     * we will need to have a way to store the new {@link Statement}s that get
     * generated.</p>
     *
     * @param e Current {@link FuncAssignStmt} we are visiting.
     */
    @Override
    public void preFuncAssignStmt(FuncAssignStmt e) {
        myNewStatementsContainer = new NewStatementsContainer();
    }

    @Override
    public void postFuncAssignStmt(FuncAssignStmt e) {
        myNewStatementsContainer = null;
    }

    @Override
    public void preIfStmt(IfStmt e) {
        // We have began a new block that can contain statements,
        // we need to store this in our stack.
        myResolveElementCollectorStack
                .push(new ResolveConceptualElementCollector(e));
    }

    @Override
    public void postIfStmt(IfStmt e) {
        // Done visiting this IfStmt, so we can pop it off the stack.
        ResolveConceptualElementCollector collector =
                myResolveElementCollectorStack.pop();

        // TODO: Build the new IfStmt and add it to the 'next' collector.
    }

    /**
     * <p>This statement doesn't need to do any syntactic sugar conversions,
     * therefore we create a new {@link MemoryStmt} and add it to our
     * {@link ResolveConceptualElementCollector} instance.</p>
     *
     * @param e Current {@link MemoryStmt} we are visiting.
     */
    @Override
    public void postMemoryStmt(MemoryStmt e) {
        addToInnerMostCollector(e.clone());
    }

    /**
     * <p>This statement doesn't need to do any syntactic sugar conversions,
     * therefore we create a new {@link PresumeStmt} and add it to our
     * {@link ResolveConceptualElementCollector} instance.</p>
     *
     * @param e Current {@link PresumeStmt} we are visiting.
     */
    @Override
    public void postPresumeStmt(PresumeStmt e) {
        addToInnerMostCollector(e.clone());
    }

    /**
     * <p>This statement could have syntactic sugar conversions, so
     * we will need to have a way to store the new {@link Statement}s that get
     * generated.</p>
     *
     * @param e Current {@link SwapStmt} we are visiting.
     */
    @Override
    public void preSwapStmt(SwapStmt e) {
        myNewStatementsContainer = new NewStatementsContainer();
    }

    @Override
    public void postSwapStmt(SwapStmt e) {
        myNewStatementsContainer = null;
    }

    @Override
    public void preWhileStmt(WhileStmt e) {
        // We have began a new block that can contain statements,
        // we need to store this in our stack.
        myResolveElementCollectorStack
                .push(new ResolveConceptualElementCollector(e));

        // A container for the loop-condition
        myNewStatementsContainer = new NewStatementsContainer();
    }

    @Override
    public void midWhileStmt(WhileStmt e, ResolveConceptualElement previous,
            ResolveConceptualElement next) {
    // TODO: Check to see if the condition item has any syntactic sugar conversions.
    // If yes, we will need to add it back to the while statement list.

    // TODO: Set myNewStatementsContainer to null if we are done visiting the loop condition
    }

    @Override
    public void postWhileStmt(WhileStmt e) {
        // Done visiting this WhileStmt, so we can pop it off the stack.
        ResolveConceptualElementCollector collector =
                myResolveElementCollectorStack.pop();

        // TODO: Build the new WhileStmt and add it to the 'next' collector.
    }

    // -----------------------------------------------------------
    // Item Nodes
    // -----------------------------------------------------------

    @Override
    public void preIfConditionItem(IfConditionItem e) {
        // We have began a new block that can contain statements,
        // we need to store this in our stack.
        myResolveElementCollectorStack
                .push(new ResolveConceptualElementCollector(e));

        // A container for the if-condition
        myNewStatementsContainer = new NewStatementsContainer();
    }

    @Override
    public void midIfConditionItem(IfConditionItem e,
            ResolveConceptualElement previous, ResolveConceptualElement next) {
    // TODO: Check to see if the condition item has any syntactic sugar conversions.
    // If yes, we will need to add it back to both the statements inside the if and also inside the else.

    // TODO: Set myNewStatementsContainer to null if we are done visiting the if condition
    }

    @Override
    public void postIfConditionItem(IfConditionItem e) {
        // Done visiting this IfConditionItem, so we can pop it off the stack.
        ResolveConceptualElementCollector collector =
                myResolveElementCollectorStack.pop();

        // TODO: Build the new IfConditionItem and add it to our new items map.
    }

    // -----------------------------------------------------------
    // Program Expression Nodes
    // -----------------------------------------------------------

    /**
     * <p>This replaces the {@link ProgramVariableArrayExp} inside
     * the calling arguments with appropriate swapping operation from
     * {@code Static_Array_Template}.</p>
     *
     * <p>Any new statements generated will be added to a
     * {@link NewStatementsContainer} instance.</p>
     *
     * @param e Current {@link ProgramFunctionExp} we are visiting.
     */
    @Override
    public void postProgramFunctionExp(ProgramFunctionExp e) {
        List<ProgramExp> args = e.getArguments();
        List<ProgramExp> newArgs = new ArrayList<>();
        for (ProgramExp arg : args) {
            // Check each of the args to see if we have ProgramVariableArrayExp.
            if (isProgArrayExp(arg)) {
                // TODO: Apply the syntactic conversions for arrays.
            }
            else {
                // Make a deep copy from the original ProgramExp.
                newArgs.add(arg.clone());
            }
        }

        // Construct a new ProgramFunctionExp to put in our map
        PosSymbol qualifier = null;
        if (e.getQualifier() != null) {
            qualifier = e.getQualifier().clone();
        }

        myReplacingElementsMap.put(e, new ProgramFunctionExp(new Location(e.getLocation()),
                qualifier, e.getName().clone(), newArgs));
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * <p>This method returns the current integer value for the
     * new element counter.</p>
     *
     * @return The counter integer.
     */
    public final int getNewElementCounter() {
        return myNewElementCounter;
    }

    /**
     * <p>This method returns the new (and potentially modified) element
     * that includes the syntactic sugar conversions.</p>
     *
     * @return A {@link ResolveConceptualElement} object.
     */
    public final ResolveConceptualElement getProcessedElement() {
        if (myFinalProcessedElement == null) {
            throw new MiscErrorException(
                    "The new element didn't get constructed appropriately!",
                    new NullPointerException());
        }

        return myFinalProcessedElement;
    }

    // ===========================================================
    // Private Methods
    // ===========================================================

    /**
     * <p>An helper method to add a {@link Statement} to the top-most
     * collector.</p>
     *
     * @param statement A {@link Statement} object.
     */
    private void addToInnerMostCollector(Statement statement) {
        // Get the top-most collector and add the statement
        ResolveConceptualElementCollector collector =
                myResolveElementCollectorStack.pop();
        collector.stmts.add(statement);

        // Put it back on the stack
        myResolveElementCollectorStack.push(collector);
    }

    /**
     * <p>An helper method to check whether or not the {@link ProgramExp} passed
     * in is a {@link ProgramVariableArrayExp}. This includes {@link ProgramVariableDotExp}
     * that contain a {@link ProgramVariableArrayExp} as the last element.</p>
     *
     * @param exp The {@link ProgramExp} to be checked.
     *
     * @return {@code true} if it is a programming array expression, {@code false} otherwise.
     */
    private boolean isProgArrayExp(ProgramExp exp) {
        boolean retVal = false;
        if (exp instanceof ProgramVariableArrayExp) {
            retVal = true;
        }
        else if (exp instanceof ProgramVariableDotExp) {
            List<ProgramVariableExp> segments =
                    ((ProgramVariableDotExp) exp).getSegments();
            if (segments.get(segments.size() - 1) instanceof ProgramVariableArrayExp) {
                retVal = true;
            }
        }

        return retVal;
    }

    // ===========================================================
    // Helper Constructs
    // ===========================================================

    /**
     * <p>This holds a copy of the {@link ParameterVarDec}, {@link VarDec} and
     * {@link FacilityDec} for the incoming parent node.</p>
     */
    private class ParentNodeElementsContainer {

        // ===========================================================
        // Member Fields
        // ===========================================================

        /**
         * <p>List of parameter variable declaration objects.</p>
         */
        final List<ParameterVarDec> parameterVarDecs;

        /**
         * <p>List of variable declaration objects.</p>
         */
        final List<FacilityDec> facilityDecs;

        /**
         * <p>List of variable declaration objects.</p>
         */
        final List<VarDec> varDecs;

        // ===========================================================
        // Constructors
        // ===========================================================

        /**
         * <p>This constructs a temporary structure to store the elements from
         * the incoming parent node we are walking.</p>
         *
         * @param params List of parameter variables.
         * @param facs List of facility declarations.
         * @param vars List of regular variables.
         */
        ParentNodeElementsContainer(List<ParameterVarDec> params,
                List<FacilityDec> facs, List<VarDec> vars) {
            parameterVarDecs = copyParamDecls(params);
            facilityDecs = copyFacDecls(facs);
            varDecs = copyVarDecls(vars);
        }

        // ===========================================================
        // Private Methods
        // ===========================================================

        /**
         * <p>An helper method to create a new list of {@link FacilityDec}s
         * that is a deep copy of the one passed in.</p>
         *
         * @param facilityDecs The original list of {@link FacilityDec}s.
         *
         * @return A list of {@link FacilityDec}s.
         */
        private List<FacilityDec> copyFacDecls(List<FacilityDec> facilityDecs) {
            List<FacilityDec> copyFacilityDecs = new ArrayList<>();
            for (FacilityDec facilityDec : facilityDecs) {
                copyFacilityDecs.add((FacilityDec) facilityDec.clone());
            }

            return copyFacilityDecs;
        }

        /**
         * <p>An helper method to create a new list of {@link ParameterVarDec}s
         * that is a deep copy of the one passed in.</p>
         *
         * @param parameterVarDecs The original list of {@link ParameterVarDec}s.
         *
         * @return A list of {@link ParameterVarDec}s.
         */
        private List<ParameterVarDec> copyParamDecls(List<ParameterVarDec> parameterVarDecs) {
            List<ParameterVarDec> copyParamDecs = new ArrayList<>();
            for (ParameterVarDec parameterVarDec : parameterVarDecs) {
                copyParamDecs.add((ParameterVarDec) parameterVarDec.clone());
            }

            return copyParamDecs;
        }

        /**
         * <p>An helper method to create a new list of {@link VarDec}s
         * that is a deep copy of the one passed in.</p>
         *
         * @param varDecs The original list of {@link VarDec}s.
         *
         * @return A list of {@link VarDec}s.
         */
        private List<VarDec> copyVarDecls(List<VarDec> varDecs) {
            List<VarDec> copyVarDecs = new ArrayList<>();
            for (VarDec varDec : varDecs) {
                copyVarDecs.add((VarDec) varDec.clone());
            }

            return copyVarDecs;
        }
    }

    /**
     * <p>As we walk though the various different nodes that can contain
     * a list of {@link Statement}s, once we are done with a particular
     * {@link Statement}, it will be added to this class. Once we are done
     * walking all the statements and we are back to the
     * {@link ResolveConceptualElement} that contains the list of
     * {@link Statement}s, we will use the elements
     * stored in this class to create the new object.</p>
     */
    private class ResolveConceptualElementCollector {

        // ===========================================================
        // Member Fields
        // ===========================================================

        /**
         * <p>The {@link ResolveConceptualElement} that created this collector.</p>
         */
        final ResolveConceptualElement instantiatingElement;

        /**
         * <p>List of statement objects.</p>
         */
        final List<Statement> stmts;

        // ===========================================================
        // Constructors
        // ===========================================================

        /**
         * <p>This constructs a temporary structure to store the list of
         * {@link Statement}s.</p>
         *
         * @param e The element that created this object.
         */
        ResolveConceptualElementCollector(ResolveConceptualElement e) {
            instantiatingElement = e;
            stmts = new ArrayList<>();
        }
    }

    /**
     * <p>This holds new {@link Statement}s related to syntactic sugar conversions for
     * {@link ProgramVariableArrayExp}.</p>
     */
    private class NewStatementsContainer {

        // ===========================================================
        // Member Fields
        // ===========================================================

        /**
         * <p>A stack of new statements that needs to be inserted before the code
         * that contains a program array expression.</p>
         *
         * <p><strong>Note:</strong> The only statements generated at the moment are
         * either new function assignment statements from indexes in
         * program array expressions or call statements to swap elements in the array(s).</p>
         */
        final Stack<Statement> newPreStmts;

        /**
         * <p>A queue of new statements that needs to be inserted after the code
         * that contains a program array expression.</p>
         *
         * <p><strong>Note:</strong> The only statements generated at the moment are
         * call statements to swap elements in the array(s).</p>
         */
        final Queue<Statement> newPostStmts;

        // ===========================================================
        // Constructors
        // ===========================================================

        /**
         * <p>This constructs a temporary structure to store all the new statements that
         * resulted from syntactic sugar conversions for {@link ProgramVariableArrayExp}.</p>
         */
        NewStatementsContainer() {
            newPreStmts = new Stack<>();
            newPostStmts = new ArrayDeque<>();
        }
    }
}