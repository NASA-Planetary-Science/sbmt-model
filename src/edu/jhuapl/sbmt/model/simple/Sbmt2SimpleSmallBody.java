package edu.jhuapl.sbmt.model.simple;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class Sbmt2SimpleSmallBody extends SmallBodyModel
{
    public Sbmt2SimpleSmallBody(SmallBodyViewConfig config)
    {
        super(config);
        initializeConfigParameters(
                null,
                false);
    }

	@Override
    public double getDensity()
    {
		return ((SmallBodyViewConfig)getSmallBodyConfig()).density;
    }

	@Override
    public double getRotationRate()
    {
		return ((SmallBodyViewConfig)getSmallBodyConfig()).rotationRate;
    }

}