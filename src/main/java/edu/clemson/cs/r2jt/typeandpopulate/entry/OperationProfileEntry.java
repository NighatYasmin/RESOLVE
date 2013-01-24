package edu.clemson.cs.r2jt.typeandpopulate.entry;

import edu.clemson.cs.r2jt.absyn.ResolveConceptualElement;
import edu.clemson.cs.r2jt.proving.immutableadts.ArrayBackedImmutableList;
import edu.clemson.cs.r2jt.proving.immutableadts.ImmutableList;
import edu.clemson.cs.r2jt.typeandpopulate.ModuleIdentifier;
import edu.clemson.cs.r2jt.typeandpopulate.programtypes.PTType;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ys
 */
public class OperationProfileEntry extends SymbolTableEntry {

    private final PTType myReturnType;
    private final ImmutableList<ProgramParameterEntry> myParameters;

    public OperationProfileEntry(String name,
            ResolveConceptualElement definingElement,
            ModuleIdentifier sourceModule, PTType returnType,
            List<ProgramParameterEntry> parameters) {

        this(name, definingElement, sourceModule, returnType,
                new ArrayBackedImmutableList<ProgramParameterEntry>(parameters));
    }

    public OperationProfileEntry(String name,
            ResolveConceptualElement definingElement,
            ModuleIdentifier sourceModule, PTType returnType,
            ImmutableList<ProgramParameterEntry> parameters) {

        super(name, definingElement, sourceModule);

        myParameters = parameters;
        myReturnType = returnType;
    }

    public ImmutableList<ProgramParameterEntry> getParameters() {
        return myParameters;
    }

    public PTType getReturnType() {
        return myReturnType;
    }

    @Override
    public String getEntryTypeDescription() {
        return "the profile of an operation";
    }

    @Override
    public SymbolTableEntry instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
