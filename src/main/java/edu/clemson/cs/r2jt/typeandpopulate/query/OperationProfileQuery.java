package edu.clemson.cs.r2jt.typeandpopulate.query;

import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.typeandpopulate.MathSymbolTable;
import edu.clemson.cs.r2jt.typeandpopulate.PossiblyQualifiedPath;
import edu.clemson.cs.r2jt.typeandpopulate.entry.OperationEntry;
import edu.clemson.cs.r2jt.typeandpopulate.programtypes.PTType;
import edu.clemson.cs.r2jt.typeandpopulate.searchers.OperationSearcher;
import java.util.List;

/**
 * <p>An <code>OperationProfileQuery</code> searched for a (possibly-qualified) 
 * operation and return its associated profile. If a qualifier is provided, 
 * the named facility or module is searched.  Otherwise, the operation is 
 * searched for in any directly imported modules and in instantiated versions 
 * of any available facilities.</p>
 *
 * @author Yu-Shan
 */
public class OperationProfileQuery extends BaseSymbolQuery<OperationEntry> {

    public OperationProfileQuery(PosSymbol qualifier, PosSymbol name,
            List<PTType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier,
                MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                MathSymbolTable.FacilityStrategy.FACILITY_INSTANTIATE, false),
                new OperationSearcher(name, argumentTypes));
    }
}
