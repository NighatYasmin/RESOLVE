/*
 * AbstractPipeline.java
 * ---------------------------------
 * Copyright (c) 2017
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.rsrg.init.pipeline;

import edu.clemson.cs.rsrg.init.CompileEnvironment;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.MathSymbolTableBuilder;
import edu.clemson.cs.rsrg.typeandpopulate.utilities.ModuleIdentifier;
import java.io.*;

/**
 * <p>This is the abstract base class for all pipeline objects
 * that are used to perform some sort of action.</p>
 *
 * @version 2.0
 */
public abstract class AbstractPipeline {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /**
     * <p>The current job's compilation environment
     * that stores all necessary objects and flags.</p>
     */
    protected final CompileEnvironment myCompileEnvironment;

    /** <p>The symbol table for the compiler.</p> */
    protected final MathSymbolTableBuilder mySymbolTable;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>An helper constructor that allow us to store the
     * {@link CompileEnvironment} and {@link MathSymbolTableBuilder}
     * from a class that inherits from {@code AbstractPipeline}.</p>
     *
     * @param ce The current compilation environment.
     * @param symbolTable The symbol table.
     */
    protected AbstractPipeline(CompileEnvironment ce,
            MathSymbolTableBuilder symbolTable) {
        myCompileEnvironment = ce;
        mySymbolTable = symbolTable;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * <p>This method must be implemented by all inherited classes
     * to specify how to process the module pointed by the
     * {@code currentTarget} module identifier.</p>
     *
     * @param currentTarget The module identifier
     */
    public abstract void process(ModuleIdentifier currentTarget);

    // ===========================================================
    // Protected Methods
    // ===========================================================

    /**
     * <p>Writes the content to the specified filename.</p>
     *
     * @param outputFileName Output filename.
     * @param outputString Contents to be written in file.
     */
    protected final void writeToFile(String outputFileName, String outputString) {
        try {
            // Write the contents to file
            Writer writer =
                    new BufferedWriter(new FileWriter(new File(outputFileName),
                            false));
            writer.write(outputString);
            writer.close();
        }
        catch (IOException ioe) {
            myCompileEnvironment.getStatusHandler().error(null,
                    "Error while writing to file: " + outputFileName);
        }
    }

}