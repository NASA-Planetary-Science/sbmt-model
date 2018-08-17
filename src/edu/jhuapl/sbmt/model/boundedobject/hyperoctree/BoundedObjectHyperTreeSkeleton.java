package edu.jhuapl.sbmt.model.boundedobject.hyperoctree;

import java.nio.file.Path;

import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;


public class BoundedObjectHyperTreeSkeleton extends FSHyperTreeSkeleton
{

    public BoundedObjectHyperTreeSkeleton(Path dataSourcePath)  // data source path defines where the image file representing the tree structure resides; basepath is its parent
    {
        super(dataSourcePath);
    }


}
