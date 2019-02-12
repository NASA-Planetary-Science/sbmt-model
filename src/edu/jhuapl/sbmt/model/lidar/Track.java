package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Track
{
	// State vars
	public int startId = -1;
	public int stopId = -1;
	public boolean isVisible;
	public Color color;
	List<Integer> sourceFiles;
	public String[] timeRange;
	List<Map<Integer, String>> fileMaps;

	Track()
	{
		startId = -1;
		stopId = -1;

		isVisible = true;
		color = Color.BLUE;

		sourceFiles = new ArrayList<>();
		timeRange = new String[] { "", "" };
		fileMaps = new ArrayList<>();
	}

	/**
	 * Returns the Color associated with the Track.
	 */
	public Color getColor()
	{
		return color;
	}

	public int getNumberOfPoints()
	{
		return stopId - startId + 1;
	}

	public boolean containsId(int id)
	{
		return startId >= 0 && stopId >= 0 && id >= startId && id <= stopId;
	}

	public int getNumberOfSourceFiles()
	{
		return sourceFiles.size();
	}

	public String getSourceFileName(int i)
	{
		return fileMaps.get(i).get(sourceFiles.get(i));
	}

	/**
	 * Returns true if the track is visible.
	 */
	public boolean getIsVisible()
	{
		return isVisible;
	}

	public void registerSourceFileIndex(int fileNum, Map<Integer, String> fileMap)
	{
		if (!sourceFiles.contains(fileNum))
		{
			sourceFiles.add(fileNum);
			fileMaps.add(fileMap);
		}
	}

//    public LidarPoint getPoint(int i)
//    {
//        return this.lidarSearchDataCollection.originalPoints.get(startId+i);
//    }

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + startId;
		result = prime * result + stopId;
		result = prime * result + Arrays.hashCode(timeRange);
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
		Track other = (Track) obj;
		if (startId != other.startId)
			return false;
		if (stopId != other.stopId)
			return false;
		if (!Arrays.equals(timeRange, other.timeRange))
			return false;
		return true;
	}

}