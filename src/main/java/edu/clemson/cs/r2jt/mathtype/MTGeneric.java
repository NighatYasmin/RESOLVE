package edu.clemson.cs.r2jt.mathtype;

import java.util.Collections;
import java.util.List;

import edu.clemson.cs.r2jt.typereasoning.TypeGraph;

public class MTGeneric extends MTAbstract<MTGeneric> {

    private static final int BASE_HASH = "MTGeneric".hashCode();

    private final String myName;

    public MTGeneric(TypeGraph g, String name) {
        super(g);

        myName = name;
    }

    @Override
    public void accept(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);

        v.beginChildren(this);
        v.endChildren(this);

        v.endMTAbstract(this);
        v.endMTType(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MTType> getComponentTypes() {
        return (List<MTType>) Collections.EMPTY_LIST;
    }

    @Override
    public MTType withComponentReplaced(int index, MTType newType) {
        throw new IndexOutOfBoundsException("" + index);
    }

    @Override
    public int getHashCode() {
        return BASE_HASH + myName.hashCode();
    }

    public String getName() {
        return myName;
    }
}
