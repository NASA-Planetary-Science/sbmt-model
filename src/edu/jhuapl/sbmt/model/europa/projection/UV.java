package edu.jhuapl.sbmt.model.europa.projection;

public class UV {
	public double lat = 0.0;
	public double lon = 0.0;
	public UV(){}
	public UV(double x1, double x2){ lat = x1; lon = x2;}
	public UV(UV uv){ lat = uv.lat; lon = uv.lon;}
	public void create(double latRad, double lonRad)
	{
		lat = latRad;
		lon = lonRad;
	}
	public void fromDegrees(double latDeg, double lonDeg)
	{
		lat = (latDeg * MapProjection.PI)/180.0;
		lon = (lonDeg * MapProjection.PI)/180.0;
	}
	public double getLatDegrees(){ return (180.0 * lat/MapProjection.PI);}
	public double getLonDegrees(){ return (180.0 * lon/MapProjection.PI);}
	public boolean equals(UV uv2)
	{
		boolean result = false;
		
		double deltaLat = Math.abs(uv2.lat - lat);
		
		if (deltaLat < 1.0e-7)
		{
			double deltaLon = Math.abs(uv2.lon - lon);
			if (deltaLon < 1.0e-7)
			{
				result = true;
			}
		}
		
		return result;
	}
	public void copy(UV uv)
	{
		lat = uv.lat;
		lon = uv.lon;
	}
}
