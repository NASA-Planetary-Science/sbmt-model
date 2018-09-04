package edu.jhuapl.sbmt.model.boundedobject.hyperoctree;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeNode;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;


public class BoundedObjectHyperTreeSkeleton extends FSHyperTreeSkeleton
{

    public BoundedObjectHyperTreeSkeleton(Path dataSourcePath)  // data source path defines where the image file representing the tree structure resides; basepath is its parent
    {
        super(dataSourcePath);
    }

    @Override
    public double[] readBoundsFile(Path path)
    {
        File f=FileCache.getFileFromServer(path.toString());
        if (f.exists()) {
            return FSHyperTreeNode.readBoundsFile(Paths.get(f.getAbsolutePath()), 8);
        }

        f=FileCache.getFileFromServer(FileCache.FILE_PREFIX+path.toString());
        if (f.exists()) {
            return FSHyperTreeNode.readBoundsFile(Paths.get(f.getAbsolutePath()), 8);
        }

        return null;
    }

    @Override
    public void readChildren(Scanner scanner, Node node)   // cf. OlaFSHyperTreeCondenser for code to write the skeleton
    {
        readChildren(scanner, node, 8);
    }



}
