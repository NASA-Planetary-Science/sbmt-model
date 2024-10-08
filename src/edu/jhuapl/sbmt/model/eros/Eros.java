package edu.jhuapl.sbmt.model.eros;

import edu.jhuapl.saavtk.config.IBodyViewConfig;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;

public class Eros extends SmallBodyModel
{
    static private final String[] modelNames = {
            "NEAR-A-MSI-5-EROSSHAPE-V1.0 ver64q",
            "NEAR-A-MSI-5-EROSSHAPE-V1.0 ver128q",
            "NEAR-A-MSI-5-EROSSHAPE-V1.0 ver256q",
            "NEAR-A-MSI-5-EROSSHAPE-V1.0 ver512q"
    };

    static private final String[] coloringFiles = {
            "/EROS/Eros_Slope",
            "/EROS/Eros_Elevation",
            "/EROS/Eros_GravitationalAcceleration",
            "/EROS/Eros_GravitationalPotential"
    };

    static private final String[] modelFilesInPlateFormat = {
        "/EROS/ver64q.tab.gz",
        "/EROS/ver128q.tab.gz",
        "/EROS/ver256q.tab.gz",
        "/EROS/ver512q.tab.gz"
    };

    static private final String[] imageMap = {
        "/EROS/image_map.png"
    };

    static private final double[] referencePotentials = {
        -5.3754128803056872e+01,
        -5.3762823321417372e+01,
        -5.3764665276229927e+01,
        -5.3765039959572114e+01
    };

    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr
    };

    public Eros(IBodyViewConfig config)
    {
        super(config,
                modelNames,
                coloringFiles,
                coloringNames,
                coloringUnits,
                null,
//                imageMap,
                ColoringValueType.CELLDATA,
                false);
    }


    @Override
    public double getDensity()
    {
        return 2.67;
    }

    @Override
    public double getRotationRate()
    {
        return 0.00033116576167064;
    }

    @Override
    public double getReferencePotential()
    {
        return referencePotentials[getModelResolution()];
    }

    @Override
    public String getServerPathToShapeModelFileInPlateFormat()
    {
        return modelFilesInPlateFormat[getModelResolution()];
    }
}
