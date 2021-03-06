/*
 * ProverListener.java
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
package edu.clemson.cs.r2jt.rewriteprover;

import edu.clemson.cs.r2jt.rewriteprover.model.PerVCProverModel;

public interface ProverListener {

    public void progressUpdate(double progess);

    public void vcResult(boolean proved, PerVCProverModel finalModel,
            Metrics m);

    // readCancel should return true to stop the prover
    // public boolean readCancel();
}
