package edu.jhuapl.sbmt.model.time;


import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Vector;

import edu.jhuapl.sbmt.model.europa.geodesic.Ellipsoid;
import edu.jhuapl.sbmt.model.europa.math.V3;
import edu.jhuapl.sbmt.model.europa.math.VectorOps;

//import crucible.core.collections.CollectionUtilities;

public class StateFileData {
	public String utc = "";
	public double et = 0.0;
	public V3 sc = null;
	public V3 europa = null;
	public V3 io = null;
	public V3 ganymede = null;
	public V3 callisto = null;
	public V3 sun = null;
	public V3 earth = null;
	public V3 jupSunVec = null;
	public V3 ioSunVec = null;
	public V3 ganySunVec = null;
	public V3 callSunVec = null;

	public StateFileData() {

	}

	public StateFileData(String timeStr, double t, V3 scPos, V3 europaPos,
			V3 ioPos, V3 ganymedePos, V3 callistoPos, V3 sunPos, V3 earthPos) {
		create(timeStr, t, scPos, europaPos, ioPos, ganymedePos, callistoPos,
				sunPos, earthPos);
	}

	public void create(String timeStr, double t, V3 scPos, V3 europaPos,
			V3 ioPos, V3 ganymedePos, V3 callistoPos, V3 sunPos, V3 earthPos) {
		utc = new String(timeStr);
		et = t;
		sc = scPos;
		europa = europaPos;
		io = ioPos;
		ganymede = ganymedePos;
		callisto = callistoPos;
		sun = sunPos;
		earth = earthPos;
	}

	public String getUTC() {
		return utc;
	}

	public double getET() {
		return et;
	}

	public V3 getSpacecraftPosition() {
		return sc;
	}

	public V3 getEuropaPosition() {
		return europa;
	}

	public V3 getIoPosition() {
		return io;
	}

	public V3 getGanymedePosition() {
		return ganymede;
	}

	public V3 getCallistoPosition() {
		return callisto;
	}

	public V3 getSunPosition() {
		return sun;
	}

	public static StateFileData parseFlybyData(String line) {
		StateFileData result = null;

		Vector<String> parts = new Vector<String>();
		FileUtils.splitOnChar(line, ',', parts);
		if (parts.size() >= 20) {
			try {
				int ndx = 0;
				String timeStr = parts.get(ndx++);
				double t = Double.parseDouble(parts.get(ndx++));
				V3 scPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 europaPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 ioPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 ganymedePos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 callistoPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 sunPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 earthPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));

				result = new StateFileData(timeStr, t, scPos, europaPos, ioPos,
						ganymedePos, callistoPos, sunPos, earthPos);
			} catch (Exception ex) {

			}
		}

		return result;
	}

	public static StateFileData parseFlybyData(String line, String jupLine,
			String ioLine, String ganyLine, String callLine) {
		StateFileData result = null;

		Vector<String> parts = new Vector<String>();
		Vector<String> sunParts = new Vector<String>();
		FileUtils.splitOnChar(line, ',', parts);
		if (parts.size() >= 20) {
			try {
				int ndx = 0;
				String timeStr = parts.get(ndx++);
				double t = Double.parseDouble(parts.get(ndx++));
				V3 scPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 europaPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 ioPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 ganymedePos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 callistoPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 sunPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));
				V3 earthPos = Ellipsoid.spice2Native(new V3(Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++)), Double
						.parseDouble(parts.get(ndx++))));

				result = new StateFileData(timeStr, t, scPos, europaPos, ioPos,
						ganymedePos, callistoPos, sunPos, earthPos);

				String sunLine = jupLine;
				if (sunLine != null) {
					FileUtils.splitOnChar(sunLine, ',', sunParts);
					V3 sVec = Ellipsoid.spice2Native(new V3(Double
							.parseDouble(sunParts.get(2)), Double
							.parseDouble(sunParts.get(3)), Double
							.parseDouble(sunParts.get(4))));
					result.jupSunVec = sVec;
				}

				sunLine = ioLine;
				if (sunLine != null) {
					FileUtils.splitOnChar(sunLine, ',', sunParts);
					V3 sVec = Ellipsoid.spice2Native(new V3(Double
							.parseDouble(sunParts.get(2)), Double
							.parseDouble(sunParts.get(3)), Double
							.parseDouble(sunParts.get(4))));
					result.ioSunVec = sVec;
				}

				sunLine = ganyLine;
				if (sunLine != null) {
					FileUtils.splitOnChar(sunLine, ',', sunParts);
					V3 sVec = Ellipsoid.spice2Native(new V3(Double
							.parseDouble(sunParts.get(2)), Double
							.parseDouble(sunParts.get(3)), Double
							.parseDouble(sunParts.get(4))));
					result.ganySunVec = sVec;
				}

				sunLine = callLine;
				if (sunLine != null) {
					FileUtils.splitOnChar(sunLine, ',', sunParts);
					V3 sVec = Ellipsoid.spice2Native(new V3(Double
							.parseDouble(sunParts.get(2)), Double
							.parseDouble(sunParts.get(3)), Double
							.parseDouble(sunParts.get(4))));
					result.callSunVec = sVec;
				}
			} catch (Exception ex) {
			}
		}

		return result;
	}

	public static Vector<StateFileData> getFlybyData(Vector<String> lines,
			Vector<String> jupLines, Vector<String> ioLines,
			Vector<String> ganyLines, Vector<String> callLines) {
		Vector<StateFileData> results = new Vector<StateFileData>();
		int i, n;

		n = lines.size();
		for (i = 1; i < n; i++) {
			String jupLine = null;
			String ioLine = null;
			String ganyLine = null;
			String callLine = null;
			if (i < jupLines.size())
				jupLine = jupLines.get(i);
			if (i < ioLines.size())
				ioLine = ioLines.get(i);
			if (i < ganyLines.size())
				ganyLine = ganyLines.get(i);
			if (i < callLines.size())
				callLine = callLines.get(i);

			StateFileData fb = parseFlybyData(lines.get(i), jupLine, ioLine,
					ganyLine, callLine);
			results.add(fb);
		}

		return results;
	}

	public static void main(String args[])
	{
		String folder = "C:\\data\\13-F7-ext\\";

		for (int flyby = 1; flyby <= 45; flyby++){
			String fileStr = folder + "Flyby" + FileUtils.formatInteger(flyby,4) + ".csv";
			System.out.println("Working on: "+fileStr);
			Vector<StateFileData> fbData = StateFileData.getFlybyData(fileStr);
			if (fbData.size() > 0)
			{
				String lblStr = FileUtils.getFilenameBeforeExtension(fileStr) + ".lbl";
				Vector<String> lblLines = new Vector<String>();
				lblLines.add("START_TIME = "+fbData.get(0).utc);
				lblLines.add("STOP_TIME = "+fbData.get(fbData.size()-1).utc);
				try {
					FileUtils.writeTextToFile(lblStr, lblLines);
				}
				catch (IOException ioEx)
				{
					System.out.println("Had a problem writing to: "+lblStr);
				}
			}
		}
	}
	public static Vector<StateFileData> getFlybyData(String filename) {
		Vector<StateFileData> results = null;

		Vector<String> lines = FileUtils.readAsciiFile(filename);
		String jupFile = FileUtils.getFilenameBeforeExtension(filename)
				+ "Jupiter.csv";
		String ioFile = FileUtils.getFilenameBeforeExtension(filename)
				+ "Io.csv";
		String callFile = FileUtils.getFilenameBeforeExtension(filename)
				+ "Callisto.csv";
		String ganyFile = FileUtils.getFilenameBeforeExtension(filename)
				+ "Ganymede.csv";

		Vector<String> jupLines = FileUtils.readAsciiFile(jupFile);
		Vector<String> ioLines = FileUtils.readAsciiFile(ioFile);
		Vector<String> callLines = FileUtils.readAsciiFile(callFile);
		Vector<String> ganyLines = FileUtils.readAsciiFile(ganyFile);

		results = getFlybyData(lines, jupLines, ioLines, ganyLines, callLines);

		return results;
	}

	//
	//	Given a file system location I just want to get a string listing
	//	of the flyby csv files
	//
	public static Vector<String> getFlybysInFolder(String folder)
	{
		Vector<String> results = null;

		int i, n;
		Vector<File> fbFiles = new Vector<File>();
		FileUtils.getFilesInFolder(folder, fbFiles);

		n = fbFiles.size();
		for (i = 0; i < n; i++){
			String fileOnly = FileUtils.getFilenameOnly(fbFiles.get(i).getAbsolutePath());
			String fileUp = fileOnly.toUpperCase();
			if (fileUp.startsWith("FLYBY") && fileUp.endsWith(".CSV"))
			{
				String firstPart = FileUtils.getStringBefore(fileOnly, '.');
				String intPart = firstPart.substring(5);
				if (FileUtils.isInteger(intPart))
				{
					if (results == null)
						results = new Vector<String>();
					results.add(FileUtils.getFilenameOnly(fbFiles.get(i).getAbsolutePath()));
				}
			}
		}

		return results;
	}

	public static V3 interpolateSpacecraftPosition(Vector<StateFileData> fbVec,
			double t)
	{
		V3 result = null;

		int i = findIndex(fbVec, t);

		// System.out.println("Index = "+i);

		if ((i >= 0) && (i < (fbVec.size() - 1))) {
			double t1 = fbVec.get(i).et;
			double t2 = fbVec.get(i + 1).et;

			V3 pos1 = fbVec.get(i).sc;
			V3 pos2 = fbVec.get(i + 1).sc;

			double x = pos1.X1() + (t - t1) * (pos2.X1() - pos1.X1())
					/ (t2 - t1);
			double y = pos1.X2() + (t - t1) * (pos2.X2() - pos1.X2())
					/ (t2 - t1);
			double z = pos1.X3() + (t - t1) * (pos2.X3() - pos1.X3())
					/ (t2 - t1);

			result = new V3(x, y, z);
		}

		return result;
	}

	public static double calculateGroundSpeed(Vector<StateFileData> fbVec,
			double t, double radius) {
		double result = 0.0;

		V3 v1 = interpolateSpacecraftPosition(fbVec, t - 1.0);
		V3 v2 = StateFileData.interpolateSpacecraftPosition(fbVec, t + 1.0);

		result = VectorOps.AngularSep(v1, v2) * radius * 0.50;

		return result;
	}

	public static V3 calculateVelocity(Vector<StateFileData> fbVec, double t)
	{
		V3 result = null;

		V3 v1 = StateFileData.interpolateSpacecraftPosition(fbVec, t-0.50);
		V3 v2 = StateFileData.interpolateSpacecraftPosition(fbVec, t+0.50);

		result = new V3(v2.X1() - v1.X1(), v2.X2() - v1.X2(), v2.X3() - v1.X3());

		return result;
	}

	public static V3 interpolateSunPosition(Vector<StateFileData> fbVec, double t) {
		V3 result = null;

		int i = findIndex(fbVec, t);

		result = fbVec.get(i).sun;
		// System.out.println("Index = "+i);

		/*
		 * if ((i >= 0) && (i < (fbVec.size()-1))) { double t1 =
		 * fbVec.get(i).et; double t2 = fbVec.get(i+1).et;
		 *
		 * V3 pos1 = fbVec.get(i).sun; V3 pos2 = fbVec.get(i+1).sun;
		 *
		 * double m1 = VectorOps.Mag(pos1); double m2 = VectorOps.Mag(pos2);
		 *
		 * VectorOps.MakeUnit(pos1); VectorOps.MakeUnit(pos2);
		 *
		 * double x = pos1.X1() + (t - t1) * (pos2.X1() - pos1.X1()) / (t2 -
		 * t1); double y = pos1.X2() + (t - t1) * (pos2.X2() - pos1.X2()) / (t2
		 * - t1); double z = pos1.X3() + (t - t1) * (pos2.X3() - pos1.X3()) /
		 * (t2 - t1);
		 *
		 * result = new V3(x, y, z); result = VectorOps.Scale(result,
		 * (m1+m2)*0.50); }
		 */

		return result;
	}

	private static final Comparator<StateFileData> timeOrdering = new Comparator<StateFileData>() {

		@Override
		public int compare(StateFileData o1, StateFileData o2) {
			return Double.compare(o1.et, o2.et);
		}

	};

	public static int findIndex(Vector<StateFileData> fbVec, double t) {

		StateFileData compareTo = new StateFileData();
		compareTo.et = t;
// comment out for now to avoid importing crucible -turnerj1
//		return CollectionUtilities.lastLessThanOrEqualTo(fbVec, compareTo,
//				timeOrdering);
		throw new RuntimeException("Not implemented (sorry) -turnerj1");
//		return 0;
	}

	// public static int findIndex(Vector<FlybyData> fbVec, double t)
	// {
	// int result = -1;
	//
	// int n = fbVec.size();
	// for (int i = 0; (i < (n-1)) && (result < 0); i++){
	// if ((t >= fbVec.get(i).et) && (t < fbVec.get(i+1).et))
	// {
	// result = i;
	// }
	// }
	//
	// return result;
	// }

	public static int findClosestApproach(Vector<StateFileData> fbVec) {
		int result = -1;

		int n = fbVec.size();
		for (int i = 0; (i < (n - 1)) && (result < 0); i++) {
			if (VectorOps.Mag(fbVec.get(i).sc) < VectorOps
					.Mag(fbVec.get(i + 1).sc)) {
				result = i;
			}
		}

		return result;
	}

	public static V3 getAxes(Vector<StateFileData> fbVec, double t, V3 xAxis,
			V3 yAxis, V3 zAxis) {
		V3 result = null;

		int i = findIndex(fbVec, t);

		if ((i >= 0) && (i < (fbVec.size() - 1))) {
			StateFileData fb1 = fbVec.get(i);
			StateFileData fb2 = fbVec.get(i + 1);

			// zAxis.create(-fb1.sc.X1(), -fb1.sc.X2(), -fb1.sc.X3());
			VectorOps.Negative(fb1.sc, zAxis);
			VectorOps.MakeUnit(zAxis);
			VectorOps.Subtract(fb2.sc, fb1.sc, xAxis);
			VectorOps.MakeUnit(xAxis);
			VectorOps.Cross(zAxis, xAxis, yAxis);
			VectorOps.MakeUnit(yAxis);
			VectorOps.Cross(yAxis, zAxis, xAxis);
			VectorOps.MakeUnit(xAxis);
		}

		return result;
	}

	public static int findIncomingIndex(double radius, Vector<StateFileData> fbVec) {
		int result = -1;

		int i, n;

		n = fbVec.size();
		for (i = 0; (i < (n - 1) && (result < 0)); i++) {
			V3 sc1 = fbVec.get(i).sc;
			V3 sc2 = fbVec.get(i + 1).sc;
			double m1 = VectorOps.Mag(sc1);
			double m2 = VectorOps.Mag(sc2);

			if ((m1 > radius) && (m2 < radius)) {
				result = i;
			}
		}

		return result;
	}

	public static int findOutgoingIndex(double radius, Vector<StateFileData> fbVec) {
		int result = -1;

		int i, n;

		n = fbVec.size();
		for (i = 0; (i < (n - 1) && (result < 0)); i++) {
			V3 sc1 = fbVec.get(i).sc;
			V3 sc2 = fbVec.get(i + 1).sc;
			double m1 = VectorOps.Mag(sc1);
			double m2 = VectorOps.Mag(sc2);

			if ((m1 < radius) && (m2 > radius)) {
				result = i;
			}
		}

		return result;
	}
	public static int getFlybyNumberFromFile(String fileStr)
	{
		int result = -1;

		String fileOnly = FileUtils.getFilenameOnly(fileStr);
		String fileOnlyUp = fileOnly.toUpperCase();
		if (fileOnlyUp.startsWith("FLYBY") && fileOnlyUp.endsWith(".CSV"))
		{
			String numberStr = fileOnly.substring(5, 9);
			result = Integer.parseInt(numberStr);
		}

		return result;
	}
}
