/*
 * ParameterGenericApplyingVisitor.java
 * ---------------------------------
 * Copyright (c) 2020
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt.typeandpopulate;

import java.util.Map;

public class ParameterGenericApplyingVisitor
        extends
            SymmetricBoundVariableVisitor {

    private Map<String, MTType> myBindings;

    private boolean myErrorFlag = false;

    public ParameterGenericApplyingVisitor(Map<String, MTType> bindings) {
        myBindings = bindings;
    }

    public boolean encounteredError() {
        return myErrorFlag;
    }

    @Override
    public boolean mismatch(MTType t1, MTType t2) {
        if (t1 instanceof MTGeneric) {
            myBindings.put(((MTGeneric) t1).getName(), t2);
        }
        else {
            myErrorFlag = true;
        }

        return true;
    }
}
