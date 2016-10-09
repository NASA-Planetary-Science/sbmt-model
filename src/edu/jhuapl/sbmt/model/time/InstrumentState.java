package edu.jhuapl.sbmt.model.time;

import java.io.IOException;
import java.util.Vector;

import edu.jhuapl.sbmt.model.europa.geodesic.Ellipsoid;
import edu.jhuapl.sbmt.model.europa.math.V3;
import edu.jhuapl.sbmt.model.europa.math.VectorOps;
import edu.jhuapl.sbmt.model.europa.projection.GeometricConstants;
import edu.jhuapl.sbmt.model.europa.projection.UV;
import edu.jhuapl.sbmt.model.europa.time.TimeCalc;
import edu.jhuapl.sbmt.model.europa.util.AstrometricConstants;
import edu.jhuapl.sbmt.model.europa.util.GeoBox;
import edu.jhuapl.sbmt.model.europa.util.Triplet;

public class InstrumentState {
    protected static double altitudeError = 0.0;
	protected static double alongTrackError = 0.0;
	protected static double crossTrackError = 0.0;
	protected static double pitchAngleError = 0.0;
	protected static double rollAngleError = 0.0;
	protected static double yawAngleError = 0.0;
	protected double t = -1.0;
	protected double viewAngle = -1.0e12;
	protected double rollAngle = 0.0;
	protected double pixelSpacingKm = -1.0e12;
	protected double phaseAngDeg = 180;
	protected double scAlt = 0.0;
	protected V3 surfaceIntercept = new V3();
	protected UV surfacePt = new UV();
	protected UV subPt = new UV();
	protected V3 scPos = new V3();
	protected V3 scVel = new V3();
	protected V3 xAxis = new V3();
	protected V3 yAxis = new V3();
	protected V3 zAxis = new V3();
	protected V3 sunVec = new V3();
	protected int pixelColor = 0;
	protected double incAngleDeg = 45.0;
	protected double emsAngleDeg = 0.0;
	protected V3 leftPix = null;
	protected V3 rightPix = null;
	protected Vector<InstrumentPixel> pixels = null;
	protected GeoBox gBox = null;
	protected float totalScore = 0.0f;
	protected int imageNumber = -1;
	protected int frameNumber = -1;

	public InstrumentState(){}
	public InstrumentState(InstrumentState st)
	{
		copyState(st);
	}
	public void copyState(InstrumentState st)
	{
		t = st.t;
		viewAngle = st.viewAngle;
		pixelSpacingKm = st.pixelSpacingKm;
		scAlt = st.scAlt;
		surfaceIntercept = st.surfaceIntercept;
		surfacePt = st.surfacePt;
		subPt = st.subPt;
		scPos = st.scPos;
		scVel = st.scVel;
		xAxis = st.xAxis;
		yAxis = st.yAxis;
		zAxis = st.zAxis;
		sunVec = st.sunVec;
		pixelColor = st.pixelColor;
		incAngleDeg = st.incAngleDeg;
		emsAngleDeg = st.emsAngleDeg;
		leftPix = st.leftPix;
		rightPix = st.rightPix;
		pixels = st.pixels;
		gBox = st.gBox;
		totalScore = st.totalScore;
		imageNumber = st.imageNumber;
		frameNumber = st.frameNumber;
	}
	public void clearPixels()
	{
		if (pixels != null)
			pixels.clear();
	}
	public static InstrumentState addInstrumentState(Vector<FlybyData> fbVec, double t, double scanAngle, double ifov, int imageNumber, int frameNumber, InstrumentState lastState)
	{
		InstrumentState result = null;

		V3 xAxis = new V3();
		V3 yAxis = new V3();
		V3 zAxis = new V3();

		InstrumentState st = new InstrumentState();
		st.t = t;

		V3 pos = FlybyData.interpolateSpacecraftPosition(fbVec, t);
		V3 sunPos = FlybyData.interpolateSunPosition(fbVec, t);

		//velocity calculation (RD)
		V3 posp=FlybyData.interpolateSpacecraftPosition(fbVec, t + 0.5);
		V3 posm=FlybyData.interpolateSpacecraftPosition(fbVec, t - 0.5);
		V3 vel=VectorOps.Subtract(posp, posm);

		st.scPos.copy(pos);
		st.scVel.copy(vel);
		st.sunVec = sunPos;

		Ellipsoid.xyzToLatLong(st.scPos, st.subPt);

		FlybyData.getAxes(fbVec, t, xAxis, yAxis, zAxis);
		st.xAxis.copy(xAxis);
		st.yAxis.copy(yAxis);
		st.zAxis.copy(zAxis);

		Triplet<V3,UV,Integer> p1 = new Triplet<V3,UV,Integer>();
		p1.first = new V3();
		p1.second = new UV();

		V3 lookVec = VectorOps.ArbitraryRotate(zAxis, scanAngle, yAxis);
		VectorOps.MakeUnit(lookVec);

		V3 errX = VectorOps.Scale(xAxis, alongTrackError);
		V3 errY = VectorOps.Scale(yAxis, crossTrackError);
		V3 errZ = VectorOps.Scale(zAxis, altitudeError);
		V3 errSum = new V3(errX.X1() + errY.X1() + errZ.X1(), errX.X2() + errY.X2() + errZ.X2(), errX.X3() + errY.X3() + errZ.X3());
		V3 posErr = VectorOps.Add(pos, errSum);

		if (Ellipsoid.calculateApproachPoint(posErr, lookVec, AstrometricConstants.europaRadiusKm, p1.first, p1.second, GeometricConstants.EPS3))
		{
			result = st;

			V3 surf2Sun = VectorOps.Subtract(sunPos, p1.first);
			st.incAngleDeg = VectorOps.AngularSep(surf2Sun, p1.first) * GeometricConstants.RA2DE;
			V3 surf2SC = VectorOps.Subtract(pos, p1.first);
			st.emsAngleDeg = VectorOps.AngularSep(surf2SC, p1.first) * GeometricConstants.RA2DE;

			V3 crossAxis = VectorOps.ArbitraryRotate(xAxis, scanAngle, yAxis);
			lookVec = VectorOps.ArbitraryRotate(lookVec, ifov, crossAxis);
			VectorOps.MakeUnit(lookVec);

			Triplet<V3,UV,Integer> p2 = new Triplet<V3,UV,Integer>();
			p2.first = new V3();
			p2.second = new UV();

			Ellipsoid.calculateApproachPoint(posErr, lookVec, AstrometricConstants.europaRadiusKm, p2.first, p2.second, GeometricConstants.EPS3);

			while (p2.second.lon < 0.0) p2.second.lon += GeometricConstants.TWOPI;

			st.scPos.copy(posErr);
			st.leftPix = new V3(p2.first);
			st.viewAngle = scanAngle;

			st.pixelSpacingKm = (VectorOps.AngularSep(p1.first, p2.first) * AstrometricConstants.europaRadiusKm);
			st.surfaceIntercept.copy(p1.first);
			st.surfacePt.copy(p1.second);
			st.imageNumber = imageNumber;
			st.frameNumber = frameNumber;
			st.scAlt = VectorOps.Mag(st.scPos) -  AstrometricConstants.europaRadiusKm;
		}

		return result;
	}
	public static String getStateString(Vector<FlybyData> fbVec, InstrumentState st, InstrumentState lastState)
	{
		String result = "";

		V3 sunPos = FlybyData.interpolateSunPosition(fbVec, st.t);
		UV sunUV = new UV();
		Ellipsoid.xyzToLatLong(sunPos, sunUV);

		String utcStr = TimeCalc.et2UTC(st.t);

		double alt = VectorOps.Mag(st.scPos) - AstrometricConstants.europaRadiusKm;

		String csvLine = ""+st.imageNumber+","+st.frameNumber+","+Double.toString(st.t)+","+utcStr+","+FileUtils.formatDouble(st.viewAngle*GeometricConstants.RA2DE,4)+","+FileUtils.formatDouble(st.rollAngle*GeometricConstants.RA2DE,2)+","+FileUtils.formatDouble(alt,5)+",";
		csvLine += FileUtils.formatDouble(st.surfacePt.lat*GeometricConstants.RA2DE,5)+","+FileUtils.formatDouble(st.surfacePt.lon*GeometricConstants.RA2DE,5)+",";
		csvLine += FileUtils.formatDouble(st.pixelSpacingKm, 7) + ",";
		if (lastState != null)
		{
			V3 lastIntercept = lastState.surfaceIntercept;
			V3 currentIntercept = st.surfaceIntercept;
			V3 positionDelta = VectorOps.Subtract(currentIntercept, lastIntercept);
			double groundDistKm = AstrometricConstants.europaRadiusKm * VectorOps.AngularSep(lastIntercept, currentIntercept);
			double factor = 1.0;
			if (VectorOps.Dot(positionDelta, st.scVel) < 0.0)
			{
				factor = -1.0;
			}
			csvLine += FileUtils.formatDouble(factor * groundDistKm, 7);
		}
		else
		{
			csvLine += "0.0";
		}

		while (sunUV.lon < 0.0) sunUV.lon += GeometricConstants.TWOPI;

		double solarAngle = st.surfacePt.lon - sunUV.lon;
		double lst = 12.0 + solarAngle / (15.0 * GeometricConstants.DE2RA);
		int hours = (int)lst;
		int minutes = (int)(0.50 + lst * 60.0 - (double)hours*60.0);
		if (hours >= 13)
			hours -= 12;
		String lstStr = Integer.toString(hours) + ":" + FileUtils.formatInteger(minutes,2) + ((solarAngle>=0.0) ? " pm" : " am");

		csvLine += "," + FileUtils.formatDouble(st.incAngleDeg,3);
		csvLine += "," + FileUtils.formatDouble(st.emsAngleDeg,3);
		csvLine += "," + lstStr;
		V3 scPos = new V3();
		V3 scVel = new V3();
		V3 sun = new V3();
		V3 xAxis = new V3();
		V3 yAxis = new V3();
		V3 zAxis = new V3();
		Ellipsoid.native2Spice(st.scPos, scPos);
		Ellipsoid.native2Spice(st.scVel, scVel);
		Ellipsoid.native2Spice(st.xAxis, xAxis);
		Ellipsoid.native2Spice(st.yAxis, yAxis);
		Ellipsoid.native2Spice(st.zAxis, zAxis);
		Ellipsoid.native2Spice(sunPos, sun);
		csvLine += "," + Double.toString(scPos.X1()) + "," + Double.toString(scPos.X2()) + "," + Double.toString(scPos.X3());
//		V3 posPlus1 = FlybyData.interpolateSpacecraftPosition(fbVec, st.t + 1.0);
		csvLine += "," + Double.toString(scVel.X1()) + "," + Double.toString(scVel.X2()) + "," + Double.toString(scVel.X3());
		csvLine += "," + Double.toString(sun.X1()) + "," + Double.toString(sun.X2()) + "," + Double.toString(sun.X3());
		csvLine += "," + Double.toString(sunUV.lat*GeometricConstants.RA2DE) + "," + Double.toString(sunUV.lon*GeometricConstants.RA2DE);
		csvLine += "," + Double.toString(st.totalScore);
		csvLine += "," + Double.toString(xAxis.X1()) + "," + Double.toString(xAxis.X2()) + "," + Double.toString(xAxis.X3());
		csvLine += "," + Double.toString(yAxis.X1()) + "," + Double.toString(yAxis.X2()) + "," + Double.toString(yAxis.X3());
		csvLine += "," + Double.toString(zAxis.X1()) + "," + Double.toString(zAxis.X2()) + "," + Double.toString(zAxis.X3());

		result = csvLine;

		return result;
	}
	public static String getStateHeader()
	{
		String result = "Image,Frame,Ephemeris Time,UTC Time,Viewing Angle (deg),Roll Angle (deg),Altitude (km),Intercept Lat (deg),Intercept Lon (deg),Cross Track Spacing (km),Along Track Spacing (km),";
		result += "Solar Incidence Angle (deg),Emission Angle (deg),Local Solar Time,Spacecraft-X (km),Spacecraft-Y (km),Spacecraft-Z (km),Velocity-X (km/s),Velocity-Y (km/s),Velocity-Z (km/s),";
		result += "Sun-X (km),Sun-Y (km),Sun-Z (km),Sub-Solar Lat. (deg),Sub-Solar Lon. (deg),Frame Score,";
		result += "SC X axis-X,SC X axis-Y,SC X axis-Z,SC Y axis-X,SC Y axis-Y,SC Y axis-Z,SC Z axis-X,SC Z axis-Y,SC Z axis-Z";
		return result;
	}

    public static InstrumentState readStateString(String ststring)
    {
        InstrumentState st= new InstrumentState();
        return readStateString(st, ststring);
    }

    public static InstrumentState readStateString(InstrumentState st, String ststring)
    {
		String csvdlim="[,]";
		String[] tok=ststring.split(csvdlim);

        // hack to correct malformed values -turnerj1
        for (int i=0; i<tok.length; i++)
            if (tok[i].startsWith("."))
                tok[i] = "0.0";

		st.imageNumber=Integer.parseInt(tok[0]);

		st.frameNumber=Integer.parseInt(tok[1]);

		st.t=Double.parseDouble(tok[2]);
		// UTC Time (3)

		st.viewAngle=Double.parseDouble(tok[4])*GeometricConstants.DE2RA;
		st.rollAngle=Double.parseDouble(tok[5])*GeometricConstants.DE2RA;

		st.scAlt=Double.parseDouble(tok[6]);
		st.surfacePt.lat=Double.parseDouble(tok[7])*GeometricConstants.DE2RA;
		st.surfacePt.lon=Double.parseDouble(tok[8])*GeometricConstants.DE2RA;

		st.pixelSpacingKm=Double.parseDouble(tok[9]);
		// Along Track Spacing (10)

		st.incAngleDeg=Double.parseDouble(tok[11]);
		st.emsAngleDeg=Double.parseDouble(tok[12]);

		// Local Solar Time (13)

		st.scPos.setX1(Double.parseDouble(tok[14]));
		st.scPos.setX2(Double.parseDouble(tok[15]));
		st.scPos.setX3(Double.parseDouble(tok[16]));

		st.scVel.setX1(Double.parseDouble(tok[17]));
		st.scVel.setX2(Double.parseDouble(tok[18]));
		st.scVel.setX3(Double.parseDouble(tok[19]));

		st.sunVec.setX1(Double.parseDouble(tok[20]));
		st.sunVec.setX2(Double.parseDouble(tok[21]));
		st.sunVec.setX3(Double.parseDouble(tok[22]));

		// Sub-Solar Lat-Lon (23,-24)

		st.totalScore=Float.parseFloat(tok[25]);

		st.xAxis.setX1(Double.parseDouble(tok[26]));
		st.xAxis.setX2(Double.parseDouble(tok[27]));
		st.xAxis.setX3(Double.parseDouble(tok[28]));

		st.yAxis.setX1(Double.parseDouble(tok[29]));
		st.yAxis.setX2(Double.parseDouble(tok[30]));
		st.yAxis.setX3(Double.parseDouble(tok[31]));

		st.zAxis.setX1(Double.parseDouble(tok[32]));
		st.zAxis.setX2(Double.parseDouble(tok[33]));
		st.zAxis.setX3(Double.parseDouble(tok[34]));

// the following are derived from the file data from algorithms in addInstrumentState
// only surfaceIntercept and subPt are populated. The following are not
// populated here or in addInstrumentState:
// altitudeError,alongTrackError,crossTrackError,pitchAngleError,rollAngleError,
// yawAngleError, phaseAngDeg,pixelColor,leftPix,rightPix,pixels,gBox
		Ellipsoid.xyzToLatLong(st.scPos, st.subPt);
		Ellipsoid.latLongToV3(st.surfacePt,AstrometricConstants.europaRadiusKm,st.surfaceIntercept);

		return st;
	}
	public static Vector<InstrumentState> readStateCSV(String file)
	{
		Vector<InstrumentState> results = new Vector<InstrumentState>();
		Vector<String> lines = new Vector<String>();
		InstrumentState stin;

		try {
			FileUtils.readAsciiFile(file, lines);
			for (int ic = 1;ic < lines.size(); ic++) {
				stin=readStateString(lines.elementAt(ic));
				results.add(stin);
			}
		}
		catch (IOException ioEx)
		{
			System.out.println(ioEx);
			ioEx.printStackTrace();
		}

		return results;
	}


	public static float calculateScore(double incAngleDeg, double pixelSpacingKm, double emsAngleDeg)
	{
		float result = 0.0f;
		double resScore = 0.0;
		double incScore = 0.0;
		double emsScore = 0.0;

		if (incAngleDeg < 90.0)
		{
			incScore = 1.5 - incAngleDeg / 100.0;
		}

		resScore = 1.5 - pixelSpacingKm / 5.0;
		if (resScore < 0.0)
			resScore = 0.0;

		if (emsAngleDeg < 90.0)
		{
			emsScore = 1.5 - emsAngleDeg/200.0;
		}

		result = (float)(incScore * resScore * emsScore);

		return result;
	}
	public int GetImage() {
		return imageNumber;
	}
	public int GetFrame() {
		return frameNumber;
	}
}
