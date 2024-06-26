/*
 * ConstantParamDec.java
 * ---------------------------------
 * Copyright (c) 2024
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.rsrg.absyn.declarations.paramdecl;

import edu.clemson.rsrg.absyn.declarations.Dec;
import edu.clemson.rsrg.absyn.declarations.variabledecl.ParameterVarDec;
import edu.clemson.rsrg.absyn.rawtypes.Ty;
import edu.clemson.rsrg.parsing.data.PosSymbol;
import edu.clemson.rsrg.typeandpopulate.entry.ProgramParameterEntry;

/**
 * <p>
 * This is the class for all the constant parameter declaration objects that the compiler builds using the ANTLR4 AST
 * nodes.
 * </p>
 *
 * @version 2.0
 */
public class ConstantParamDec extends Dec implements ModuleParameter {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /**
     * <p>
     * The parameter variable.
     * </p>
     */
    private final ParameterVarDec myParameterDec;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>
     * This constructs a constant variable that is passed as a parameter to a module.
     * </p>
     *
     * @param name
     *            A {@link PosSymbol} representing the variable's name.
     * @param ty
     *            A {@link Ty} representing the variable's raw type.
     */
    public ConstantParamDec(PosSymbol name, Ty ty) {
        super(name.getLocation(), name);
        myParameterDec = new ParameterVarDec(ProgramParameterEntry.ParameterMode.EVALUATES, name, ty);
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public final String asString(int indentSize, int innerIndentInc) {
        return myParameterDec.asString(indentSize, innerIndentInc);
    }

    /**
     * <p>
     * Returns the variable defined in this parameter declaration.
     * </p>
     *
     * @return A {@link ParameterVarDec} representation object.
     */
    public final ParameterVarDec getVarDec() {
        return myParameterDec;
    }

    // ===========================================================
    // Protected Methods
    // ===========================================================

    /**
     * {@inheritDoc}
     */
    @Override
    protected final ConstantParamDec copy() {
        return new ConstantParamDec(myName.clone(), myParameterDec.getTy().clone());
    }

}
