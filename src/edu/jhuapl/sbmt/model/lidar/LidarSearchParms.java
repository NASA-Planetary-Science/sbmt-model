package edu.jhuapl.sbmt.model.lidar;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import edu.jhuapl.saavtk.model.LidarDataSource;

/**
 * Immutable class that defines the parameters for a specific search query.
 *
 * @author lopeznr1
 */
public class LidarSearchParms
{
	// Attributes
	private final LidarDataSource dataSource;

	private final double begTime;
	private final double endTime;

	private final double minRange;
	private final double maxRange;

	private final int minTrackLen;
	private final double timeSeparationBetweenTracks;

	private final ImmutableSet<Integer> cubeSet;

	/**
	 * Standard Constructor
	 */
	public LidarSearchParms(LidarDataSource aDataSource, double aBegTime, double aEndTime, double aMinRange, double aMaxRange,
			double aTimeSeparationBetweenTracks, int aMinTrackLen, Set<Integer> aCubeSet)
	{
		dataSource = aDataSource;

		begTime = aBegTime;
		endTime = aEndTime;

		minRange = aMinRange;
		maxRange = aMaxRange;

		minTrackLen = aMinTrackLen;
		timeSeparationBetweenTracks = aTimeSeparationBetweenTracks;

		cubeSet = ImmutableSet.copyOf(aCubeSet);
	}

	public LidarDataSource getDataSource()
	{
		return dataSource;
	}

	public double getBegTime()
	{
		return begTime;
	}

	public double getEndTime()
	{
		return endTime;
	}

	public double getMinRange()
	{
		return minRange;
	}

	public double getMaxRange()
	{
		return maxRange;
	}

	public int getMinTrackLen()
	{
		return minTrackLen;
	}

	public double getTimeSeparationBetweenTracks()
	{
		return timeSeparationBetweenTracks;
	}

	public ImmutableSet<Integer> getCubeSet()
	{
		return cubeSet;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
		long temp;
		temp = Double.doubleToLongBits(begTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(endTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxRange);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minRange);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + minTrackLen;
		temp = Double.doubleToLongBits(timeSeparationBetweenTracks);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((cubeSet == null) ? 0 : cubeSet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LidarSearchParms other = (LidarSearchParms) obj;
		if (dataSource == null)
		{
			if (other.dataSource != null)
				return false;
		}
		else if (!dataSource.equals(other.dataSource))
			return false;
		if (Double.doubleToLongBits(begTime) != Double.doubleToLongBits(other.begTime))
			return false;
		if (Double.doubleToLongBits(endTime) != Double.doubleToLongBits(other.endTime))
			return false;
		if (Double.doubleToLongBits(maxRange) != Double.doubleToLongBits(other.maxRange))
			return false;
		if (Double.doubleToLongBits(minRange) != Double.doubleToLongBits(other.minRange))
			return false;
		if (minTrackLen != other.minTrackLen)
			return false;
		if (Double.doubleToLongBits(timeSeparationBetweenTracks) != Double
				.doubleToLongBits(other.timeSeparationBetweenTracks))
			return false;
		if (cubeSet == null)
		{
			if (other.cubeSet != null)
				return false;
		}
		else if (!cubeSet.equals(other.cubeSet))
			return false;
		return true;
	}

}
