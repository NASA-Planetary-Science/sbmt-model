package edu.jhuapl.sbmt.model.simple;

import java.io.File;

import edu.jhuapl.saavtk.config.IBodyViewConfig;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;

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

    static private String[] getImageMap(IBodyViewConfig config)
    {
        System.out.println((new File(config.getRootDirOnServer())).getParent() + "/image_map.png");
        return new String[] {(new File(config.getRootDirOnServer())).getParent() + "/image_map.png"};
    }

    static private final String[] coloringNames = {
        SlopeStr, ElevStr, GravAccStr, GravPotStr
    };

    static private final String[] coloringUnits = {
        SlopeUnitsStr, ElevUnitsStr, GravAccUnitsStr, GravPotUnitsStr
    };

    public SimpleSmallBody(
            IBodyViewConfig config,
            String[] modelNames)
    {
        super(config,
                modelNames,
                config.hasColoringData() ? getColoringFiles(config.getShapeModelFileNames()[0]) : null,
                config.hasColoringData() ? coloringNames : null,
                config.hasColoringData() ? coloringUnits : null,
                null,
                ColoringValueType.CELLDATA,
                false);
    }

    public SimpleSmallBody(IBodyViewConfig config)
    {
        super(config,
                new String[] {config.getBody().toString()},
                config.hasColoringData() ? getColoringFiles(config.getRootDirOnServer()) : null,
                config.hasColoringData() ? coloringNames : null,
                config.hasColoringData() ? coloringUnits : null,
                null,
                ColoringValueType.CELLDATA,
                false);
    }

    @Override
    public double getDensity()
    {
    	return getConfig().getDensity();
    }

    @Override
    public double getRotationRate()
    {
    	return getConfig().getRotationRate();
    }
}
