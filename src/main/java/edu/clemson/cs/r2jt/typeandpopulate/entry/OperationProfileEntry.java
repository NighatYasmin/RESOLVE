package edu.clemson.cs.r2jt.typeandpopulate.entry;

import edu.clemson.cs.r2jt.absyn.ResolveConceptualElement;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.typeandpopulate.ModuleIdentifier;
import edu.clemson.cs.r2jt.typeandpopulate.programtypes.PTType;
import java.util.Map;

/**
 *
 * @author ys
 */
public class OperationProfileEntry extends SymbolTableEntry {

    private final OperationEntry myCorrespondingOperation;

    public OperationProfileEntry(String name,
            ResolveConceptualElement definingElement,
            ModuleIdentifier sourceModule, OperationEntry correspondingOperation) {

        super(name, definingElement, sourceModule);

        myCorrespondingOperation = correspondingOperation;
    }

    public OperationEntry getCorrespondingOperation() {
        return myCorrespondingOperation;
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

    @Override
    public OperationProfileEntry toOperationProfileEntry(Location l) {
        return this;
    }
}
