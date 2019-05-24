package edu.jhuapl.sbmt.model.lidar;

import edu.jhuapl.sbmt.util.TimeUtil;

/**
 * Immutable object that contains information about a single lidar file.
 *
 * @author lopeznr1
 */
public class LidarFileSpec
{
	// Attributes
	private final String path;
	private final String name;
	private final double timeBeg;
	private final double timeEnd;

	/**
	 * Standard Constructor
	 */
	public LidarFileSpec(String aPath, String aName, double aTimeBeg, double aTimeEnd)
	{
		path = aPath;
		name = aName;
		timeBeg = aTimeBeg;
		timeEnd = aTimeEnd;
	}

	public String getName()
	{
		return name;
	}

	public String getPath()
	{
		return path;
	}

	public double getTimeBeg()
	{
		return timeBeg;
	}

	public double getTimeEnd()
	{
		return timeEnd;
	}

	@Override
	public String toString()
	{
		return TimeUtil.et2str(timeBeg) + " - " + TimeUtil.et2str(timeEnd);
	}
}