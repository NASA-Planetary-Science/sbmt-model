package edu.jhuapl.sbmt.model.boundedobject.hyperoctree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SbmtMultiMissionTool;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.lidar.DataOutputStreamPool;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperBox;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException;
import edu.jhuapl.sbmt.model.spectrum.hypertree.SpectrumHypertreeGenerator;
import edu.jhuapl.sbmt.tools.Authenticator;

public class BoundedObjectHyperTreeGenerator
{
    final Path outputDirectory;
    final int maxNumberObjectsPerLeaf;
    final HyperBox bbox;
    final int maxNumberOfOpenOutputFiles;
    final DataOutputStreamPool pool;
    BoundedObjectHyperTreeNode root;
    long totalObjectsWritten = 0;


    BiMap<String, Integer> fileMap = HashBiMap.create();



    public BoundedObjectHyperTreeGenerator(Path outputDirectory, int maxObjectsPerLeaf, HyperBox bbox, int maxNumberOfOpenOutputFiles, DataOutputStreamPool pool)
    {
        this.outputDirectory = outputDirectory;
        this.maxNumberObjectsPerLeaf = maxObjectsPerLeaf;
        this.maxNumberOfOpenOutputFiles = maxNumberOfOpenOutputFiles;
        this.bbox = bbox; // bounding box of body
        this.pool = pool;
        root = new BoundedObjectHyperTreeNode(null, outputDirectory, bbox, maxObjectsPerLeaf,pool);
    }

    private void addAllObjectsFromFile(String inputPath) throws HyperException, IOException
    {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try (BufferedReader br = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] toks = line.split(" ");
                String objName = toks[0];
                try {
                    Date minTime = df.parse(toks[7]);
                    double minT = minTime.getTime();
                    double maxT = df.parse(toks[8]).getTime();

                    HyperBox objBBox = new HyperBox(new double[]{Double.parseDouble(toks[1]), Double.parseDouble(toks[3]), Double.parseDouble(toks[5]), minT},
                            new double[]{Double.parseDouble(toks[2]), Double.parseDouble(toks[4]), Double.parseDouble(toks[6]), maxT}); //emission, incidence, phase, distance

                    int objId = objName.hashCode();
                    fileMap.put(objName, objId);
                    HyperBoundedObject obj = new HyperBoundedObject(objName, objId, objBBox);
                    root.add(obj);
                    totalObjectsWritten++;

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        }

    }


    public void expand() throws HyperException, IOException
    {
        expandNode(root);
    }

    public void expandNode(BoundedObjectHyperTreeNode node) throws HyperException, IOException
    {
        if (node.getNumberOfObjects() > maxNumberObjectsPerLeaf)
        {
            node.split();
            for (int i=0; i<node.getNumberOfChildren(); i++)
                if (node.childExists(i))
                {
                    expandNode((BoundedObjectHyperTreeNode)node.getChild(i));
                }
        }
    }

    public void commit() throws IOException
    {
        pool.closeAllStreams();// close any files that are still open
        finalCommit(root);
    }

    void finalCommit(BoundedObjectHyperTreeNode node) throws IOException
    {
        File dataFile = node.getDataFilePath().toFile();  // clean up any data files with zero points
        if (!node.isLeaf())
        {
            if (dataFile.exists())
                dataFile.delete();
            for (int i=0; i<node.getNumberOfChildren(); i++)
                finalCommit((BoundedObjectHyperTreeNode)node.getChild(i));
        }
        else {
            if (!dataFile.exists() || dataFile.length()==0l)
            {
                node.getBoundsFilePath().toFile().delete();
                node.getPath().toFile().delete();
            }
        }
    }

    public List<BoundedObjectHyperTreeNode> getAllNonEmptyLeafNodes()
    {
        List<BoundedObjectHyperTreeNode> nodeList=Lists.newArrayList();
        getAllNonEmptyLeafNodes(root, nodeList);
        return nodeList;
    }

    void getAllNonEmptyLeafNodes(BoundedObjectHyperTreeNode node, List<BoundedObjectHyperTreeNode> nodeList)
    {
        if (!node.isLeaf())
            for (int i=0; i<node.getNumberOfChildren(); i++)
                getAllNonEmptyLeafNodes((BoundedObjectHyperTreeNode)node.getChild(i), nodeList);
        else if (node.getDataFilePath().toFile().exists())
            nodeList.add(node);
    }

    private static void printUsage()
    {
        System.out.println("Arguments:");
        System.out.println("   (1)   Intrument");
        System.out.println("   (2)   Type");
        System.out.println("   (3)   Body");
        System.out.println("   (4)   max number of items per leaf");
        System.out.println("   (5)   type of object: 'IMAGE' or 'SPECTRA'");
    }


    public static void main(String[] args) throws IOException, HyperException
    {
        Configuration.setAPLVersion(true);
        SbmtMultiMissionTool.configureMission();

        // need password to access OREX data
        Authenticator.authenticate();

        SmallBodyViewConfig.initialize();

        if (args.length!=5)
        {
            printUsage();
            return;
        }

        String instrument = args[0];
        String type = args[1];
        String bodyName = args[2];
        int maxObjectsPerLeaf = Integer.parseInt(args[3]);
        int maxNumOpenOutputFiles=32;

        /*
         * Set up
         */
        String inputFile = "bounds_" + instrument.toLowerCase() + "_" + type.toLowerCase().replace("/", "") + ".bounds";
        System.out.println("Input file = "+ inputFile);
        // make a temp hypertree folder
        String outputDirectoryString = "temp_hypertree/";
        new File(outputDirectoryString).mkdirs();
        System.out.println("Output tree location = " + outputDirectoryString);
        System.out.println("Max # open output files = " + maxNumOpenOutputFiles);



        Path outputDirectory = Paths.get(outputDirectoryString);
        DataOutputStreamPool pool=new DataOutputStreamPool(maxNumOpenOutputFiles);


        /*
         * Get body model
         */
        System.setProperty("java.awt.headless", "true");
        NativeLibraryLoader.loadVtkLibrariesHeadless();

        SmallBodyViewConfig config;
        if (bodyName.equalsIgnoreCase("EARTH")) {
            config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EARTH, ShapeModelType.OREX);
        }
        else if (bodyName.equalsIgnoreCase("BENNU")) {
            config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.RQ36, ShapeModelType.OREX);
        }
        else {
            System.err.println("No support for body named " + bodyName);
            return;
        }
        SmallBodyModel body = SbmtModelFactory.createSmallBodyModel(config);
        BoundingBox bodyBBox = body.getBoundingBox();



        double today = new Date().getTime();

        BoundedObjectHyperTreeGenerator generator;
        if (args[4].equalsIgnoreCase("SPECTRA")) {
            // bounds from input PLUS min/max angles
            double[] min = {bodyBBox.xmin, bodyBBox.ymin, bodyBBox.zmin, -Double.MAX_VALUE, 0, 0, 0, 0};
            double[] max = {bodyBBox.xmax, bodyBBox.ymax, bodyBBox.zmax, today,  180, 180, 180, Double.MAX_VALUE};
            HyperBox hbox = new HyperBox(min, max);

            generator = new SpectrumHypertreeGenerator(outputDirectory, maxObjectsPerLeaf, hbox, maxNumOpenOutputFiles, pool);
        }
        else {
            // bounds from input
            double[] min = {bodyBBox.xmin, bodyBBox.ymin, bodyBBox.zmin, -Double.MAX_VALUE};
            double[] max = {bodyBBox.xmax, bodyBBox.ymax, bodyBBox.zmax, today};
            HyperBox hbox = new HyperBox(min, max);

            generator = new BoundedObjectHyperTreeGenerator(outputDirectory, maxObjectsPerLeaf, hbox, maxNumOpenOutputFiles, pool);
        }

        if (generator instanceof SpectrumHypertreeGenerator) {
            ((SpectrumHypertreeGenerator)generator).addAllObjectsFromFile(inputFile);
        }
        else {
            generator.addAllObjectsFromFile(inputFile);
        }
        Path fileMapPath = outputDirectory.resolve("fileMap.txt");
        System.out.print("Writing file map to "+fileMapPath+"... ");
        FileWriter writer = new FileWriter(fileMapPath.toFile());
        for (int i : generator.fileMap.inverse().keySet())
            writer.write(i+" "+generator.fileMap.inverse().get(i)+"\n");
        writer.close();
        System.out.println("Done.");


        System.out.println("Expanding tree.");
        System.out.println("Max # pts per leaf="+maxObjectsPerLeaf);
        generator.expand();
        System.out.println();
        generator.commit(); // clean up any empty or open data files



        // get all non-empty leaf nodes and list the contents
        List<BoundedObjectHyperTreeNode> nodeList = new ArrayList<BoundedObjectHyperTreeNode>();
        generator.getAllNonEmptyLeafNodes(generator.getRoot(),nodeList);
        System.out.println("Number of leaves with files: " + nodeList.size());
        for(BoundedObjectHyperTreeNode node : nodeList) {
           System.out.println("Number of objects in node " + node.getPath() + ": " + node.getNumberOfObjects());
        }

    }

    public BiMap<String, Integer> getFileMap() {
        return fileMap;
    }

    public BoundedObjectHyperTreeNode getRoot()
    {
        return root;
    }

    public long getTotalObjectsWritten()
    {
        return totalObjectsWritten;
    }

    public void setTotalObjectsWritten(long totalObjectsWritten)
    {
        this.totalObjectsWritten = totalObjectsWritten;
    }

}
