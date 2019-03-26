package edu.jhuapl.sbmt.model.lidar;

public enum TrackFileType
{
	LIDAR_ONLY("Lidar Point Only (X,Y,Z)"),
	LIDAR_WITH_INTENSITY("Lidar Point (X,Y,Z) with Intensity (Albedo)"),
	TIME_WITH_LIDAR("Time, Lidar Point (X,Y,Z)"),
	TIME_LIDAR_RANGE("Time, Lidar (X,Y,Z), Range"),
	TIME_LIDAR_ALBEDO("Time, Lidar (X,Y,Z), Albedo"),
	LIDAR_SC("Lidar (X,Y,Z), S/C (SCx, SCy, SCz)"),
	TIME_LIDAR_SC("Time, Lidar (X,Y,Z), S/C (SCx, SCy, SCz)"),
	TIME_LIDAR_SC_ALBEDO("Time, Lidar (X,Y,Z), S/C (SCx, SCy, SCz), Albedo"),
	BINARY("Binary"),
	OLA_LEVEL_2("OLA Level 2"),
	HAYABUSA2_LEVEL_2("Hayabusa2 Level 2"),
	PLY("PLY");

	private String name;

	private TrackFileType(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public static TrackFileType find(String name)
	{
		for (TrackFileType type : values())
		{
			if (name == type.getName())
				return type;
		}
		return null;
	}

	public static String[] names()
	{
		String[] titles = new String[values().length];
		int i = 0;
		for (TrackFileType type : values())
		{
			titles[i++] = type.getName();
		}
		return titles;
	}
}