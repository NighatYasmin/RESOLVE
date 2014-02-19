package edu.clemson.cs.r2jt.translation;

import edu.clemson.cs.r2jt.ResolveCompiler;
import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.archiving.Archiver;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.data.ModuleID;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.data.Symbol;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.treewalk.TreeWalkerStackVisitor;
import edu.clemson.cs.r2jt.typeandpopulate.*;
import edu.clemson.cs.r2jt.typeandpopulate.entry.FacilityEntry;
import edu.clemson.cs.r2jt.typeandpopulate.entry.OperationEntry;
import edu.clemson.cs.r2jt.typeandpopulate.entry.ProgramParameterEntry;
import edu.clemson.cs.r2jt.typeandpopulate.entry.ProgramTypeEntry;
import edu.clemson.cs.r2jt.typeandpopulate.programtypes.*;
import edu.clemson.cs.r2jt.typeandpopulate.query.OperationQuery;
import edu.clemson.cs.r2jt.typeandpopulate.query.UnqualifiedNameQuery;
import edu.clemson.cs.r2jt.utilities.SourceErrorException;
import org.stringtemplate.v4.*;

import java.io.File;
import java.util.*;

public abstract class AbstractTranslator extends TreeWalkerStackVisitor {

    protected static final boolean PRINT_DEBUG = true;

    protected final CompileEnvironment myInstanceEnvironment;
    protected ModuleScope myModuleScope = null;

    /**
     * <p>This gives us access to additional <code>ModuleScope</code>s. This
     * comes in handy for <code>ConceptBodyModuleDec</code> translation.</p>
     */
    protected final MathSymbolTableBuilder myBuilder;

    /**
     * <p>A pointer to a <code>SymbolTableEntry</code> that corresponds to
     * the <code>FacilityDec</code> currently being walked.  If one isn't
     * being walked, this should be <code>null</code>.</p>
     */
    protected FacilityEntry myCurrentFacilityEntry = null;

    /**
     * <p>The <code>STGroup</code> that houses all templates used by a
     * given target language.</p>
     */
    protected STGroup myGroup;

    /**
     * <p>The top of this <code>Stack</code> maintains a reference to the
     * template actively being built or added to, and the bottom refers to
     * <code>module</code> - the outermost enclosing template for all target
     * languages.</p>
     *
     * <p>Proper usage should generally involve: Pushing in <tt>pre</tt>,
     * modifying top arbitrarily with <tt>pre</tt>'s children, popping in the
     * corresponding <tt>post</tt>, then adding the popped template to the
     * appropriate enclosing template (i.e. the new/current top).</p>
     */
    protected Stack<ST> myActiveTemplates = new Stack<ST>();

    /**
     * <p>This <code>Set</code> keeps track of any additional
     * <code>includes</code>/imports needed to run the translated file. We call
     * it <code>Dynamic</code> since only certain nodes add to this collection
     * (i.e. <code>FacilityDec</code> nodes).</p>
     */
    protected Set<String> myDynamicImports = new HashSet<String>();

    public AbstractTranslator(CompileEnvironment env, ScopeRepository repo) {
        myInstanceEnvironment = env;
        myBuilder = (MathSymbolTableBuilder) repo;
    }

    //-------------------------------------------------------------------
    //   Visitor methods
    //-------------------------------------------------------------------

    @Override
    public void preModuleDec(ModuleDec node) {

        try {
            myModuleScope =
                    myBuilder.getModuleScope(new ModuleIdentifier(node));

            ST myEnclosingTemplate = myGroup.getInstanceOf("module");

            myEnclosingTemplate.add("includes", myGroup
                    .getInstanceOf("include").add("directories", "RESOLVE"));

            myActiveTemplates.push(myEnclosingTemplate);

            AbstractTranslator.emitDebug("----------------------------------\n"
                    + "Translate: " + node.getName().getName()
                    + "\n----------------------------------");
        }
        catch (NoSuchSymbolException nsse) {
            noSuchModule(node.getName());
            throw new RuntimeException();
        }
    }

    @Override
    public void preUsesItem(UsesItem node) {

        try {
            FacilityEntry e =
                    myModuleScope.queryForOne(
                            new UnqualifiedNameQuery(node.getName().getName()))
                            .toFacilityEntry(null);

            String spec =
                    e.getFacility().getSpecification().getModuleIdentifier()
                            .toString();

            List<String> pathPieces = getPathList(getFile(null, spec));

            myActiveTemplates.firstElement().add(
                    "includes",
                    myGroup.getInstanceOf("include").add("directories",
                            pathPieces));
        }
        catch (NoSuchSymbolException nsse) {
            // TODO: Hack Hack. Figure out a way to do this properly.
            // things like static_array_template show up here.
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException(dse);
        }
    }

    @Override
    public void preFacilityOperationDec(FacilityOperationDec node) {
        ST operation =
                createOperationLikeTemplate((node.getReturnTy() != null) ? node
                        .getReturnTy().getProgramTypeValue() : null, node
                        .getName().getName(), true);

        myActiveTemplates.push(operation);

    }

    @Override
    public void preOperationDec(OperationDec node) {
        ST operation =
                createOperationLikeTemplate((node.getReturnTy() != null) ? node
                        .getReturnTy().getProgramTypeValue() : null, node
                        .getName().getName(), false);

        myActiveTemplates.push(operation);
    }

    @Override
    public void preProcedureDec(ProcedureDec node) {
        ST operation =
                createOperationLikeTemplate((node.getReturnTy() != null) ? node
                        .getReturnTy().getProgramTypeValue() : null, node
                        .getName().getName(), true);

        myActiveTemplates.push(operation);
    }

    @Override
    public void postOperationDec(OperationDec node) {
        ST operation = myActiveTemplates.pop();
        myActiveTemplates.peek().add("functions", operation);
    }

    @Override
    public void postProcedureDec(ProcedureDec node) {
        ST operation = myActiveTemplates.pop();
        myActiveTemplates.peek().add("functions", operation);
    }

    @Override
    public void postFacilityOperationDec(FacilityOperationDec node) {
        ST operation = myActiveTemplates.pop();
        myActiveTemplates.peek().add("functions", operation);
    }

    @Override
    public void preVarDec(VarDec node) {
        addVariableTemplate(node.getTy().getProgramTypeValue(), node.getName()
                .getName());
    }

    @Override
    public void preParameterVarDec(ParameterVarDec node) {

        PTType type = node.getTy().getProgramTypeValue();

        ST parameter =
                myGroup.getInstanceOf("parameter").add("type",
                        getParameterTypeTemplate(type)).add("name",
                        node.getName().getName());

        myActiveTemplates.peek().add("parameters", parameter);
    }

    @Override
    public void postModuleDec(ModuleDec node) {

        myActiveTemplates.firstElement().add("includes", myDynamicImports);

        AbstractTranslator.emitDebug("----------------------------------\n"
                + "End: " + node.getName().getName()
                + "\n----------------------------------");
    }

    //-------------------------------------------------------------------
    //   Helper methods
    //-------------------------------------------------------------------

    protected abstract ST getVariableTypeTemplate(PTType type);

    protected abstract ST getOperationTypeTemplate(PTType type);

    protected abstract ST getParameterTypeTemplate(PTType type);

    protected abstract String getFunctionModifier();

    /**
     * <p>Creates, fills-in, and inserts a formed <code>parameter</code>
     * template into the active template.</p>
     *
     * @param type A <code>PTType</code>.
     * @param name A string containing the name of the parameter.
     */
    protected void addParameterTemplate(PTType type, String name) {
        ST parameter =
                myGroup.getInstanceOf("parameter").add("type",
                        getVariableTypeTemplate(type)).add("name", name);

        myActiveTemplates.peek().add("params", parameter);
    }

    /**
     * Places both generic variables and "regular" variables into..
     * @param type
     * @param name
     */
    protected void addVariableTemplate(PTType type, String name) {
        ST init, variable;

        if (type instanceof PTGeneric) {
            init =
                    myGroup.getInstanceOf("rtype_init").add("typeName",
                            getTypeName(type));
        }
        else {
            init =
                    myGroup.getInstanceOf("var_init").add("type",
                            getVariableTypeTemplate(type)).add("facility",
                            getDefiningFacilityEntry(type).getName());
        }
        variable =
                myGroup.getInstanceOf("var_decl").add("name", name).add("type",
                        getVariableTypeTemplate(type)).add("init", init);

        myActiveTemplates.peek().add("variables", variable);
    }

    /**
     * <p></p>
     * @param returnType
     * @param name
     *
     * @param hasBody
     */
    protected ST createOperationLikeTemplate(PTType returnType, String name,
            boolean hasBody) {

        String attributeName = (hasBody) ? "function_def" : "function_decl";

        ST operationLikeThingy =
                myGroup.getInstanceOf(attributeName).add("name", name).add(
                        "modifier", getFunctionModifier());

        operationLikeThingy.add("type",
                (returnType != null) ? getOperationTypeTemplate(returnType)
                        : "void");
        return operationLikeThingy;
    }

    /**
     * <p>Returns a <code>List</code> of <code>ProgramParameterEntry</code>s
     * representing the formal params of module <code>moduleName</code>.</p>
     *
     * @param moduleName A <code>PosSymbol</code> containing the name of the
     *                   module whose parameters
     * @return The formal parameters.
     */
    protected List<ProgramParameterEntry> getModuleFormalParameters(
            PosSymbol moduleName) {
        try {
            ModuleDec spec =
                    myBuilder.getModuleScope(
                            new ModuleIdentifier(moduleName.getName()))
                            .getDefiningElement();

            return myBuilder.getScope(spec).getFormalParameterEntries();
        }
        catch (NoSuchSymbolException nsse) {
            noSuchModule(moduleName);
            throw new RuntimeException();
        }
    }

    /**
     * <p>Retrieves the <code>name</code> of a <code>PTType</code>. The
     * <code>PTType</code> baseclass by itself doesn't provide this
     * functionality. This method goes through the trouble of casting to the
     * correct subclass so we can use the <code>getName</code> method.</p>
     *
     * @param type A <code>PTType</code>.
     * @return <code>type</code>'s actual name rather than the more easily
     *         accessible <code>toString</code> representation.
     */
    protected String getTypeName(PTType type) {

        String result;

        if (type == null) {
            return null;
        }
        if (type instanceof PTElement) {
            // Not sure under what conditions this would appear in output.
            result = "PTELEMENT";
        }
        else if (type instanceof PTGeneric) {
            result = ((PTGeneric) type).getName();
        }
        else if (type instanceof PTRepresentation) {
            result = ((PTRepresentation) type).getFamily().getName();
        }
        else if (type instanceof PTFamily) {
            result = ((PTFamily) type).getName();
        }
        else {
            throw new UnsupportedOperationException("Translation has "
                    + "encountered an unrecognized PTType: " + type.toString()
                    + ". Backing out.");
        }
        return result;
    }

    /**
     * <p></p>
     * @param type
     * @return
     */
    protected FacilityEntry getDefiningFacilityEntry(PTType type) {

        FacilityEntry result = null;
        String searchString = getTypeName(type);

        try {
            ProgramTypeEntry te =
                    myModuleScope.queryForOne(
                            new UnqualifiedNameQuery(type.toString()))
                            .toProgramTypeEntry(null);

            List<FacilityEntry> facilities =
                    myModuleScope.query(new EntryTypeQuery(FacilityEntry.class,
                            MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                            MathSymbolTable.FacilityStrategy.FACILITY_IGNORE));

            for (FacilityEntry facility : facilities) {
                if (te.getSourceModuleIdentifier().equals(
                        facility.getFacility().getSpecification()
                                .getModuleIdentifier())) {

                    result = facility;
                }
            }
        }
        catch (NoSuchSymbolException nsse) {
            throw new RuntimeException("Translation unable to find a "
                    + "FacilityEntry locally or otherwise that defines type: "
                    + type.toString());
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException(dse); // shouldn't fire.
        }
        return result;
    }

    /**
     *
     * @param qualifier
     * @param name
     * @param args
     * @return
     */
    protected String getCallQualifier(PosSymbol qualifier, PosSymbol name,
            List<ProgramExp> args) {

        String result = null;
        List<PTType> argTypes = new LinkedList<PTType>();
        List<FacilityEntry> matches = new LinkedList<FacilityEntry>();

        if (qualifier != null) {
            return qualifier.getName();
        }
        try {

            for (ProgramExp arg : args) {
                argTypes.add(arg.getProgramType());
            }

            OperationEntry oe =
                    myModuleScope.queryForOne(
                            new OperationQuery(null, name, argTypes))
                            .toOperationEntry(null);

            // Grab FacilityEntries in scope whose specification matches
            // oe's SourceModuleIdentifier.
            List<FacilityEntry> facilities =
                    myModuleScope.query(new EntryTypeQuery(FacilityEntry.class,
                            MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                            MathSymbolTable.FacilityStrategy.FACILITY_IGNORE));

            for (FacilityEntry f : facilities) {
                if (oe.getSourceModuleIdentifier().equals(
                        f.getFacility().getSpecification()
                                .getModuleIdentifier())) {
                    matches.add(f);
                }
            }

            // There should only be two cases:
            // 1. Size == 1 => a unique facility is instantiated
            //          in scope whose specification matches oe's. So the
            //          appropriate qualifier is that facility's name.
            if (matches.size() == 1) {
                result = matches.get(0).getName();
            }
            // 2. Size > 1 => multiple facilities instantiated use
            //          oe's SourceModuleIdentifier as a specification.
            //          Which facility's name to use as a qualifier is
            //          ambiguous -- so off to argument examination we go.
            if (matches.size() > 1) {
                result = "TEMP";
                //    result = findQualifyingArgument(oe, args);
            }
            // 3. Size == 0 => the operation owning the call is
            //          defined locally. So no need to qualify.
        }
        catch (NoSuchSymbolException nsse) {
            // FOR NOW.
            return "TEMP_QUALIFIER";
            // noSuchSymbol(qualifier, name);
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException(dse);
        }
        return result;
    }

    //-------------------------------------------------------------------
    //   Error handling
    //-------------------------------------------------------------------

    public void noSuchModule(PosSymbol module) {
        throw new SourceErrorException(
                "Module does not exist or is not in scope.", module);
    }

    public void noSuchSymbol(PosSymbol qualifier, PosSymbol symbol) {
        noSuchSymbol(qualifier, symbol.getName(), symbol.getLocation());
    }

    public void noSuchSymbol(PosSymbol qualifier, String symbolName, Location l) {

        String message;

        if (qualifier == null) {
            message = "Translation was unable to find symbol: " + symbolName;
        }
        else {
            message =
                    "No such symbol in module: " + qualifier.getName() + "."
                            + symbolName;
        }

        throw new SourceErrorException(message, l);
    }

    //-------------------------------------------------------------------
    //   Utility, output, and flag-related methods
    //-------------------------------------------------------------------

    /**
     * <p>Returns a <code>File</code> given either a <code>ModuleDec</code>
     * <em>or</em> a string containing the name of a
     * <code>ConceptBodyModuleDec</code>.</p>
     *
     * @param module A <code>ModuleDec</code>.
     * @param name The name of an existing <code>ConceptBodyModuleDec</code>.
     *
     * @return A <code>File</code>.
     */
    protected File getFile(ModuleDec module, String name) {

        File result;

        if (module == null && name == null) {
            throw new IllegalArgumentException("Translation requires at least"
                    + " one non-null argument to retrieve the correct file"
                    + " from the compile environment.");
        }

        if (module != null) {
            ModuleID id = ModuleID.createID(module);
            result = myInstanceEnvironment.getFile(id);
        }
        else {
            PosSymbol conceptName = new PosSymbol(null, Symbol.symbol(name));
            ModuleID id = ModuleID.createConceptID(conceptName);
            result = myInstanceEnvironment.getFile(id);
        }
        return result;
    }

    /**
     * <p>Given a <code>File</code> object, this hacky little method returns a
     * list whose elements consist of the directory names of the input file's
     * absolute path.</p>
     *
     * <p>For example, given file (with directory path) :
     *      <pre>Resolve-Workspace/RESOLVE/Main/X.fa</pre>
     *
     * <p>this method will return :
     *      <pre>[RESOLVE, Main, X]</pre>
     *
     * <p>Note that any directories prior to the root "RESOLVE" directory are
     * stripped, along with <code>source</code>'s file extension.</p>
     *
     * @param source The input <code>File</code>.
     * @return A list whose elements correspond to the path directories
     *         of <code>source</code>.
     */
    protected List<String> getPathList(File source) {

        String currentToken, path;
        boolean rootDirectoryFound = false;

        path =
                (source.exists()) ? source.getAbsolutePath() : source
                        .getParentFile().getAbsolutePath();

        List<String> result = new LinkedList<String>();
        StringTokenizer stTok = new StringTokenizer(path, File.separator);

        while (stTok.hasMoreTokens()) {
            currentToken = stTok.nextToken();

            if (currentToken.equalsIgnoreCase("RESOLVE") || rootDirectoryFound) {
                rootDirectoryFound = true;

                if (currentToken.contains(".")) {
                    currentToken =
                            currentToken
                                    .substring(0, currentToken.indexOf('.'));
                }
                result.add(currentToken);
            }
        }
        return result;
    }

    public static void emitDebug(String msg) {
        if (PRINT_DEBUG) {
            System.out.println(msg);
        }
    }

    public void outputCode(File outputFile) {
        if (!myInstanceEnvironment.flags.isFlagSet(ResolveCompiler.FLAG_WEB)
                || myInstanceEnvironment.flags.isFlagSet(Archiver.FLAG_ARCHIVE)) {
            //    outputAsFile(outputFile.getAbsolutePath(),
            //            myOutermostEnclosingTemplate.render());
            System.out.println(Formatter.formatCode(myActiveTemplates.peek()
                    .render()));
        }
    }
}