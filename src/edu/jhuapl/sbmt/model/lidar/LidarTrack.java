package edu.jhuapl.sbmt.model.lidar;

import java.util.List;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.sbmt.lidar.LidarPoint;

/**
 * Immutable object that defines a "track" of LidarPoints.
 * <P>
 * Each created Track should have unique id which is used to differentiate
 * between other Tracks. No attempt is made to evaluate the LidarPoints when
 * determining if 2 Tracks are equal - only the uniqueId is utilized.
 *
 * @author lopeznr1
 */
public class LidarTrack
{
	// Attributes
	private final int uniqueId;
	private final ImmutableList<LidarPoint> pointL;
	private final ImmutableList<String> sourceL;

	/**
	 * Standard Constructor
	 */
	public LidarTrack(int aUniqueId, List<LidarPoint> aPointL, List<String> aSourceL)
	{
		uniqueId = aUniqueId;
		pointL = ImmutableList.copyOf(aPointL);
		sourceL = ImmutableList.copyOf(aSourceL);
	}

	/**
	 * Returns the unique id assigned to this Track
	 */
	public int getId()
	{
		return uniqueId;
	}

	/**
	 * Returns the list of LidarPoints that are in the Track.
	 */
	public ImmutableList<LidarPoint> getPointList()
	{
		return pointL;
	}

	/**
	 * Returns the time corresponding to the first LidarPoint.
	 */
	public double getTimeBeg()
	{
		if (pointL.size() == 0)
//			return Double.NaN;
			return Double.NEGATIVE_INFINITY;

		return pointL.get(0).getTime();
	}

	/**
	 * Returns the time corresponding to the first LidarPoint.
	 */
	public double getTimeEnd()
	{
		if (pointL.size() == 0)
			return Double.NaN;

		int lastIdx = pointL.size() - 1;
		return pointL.get(lastIdx).getTime();
	}

	/**
	 * returns the number of points in the lidar track.
	 */
	public int getNumberOfPoints()
	{
		return pointL.size();
	}

	/**
	 * Returns a list of sources that were used to synthesize this Track.
	 */
	public ImmutableList<String> getSourceList()
	{
		return sourceL;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + uniqueId;
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
		LidarTrack other = (LidarTrack) obj;
		if (uniqueId != other.uniqueId)
			return false;
		return true;
	}

}