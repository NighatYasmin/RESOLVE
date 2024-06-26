/*
 * VarDec.java
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
package edu.clemson.rsrg.absyn.declarations.variabledecl;

import edu.clemson.rsrg.absyn.rawtypes.Ty;
import edu.clemson.rsrg.parsing.data.PosSymbol;

/**
 * <p>
 * This is the class for all the regular programming variable declaration objects that the compiler builds using the
 * ANTLR4 AST nodes.
 * </p>
 *
 * @version 2.0
 */
public class VarDec extends AbstractVarDec {

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>
     * This constructs a regular program variable.
     * </p>
     *
     * @param name
     *            A {@link PosSymbol} representing the variable's name.
     * @param ty
     *            A {@link Ty} representing the variable's raw type.
     */
    public VarDec(PosSymbol name, Ty ty) {
        super(name.getLocation(), name, ty);
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public final String asString(int indentSize, int innerIndentInc) {
        return super.asStringVarDec(indentSize, innerIndentInc);
    }

    // ===========================================================
    // Protected Methods
    // ===========================================================

    /**
     * {@inheritDoc}
     */
    @Override
    protected final VarDec copy() {
        return new VarDec(myName.clone(), myTy.clone());
    }

}
