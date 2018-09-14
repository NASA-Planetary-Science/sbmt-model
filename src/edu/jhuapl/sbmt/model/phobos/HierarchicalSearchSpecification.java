package edu.jhuapl.sbmt.model.phobos;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.google.common.collect.ImmutableList;

public abstract class HierarchicalSearchSpecification
{
    private final TreeModel treeModel;
    private final Map<List<Object>, CameraInfo> cameraMap;

    public HierarchicalSearchSpecification(String rootName)
    {
        // Create a tree model with just the root
        this.treeModel = new DefaultTreeModel(createTreeNode(rootName));
        this.cameraMap = new HashMap<>();
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
        DefaultMutableTreeNode currNode = (DefaultMutableTreeNode) treeModel.getRoot();

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
                if(childNode.getUserObject().equals(path[i]))
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
                DefaultMutableTreeNode newChildNode = (DefaultMutableTreeNode) createTreeNode(path[i]);
                currNode.add(newChildNode);

                // Set current node to that child
                currNode = newChildNode;
            }
        }

        // Last node is the leaf; must add it no matter what. Use a String here like in all the other nodes.
        DefaultMutableTreeNode leafNode = (DefaultMutableTreeNode) createTreeNode(path[path.length - 1]);
        currNode.add(leafNode);

        // Put the camera information in a map where we can get it later.
        CameraInfo info = new CameraInfo(cameraCheckbox, filterCheckbox);
        cameraMap.put(ImmutableList.copyOf(leafNode.getUserObjectPath()), info);
    }

    // Method for processing tree selections
    public Selection processTreeSelections(TreePath[] selectedPaths)
    {
        ImmutableList.Builder<CameraInfo> builder = ImmutableList.builder();
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
                    builder.add(cameraMap.get(ImmutableList.copyOf(tempNode.getUserObjectPath())));
                }
            }
        }
        return new Selection(builder.build());
    }

    protected MutableTreeNode createTreeNode(String nodeName)
    {
        return new DefaultMutableTreeNode(nodeName);
    }

    /**
     * The camera and filter checkbox identifiers associated with a particular
     * imager.
     */
    public class CameraInfo
    {
        private final int cameraCheckbox;
        private final int filterCheckbox;

        private CameraInfo(int cameraCheckbox, int filterCheckbox)
        {
            this.cameraCheckbox = cameraCheckbox;
            this.filterCheckbox = filterCheckbox;
        }

        public int getCameraCheckbox()
        {
            return cameraCheckbox;
        }

        public int getFilterCheckbox()
        {
            return filterCheckbox;
        }

    }

    public class Selection
    {
        private final ImmutableList<Integer> cameras;
        private final ImmutableList<Integer> filters;

        private Selection(Iterable<CameraInfo> selection)
        {
            ImmutableList.Builder<Integer> cameraBuilder = ImmutableList.builder();
            ImmutableList.Builder<Integer> filterBuilder = ImmutableList.builder();
            for (CameraInfo node : selection)
            {
                cameraBuilder.add(node.cameraCheckbox);
                filterBuilder.add(node.filterCheckbox);
            }
            this.cameras = cameraBuilder.build();
            this.filters = filterBuilder.build();
        }

        public ImmutableList<Integer> getSelectedCameras()
        {
            return cameras;
        }

        public ImmutableList<Integer> getSelectedFilters()
        {
            return filters;
        }
    }
}
