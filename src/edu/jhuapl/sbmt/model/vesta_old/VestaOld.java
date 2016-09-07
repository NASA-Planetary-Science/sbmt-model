package edu.jhuapl.sbmt.model.vesta_old;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class VestaOld extends SmallBodyModel
{
    static private final String[] modelNames = {
        "VESTA-old"
    };

    static private final String[] modelFiles = {
        "/VESTA_OLD/VESTA.vtk.gz"
    };

    static private final String[] coloringFiles = {
        "/VESTA_OLD/VESTA_Slope",
        "/VESTA_OLD/VESTA_Elevation",
        "/VESTA_OLD/VESTA_GravitationalAcceleration",
        "/VESTA_OLD/VESTA_GravitationalPotential",
        "/VESTA_OLD/VESTA_439",
        "/VESTA_OLD/VESTA_673",
        "/VESTA_OLD/VESTA_953",
        "/VESTA_OLD/VESTA_1042"
    };

    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr, "HST 439 nm", "HST 673 nm", "HST 953 nm", "HST 1042 nm"
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr, "", "", "", ""
    };

    static private final boolean[] coloringHasNulls = {
            false, false, false, false, true, true, true, true
    };

    public VestaOld(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                modelFiles,
                coloringFiles,
                coloringNames,
                coloringUnits,
                coloringHasNulls,
                null,
                ColoringValueType.CELLDATA,
                false);
    }
}
