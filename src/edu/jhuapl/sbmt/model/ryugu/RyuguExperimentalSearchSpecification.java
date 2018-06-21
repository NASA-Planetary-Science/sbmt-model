package edu.jhuapl.sbmt.model.ryugu;

import edu.jhuapl.sbmt.model.phobos.HierarchicalSearchSpecification;

public class RyuguExperimentalSearchSpecification extends HierarchicalSearchSpecification
{
    public enum DataProduct
    {
        L2A,
        L2B,
        L3A,
        L3A_ALPHA,
        L3B,
        L4B,
        L4C,
        L4D
    }

    /**
     *  This is where all the paths and corresponding mapping to camera/filter checkbox numbers are specified
     */
    public RyuguExperimentalSearchSpecification()
    {
        // Call parent constructor with name of the root node that we want
        super("Ryugu/TIR");

        // Phobos 2
        addHierarchicalSearchPath(new String[] {DataProduct.L2A.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L2B.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L3A.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L3A_ALPHA.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L3B.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L4B.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L4C.toString()}, -1, -1 );
        addHierarchicalSearchPath(new String[] {DataProduct.L4D.toString()}, -1, -1 );

    }
}
