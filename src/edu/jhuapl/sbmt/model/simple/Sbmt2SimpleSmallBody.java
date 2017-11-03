package edu.jhuapl.sbmt.model.simple;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class Sbmt2SimpleSmallBody extends SmallBodyModel
{
    static private String[] getImageMap(SmallBodyViewConfig config)
    {
        return new String[] { config.serverPath("basemap/image_map.png") };
    }

    public Sbmt2SimpleSmallBody(SmallBodyViewConfig config)
    {
        super(config);
        initializeConfigParameters(
                config.hasImageMap ? getImageMap(config) : null,
                false);
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
}
