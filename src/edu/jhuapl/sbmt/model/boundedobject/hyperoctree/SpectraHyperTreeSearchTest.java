
package edu.jhuapl.sbmt.model.boundedobject.hyperoctree;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton.Node;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperBox;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException.HyperDimensionMismatchException;
import edu.jhuapl.sbmt.model.spectrum.SpectraSearchDataCollection;

public class SpectraHyperTreeSearchTest
{

    public static void main(String[] args) throws HyperDimensionMismatchException, FileNotFoundException {

        String start = "2017-09-22T23:35:00.000";
        String end = "2017-09-22T23:40:00.000";

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        double minT = 0;
        double maxT = 0;
        try
        {
            minT = df.parse(start).getTime();
            maxT = df.parse(end).getTime();
        }
        catch (ParseException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }




        // read in the skeleton
        SpectraSearchDataCollection spectraModel = new SpectraSearchDataCollection(null);
        spectraModel.addDatasourceSkeleton("EarthOTEStest", "/Users/osheacm1/Documents/SAA/SBMT/testSpectrum/treeTest719/dataSource.spectra");
        spectraModel.setCurrentDatasourceSkeleton("EarthOTEStest");
        spectraModel.readSkeleton();
        FSHyperTreeSkeleton skeleton = spectraModel.getCurrentSkeleton();

        Set<Integer> cubeList = null;

        BoundingBox bb = new BoundingBox(new double[]{-6000, 800, -4000, 3000, 1000, 8000});
        HyperBox hbb = new HyperBox(new double[]{-6000, -4000, 1000, minT}, new double[]{800, 3000, 8000, maxT});

        cubeList = ((SpectraSearchDataCollection)spectraModel).getLeavesIntersectingBoundingBox(bb, new double[]{minT, maxT});

        Set<String> files = new HashSet<String>();
        HashMap<String, HyperBoundedObject> fileImgMap = new HashMap<String, HyperBoundedObject>();


        for (Integer cubeid : cubeList)
        {
            System.out.println("cubeId: " + cubeid);
            Node currNode = skeleton.getNodeById(cubeid);
            Path path = currNode.getPath();
            Path dataPath = path.resolve("data");
            DataInputStream instream= new DataInputStream(new BufferedInputStream(new FileInputStream(dataPath.toFile())));
            try
            {
                while (instream.available() > 0) {
                    HyperBoundedObject obj = BoundedObjectHyperTreeNode.createNewBoundedObject(instream);
                    int fileNum = obj.getFileNum();
                    Map<Integer, String> fileMap = skeleton.getFileMap();
                    String file = fileMap.get(fileNum);
                    if (files.add(file)) {
                        fileImgMap.put(file, obj);
                    }
                }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        for (String file : files) {
            System.out.println(file);
        }

        ArrayList<String> intFiles = new ArrayList<String>();

        // NOW CHECK WHICH SPECTRA ACTUALLY INTERSECT REGION
        for (String fi : files) {
            HyperBoundedObject img = fileImgMap.get(fi);
            HyperBox bbox = img.getBbox();
            try
            {
                if (hbb.intersects(bbox)) {
                    intFiles.add(fi);
                }
            }
            catch (HyperException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // print final list of images that intersect region
        System.out.println("IMAGES THAT INTERSECT SEARCH REGION: ");
        for (String file : intFiles) {
            System.out.println(file);
        }

    }


}
