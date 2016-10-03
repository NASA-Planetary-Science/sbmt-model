package edu.jhuapl.sbmt.model.europa.util;

import edu.jhuapl.sbmt.model.europa.projection.XY;


public class GeoBox {
	public float north = -100.0f;
	public float south = 100.0f;
	public float east = -370.0f;
	public float west = 370.0f;
	public boolean validBox = false;
	public GeoBox()
	{
	}
	public GeoBox(float n, float s, float e, float w)
	{
		CreateBox(n, s, e, w);
	}
	public GeoBox(double n, double s, double e, double w)
	{
		CreateBox((float)n, (float)s, (float)e, (float)w);
	}
	public void CreateBox(float n, float s, float e, float w)
	{
		if ((n >= -90.0f) && (n <= 90.0f))
			north = n;
		if ((s >= -90.0f) && (s <= 90.0f))
			south = s;
		if ((e >= -360.0f) && (e <= 360.0f))
			east = e;
		if ((w >= -360.0f) && (w <= 360.0f))
			west = w;

		validBox = true;
	}
	public void CreateBox(double n, double s, double e, double w)
	{
		if ((n >= -90.0f) && (n <= 90.0f))
			north = (float)n;
		if ((s >= -90.0f) && (s <= 90.0f))
			south = (float)s;
		if ((e >= -360.0f) && (e <= 360.0f))
			east = (float)e;
		if ((w >= -360.0f) && (w <= 360.0f))
			west = (float)w;

		validBox = true;
	}
	public void Copy(GeoBox box)
	{
		north = box.north;
		south = box.south;
		east = box.east;
		west = box.west;
		validBox = true;
	}
	public void getPosition(float lat, float lon, XY xy)
	{
		xy.x = (lon - west)/(east - west);
		xy.y = (north - lat)/(north - south);
	}
	public boolean IsInside(float lat, float lon)
	{
		boolean result = false;

		while (lon > 360.0) lon -= 360.0;
		while (lon < 0.0) lon += 360.0;

		if ((lat <= north) && (lat >= south))
		{
			if ((lon >= west) && (lon <= east))
			{
				result = true;
			}
		}

		return result;
	}
	public boolean Overlaps(GeoBox box)
	{
		if (box.north < south) return false;
		if (box.south > north) return false;
		if (box.west > east) return false;
		if (box.east < west) return false;
		return true;
	}
	public void expand(float lat, float lon)
	{
		if ((lat >= -90.0f) && (lat <= 90.0f))
		{
			if (lat > north) north = lat;
			if (lat < south) south = lat;
		}
		if ((lon >= -360.0f) && (lon <= 360.0f))
		{
			if (lon > east) east = lon;
			if (lon < west) west = lon;
		}

		validBox = true;
	}
	public boolean isValid()
	{
		boolean result = false;

		if (validBox)
		{
			if (north > 90.0f)
			{
				result = false;
			}
			else if (north < -90.0f)
			{
				result = false;
			}
			else if (south > 90.0f)
			{
				result = false;
			}
			else if (south < -90.0f)
			{
				result = false;
			}
			else if (west < -360.0f)
			{
				result = false;
			}
			else if (west > 360.0f)
			{
				result = false;
			}
			else if (east < -360.0f)
			{
				result = false;
			}
			else if (east > 360.0f)
			{
				result = false;
			}
		}

		return result;
	}
	public float getCenterLat()
	{
		return (north+south)/2.0f;
	}
}
