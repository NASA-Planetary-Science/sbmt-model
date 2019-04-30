package edu.jhuapl.sbmt.model.bennu;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class Bennu extends SmallBodyModel
{
    static private final String[] modelNames = {
            "ver64q",
            "ver128q",
            "ver256q",
            "ver512q"
    };

    static private final String[] modelFilesInPlateFormat = null;

    static private final String[] imageMap = null;


    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr,
            "Facet Tilt",
            "Facet Tilt Direction",
            "Mean Tilt",
            "Tilt Variation",
            "Mean Tilt Direction",
            "Tilt Direction Variation",
            "Relative Tilt",
            "Relative Tilt Direction",
            "Maximum Relative Height"
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr,
            SlopeUnitsStr, // Facet Tilt
            SlopeUnitsStr, // Facet Tilt Direction
            SlopeUnitsStr, // Mean Tilt
            SlopeUnitsStr, // Tilt Variation
            SlopeUnitsStr, // Mean Tilt Direction
            SlopeUnitsStr, // Tilt Direction Variation
            SlopeUnitsStr, // Relative Tilt
            SlopeUnitsStr, // Relative Tilt Direction
            "km" // Maximum Relative Height
    };

    public Bennu(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                getModelFiles(config),
                getColoringFiles(config.rootDirOnServer),
                coloringNames,
                coloringUnits,
                null,
                imageMap,
                ColoringValueType.CELLDATA,
                false);
    }

    private static final String[] getModelFiles(SmallBodyViewConfig config)
    {
        String[] paths = {
                config.rootDirOnServer + "/ver64q.vtk.gz",
                config.rootDirOnServer + "/ver128q.vtk.gz",
                config.rootDirOnServer + "/ver256q.vtk.gz",
                config.rootDirOnServer + "/ver512q.vtk.gz"
        };
        return paths;
    };

    private static final String[] getColoringFiles(String path)
    {
        return new String[] {
                path + "/Slope",
                path + "/Elevation",
                path + "/GravitationalAcceleration",
                path + "/GravitationalPotential",
                path + "/FacetTilt",
                path + "/FacetTiltDirection",
                path + "/MeanTilt",
                path + "/TiltVariation",
                path + "/MeanTiltDirection",
                path + "/TiltDirectionVariation",
                path + "/RelativeTilt",
                path + "/RelativeTiltDirection",
                path + "/MaximumRelativeHeight"
        };
    }

    @Override
    public double getDensity()
    {
        return ((SmallBodyViewConfig)getSmallBodyConfig()).density;
    }

    @Override
    public double getRotationRate()
    {
        return ((SmallBodyViewConfig)getSmallBodyConfig()).rotationRate;
    }

    @Override
    public String getServerPathToShapeModelFileInPlateFormat()
    {
        return modelFilesInPlateFormat[getModelResolution()];
    }
}
