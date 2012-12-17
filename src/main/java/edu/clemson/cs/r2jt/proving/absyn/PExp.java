package edu.clemson.cs.r2jt.proving.absyn;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.clemson.cs.r2jt.absyn.AlternativeExp;
import edu.clemson.cs.r2jt.absyn.DotExp;
import edu.clemson.cs.r2jt.absyn.EqualsExp;
import edu.clemson.cs.r2jt.absyn.Exp;
import edu.clemson.cs.r2jt.absyn.FunctionExp;
import edu.clemson.cs.r2jt.absyn.InfixExp;
import edu.clemson.cs.r2jt.absyn.IntegerExp;
import edu.clemson.cs.r2jt.absyn.IsInExp;
import edu.clemson.cs.r2jt.absyn.LambdaExp;
import edu.clemson.cs.r2jt.absyn.OutfixExp;
import edu.clemson.cs.r2jt.absyn.PrefixExp;
import edu.clemson.cs.r2jt.absyn.VarExp;
import edu.clemson.cs.r2jt.absyn.VariableDotExp;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.mathtype.MTType;
import edu.clemson.cs.r2jt.proving.immutableadts.ImmutableList;

/**
 * <p><code>PExp</code> is the root of the prover abstract syntax tree 
 * hierarchy.  Unlike {@link edu.clemson.cs.r2jt.absyn.Exp Exp}s, 
 * <code>PExp</code>s are immutable and exist without the complications 
 * introduced by control structures.  <code>PExp</code>s exist to represent
 * mathematical expressions only.</p>
 */
public abstract class PExp {

    protected final static BindingException BINDING_EXCEPTION =
            new BindingException();

    public final int structureHash;
    public final int valueHash;

    protected final MTType myType;
    protected final MTType myTypeValue;

    private Set<String> myCachedSymbolNames = null;
    private List<PExp> myCachedFunctionApplications = null;
    private Set<PSymbol> myCachedQuantifiedVariables = null;

    public PExp(HashDuple hashes, MTType type, MTType typeValue) {
        this(hashes.structureHash, hashes.valueHash, type, typeValue);
    }

    public PExp(int structureHash, int valueHash, MTType type, MTType typeValue) {
        myType = type;
        myTypeValue = typeValue;
        this.structureHash = structureHash;
        this.valueHash = valueHash;
    }

    public abstract void accept(PExpVisitor v);

    public final MTType getType() {
        return myType;
    }

    public final MTType getTypeValue() {
        return myTypeValue;
    }

    @SuppressWarnings("unchecked")
    public PExp withTypesSubstituted(Map<MTType, MTType> substitutions) {

        TypeModifyingVisitor v = new TypeModifyingVisitor(substitutions);
        this.accept(v);

        return v.getFinalPExp();
    }

    public abstract PExp withTypeReplaced(MTType t);

    public abstract PExp withTypeValueReplaced(MTType t);

    public abstract PExp withSubExpressionReplaced(int index, PExp e);

    public PExp withSubExpressionsReplaced(Map<Integer, PExp> e) {
        PExp working = this;

        for (Map.Entry<Integer, PExp> entry : e.entrySet()) {
            working =
                    working.withSubExpressionReplaced(entry.getKey(), entry
                            .getValue());
        }

        return working;
    }

    public abstract ImmutableList<PExp> getSubExpressions();

    public abstract PExpSubexpressionIterator getSubExpressionIterator();

    public abstract boolean isObviouslyTrue();

    public final List<PExp> splitIntoConjuncts() {
        List<PExp> conjuncts = new LinkedList<PExp>();

        splitIntoConjuncts(conjuncts);

        return conjuncts;
    }

    protected abstract void splitIntoConjuncts(List<PExp> accumulator);

    public abstract PExp flipQuantifiers();

    /**
     * <p>Simply walks the tree represented by the given <code>Exp</code> and
     * sounds the alarm if it or any sub-expression does not have a type.  As
     * a convenience, returns the same expression it is given so that it can
     * be used without introducing intermediate variables.</p>
     * 
     * @param e
     */
    public static final <E extends Exp> E sanityCheckExp(E e) {

        if (e.getMathType() == null) {

            String varExpAdditional = "";
            if (e instanceof VarExp) {
                varExpAdditional =
                        " = \"" + ((VarExp) e).getName().getName() + "\", "
                                + ((VarExp) e).getName().getLocation();
            }

            throw new UnsupportedOperationException(
                    "Expression has null type.\n\n" + e + " (" + e.getClass()
                            + ")" + varExpAdditional);
        }

        for (Exp subexp : e.getSubExpressions()) {
            sanityCheckExp(subexp);
        }

        return e;
    }

    public static final PExp buildPExp(Exp e) {
        PExp retval;

        if (e == null) {
            throw new IllegalArgumentException("Prover does not accept null "
                    + "as an expression.");
        }

        if (e instanceof FunctionExp) {
            FunctionExp eAsFunctionExp = (FunctionExp) e;

            List<PExp> arguments = new LinkedList<PExp>();
            Iterator<Exp> eArgs = eAsFunctionExp.argumentIterator();
            while (eArgs.hasNext()) {
                arguments.add(PExp.buildPExp(eArgs.next()));
            }

            retval =
                    new PSymbol(e.getMathType(), e.getMathTypeValue(),
                            fullName(eAsFunctionExp.getQualifier(),
                                    eAsFunctionExp.getName().getName()),
                            arguments, convertExpQuantification(eAsFunctionExp
                                    .getQuantification()));
        }
        else if (e instanceof PrefixExp) {
            PrefixExp eAsPrefixExp = (PrefixExp) e;

            List<PExp> arguments = new LinkedList<PExp>();
            arguments.add(PExp.buildPExp(eAsPrefixExp.getArgument()));

            retval =
                    new PSymbol(e.getMathType(), e.getMathTypeValue(),
                            eAsPrefixExp.getSymbol().getName(), arguments);
        }
        else if (e instanceof InfixExp) {
            InfixExp eAsInfixExp = (InfixExp) e;

            List<PExp> arguments = new LinkedList<PExp>();
            arguments.add(PExp.buildPExp(eAsInfixExp.getLeft()));
            arguments.add(PExp.buildPExp(eAsInfixExp.getRight()));

            retval =
                    new PSymbol(e.getMathType(), e.getMathTypeValue(),
                            eAsInfixExp.getOpName().getName(), arguments,
                            PSymbol.DisplayType.INFIX);
        }
        else if (e instanceof IsInExp) {
            IsInExp eAsIsInExp = (IsInExp) e;

            List<PExp> arguments = new LinkedList<PExp>();
            arguments.add(PExp.buildPExp(eAsIsInExp.getLeft()));
            arguments.add(PExp.buildPExp(eAsIsInExp.getRight()));

            retval =
                    new PSymbol(e.getMathType(), e.getMathTypeValue(), "is_in",
                            arguments, PSymbol.DisplayType.INFIX);
        }
        else if (e instanceof OutfixExp) {
            OutfixExp eAsOutfixExp = (OutfixExp) e;

            List<PExp> arguments = new LinkedList<PExp>();
            arguments.add(PExp.buildPExp(eAsOutfixExp.getArgument()));

            retval =
                    new PSymbol(e.getMathType(), e.getMathTypeValue(),
                            eAsOutfixExp.getLeftDelimiter(), eAsOutfixExp
                                    .getRightDelimiter(), arguments,
                            PSymbol.DisplayType.OUTFIX);
        }
        else if (e instanceof EqualsExp) {
            EqualsExp eAsEqualsExp = (EqualsExp) e;

            List<PExp> arguments = new LinkedList<PExp>();
            arguments.add(PExp.buildPExp(eAsEqualsExp.getLeft()));
            arguments.add(PExp.buildPExp(eAsEqualsExp.getRight()));

            retval =
                    new PSymbol(e.getMathType(), e.getMathTypeValue(),
                            eAsEqualsExp.getOperatorAsString(), arguments,
                            PSymbol.DisplayType.INFIX);
        }
        else if (e instanceof IntegerExp) {
            IntegerExp eAsIntegerExp = (IntegerExp) e;

            String symbol = "" + eAsIntegerExp.getValue();

            retval = new PSymbol(e.getMathType(), e.getMathTypeValue(), symbol);
        }
        else if (e instanceof DotExp) {
            DotExp eAsDotExp = (DotExp) e;

            String symbol = "";

            boolean first = true;
            for (Exp s : eAsDotExp.getSegments()) {
                if (!first) {
                    symbol += ".";
                }
                else {
                    first = false;
                }

                symbol += s;
            }

            if (eAsDotExp.getSemanticExp() != null) {
                symbol += PExp.buildPExp(eAsDotExp.getSemanticExp());
            }

            retval = new PSymbol(e.getMathType(), e.getMathTypeValue(), symbol);
        }
        else if (e instanceof VarExp) {
            VarExp eAsVarExp = (VarExp) e;

            retval =
                    new PSymbol(eAsVarExp.getMathType(), eAsVarExp
                            .getMathTypeValue(), fullName(eAsVarExp
                            .getQualifier(), eAsVarExp.getName().getName()),
                            convertExpQuantification(eAsVarExp
                                    .getQuantification()));
        }
        else if (e instanceof VariableDotExp) {
            VariableDotExp eAsDotExp = (VariableDotExp) e;

            String finalName = "";
            for (Exp s : eAsDotExp.getSegments()) {
                finalName += "." + s.toString(0);
            }

            finalName += eAsDotExp.getSemanticExp().toString(0);

            retval =
                    new PSymbol(eAsDotExp.getSemanticExp().getMathType(),
                            eAsDotExp.getSemanticExp().getMathTypeValue(),
                            finalName);
        }
        else if (e instanceof LambdaExp) {
            LambdaExp eAsLambdaExp = (LambdaExp) e;

            retval =
                    new PLambda(eAsLambdaExp.getName().getName(), eAsLambdaExp
                            .getMathType(), PExp.buildPExp(eAsLambdaExp
                            .getBody()));
        }
        else if (e instanceof AlternativeExp) {
            AlternativeExp eAsAlternativeExp = (AlternativeExp) e;

            retval = new PAlternatives(eAsAlternativeExp);
        }
        else if (e instanceof VariableDotExp) {
            VariableDotExp eAsDotExp = (VariableDotExp) e;

            String finalName = "";
            for (Exp s : eAsDotExp.getSegments()) {
                finalName += "." + s.toString(0);
            }

            finalName += eAsDotExp.getSemanticExp().toString(0);

            retval =
                    new PSymbol(eAsDotExp.getSemanticExp().getMathType(),
                            eAsDotExp.getSemanticExp().getMathTypeValue(),
                            finalName);
        }
        else if (e instanceof LambdaExp) {
            LambdaExp eAsLambdaExp = (LambdaExp) e;

            retval =
                    new PLambda(eAsLambdaExp.getName().getName(), eAsLambdaExp
                            .getMathType(), PExp.buildPExp(eAsLambdaExp
                            .getBody()));
        }
        else if (e instanceof AlternativeExp) {
            AlternativeExp eAsAlternativeExp = (AlternativeExp) e;

            retval = new PAlternatives(eAsAlternativeExp);
        }
        else {
            throw new RuntimeException("Expressions of type " + e.getClass()
                    + " are not accepted by the prover.");
        }

        //The Analyzer doesn't work consistently.  Fail early if we don't have
        //typing information
        if (retval.getType() == null) {

            String varExpAdditional = "";
            if (e instanceof VarExp) {
                varExpAdditional =
                        " = \"" + ((VarExp) e).getName().getName() + "\", "
                                + ((VarExp) e).getName().getLocation();
            }

            throw new UnsupportedOperationException(
                    "Expression has null type.\n\n" + e + " (" + e.getClass()
                            + ")" + varExpAdditional);
        }

        return retval;
    }

    public final Map<PExp, PExp> bindTo(PExp target) throws BindingException {
        Map<PExp, PExp> bindings = new HashMap<PExp, PExp>();

        bindTo(target, bindings);

        return bindings;
    }

    protected abstract void bindTo(PExp target, Map<PExp, PExp> accumulator)
            throws BindingException;

    public int hashCode() {
        return valueHash;
    }

    public abstract PExp substitute(Map<PExp, PExp> substitutions);

    public abstract boolean containsName(String name);

    public final Set<String> getSymbolNames() {
        if (myCachedSymbolNames == null) {
            //We're immutable, so only do this once
            myCachedSymbolNames = getSymbolNamesNoCache();
        }

        return myCachedSymbolNames;
    }

    protected abstract Set<String> getSymbolNamesNoCache();

    public final Set<PSymbol> getQuantifiedVariables() {
        if (myCachedQuantifiedVariables == null) {
            //We're immutable, so only do this once
            myCachedQuantifiedVariables = getQuantifiedVariablesNoCache();
        }

        return myCachedQuantifiedVariables;
    }

    public abstract Set<PSymbol> getQuantifiedVariablesNoCache();

    public final List<PExp> getFunctionApplications() {
        if (myCachedFunctionApplications == null) {
            //We're immutable, so only do this once
            myCachedFunctionApplications = getFunctionApplicationsNoCache();
        }

        return myCachedFunctionApplications;
    }

    public abstract List<PExp> getFunctionApplicationsNoCache();

    public abstract boolean containsExistential();

    public abstract boolean isEquality();

    public abstract boolean isLiteral();

    private final static PSymbol.Quantification convertExpQuantification(int q) {

        PSymbol.Quantification retval;

        switch (q) {
        case VarExp.EXISTS:
            retval = PSymbol.Quantification.THERE_EXISTS;
            break;
        case VarExp.FORALL:
            retval = PSymbol.Quantification.FOR_ALL;
            break;
        case VarExp.NONE:
            retval = PSymbol.Quantification.NONE;
            break;
        default:
            throw new RuntimeException("Unrecognized quantification");
        }

        return retval;
    }

    private final static String fullName(PosSymbol qualifier, String name) {
        String retval;

        if (qualifier == null) {
            retval = "";
        }
        else {
            if (qualifier.getName() == null) {
                retval = "";
            }
            else {
                retval = qualifier.getName() + ".";
            }
        }

        return retval + name;
    }

    public boolean typeMatches(MTType other) {
        return myType.isSubtypeOf(other);
    }

    public boolean typeMatches(PExp other) {
        return typeMatches(other.getType());
    }

    public void processStringRepresentation(PExpVisitor visitor, Appendable a) {
        PExpTextRenderingVisitor renderer = new PExpTextRenderingVisitor(a);
        PExpVisitor finalVisitor = new NestedPExpVisitors(visitor, renderer);

        this.accept(finalVisitor);
    }

    public abstract boolean isVariable();

    public static class HashDuple {

        public int structureHash;
        public int valueHash;

        public HashDuple(int structureHash, int valueHash) {
            this.structureHash = structureHash;
            this.valueHash = valueHash;
        }
    }

    public final String toString() {
        StringWriter output = new StringWriter();
        PExpTextRenderingVisitor renderer =
                new PExpTextRenderingVisitor(output);

        this.accept(renderer);

        return output.toString();
    }
}
