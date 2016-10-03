package edu.jhuapl.sbmt.model.europa.projection;

import java.util.Vector;

import edu.jhuapl.sbmt.model.europa.util.Pair;

public class MapProjection {
	public static enum PlanetModels { Ellipsoid, Spherical};
	public static enum ProjectionTypes {
		PolarStereographic, ObliqueStereographic, SimpleCylindrical, EquiRectangular, Orthographic, Unknown
	};

	public static double PI = 3.14159265358979323846;
	public static double PI2 = 1.5707963267948966;
	public static double PI4 = 0.78539816339744833;
	public static double TWOPI = 6.2831853071795864769;
	public static double EPS10 = 0.000000001;

	public PlanetModels planetaryModel = PlanetModels.Ellipsoid;

	protected String mapName = null;
	protected double mapScale = 1.0;
	protected double bodyRadii = 1.0;
	protected XY centerPt = new XY();

	protected boolean isInitialized = false;

	protected Vector<Pair<String,String>> mapParams;

	//
	//	basic keys used by the MapProjection base class - all map
	//	projections should be initialized with these keys
	//
	public static String bodyRadiusKey = "bodyRadiusKey";
	public static String mapScaleKey = "mapScaleKey";
	public static String mapProjectionKey = "mapProjectionKey";

	//
	//	keys for parameters
	//
	public static String projKey = "name";
	public static String zoneKey = "zone";
	public static String phi0key = "phi0";
	public static String phi1key = "phi1";
	public static String phi2key = "phi2";
	public static String lambda0key = "lambda0";
	public static String lat1key = "lat1";
	public static String lat2key = "lat2";
	public static String ellipseKey = "ellipse";
	public static String radiuskey = "radius";
	public static String polarkey = "polar";
	public static String scalekey = "scale";
	public static String equitorialkey = "equitorial";
	public static String flatteningkey = "flattening";
	public static String k0key = "k0";
	public static String bodykey = "bodyName";
	public static String falseEastingKey = "FalseEasting";
	public static String falseNorthingKey = "FalseNorthing";

	//
	//	names for each projection
	//
	public static String UTMProjKey = "UTM";
	public static String StereographicProjKey = "Stereographic";
	public static String OrthographicProjKey = "Orthographic";
	public static String SimpleCylindricalProjKey = "SimpleCylindrical";
	public static String AlbersEqualAreaProjKey = "AlbersEqualArea";
	public static String ObliqueMercatorProjKey = "ObliqueMercator";
	public static String LambertConformalConicProjKey = "LambertConformalConic";

	public MapProjection(Vector<Pair<String,String>> params)
	{
		copyParameters(params);

		Pair<String,String> radiiPair = findParam(bodyRadiusKey, params);
		if (radiiPair != null)
		{
			bodyRadii = Double.parseDouble(radiiPair.second);
		}

		Pair<String,String> scalePair = findParam(mapScaleKey, params);
		if (scalePair != null)
		{
			mapScale = Double.parseDouble(scalePair.second);
		}
	}

	//	this should be implemented for each map projection class
	public void createFromParams(Vector<Pair<String,String>> params)
	{
	}

	//	forward converts from spherical coordinates (latitude, longitude, and implied radius) to
	//	two dimensional map coordinates (typically in meters from a center point)
	//	must be implemented for each projection class derived from this one
	public XY forward(UV uv)
	{
		XY result = null;
		return result;
	}

	public void forward(UV uv, XY result)
	{
	}

	//	inverse converts from two dimensional map coordinates to spherical
	//	coordinates (latitude, longitude, and implied radius)
	//	must be implemented for each projection class derived from this one
	public UV inverse(XY xy)
	{
		UV result = null;
		return result;
	}

	public void inverse(XY xy, UV result)
	{
	}

	//	calls forward for each UV point in the given array
	public Vector<XY> forward(Vector<UV> uvs)
	{
		Vector<XY> results = null;

		int size = uvs.size();
		if (size > 0)
		{
			results = new Vector<XY>(size);

			for (int i = 0; i < size; i++){
				results.add(forward(uvs.get(i)));
			}
		}

		return results;
	}

	//	calls inverse for each XY point in the provided array
	public Vector<UV> inverse(Vector<XY> xys)
	{
		Vector<UV> results = null;

		int size = xys.size();
		if (size > 0)
		{
			results = new Vector<UV>(size);

			for (int i = 0; i < size; i++){
				results.add(inverse(xys.get(i)));
			}
		}

		return results;
	}

	public IJ worldToImage(XY pt)
	{
		IJ result = new IJ();

		result.i = (int)(0.50 + pt.x / mapScale);
		result.j = (int)(0.50 + pt.y / mapScale);

		return result;
	}

	public void worldToImage(XY pt, IJ result)
	{
		result.i = (int)(0.50 + pt.x / mapScale);
		result.j = (int)(0.50 + pt.y / mapScale);
	}

	public XY imageToWorld(IJ pt)
	{
		XY result = new XY();

		result.x = (double)pt.i * mapScale;
		result.y = (double)pt.j * mapScale;

		return result;
	}

	public void imageToWorld(IJ pt, XY result)
	{
		result.x = (double)pt.i * mapScale;
		result.y = (double)pt.j * mapScale;
	}

	public String getName(){ return mapName;}
	public void setName(String name){ mapName = name;}

	public void setMapScale(double scale){ mapScale = scale;}
	public double getMapScale(){ return mapScale;}

	public XY getCenter(){ return centerPt;}

	public void copyParameters(Vector<Pair<String,String>> params)
	{
		int size = params.size();
		mapParams = new Vector<Pair<String,String>>((size>0) ? size : 1);

		for (int i = 0; i < size; i++){
			Pair<String,String> strPair = new Pair<String,String>(new String(params.get(i).first), new String(params.get(i).second));
			mapParams.add(strPair);
		}
	}

	public static Pair<String,String> findParam(String paramName, Vector<Pair<String,String>> params)
	{
		Pair<String,String> result = null;

		String key = paramName.trim().toUpperCase();

		int size = params.size();

		for (int i = 0; (i < size) && (result == null); i++){
			if (key.equals(params.get(i).first.trim().toUpperCase()))
			{
				result = params.get(i);
			}
		}

		return result;
	}
}
