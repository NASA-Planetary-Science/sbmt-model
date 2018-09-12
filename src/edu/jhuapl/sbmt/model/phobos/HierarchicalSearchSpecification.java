package edu.jhuapl.sbmt.model.phobos;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public abstract class HierarchicalSearchSpecification
{
    private final TreeModel treeModel;
    private final List<HierarchicalSearchLeafNode> allLeafNodes;
    private ImmutableSet<HierarchicalSearchLeafNode> selection;

    public HierarchicalSearchSpecification(String rootName)
    {
        // Create a tree model with just the root
        this.treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(rootName));
        this.allLeafNodes = new ArrayList<>();
        this.selection = ImmutableSet.of();
    }

    // Method used to get the tree model
    public TreeModel getTreeModel()
    {
        return treeModel;
    }

    // Deep copy of the search specification
    @Override
    public HierarchicalSearchSpecification clone()
    {
        // TBD, do nothing for now
        System.err.println("REMINDER: HierarchicalSearchSpecification.clone() not yet implemented!");
        return null;
    }

    // Adds nodes to tree as necessary to create the path
    protected void addHierarchicalSearchPath(String[] path, int cameraCheckbox, int filterCheckbox)
    {
        // Get the root node
        DefaultMutableTreeNode currNode = (DefaultMutableTreeNode)treeModel.getRoot();

        // Go through each level of path before child and make sure that it exists
        for(int i=0; i<path.length-1; i++)
        {
            // See if node has a child called path[i]
            @SuppressWarnings("unchecked")
            Enumeration<DefaultMutableTreeNode> e = currNode.children();
            boolean childFound = false;
            while(e.hasMoreElements())
            {
                DefaultMutableTreeNode childNode = e.nextElement();
                if(childNode.toString().equals(path[i]))
                {
                    childFound = true;
                    currNode = childNode;
                    break;
                }
            }

            // If child was not found then create one and insert
            if(!childFound)
            {
                // Add the new node
                DefaultMutableTreeNode newChildNode = new DefaultMutableTreeNode(path[i]);
                currNode.add(newChildNode);

                // Set current node to that child
                currNode = newChildNode;
            }
        }

        // Always insert the child node
        HierarchicalSearchLeafNode newLeafNode = new HierarchicalSearchLeafNode(path, cameraCheckbox, filterCheckbox);
        currNode.add(new DefaultMutableTreeNode(newLeafNode));
        allLeafNodes.add(newLeafNode);
    }

    // Method for processing tree selections
    public void processTreeSelections(TreePath[] selectedPaths)
    {
        ImmutableSet.Builder<HierarchicalSearchLeafNode> builder = ImmutableSet.builder();
        // Iterate through the selected paths
        for(TreePath tp : selectedPaths)
        {
            // Note: This is a common source of confusion for a lot of users of CheckboxTree, each selected path
            //       last component is actually the deepest selected node for which all its children are selected
            //       as opposed to one path for each selected leaf
            DefaultMutableTreeNode selectedParentNode = (DefaultMutableTreeNode)tp.getLastPathComponent();

            // Get all leaves from the selected parent node
            @SuppressWarnings("unchecked")
            Enumeration<DefaultMutableTreeNode> en = selectedParentNode.depthFirstEnumeration();
            while(en.hasMoreElements())
            {
                // Information that we want is located at the leaf nodes
                DefaultMutableTreeNode tempNode = en.nextElement();
                if(tempNode.isLeaf())
                {
                    // Extract the saved object at the leaf node containing camera and filter checkbox numbers
                    builder.add((HierarchicalSearchLeafNode)tempNode.getUserObject());
                }
            }
        }
        selection = builder.build();
    }

    // Get camera portion of selected (camera,filter) pairs
    public List<Integer> getSelectedCameras()
    {
        LinkedList<Integer> result = new LinkedList<>();
        for (HierarchicalSearchLeafNode node : allLeafNodes)
        {
            if (selection.contains(node))
            {
                result.add(node.cameraCheckbox);
            }
        }
        return result;
    }

    // Get filter portion of selected (camera,filter) pairs
    public List<Integer> getSelectedFilters()
    {
        LinkedList<Integer> result = new LinkedList<>();
        for (HierarchicalSearchLeafNode node : allLeafNodes)
        {
            if (selection.contains(node))
            {
                result.add(node.filterCheckbox);
            }
        }
        return result;
    }

    public ImmutableList<HierarchicalSearchLeafNode> getAllLeafNodes()
    {
        return ImmutableList.copyOf(allLeafNodes);
    }

    public ImmutableSet<HierarchicalSearchLeafNode> getSelectedLeafNodes()
    {
        return selection;
    }

    /**
     * Helper class for storing data at TreeModel leaf nodes
     */
    public class HierarchicalSearchLeafNode
    {
        private final ImmutableList<String> path;
        private final int cameraCheckbox;
        private final int filterCheckbox;

        private HierarchicalSearchLeafNode(String[] path, int cameraCheckbox, int filterCheckbox)
        {
            Preconditions.checkNotNull(path);
            Preconditions.checkArgument(path.length > 0);
            this.path = ImmutableList.copyOf(path);
            this.cameraCheckbox = cameraCheckbox;
            this.filterCheckbox = filterCheckbox;
        }

        public ImmutableList<String> getPath()
        {
            return path;
        }

        public int getCameraCheckbox()
        {
            return cameraCheckbox;
        }

        public int getFilterCheckbox()
        {
            return filterCheckbox;
        }

        @Override
        public int hashCode()
        {
            return path.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (other instanceof HierarchicalSearchLeafNode)
            {
                HierarchicalSearchLeafNode that = (HierarchicalSearchLeafNode) other;
                return this.path.equals(that.path);
            }
            return false;
        }

        // This method must return what we want to be displayed for the leaf node in the GUI
        @Override
        public String toString()
        {
            return path.get(path.size() - 1);
        }
    }
}
