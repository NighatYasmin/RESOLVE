/**
 * ResolveCompiler.java
 * ---------------------------------
 * Copyright (c) 2014
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt.absynnew;

import edu.clemson.cs.r2jt.misc.*;
import edu.clemson.cs.r2jt.parsing.ResolveParser;
import edu.clemson.cs.r2jt.typeandpopulate.ModuleIdentifier;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ResolveCompiler {

    public static final String VERSION = "15.13.02a";

    public static final String FLAG_SECTION_NAME = "General";

    private static final String FLAG_DESC_LIB_DIR =
            "Specify the projects workspace directory.";

    private static final String FLAG_DESC_EXTENDED_HELP =
            "Displays all flags including development flags.";

    private static final String FLAG_DESC_HELP =
            "Displays this help information.";

    public static final Flag FLAG_HELP =
            new Flag(FLAG_SECTION_NAME, "help", FLAG_DESC_HELP);

    public static final Flag FLAG_EXTENDED_HELP =
            new Flag("General", "xhelp", FLAG_DESC_EXTENDED_HELP);

    public static final Flag FLAG_NO_DEFAULT_IMPORTS =
            new Flag("General", "nostdimport",
                    "Prevents the compiler from importing default facilities.");

    public static final Flag FLAG_LIB_DIR =
            new Flag(FLAG_SECTION_NAME, "lib", FLAG_DESC_LIB_DIR,
                    new String[] { "libDir" }, new String[] { System
                            .getProperty("user.dir") });

    public static final ResolveParserFactory PARSER_FACTORY =
            new ResolveParserFactory();

    public static final List<String> NATIVE_EXT =
            Collections.unmodifiableList(Arrays.asList("co", "fa", "mt", "en",
                    "rb"));

    public static final List<String> NON_NATIVE_EXT =
            Collections.unmodifiableList(Arrays.asList("java", "c", "h"));

    private final List<String> myTargetFiles = new ArrayList<String>();

    private final String myLibDirectory;

    public final FlagManager myFlagManager;

    //public final MathSymbolTableBuilder mySymbolTable =
    //        new MathSymbolTableBuilder();

    public final Map<ModuleIdentifier, File> myFiles =
            new HashMap<ModuleIdentifier, File>();
    public final Map<ModuleIdentifier, ModuleAST> myModules =
            new HashMap<ModuleIdentifier, ModuleAST>();

    public ResolveCompiler(String[] args) {
        setUpFlagDependencies();
        try {
            myFlagManager = new FlagManager(args);
        }
        catch (FlagDependencyException fde) {
            throw new IllegalArgumentException(fde.getMessage());
        }
        myLibDirectory = myFlagManager.getFlagArgument(FLAG_LIB_DIR, "libDir");
        handleArgs();
    }

    protected final void handleArgs() {
        if (myFlagManager.isFlagSet(FLAG_HELP)) {
            help();
            exit(0);
        }
        for (String s : myFlagManager.getRemainingArgs()) {
            if (s.charAt(0) != '-') { // filename
                if (!myTargetFiles.contains(s)) {
                    myTargetFiles.add(s);
                }
            }
            else {
                System.err.println("unrecognized flag " + s);
            }
        }
        //Todo: Sanity check lib directory (make sure it's valid, exists, etc)
    }

    public void compile() {
        try {
            for (String target : myTargetFiles) {
                File currentFile = new File(target);
                if (!currentFile.isAbsolute()) {
                    currentFile = new File(myLibDirectory, target);
                }

                ModuleAST targetModule = createModuleAST(currentFile);
                ModuleIdentifier id = new ModuleIdentifier(targetModule);

                myFiles.put(id, currentFile);
                myModules.put(id, targetModule);

                DefaultDirectedGraph<ModuleIdentifier, DefaultEdge> g =
                        new DefaultDirectedGraph<ModuleIdentifier, DefaultEdge>(
                                DefaultEdge.class);
                g.addVertex(id);
                findDependencies(g, targetModule);

                //Todo: We're going to have pipelines here in the future.
                for (ModuleIdentifier m : getCompileOrder(g)) {
                    System.out.println("Populating: " + m);
                }
            }
        }
        catch (Throwable e) {
            Throwable cause = e;
            while (cause != null && !(cause instanceof SrcErrorException)) {
                cause = cause.getCause();
            }
            if (cause == null) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
            else {
                SrcErrorException see = (SrcErrorException) cause;
                UnderliningErrorListener.INSTANCE.reportError(see
                        .getOffendingToken(), e.getMessage());
            }
        }
    }

    private void findDependencies(DefaultDirectedGraph g, ModuleAST root) {
        for (Token importRequest : root.getImportBlock().getImportsExcluding(
                ImportBlockAST.ImportType.EXTERNAL)) {

            ConnectivityInspector<ModuleIdentifier, DefaultEdge> conn =
                    new ConnectivityInspector<ModuleIdentifier, DefaultEdge>(g);

            File file = findResolveFile(importRequest.getText(), NATIVE_EXT);
            ModuleAST module = myModules.get(importRequest);

            if (module == null) {
                module = createModuleAST(file);

                myModules.put(id(module), module);
                myFiles.put(id(module), file);
            }

            if (root.getImportBlock().inCategory(
                    ImportBlockAST.ImportType.IMPLICIT, importRequest)) {
                if (!module.appropriateForImport()) {
                    throw new IllegalArgumentException("Invalid import: "
                            + module.getName() + ". Cannot import module of "
                            + "type: " + module.getClass() + ".");
                }
            }
            // If there exists a path from the dependent (module referenced) to
            // the root (source node), then adding the edge will cause a cycle.
            if (conn.pathExists(id(root), id(module))) {
                throw new CircularDependencyException("Circular dependency!");
            }
            Graphs.addEdgeWithVertices(g, id(root), id(module));
            findDependencies(g, module);
        }
        addFilesForExternalImports(root);
    }

    protected List<ModuleIdentifier> getCompileOrder(DefaultDirectedGraph g) {
        List<ModuleIdentifier> result = new ArrayList<ModuleIdentifier>();

        EdgeReversedGraph<ModuleIdentifier, DefaultEdge> reversed =
                new EdgeReversedGraph<ModuleIdentifier, DefaultEdge>(g);

        TopologicalOrderIterator<ModuleIdentifier, DefaultEdge> dependencies =
                new TopologicalOrderIterator<ModuleIdentifier, DefaultEdge>(
                        reversed);
        while (dependencies.hasNext()) {
            result.add(dependencies.next());
        }
        return result;
    }

    protected static ModuleIdentifier id(ModuleAST m) {
        return new ModuleIdentifier(m);
    }

    private void addFilesForExternalImports(ModuleAST m) {
        Set<Token> externals =
                m.getImportBlock().getImportsOfType(
                        ImportBlockAST.ImportType.EXTERNAL);

        for (Token externalImport : externals) {
            File file =
                    findResolveFile(externalImport.getText(), NON_NATIVE_EXT);
            myFiles.put(new ModuleIdentifier(externalImport), file);
        }
    }

    private File findResolveFile(String baseName, List<String> extensions) {
        try {
            FileLocator l = new FileLocator(baseName, extensions);
            Files.walkFileTree(new File(myLibDirectory).toPath(), l);
            return l.getFile();
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    private ModuleAST createModuleAST(File file) {
        try {
            ResolveParser parser =
                    PARSER_FACTORY.createParser(new ANTLRFileStream(file
                            .getAbsolutePath()));
            ParserRuleContext start = parser.module();
            return TreeUtil.createASTNodeFrom(start);
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void main(String[] args) {
        ResolveCompiler resolve = new ResolveCompiler(args);
        resolve.version();
        resolve.compile();
    }

    /**
     * <p>Prints compiler usage and flag related information to standard
     * output. Note: messages printed are actually formed in the
     * <code>description</code> field of that particular <code>Flag</code>.</p>
     */
    private void help() {
        System.out.println("RESOLVE Compiler Version " + VERSION);
        System.out.println(FlagDependencies.getListingString(myFlagManager
                .isFlagSet(FLAG_EXTENDED_HELP)));
    }

    /**
     * <p>This method sets up dependencies between compiler flags.  If you are
     * integrating your module into the compiler flag management system, this is
     * where to do it.</p>
     */
    private synchronized static void setUpFlagDependencies() {
        if (!FlagDependencies.isSealed()) {
            setUpFlags();
            //Your module here!
            FlagDependencies.seal();
        }
    }

    private static void setUpFlags() {
        FlagDependencies.addImplies(FLAG_EXTENDED_HELP, FLAG_HELP);
    }

    public void version() {
        System.out.println("RESOLVE Compiler Version " + VERSION);
    }

    public void exit(int e) {
        System.exit(e);
    }
}