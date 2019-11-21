package edu.jhuapl.sbmt.model.bennu.lidar.old;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;

import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.model.bennu.shapeModel.Bennu;
import edu.jhuapl.sbmt.model.lidar.TrackFileType;
import edu.jhuapl.sbmt.util.TimeUtil;

/**
 * This divides an OLA L2 file into cubes and saves each cube
 * to a separate file. The L2 file format can be found in
 * /altwg/java-tools/src/altwg/util/LidarPoint.java and/or
 * LidarSearchDataCollection.loadTrackOlaL2()
 *
 * This programs is meant to run in parallel. It process only one
 * OLA L2 file at a time.
 *
 */
public class OlaCubesGenerator //extends LidarCubesGenerator
{
    private static Bennu bennu = null;
    public static TrackFileType type = TrackFileType.OLA_LEVEL_2;
    public static int[] xyzIndices = new int[]{114, 114+8, 114+16};
    public static int[] scIndices = new int[]{162, 162+8, 162+16};


    public static void main(String[] args)
    {
        Configuration.setAPLVersion(true);
        if (args.length > 0)
        {
            String lidarFileFullPath = args[0];
            new OlaCubesGenerator().run(lidarFileFullPath);
        }
        else
        {
            new OlaCubesGeneratorSeries().run();
        }
    }

    private SmallBodyModel getSmallBodyModel()
    {
        if (bennu == null)
        {
            bennu = new Bennu(SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.RQ36, ShapeModelType.GASKELL, "V3 Image"));
        }

        return bennu;
    }

    private String getOutputFolderPath()
    {
//        //Use this to run locally in Eclipse:
//        return "C:/Users/nguyel1/Projects/workspace/sbmt/src/edu/jhuapl/near/data/OLA";
        //To run on DMZ:
        return "/project/sbmtpipeline/processed/osirisrex/OLA/cubes";
    }

    /**
     * First create empty files for all the cubes files
     * @throws IOException
     */
    private void createInitialFiles() throws IOException
    {
        SmallBodyModel smallBodyModel = getSmallBodyModel();
        String outputFolder = getOutputFolderPath();

        TreeSet<Integer> cubes = smallBodyModel.getIntersectingCubes(smallBodyModel.getLowResSmallBodyPolyData());

        for (Integer cubeid : cubes)
        {
            FileWriter fstream = new FileWriter(outputFolder + "/" + cubeid + ".lidarcube");
            BufferedWriter out = new BufferedWriter(fstream);
            out.close();
        }
    }

    private void skip(DataInputStream in, int n) throws IOException
    {
        for (int i = 0; i < n; ++i)
        {
            in.readByte();
        }
    }

    public void run(String lidarFile)
    {
        NativeLibraryLoader.loadVtkLibraries();

        SmallBodyModel smallBodyModel = getSmallBodyModel();

        //pull out the L2 file base name to use as output folder name
        String basename = FilenameUtils.getBaseName(lidarFile);
        String lidarFilePath = lidarFile.substring(0,lidarFile.lastIndexOf(File.separator));
        String outputFolder = getOutputFolderPath() + File.separator + basename;
        new File(outputFolder).mkdirs();

        try
        {
            System.err.println("Processing file " + lidarFilePath);
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(lidarFile))));

            while (true)
            {
                double time = 0;
                double[] target = {0.0, 0.0, 0.0};
                double[] scpos = {0.0, 0.0, 0.0};
                boolean noise = false;

                try
                {
                    in.readByte();
                }
                catch(EOFException e)
                {
                    break;
                }

                try
                {
                    skip(in, 17 + 8 + 24);
                    time = FileUtil.readDoubleAndSwap(in);
                    skip(in, 8 + 2 * 3);
                    short flagStatus = MathUtil.swap(in.readShort());
                    noise = ((flagStatus == 0 || flagStatus == 1) ? false : true);
                    skip(in, 8 + 8 * 4);
                    target[0] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                    target[1] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                    target[2] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                    skip(in, 8 * 3);
                    scpos[0] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                    scpos[1] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                    scpos[2] = FileUtil.readDoubleAndSwap(in) / 1000.0;
                }
                catch(IOException e)
                {
                    in.close();
                    throw e;
                }

                if (!noise)
                {
                    // Compute closest point on asteroid to target
                    double[] closest = smallBodyModel.findClosestPoint(new double[]{target[0],target[1],target[2]});

                    // If no potential is provided in file, then use potential
                    // of plate of closest point
                    double[] coloringValues = smallBodyModel.getAllColoringValues(closest);
                    double potential = coloringValues[3];

                    int cubeid = smallBodyModel.getCubeId(closest);

                    if (cubeid >= 0)
                    {
                        // Open the file for appending
                        FileWriter fstream = new FileWriter(outputFolder + "/" + cubeid + ".lidarcube", true);
                        BufferedWriter out = new BufferedWriter(fstream);

                        String record =
                                TimeUtil.et2str(time) + " " +
                            (double)target[0] + " " + (double)target[1] + " " + (double)target[2] + " " +
                            (double)scpos[0] + " " + (double)scpos[1] + " " + (double)scpos[2] + " " +
                            (double)potential + "\n";

                        out.write(record);

                        out.close();
//                                System.err.println("Done writing " + (outputFolder + "/" + cubeid + ".lidarcube"));
                    }
                }
            }
            in.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
