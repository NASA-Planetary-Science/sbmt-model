package edu.jhuapl.sbmt.model.simple;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class Sbmt2SimpleSmallBody extends SmallBodyModel
{

	static private String[] getImageMapPaths(SmallBodyViewConfig config)
    {
    	final String[] baseMapNames = config.imageMaps;
    	String[] paths;

    	if (baseMapNames != null)
    	{
    		paths = new String[baseMapNames.length];
    		for (int index = 0; index < baseMapNames.length; ++index)
    		{
    			paths[index] = config.serverPath(baseMapNames[index]);
    		}
    	}
    	else
    	{
    		paths = new String[] { config.serverPath("basemap/image_map.png") };
    	}

    	return paths;
    }

	public Sbmt2SimpleSmallBody(SmallBodyViewConfig config)
    {
        super(config);
        initializeConfigParameters(
                config.hasImageMap ? getImageMapPaths(config) : null,
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