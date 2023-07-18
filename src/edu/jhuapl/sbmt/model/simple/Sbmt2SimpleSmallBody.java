package edu.jhuapl.sbmt.model.simple;

import edu.jhuapl.saavtk.config.IBodyViewConfig;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;

public class Sbmt2SimpleSmallBody extends SmallBodyModel
{
	public Sbmt2SimpleSmallBody(IBodyViewConfig config)
	{
		super(config);
		initializeConfigParameters(null, false);
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