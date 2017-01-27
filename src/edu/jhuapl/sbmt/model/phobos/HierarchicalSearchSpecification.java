package edu.jhuapl.sbmt.model.phobos;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public abstract class HierarchicalSearchSpecification
{
    protected TreeModel treeModel;

    // Get method, move this to parent class later
    public TreeModel getTreeModel()
    {
        return treeModel;
    }

    // Deep copy of the search specification
    public HierarchicalSearchSpecification clone()
    {
        // For now
        return null;
    }

    // Method for processing tree selections
    public abstract void processTreeSelections(TreePath[] selectedPaths);
}
