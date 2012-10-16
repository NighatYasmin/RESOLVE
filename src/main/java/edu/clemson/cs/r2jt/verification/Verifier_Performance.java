/*
 * This software is released under the new BSD 2006 license.
 * 
 * Note the new BSD license is equivalent to the MIT License, except for the
 * no-endorsement final clause.
 * 
 * Copyright (c) 2007, Clemson University
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the Clemson University nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This sofware has been developed by past and present members of the
 * Reusable Sofware Research Group (RSRG) in the School of Computing at
 * Clemson University. Contributors to the initial version are:
 * 
 * Steven Atkinson
 * Greg Kulczycki
 * Kunal Chopra
 * John Hunt
 * Heather Keown
 * Ben Markle
 * Kim Roche
 * Murali Sitaraman
 * Nighat Yasmin
 */
/*
 * Verifier.java
 * 
 * The Resolve Software Composition Workbench Project
 * 
 * Copyright (c) 2005-2010
 * Resolve Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.verification;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList; // import java.util.ListIterator;
import java.util.StringTokenizer;

import edu.clemson.cs.r2jt.ResolveCompiler;
import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.scope.*;
import edu.clemson.cs.r2jt.entry.*;
import edu.clemson.cs.r2jt.type.*;
import edu.clemson.cs.r2jt.utilities.Flag;
import edu.clemson.cs.r2jt.utilities.FlagDependencies;
import edu.clemson.cs.r2jt.location.*;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.collections.Iterator;
import edu.clemson.cs.r2jt.data.*; // import edu.clemson.cs.r2jt.errors.*;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.analysis.TypeResolutionException;
import edu.clemson.cs.r2jt.analysis.ProgramExpTypeResolver;

public class Verifier_Performance {

    AssertiveCode assertion1;
    FacilityOperationDec dec1;
    OldSymbolTable table1;
    ProcedureDec dec2;
    EqualsExp recurs;

    /**
     * Construct a Verifier_Performance.
     */
    public Verifier_Performance(AssertiveCode assertion,
            FacilityOperationDec dec, OldSymbolTable table) {
        this.assertion1 = assertion;
        this.dec1 = dec;
        this.table1 = table;
    }

    //forms assertion from Procedure Declaration
    public AssertiveCode VerifierPerfProcedureDec() {

        /* **********************Murali: Cum_Dur test************************* */

        VarExp cumdur = new VarExp();
        ConcType cumDUR = getCumDur();
        assertion1.addFreeVar(cumDUR);
        cumdur.setName(cumDUR.getName());
        setLocation(cumdur, dec1.getDecreasing().getLocation());
        //    	EqualsExp recurs = new EqualsExp(dec.getDecreasing().getLocation(),
        recurs =
                new EqualsExp(dec1.getDecreasing().getLocation(), cumdur,
                        EqualsExp.EQUAL, dec1.getDecreasing());
        Location recurLocation = dec1.getDecreasing().getLocation();
        recurLocation.setDetails("Progress Metric for Recursive Procedure");
        setLocation(recurs, recurLocation);
        recurs.setType(table1.getTypeHolder().getTypeB());
        assertion1.addAssume(recurs);
        return assertion1;
    }

    private ConcType getCumDur() {
        ModuleScope curr = table1.getModuleScope();
        TypeHolder typeHold = curr.getTypeHolder();
        typeHold.searchForBuiltInTypes();
        Type type = typeHold.getTypeN();

        if (type == null) {
            PosSymbol modName = createPosSymbol("Natural_Number_Theory");
            ModuleID mid = ModuleID.createTheoryID(modName);
            PosSymbol name = createPosSymbol("N");
            IndirectType ty = new IndirectType(null, name, null);
            type = new NameType(mid, name, ty);
        }

        return convertToConcType(createPosSymbol("Cum_Dur"), type);
    }

    private void setLocation(Exp exp, Location loc) {
        if (loc == null)
            return;
        if (exp instanceof InfixExp) {
            ((InfixExp) exp).setAllLocations(loc);
        }
        else {
            exp.setLocation(loc);
        }
    }

    private PosSymbol createPosSymbol(String name) {
        PosSymbol posSym = new PosSymbol();
        posSym.setSymbol(Symbol.symbol(name));
        return posSym;
    }

    private ConcType convertToConcType(PosSymbol name, Type type) {
        ModuleScope curr = table1.getModuleScope();
        /* The type forms a ConcType */
        if (type instanceof ArrayType) {
            ConcType tmp =
                    new ConcType(((ArrayType) type).getModuleID(), name,
                            (ArrayType) type);
            return tmp;
        }
        else if (type instanceof ConcType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, ((ConcType) type)
                            .getType());
            return tmp;
        }
        else if (type instanceof ConstructedType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name,
                            (ConstructedType) type);
            return tmp;
        }
        else if (type instanceof FieldItem) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (FieldItem) type);
            return tmp;
        }
        else if (type instanceof FormalType) {
            ConcType tmp =
                    new ConcType(((FormalType) type).getModuleID(), name,
                            (FormalType) type);
            return tmp;
        }
        else if (type instanceof FunctionType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (FunctionType) type);
            return tmp;
        }
        else if (type instanceof IndirectType) {

            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (IndirectType) type);
            return tmp;
        }
        else if (type instanceof NameType) {
            ConcType tmp =
                    new ConcType(((NameType) type).getModuleID(), name, type);
            return tmp;
        }
        else if (type instanceof PrimitiveType) {
            ConcType tmp =
                    new ConcType(((PrimitiveType) type).getModuleID(), name,
                            (PrimitiveType) type);
            return tmp;
        }
        else if (type instanceof RecordType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (RecordType) type);
            return tmp;
        }
        else if (type instanceof VoidType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (VoidType) type);
            return tmp;
        }
        else if (type instanceof TupleType) {
            ConcType tmp =
                    new ConcType(curr.getModuleID(), name, (TupleType) type);
            return tmp;
        }

        return null;
    }
}
