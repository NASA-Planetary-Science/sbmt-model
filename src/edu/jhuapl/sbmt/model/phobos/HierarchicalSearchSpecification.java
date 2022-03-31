package edu.jhuapl.sbmt.model.phobos;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.SettableMetadata;

public class HierarchicalSearchSpecification
{
    private static final Key<String> TREE_ROOT_NAME = Key.of("treeRoot");
    private static final Key<Map<String, List<Integer>>> CAMERA_MAP_KEY = Key.of("cameraMap");
    private static final Key<List<String[]>> TREE_PATH_KEY = Key.of("selectionTree");

    private TreeModel treeModel;
    private final Map<List<Object>, CameraInfo> cameraMap;
    private TreeSelectionModel selectionModel;

    /**
     * This constructor is for use when initializing from metadata. 
     */
    public HierarchicalSearchSpecification()
    {
        this.treeModel = null;
        this.cameraMap = new LinkedHashMap<>();
        this.selectionModel = null;
    }

    public HierarchicalSearchSpecification(String rootName)
    {
        // Create a tree model with just the root
        this.treeModel = new DefaultTreeModel(createTreeNode(rootName));
        this.cameraMap = new LinkedHashMap<>();
        this.selectionModel = null;
    }

    // Method used to get the tree model
    public TreeModel getTreeModel()
    {
        return treeModel;
    }

    public void setTreeModel(TreeModel treeModel)
    {
        this.treeModel = treeModel;
    }

    public void setSelectionModel(TreeSelectionModel selectionModel)
    {
        this.selectionModel = selectionModel;
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
            Enumeration<TreeNode> e = currNode.children();
            boolean childFound = false;
            while(e.hasMoreElements())
            {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
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
    public Selection processTreeSelections()
    {
        Preconditions.checkState(selectionModel != null, "Set the selection model for the tree search before processing selections");

        TreePath[] selectedPaths = selectionModel.getSelectionPaths();

        ImmutableList.Builder<CameraInfo> builder = ImmutableList.builder();
        // Iterate through the selected paths
        for(TreePath tp : selectedPaths)
        {
            // Note: This is a common source of confusion for a lot of users of CheckboxTree, each selected path
            //       last component is actually the deepest selected node for which all its children are selected
            //       as opposed to one path for each selected leaf
            DefaultMutableTreeNode selectedParentNode = (DefaultMutableTreeNode)tp.getLastPathComponent();

            // Get all leaves from the selected parent node
            Enumeration<TreeNode> en = selectedParentNode.depthFirstEnumeration();
            while(en.hasMoreElements())
            {
                // Information that we want is located at the leaf nodes
                DefaultMutableTreeNode tempNode = (DefaultMutableTreeNode) en.nextElement();
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

        @Override
        public String toString()
        {
            return "Camera " + cameraCheckbox + ", filter " + filterCheckbox;
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

    public MetadataManager getMetadataManager()
    {
        return new MetadataManager() {

            @Override
            public Metadata store()
            {
                Preconditions.checkState(selectionModel != null, "Set selection model prior to storing tree search metadata");

                SettableMetadata result = SettableMetadata.of(Version.of(1, 0));

                result.put(TREE_ROOT_NAME, treeModel.getRoot().toString());

                Map<String, List<Integer>> mdCameraMap = new LinkedHashMap<>();
                for (Entry<List<Object>, CameraInfo> entry : cameraMap.entrySet())
                {
                    StringBuilder sb = new StringBuilder();
                    String delim = "";
                    for (Object s : entry.getKey())
                    {
                      sb.append(delim);
                      sb.append(s);
                      delim = ", ";
                    }
                    String key = sb.toString();
                    CameraInfo cam = entry.getValue();
                    ImmutableList<Integer> cameraInfo = ImmutableList.of(cam.getCameraCheckbox(), cam.getFilterCheckbox());
                    mdCameraMap.put(key, cameraInfo);
                }
                result.put(CAMERA_MAP_KEY, mdCameraMap);

                ImmutableList.Builder<String[]> builder = ImmutableList.builder();

                TreePath[] treePaths = selectionModel.getSelectionPaths();
                for (TreePath treePath : treePaths)
                {
                    Object[] pathObjects = treePath.getPath();
                    String[] pathStrings = new String[pathObjects.length];
                    for (int index = 0; index < pathObjects.length; ++index)
                    {
                        // For serializing, it's OK just to rely on the fact that
                        // each of these nodes looks like the string it wraps.
                        pathStrings[index] = pathObjects[index].toString();
                    }
                    builder.add(pathStrings);
                }
                result.put(TREE_PATH_KEY, builder.build());
                return result;
            }

            @Override
            public void retrieve(Metadata source)
            {
                String rootName = source.get(TREE_ROOT_NAME);
                if (rootName != null)
                {
                    treeModel = new DefaultTreeModel(createTreeNode(rootName));
                }

                cameraMap.clear();
                Map<String, List<Integer>> mdCameraMap = source.get(CAMERA_MAP_KEY);
                for (Entry<String, List<Integer>> entry : mdCameraMap.entrySet())
                {
                    ImmutableList.Builder<Object> b = ImmutableList.builder();
                    for (String s : entry.getKey().split(", "))
                    {
                        b.add(s);
                    }
                    ImmutableList<Object> key = b.build();

                    List<Integer> mdCamInfo = entry.getValue();
                    CameraInfo cam = new CameraInfo(mdCamInfo.get(0), mdCamInfo.get(1));
                    cameraMap.put(key, cam);
                }

                if (selectionModel != null)
                {
                	List<String[]> pathStringList = source.get(TREE_PATH_KEY);
                    TreePath[] treePaths = new TreePath[pathStringList.size()];

                    for (int treeIndex = 0; treeIndex < pathStringList.size(); ++treeIndex)
                    {
                        String[] pathStrings = pathStringList.get(treeIndex);
                        DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[pathStrings.length];
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) treeModel.getRoot();
                        nodes[0] = currentNode;
                        for (int index = 0; index < pathStrings.length; ++index)
                        {
                            Enumeration<TreeNode> children = currentNode.children();
                            while (children.hasMoreElements())
                            {
                                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                                if (child.getUserObject().equals(pathStrings[index]))
                                {
                                    nodes[index] = child;
                                    currentNode = child;
                                    break;
                                }
                            }
                        }
                        treePaths[treeIndex] = new TreePath(nodes);
                    }
                    selectionModel.setSelectionPaths(treePaths);
                }
            }
        };
    }
}
