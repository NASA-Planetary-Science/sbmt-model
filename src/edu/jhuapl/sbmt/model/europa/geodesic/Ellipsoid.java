package edu.jhuapl.sbmt.model.europa.geodesic;

import edu.jhuapl.sbmt.model.europa.math.MathToolkit;
import edu.jhuapl.sbmt.model.europa.math.V3;
import edu.jhuapl.sbmt.model.europa.math.VectorOps;
import edu.jhuapl.sbmt.model.europa.projection.GeometricConstants;
import edu.jhuapl.sbmt.model.europa.projection.UV;
import edu.jhuapl.sbmt.model.europa.util.Pair;

//
//	For this class there is an assumption of the following body fixed coordinate system:
//	X-axis runs from the center of the body through the prime meridian at the equator (Africa)
//	Y-axis runs from the center of the body through the north pole
//	Z-axis is (X cross Y) and runs from the center of the body through 90 degrees west longitude (south of Texas)
//
public class Ellipsoid {
	//	semimajor axis in kilometers, and flattening
	public double a = 1.0;
	public double f = 0.0;
	public Ellipsoid(double semiMajorAxis, double flattening)
	{
		a = semiMajorAxis;
		f = flattening;
	}
	public Ellipsoid(Ellipsoid ell)
	{
		copy(ell);
	}
	public void copy(Ellipsoid ell)
	{
		a = ell.a;
		f = ell.f;
	}
	public double getSemiMajorAxis(){ return a;}
	public double getFlattening(){ return f;}
	public static Ellipsoid createEllipsoid(String name)
	{
		Ellipsoid result = new Ellipsoid(Ellipsoid.getBaseRadiusKm(name), Ellipsoid.getFlattening(name));
		return result;
	}
	public static double getFlattening(String name)
	{
		double result = 0.0;

		String upName = name.trim().toUpperCase();

		if (upName.equals("MOLA") || upName.equals("MARS"))
		{
			result = (3396.19 - 3376.2)/3396.19;
		}
		else if (upName.startsWith("MOON") || upName.startsWith("LUNAR"))
		{
			result = 0.0;
		}
		else if (upName.equals("AIRY1830"))
		{
			result = 1.0/299.32;
		}
		else if (upName.equals("AUSTRALIAN1965"))
		{
			result = 1.0/298.250;
		}
		else if (upName.equals("BESSEL1841"))
		{
			result = 1.0/299.1528128;
		}
		else if (upName.equals("CLARKE1866"))
		{
			result = 1.0/294.9786982;
		}
		else if (upName.equals("GRS1980"))
		{
			result = 1.0/298.257;
		}
		else if (upName.equals("WGS72"))
		{
			result = 1.0/298.260;
		}
		else if (upName.equals("WGS84"))
		{
			result = 1.0/298.2572221;
		}

		return result;
	}
	public static double getBaseRadiusKm(String name)
	{
		double result = 1.0;

		String upName = name.trim().toUpperCase();

		if (upName.equals("MOLA") || upName.equals("MARS"))
		{
			result = 3396.19;
		}
		else if (upName.startsWith("MOON") || upName.startsWith("LUNAR"))
		{
			result = 1737.40;
		}
		else if (upName.equals("AIRY1830"))
		{
			result = 6777.5634;
		}
		else if (upName.equals("AUSTRALIAN1965"))
		{
			result = 6378.1600;
		}
		else if (upName.equals("BESSEL1841"))
		{
			result = 6377.397155;
		}
		else if (upName.equals("CLARKE1866"))
		{
			result = 6378.2064;
		}
		else if (upName.equals("GRS1980"))
		{
			result = 6378.1370;
		}
		else if (upName.equals("WGS72"))
		{
			result = 6378.1370;
		}
		else if (upName.equals("WGS84"))
		{
			result = 6378.1370;
		}

		return result;
	}
	public static void latLongToV3(double latRad, double lonRad, double radius, V3 result)
	{
		result.v[0] = radius * Math.cos(latRad) * Math.cos(-lonRad);
		result.v[1] = radius * Math.sin(latRad);
		result.v[2] = radius * Math.cos(latRad) * Math.sin(-lonRad);
	}
	public static void latLongToV3(UV uv, double radius, V3 result)
	{
		result.v[0] = radius * Math.cos(uv.lat) * Math.cos(-uv.lon);
		result.v[1] = radius * Math.sin(uv.lat);
		result.v[2] = radius * Math.cos(uv.lat) * Math.sin(-uv.lon);
	}
	public static void xyzToLatLong(V3 xyz, UV uv)
	{
		xyzToLatLong(xyz.X1(), xyz.X2(), xyz.X3(), uv);
	}
	public static void xyzToLatLong(double x, double y, double z, UV uv)
	{
		double vmax = MathToolkit.MaxAbs( x, MathToolkit.MaxAbs( z, y ));

		if ( vmax > 0.0)
		{
			double x1        = x / vmax;
			double y1        = z / vmax;
			double z1        = y / vmax;

			//R = sqrt(x*x + y*y + z*z);
			uv.lat = Math.atan2(z1, Math.sqrt( x1*x1 + y1*y1 ) );

			if ((x1 == 0.0) && (y1 == 0.0))
			{
				uv.lon = 0.0;
			}
			else
			{
				uv.lon = -Math.atan2(y1, x1);
			}
		}
		else
		{
			uv.lat = uv.lon = 0.0;
		}
	}
	public static void xyzToLatLong(V3 xyz, Pair<Double,UV> pair)
	{
		double x = xyz.X1();
		double y = xyz.X2();
		double z = xyz.X3();

		double vmax = MathToolkit.MaxAbs( x, MathToolkit.MaxAbs( z, y ));

		if ( vmax > 0.0)
		{
			double x1        = x / vmax;
			double y1        = z / vmax;
			double z1        = y / vmax;

			pair.first = new Double(Math.sqrt(x*x + y*y + z*z));
			pair.second.lat = Math.atan2(z1, Math.sqrt( x1*x1 + y1*y1 ) );

			if ((x1 == 0.0) && (y1 == 0.0))
			{
				pair.second.lon = 0.0;
			}
			else
			{
				pair.second.lon = -Math.atan2(y1, x1);
			}
		}
		else
		{
			pair.second.lat = pair.second.lon = 0.0;
		}
	}
	public static V3 native2Spice(V3 v)
	{
		V3 result = new V3(v.X1(), -v.X3(), v.X2());
		return result;
	}
	public static V3 native2Spice(V3 v, V3 result)
	{
		result.create(v.X1(), -v.X3(), v.X2());
		return result;
	}
	public static V3 spice2Native(V3 v)
	{
		V3 result = new V3(v.X1(), v.X3(), -v.X2());
		return result;
	}
	public static void spice2Native(V3 v, V3 result)
	{
		result.create(v.X1(), v.X3(), -v.X2());
	}
	public static void calculateDerivedData(UV siteUV, V3 siteUnitVec, V3 siteNorthVec)
	{
		V3 yAxis = new V3(0.0,1.0,0.0);
		V3 xAxis = new V3(1.0,0.0,0.0);
		V3 lonAxis = VectorOps.ArbitraryRotate(xAxis, siteUV.lon, yAxis);
		V3 crossAxis = VectorOps.Cross(lonAxis, yAxis);
		VectorOps.ArbitraryRotate(yAxis, siteUV.lat, crossAxis, siteNorthVec);
		VectorOps.MakeUnit(siteNorthVec);

		Ellipsoid.latLongToV3(siteUV, 1.0, siteUnitVec);
		VectorOps.MakeUnit(siteUnitVec);
	}
	public static void calculateClosestApproachPoint(V3 pos, V3 lookDir, V3 perpPt)
	{
		V3 lookUnit = VectorOps.Unit(lookDir);
		V3 posUnit = VectorOps.Unit(pos);

		double dotProd = VectorOps.Dot(lookUnit, posUnit);
		if (dotProd > 0.0)
		{
			perpPt.create(pos.X1(), pos.X2(), pos.X3());
		}
		else if (Math.abs(dotProd + 1.0) < GeometricConstants.EPS5)
		{
			perpPt.create(0.0, 0.0, 0.0);
		}
		else
		{
			V3 negPosUnit = VectorOps.Negative(posUnit);
			VectorOps.MakeUnit(negPosUnit);
			V3 rotAxis = VectorOps.Cross(lookUnit, negPosUnit);
			VectorOps.MakeUnit(rotAxis);
			double angle = VectorOps.AngularSep(negPosUnit, lookUnit);
			double perpDist = VectorOps.Mag(pos) * Math.sin(angle);
			VectorOps.ArbitraryRotate(posUnit, GeometricConstants.PIOVER2 - angle, rotAxis, perpPt);
			VectorOps.MakeUnit(perpPt);
			VectorOps.Scale(perpPt, perpDist, perpPt);
		}
	}
	public static boolean calculateApproachPoint(V3 pos, V3 lookDir, double rMax, V3 maxPt)
	{
		boolean result = false;

		V3 lookUnit = VectorOps.Unit(lookDir);
		V3 posUnit = VectorOps.Unit(pos);

		double dotProd = VectorOps.Dot(lookUnit, posUnit);
		if (dotProd > 0.0)
		{
		}
		else if (Math.abs(dotProd + 1.0) < GeometricConstants.EPS7)
		{
			maxPt.copy(pos);
			VectorOps.MakeUnit(maxPt);
			VectorOps.Scale(maxPt, rMax);
			result = true;
		}
		else
		{
			V3 negPosUnit = VectorOps.Negative(posUnit);
			VectorOps.MakeUnit(negPosUnit);
			V3 rotAxis = VectorOps.Cross(lookUnit, negPosUnit);
			VectorOps.MakeUnit(rotAxis);
			double angle = VectorOps.AngularSep(negPosUnit, lookUnit);
			double perpDist = VectorOps.Mag(pos) * Math.sin(angle);
			if (perpDist < rMax)
			{
				V3 perpPt = new V3();
				VectorOps.ArbitraryRotate(posUnit, GeometricConstants.PIOVER2 - angle, rotAxis, perpPt);
				VectorOps.MakeUnit(perpPt);
				VectorOps.Scale(perpPt, perpDist, perpPt);
				V3 vToPerp = VectorOps.Subtract(perpPt, pos);
				double distToPerp = VectorOps.Mag(vToPerp);

				double alpha = Math.acos(perpDist/rMax);
				double backDist = rMax * Math.sin(alpha);
				V3 lookV = new V3();
				lookV.copy(lookDir);
				VectorOps.MakeUnit(lookV);
				lookV = VectorOps.Scale(lookV, distToPerp - backDist);
				VectorOps.Add(pos, lookV, maxPt);

				result = true;
			}
		}

		return result;
	}
	public static boolean calculateApproachPoint(V3 pos, V3 lookDir, double a, V3 pt, UV uv)
	{
		boolean result = false;

		V3 lookUnit = VectorOps.Unit(lookDir);
		V3 posUnit = VectorOps.Unit(pos);

		double r = VectorOps.Mag(pos);
		V3 negPosUnit = VectorOps.Negative(posUnit);
		VectorOps.MakeUnit(negPosUnit);
		double angle = VectorOps.AngularSep(negPosUnit, lookUnit);

		double limit = Math.asin(a/r);
		double tolerance = GeometricConstants.EPS3;

		if (angle < limit)
		{
			double distToClosest = r * Math.cos(angle);

			double dHigh = distToClosest;
			double dLow = r - a - tolerance;
			double dMid = (dHigh + dLow) * 0.50;

			V3 lookScaled = VectorOps.Scale(lookUnit, dMid);
			V3 lookSpot = VectorOps.Add(pos, lookScaled);
			double delta = VectorOps.Mag(lookSpot) - a;

			while (Math.abs(delta) > tolerance){
				if (delta > 0.0)
				{
					dLow = dMid;
				}
				else
				{
					dHigh = dMid;
				}
				dMid = (dHigh + dLow) * 0.50;
				VectorOps.Scale(lookUnit, dMid, lookScaled);
				VectorOps.Add(pos, lookScaled, lookSpot);
				delta = VectorOps.Mag(lookSpot) - a;
			}

			pt.copy(lookSpot);
			Ellipsoid.xyzToLatLong(pt, uv);

			while (uv.lon < 0.0) uv.lon += GeometricConstants.TWOPI;

			result = true;
		}

		return result;
	}
	public static boolean calculateApproachPoint(V3 pos, V3 lookDir, double a, V3 pt, UV uv, double tolerance)
	{
		boolean result = false;

		V3 lookUnit = VectorOps.Unit(lookDir);
		V3 posUnit = VectorOps.Unit(pos);

		double r = VectorOps.Mag(pos);
		V3 negPosUnit = VectorOps.Negative(posUnit);
		VectorOps.MakeUnit(negPosUnit);
		double angle = VectorOps.AngularSep(negPosUnit, lookUnit);

		double limit = Math.asin(a/r);

		if (angle < limit)
		{
			//V3 rotAxis = VectorOps.Cross(lookUnit, negPosUnit);
			//double beta = Math.PI - angle - Math.asin(((a + r)/r)*Math.sin(angle));
			//V3 surfPt = VectorOps.ArbitraryRotate(posUnit, beta, rotAxis);
			//VectorOps.MakeUnit(surfPt);
			//VectorOps.Scale(surfPt, a, pt);
			//Ellipsoid.xyzToLatLong(pt, uv);

			double distToClosest = r * Math.cos(angle);

			double dHigh = distToClosest;
			double dLow = r - a - tolerance;
			double dMid = (dHigh + dLow) * 0.50;

			V3 lookScaled = VectorOps.Scale(lookUnit, dMid);
			V3 lookSpot = VectorOps.Add(pos, lookScaled);
			double delta = VectorOps.Mag(lookSpot) - a;

			while (Math.abs(delta) > tolerance){
				if (delta > 0.0)
				{
					dLow = dMid;
				}
				else
				{
					dHigh = dMid;
				}
				dMid = (dHigh + dLow) * 0.50;
				VectorOps.Scale(lookUnit, dMid, lookScaled);
				VectorOps.Add(pos, lookScaled, lookSpot);
				delta = VectorOps.Mag(lookSpot) - a;
			}

			pt.copy(lookSpot);
			Ellipsoid.xyzToLatLong(pt, uv);

			while (uv.lon < 0.0) uv.lon += GeometricConstants.TWOPI;

			result = true;
		}

		return result;
	}
	public static void getNorthVector(UV uv, V3 northVec)
	{
		V3 vec0 = new V3(0.0, 1.0, 0.0);

		V3 pt = new V3();
		Ellipsoid.latLongToV3(uv, 1.0, pt);
		VectorOps.MakeUnit(pt);

		UV tempUV = new UV(uv.lat, uv.lon);
		V3 tempV = new V3();
		V3 rotAxis = null;

		if (uv.lat > 0.0)
		{
			tempUV.lat = uv.lat - 45.0*GeometricConstants.DE2RA;
			Ellipsoid.latLongToV3(tempUV, 1.0, tempV);
			VectorOps.MakeUnit(tempV);
			rotAxis = VectorOps.Cross(tempV, pt);
		}
		else
		{
			tempUV.lat = uv.lat + 45.0*GeometricConstants.DE2RA;
			Ellipsoid.latLongToV3(tempUV, 1.0, tempV);
			VectorOps.MakeUnit(tempV);
			rotAxis = VectorOps.Cross(pt, tempV);
		}

		//UV tempUV = new UV(0.0, uv.lon);
		//Ellipsoid.latLongToV3(tempUV, 1.0, vec0);
		//tempUV.lat = 45.0 * GeometricConstants.DE2RA;
		//Ellipsoid.latLongToV3(tempUV, 1.0, vec45);

		//VectorOps.MakeUnit(vec0);
		//VectorOps.MakeUnit(vec45);

		//V3 rotAxis = VectorOps.Cross(vec0, vec45);
		//VectorOps.MakeUnit(rotAxis);

		VectorOps.ArbitraryRotate(vec0, uv.lat, rotAxis, northVec);
		VectorOps.MakeUnit(northVec);
	}
	public static void getSouthVector(UV uv, V3 northVec)
	{
		V3 vec0 = new V3(0.0, -1.0, 0.0);

		V3 pt = new V3();
		Ellipsoid.latLongToV3(uv, 1.0, pt);
		VectorOps.MakeUnit(pt);

		UV tempUV = new UV(uv.lat, uv.lon);
		V3 tempV = new V3();
		V3 rotAxis = null;

		if (uv.lat > 0.0)
		{
			tempUV.lat = uv.lat - 45.0*GeometricConstants.DE2RA;
			Ellipsoid.latLongToV3(tempUV, 1.0, tempV);
			VectorOps.MakeUnit(tempV);
			rotAxis = VectorOps.Cross(tempV, pt);
		}
		else
		{
			tempUV.lat = uv.lat + 45.0*GeometricConstants.DE2RA;
			Ellipsoid.latLongToV3(tempUV, 1.0, tempV);
			VectorOps.MakeUnit(tempV);
			rotAxis = VectorOps.Cross(pt, tempV);
		}

		//UV tempUV = new UV(0.0, uv.lon);
		//Ellipsoid.latLongToV3(tempUV, 1.0, vec0);
		//tempUV.lat = 45.0 * GeometricConstants.DE2RA;
		//Ellipsoid.latLongToV3(tempUV, 1.0, vec45);

		//VectorOps.MakeUnit(vec0);
		//VectorOps.MakeUnit(vec45);

		//V3 rotAxis = VectorOps.Cross(vec0, vec45);
		//VectorOps.MakeUnit(rotAxis);

		VectorOps.ArbitraryRotate(vec0, uv.lat, rotAxis, northVec);
		VectorOps.MakeUnit(northVec);
	}
}
