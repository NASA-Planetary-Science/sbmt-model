package edu.jhuapl.sbmt.model.simple;

import java.io.File;

import edu.jhuapl.sbmt.app.SmallBodyModel;
import edu.jhuapl.sbmt.app.SmallBodyViewConfig;

public class SimpleSmallBody extends SmallBodyModel
{
    static private String[] getColoringFiles(String path)
    {
        return new String[] {
                new File(path).getParent() + "/Slope",
                new File(path).getParent() + "/Elevation",
                new File(path).getParent() + "/GravitationalAcceleration",
                new File(path).getParent() + "/GravitationalPotential"
        };
    }

    static private String[] getImageMap(SmallBodyViewConfig config)
    {
        System.out.println((new File(config.rootDirOnServer)).getParent() + "/image_map.png");
        return new String[] {(new File(config.rootDirOnServer)).getParent() + "/image_map.png"};
    }

    static private final String[] coloringNames = {
        SlopeStr, ElevStr, GravAccStr, GravPotStr
    };

    static private final String[] coloringUnits = {
        SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr
    };

    public SimpleSmallBody(
            SmallBodyViewConfig config,
            String[] modelNames,
            String[] paths)
    {
        super(config,
                modelNames,
                paths,
                config.hasColoringData ? getColoringFiles(paths[0]) : null,
                config.hasColoringData ? coloringNames : null,
                config.hasColoringData ? coloringUnits : null,
                null,
                config.hasImageMap ? getImageMap(config) : null,
                ColoringValueType.CELLDATA,
                false);
    }

    public SimpleSmallBody(SmallBodyViewConfig config)
    {
        super(config,
                new String[] {config.body.toString()},
                new String[] {config.rootDirOnServer},
                config.hasColoringData ? getColoringFiles(config.rootDirOnServer) : null,
                config.hasColoringData ? coloringNames : null,
                config.hasColoringData ? coloringUnits : null,
                null,
                config.hasImageMap ? getImageMap(config) : null,
                ColoringValueType.CELLDATA,
                false);
    }
}
