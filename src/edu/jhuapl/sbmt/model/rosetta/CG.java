package edu.jhuapl.sbmt.model.rosetta;

import edu.jhuapl.sbmt.app.SmallBodyModel;
import edu.jhuapl.sbmt.app.SmallBodyViewConfig;

public class CG extends SmallBodyModel
{
    static private final String[] modelNames = {
        "67P 17442",
        "67P 72770",
        "67P 298442",
        "67P 1214922",
        "67P 4895631",
        "67P 16745283"
    };

    static private final String[] modelFiles = {
        "/DLR/67P/cg-dlr_spg-shap4s-v0.9_64m.ply.gz",
        "/DLR/67P/cg-dlr_spg-shap4s-v0.9_32m.ply.gz",
        "/DLR/67P/cg-dlr_spg-shap4s-v0.9_16m.ply.gz",
        "/DLR/67P/cg-dlr_spg-shap4s-v0.9_8m.ply.gz",
        "/DLR/67P/cg-dlr_spg-shap4s-v0.9_4m.ply.gz",
        "/DLR/67P/cg-dlr_spg-shap4s-v0.9.ply.gz"
    };
/*
    static private final String[] coloringFiles = {
            "/DLR/67P/Slope",
            "/DLR/67P/Elevation",
            "/DLR/67P/GravitationalAcceleration",
            "/DLR/67P/GravitationalPotential"
    };

    static private final String[] coloringNames = {
            SlopeStr, ElevStr, GravAccStr, GravPotStr
    };

    static private final String[] coloringUnits = {
            SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr
    };
*/
    public CG(SmallBodyViewConfig config)
    {
        super(config,
                modelNames,
                modelFiles,
                null,
                null,
                null,
                null,
                null,
                ColoringValueType.CELLDATA,
                false);
    }
}
