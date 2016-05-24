/**
 * RecordTy.java
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
package edu.clemson.cs.rsrg.absyn.rawtypes;

import edu.clemson.cs.rsrg.absyn.Ty;
import edu.clemson.cs.rsrg.absyn.variables.VarDec;
import edu.clemson.cs.rsrg.parsing.data.Location;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>This is the class for all the raw record type objects
 * that the compiler builds using the ANTLR4 AST nodes.</p>
 *
 * @version 2.0
 */
public class RecordTy extends Ty {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /** <p>The raw type's fields</p> */
    private final List<VarDec> myInnerFields;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>This constructs a raw record type.</p>
     *
     * @param l A {@link Location} representation object.
     * @param fields A list of {@link VarDec} representing the fields
     *               inside this raw record type.
     */
    public RecordTy(Location l, List<VarDec> fields) {
        super(l);
        myInnerFields = fields;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public final String asString(int indentSize, int innerIndentInc) {
        StringBuffer sb = new StringBuffer();
        printSpace(indentSize, sb);

        for (VarDec v : myInnerFields) {
            sb.append(v.asString(indentSize + innerIndentInc, innerIndentInc));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RecordTy recordTy = (RecordTy) o;

        return myInnerFields.equals(recordTy.myInnerFields);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return myInnerFields.hashCode();
    }

    /**
     * <p>This method returns list containing all the field
     * elements.</p>
     *
     * @return A list of {@link VarDec} representation objects.
     */
    public final List<VarDec> getFields() {
        return myInnerFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        StringBuffer sb = new StringBuffer();

        for (VarDec v : myInnerFields) {
            sb.append(v.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    // ===========================================================
    // Protected Methods
    // ===========================================================

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Ty copy() {
        return new RecordTy(new Location(myLoc), getFields());
    }

}