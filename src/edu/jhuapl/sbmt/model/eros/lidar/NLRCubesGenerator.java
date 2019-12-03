package edu.jhuapl.sbmt.model.eros.lidar;

import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.lidar.old.LidarCubesGenerator;
import edu.jhuapl.sbmt.lidar.old.LidarCubesGenerator.LidarDataType;
import edu.jhuapl.sbmt.model.eros.Eros;

/**
 * This program goes through all the NLR data and divides all the data
 * up into cubes and saves each cube to a separate file.
 *
 * This program also can generate a single vtk file containing all
 * the NLR data (see comments in code).
 */
public class NLRCubesGenerator extends LidarCubesGenerator
{
    private static Eros eros = null;

    public static void main(String[] args)
    {
        new NLRCubesGenerator().run();
    }

    @Override
    protected SmallBodyModel getSmallBodyModel()
    {
        if (eros == null)
        {
            eros = new Eros(SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EROS, ShapeModelType.GASKELL));
        }

        return eros;
    }

    @Override
    protected int[] getXYZIndices()
    {
        return new int[]{14, 15, 16};
    }

    @Override
    protected int[] getSpacecraftIndices()
    {
        return new int[]{8, 9, 10};
    }

    @Override
    protected String getFileListPath()
    {
        return "/project/nearsdc/data/NLR/NlrFiles.txt";
    }

    @Override
    protected String getOutputFolderPath()
    {
        return "/project/nearsdc/data/NLR/cubes";
    }

    @Override
    protected int getNumberHeaderLines()
    {
        return 2;
    }

    @Override
    protected boolean isInMeters()
    {
        return true;
    }

    @Override
    protected boolean isSpacecraftInSphericalCoordinates()
    {
        return true;
    }

    @Override
    protected int getTimeIndex()
    {
        return 4;
    }

    @Override
    protected int getNoiseIndex()
    {
        return 7;
    }

    @Override
    protected int getPotentialIndex()
    {
        return 18;
    }

    @Override
    protected LidarDataType getLidarDataType()
    {
        return LidarDataType.OTHER;
    }
}
