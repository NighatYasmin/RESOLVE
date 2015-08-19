/**
 * NullMathTypeException.java
 * ---------------------------------
 * Copyright (c) 2015
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.rsrg.errorhandling.exception;

/**
 * <p>An {@code NullMathTypeException} indicates we encountered an
 * null {link MTType} and is trying to use it in some way.</p>
 *
 * @author Yu-Shan Sun
 * @version 1.0
 */
public class NullMathTypeException extends CompilerException {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /** <p>Serial version for Serializable objects</p> */
    private static final long serialVersionUID = 5L;

    // ==========================================================
    // Constructors
    // ==========================================================

    /**
     * <p>This constructor takes in a message
     * that caused a import exception to be thrown.</p>
     *
     * @param message Message to be displayed when the exception is thrown.
     */
    public NullMathTypeException(String message) {
        super(message, (Throwable) null);
    }

}