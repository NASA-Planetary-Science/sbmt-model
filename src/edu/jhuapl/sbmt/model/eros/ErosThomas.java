package edu.jhuapl.sbmt.model.eros;

import edu.jhuapl.sbmt.common.client.SmallBodyModel;
import edu.jhuapl.sbmt.common.client.SmallBodyViewConfig;

public class ErosThomas extends SmallBodyModel
{
    static private final String[] modelNames = {
            "433 EROS PLATE MODEL MSI 1708",
            "433 EROS PLATE MODEL MSI 7790",
            "433 EROS PLATE MODEL MSI 10152",
            "433 EROS PLATE MODEL MSI 22540",
            "433 EROS PLATE MODEL MSI 89398",
            "433 EROS PLATE MODEL MSI 200700"
    };

    static private final String[] coloringFiles = {
            "/THOMAS/EROS/Slope",
            "/THOMAS/EROS/Elevation",
            "/THOMAS/EROS/GravitationalAcceleration",
            "/THOMAS/EROS/GravitationalPotential"
    };

    static private final String[] imageMap = {
        "/THOMAS/EROS/image_map.png"
    };

    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr
    };

    public ErosThomas(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                coloringFiles,
                coloringNames,
                coloringUnits,
                null,
                imageMap,
                ColoringValueType.CELLDATA,
                false);
    }
}
