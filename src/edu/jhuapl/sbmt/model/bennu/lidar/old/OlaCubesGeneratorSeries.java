package edu.jhuapl.sbmt.model.bennu.lidar.old;

import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.lidar.old.LidarCubesGenerator;
import edu.jhuapl.sbmt.lidar.old.LidarCubesGenerator.LidarDataType;
import edu.jhuapl.sbmt.model.bennu.shapeModel.Bennu;
import edu.jhuapl.sbmt.model.lidar.TrackFileType;

/**
 * This program goes through all the OLA L2 data and divides the data
 * up into cubes and saves each cube to a separate file. The L2 file
 * format can be found in
 * /altwg/java-tools/src/altwg/util/LidarPoint.java and/or
 * LidarSearchDataCollection.loadTrackOlaL2()
 *
 * This programs runs in series, processing all OLA files one by one.
 *
 * This program also can generate a single vtk file containing all
 * the OLA data (see comments in code).
 */
public class OlaCubesGeneratorSeries extends LidarCubesGenerator
{
    private static Bennu bennu = null;
    public static TrackFileType type = TrackFileType.OLA_LEVEL_2;
    public static int[] xyzIndices = new int[]{114, 114+8, 114+16};
    public static int[] scIndices = new int[]{162, 162+8, 162+16};

    public static void main(String[] args)
    {
        Configuration.setAPLVersion(true);
        new OlaCubesGeneratorSeries().run();
    }

    @Override
    protected SmallBodyModel getSmallBodyModel()
    {
        if (bennu == null)
        {
            bennu = new Bennu(SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.RQ36, ShapeModelType.GASKELL, "V3 Image"));
        }

        return bennu;
    }

    @Override
    protected int[] getXYZIndices()
    {
        return xyzIndices;
    }

    @Override
    protected int[] getSpacecraftIndices()
    {
        return scIndices;
    }

    @Override
    protected String getFileListPath()
    {
//        //Use this to run locally in Eclipse:
//        return "C:/Users/nguyel1/Projects/workspace/sbmt/src/edu/jhuapl/near/data/allOlaFiles.txt";
        //To run on DMZ:
        return "/project/sbmtpipeline/processed/osirisrex/OLA/allOlaFiles.txt";
    }

    @Override
    protected String getOutputFolderPath()
    {
//        //Use this to run locally in Eclipse:
//        return "C:/Users/nguyel1/Projects/workspace/sbmt/src/edu/jhuapl/near/data/OLA";
        //To run on DMZ:
        return "/project/sbmtpipeline/processed/osirisrex/OLA/cubes";
    }

    @Override
    protected int getNumberHeaderLines()
    {
        return 0;
    }

    @Override
    protected boolean isInMeters()
    {
        return true;
    }

    @Override
    protected boolean isSpacecraftInSphericalCoordinates()
    {
        return false;
    }

    @Override
    protected int getTimeIndex()
    {
        return 18;
    }

    @Override
    protected int getNoiseIndex()
    {
        return -1;
    }

    @Override
    protected int getPotentialIndex()
    {
        return -1;
    }

    @Override
    protected LidarDataType getLidarDataType()
    {
        return LidarDataType.OLA_LEVEL_2;
    }

}
