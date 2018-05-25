package edu.jhuapl.sbmt.model.bennu;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class BennuV4 extends SmallBodyModel
{
    static private final String[] modelNames = {
            "g_1254cm_tru_obj_0000n00000_v100",
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
            "Maximum Relative Height",
            "Gravity Vector X",
            "Normal Vector X",
            "Tilt Standard Deviation"
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
            "km", // Maximum Relative Height
            GravAccUnitsStr, // Gravity Vector X
            "", // Normal Vector is dimensionless
            SlopeUnitsStr, // Tilt Standard Deviation
    };

    public BennuV4(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                getModelFiles(config),
                getColoringFiles(config),
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
                config.serverPath("shape/shape0.obj.gz"),
                config.serverPath("shape/shape1.vtk.gz"),
                config.serverPath("shape/shape2.vtk.gz"),
                config.serverPath("shape/shape3.vtk.gz"),
                config.serverPath("shape/shape4.vtk.gz")
        };
        return paths;
    };

    private static final String[] getColoringFiles(SmallBodyViewConfig config)
    {
        return new String[] {
                config.serverPath("coloring/Slope"),
                config.serverPath("coloring/Elevation"),
                config.serverPath("coloring/GravitationalAcceleration"),
                config.serverPath("coloring/GravitationalPotential"),
                config.serverPath("coloring/FacetTilt"),
                config.serverPath("coloring/FacetTiltDirection"),
                config.serverPath("coloring/MeanTilt"),
                config.serverPath("coloring/TiltVariation"),
                config.serverPath("coloring/MeanTiltDirection"),
                config.serverPath("coloring/TiltDirectionVariation"),
                config.serverPath("coloring/RelativeTilt"),
                config.serverPath("coloring/RelativeTiltDirection"),
                config.serverPath("coloring/MaximumRelativeHeight"),
                config.serverPath("coloring/GravitationalAccelerationVector"),
                config.serverPath("coloring/NormalVector"),
                config.serverPath("coloring/TiltStandardDeviation")
        };
    }

    @Override
    public double getDensity()
    {
        return getSmallBodyConfig().density;
    }

    @Override
    public double getRotationRate()
    {
        return getSmallBodyConfig().rotationRate;
    }

    @Override
    public String getServerPathToShapeModelFileInPlateFormat()
    {
        return modelFilesInPlateFormat[getModelResolution()];
    }
}
