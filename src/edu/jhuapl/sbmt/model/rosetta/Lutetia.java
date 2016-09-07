package edu.jhuapl.sbmt.model.rosetta;

import edu.jhuapl.sbmt.app.SmallBodyModel;
import edu.jhuapl.sbmt.app.SmallBodyViewConfig;

public class Lutetia extends SmallBodyModel
{
    static private final String[] modelNames = {
        "LUTETIA 2962",
        "LUTETIA 5824",
        "LUTETIA 11954",
        "LUTETIA 24526",
        "LUTETIA 47784",
        "LUTETIA 98280",
        "LUTETIA 189724",
        "LUTETIA 244128",
        "LUTETIA 382620",
        "LUTETIA 784510",
        "LUTETIA 1586194",
        "LUTETIA 3145728"
    };

    static private final String[] modelFiles = {
        "/JORDA/LUTETIA/shape_res0.vtk.gz",
        "/JORDA/LUTETIA/shape_res1.vtk.gz",
        "/JORDA/LUTETIA/shape_res2.vtk.gz",
        "/JORDA/LUTETIA/shape_res3.vtk.gz",
        "/JORDA/LUTETIA/shape_res4.vtk.gz",
        "/JORDA/LUTETIA/shape_res5.vtk.gz",
        "/JORDA/LUTETIA/shape_res6.vtk.gz",
        "/JORDA/LUTETIA/shape_res7.vtk.gz",
        "/JORDA/LUTETIA/shape_res8.vtk.gz",
        "/JORDA/LUTETIA/shape_res9.vtk.gz",
        "/JORDA/LUTETIA/shape_res10.vtk.gz",
        "/JORDA/LUTETIA/shape_res11.vtk.gz"
    };

    static private final String[] coloringFiles = {
            "/JORDA/LUTETIA/Slope",
            "/JORDA/LUTETIA/Elevation",
            "/JORDA/LUTETIA/GravitationalAcceleration",
            "/JORDA/LUTETIA/GravitationalPotential"
    };

    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr
    };

    public Lutetia(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                modelFiles,
                coloringFiles,
                coloringNames,
                coloringUnits,
                null,
                null,
                ColoringValueType.CELLDATA,
                false);
    }
}
