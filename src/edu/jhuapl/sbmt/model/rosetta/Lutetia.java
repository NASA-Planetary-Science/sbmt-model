package edu.jhuapl.sbmt.model.rosetta;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

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
                coloringFiles,
                coloringNames,
                coloringUnits,
                null,
                null,
                ColoringValueType.CELLDATA,
                false);
    }
}
