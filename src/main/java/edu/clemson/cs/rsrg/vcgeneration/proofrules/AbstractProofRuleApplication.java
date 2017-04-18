/*
 * AbstractProofRuleApplication.java
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
package edu.clemson.cs.rsrg.vcgeneration.proofrules;

import edu.clemson.cs.rsrg.parsing.data.Location;
import edu.clemson.cs.rsrg.vcgeneration.vcs.AssertiveCodeBlock;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * <p>This is the abstract base class for all the {@code Proof Rules}.</p>
 *
 * @author Yu-Shan Sun
 * @version 1.0
 */
public abstract class AbstractProofRuleApplication
        implements
            ProofRuleApplication {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /**
     * <p>The current {@link AssertiveCodeBlock} that the {@code Proof Rules}
     * will operate on.</p>
     */
    protected final AssertiveCodeBlock myCurrentAssertiveCodeBlock;

    /**
     * <p>A map that stores all the details associated with
     * a particular {@link Location}.</p>
     */
    protected final Map<Location, String> myLocationDetails;

    /**
     * <p>A double ended queue that contains all the assertive code blocks
     * that was either passed in or generated by the {@code Proof Rule}.</p>
     */
    protected final Deque<AssertiveCodeBlock> myResultingAssertiveCodeBlocks;

    /** <p>String template groups for storing all the VC generation details.</p> */
    protected final STGroup mySTGroup;

    /** <p>String template model for the {@link AssertiveCodeBlock}.</p> */
    protected final ST myBlockModel;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>An helper constructor that creates a double ended queue to
     * store the {@link AssertiveCodeBlock} that was passed in as well
     * as any generated from a class that inherits from
     * {@code AbstractProofRuleApplication}.</p>
     *
     * @param block The assertive code block that the subclasses are
     *              applying the rule to.
     * @param stGroup The string template group we will be using.
     * @param blockModel The model associated with {@code block}.
     */
    protected AbstractProofRuleApplication(AssertiveCodeBlock block, STGroup stGroup, ST blockModel) {
        myResultingAssertiveCodeBlocks = new LinkedList<>();
        myCurrentAssertiveCodeBlock = block;
        myLocationDetails = new HashMap<>();
        mySTGroup = stGroup;
        myBlockModel = blockModel;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * <p>This method returns the a {@link Deque} of {@link AssertiveCodeBlock AssertiveCodeBlock(s)}
     * that resulted from applying the {@code Proof Rule}.</p>
     *
     * @return A {@link Deque} containing all the {@link AssertiveCodeBlock AssertiveCodeBlock(s)}.
     */
    @Override
    public final Deque<AssertiveCodeBlock> getAssertiveCodeBlocks() {
        Deque<AssertiveCodeBlock> blocks = new LinkedList<>(myResultingAssertiveCodeBlocks);
        blocks.addFirst(myCurrentAssertiveCodeBlock);

        return blocks;
    }

    /**
     * <p>This method returns the string template associated with the incoming
     * {@link AssertiveCodeBlock}.</p>
     *
     * @return A {@link ST} object.
     */
    @Override
    public final ST getBlockModel() {
        return myBlockModel;
    }

    /**
     * <p>This method returns a map containing details about
     * a {@link Location} object that was generated during the proof
     * application process.</p>
     *
     * @return A map from {@link Location} to location detail strings.
     */
    @Override
    public final Map<Location, String> getNewLocationString() {
        return myLocationDetails;
    }

}