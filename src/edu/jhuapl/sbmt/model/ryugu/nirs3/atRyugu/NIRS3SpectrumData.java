package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;

import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.util.TimeUtil;

class NIRS3SpectrumData
{
	String filename;
	double et;
	double[] spectrum;
	double etStart;
	double etEnd;
	double[] geoFields;

	public NIRS3SpectrumData()
	{
		spectrum = new double[NIRS3.bandCentersLength];
		geoFields = new double[6];
	}

	public double[] getSpectrum()
	{
		return spectrum;
	}

	public double getEt()
	{
		return et;
	}

	public String getFilename()
	{
		return filename;
	}

	public double getUtcStart()
	{
		return etStart;
	}

	public double getUtcMid()
	{
		return et;
	}

	public double getUtcEnd()
	{
		return etEnd;
	}

	public double[] getGeoFields()
	{
		return geoFields;
	}

	public void setSpectrum(double[] spectrum)
	{
		this.spectrum = spectrum;
	}

	public void setUtcStart(double utcStart)
	{
		this.etStart = utcStart;
	}

	public void setUtcMid(double utcMid)
	{
		this.et = utcMid;
	}

	public void setUtcEnd(double utcEnd)
	{
		this.etEnd = utcEnd;
	}

	public void setGeoFields(double[] geoFields)
	{
		this.geoFields = geoFields;
	}

	public void readLine(String filename, String line)
	{
		String baseName = FilenameUtils.getBaseName(filename);
		System.out.println("NIRS3SpectrumData: readLine: line is " + line);
		Scanner scanner = new Scanner(line).useDelimiter(",");

		String startTime = scanner.next(); // get the UTC start time
		String startTimeComplete = baseName.substring(0, 4) + "-" + baseName.substring(4,6) + "-" + baseName.substring(6,8) + "T" + startTime;
		etStart = TimeUtil.str2et(startTimeComplete);

		String midTime = scanner.next(); // get the UTC mid time
		//craft the time from the filename + the midTime
		String midTimeComplete = baseName.substring(0, 4) + "-" + baseName.substring(4,6) + "-" + baseName.substring(6,8) + "T" + midTime;
		et = TimeUtil.str2et(midTimeComplete);

		String endTime = scanner.next(); // get the UTC start time
		String endTimeComplete = baseName.substring(0, 4) + "-" + baseName.substring(4,6) + "-" + baseName.substring(6,8) + "T" + endTime;
		etEnd = TimeUtil.str2et(endTimeComplete);

		geoFields[0] = Double.parseDouble(scanner.next());
		geoFields[1] = Double.parseDouble(scanner.next());
		geoFields[2] = Double.parseDouble(scanner.next());
		geoFields[3] = Double.parseDouble(scanner.next());
		geoFields[4] = Double.parseDouble(scanner.next());
		geoFields[5] = Double.parseDouble(scanner.next());
		for (int i = 9; i < NIRS3.bandCentersLength+9; i++)
			spectrum[i-9] = Double.parseDouble(scanner.next());
		scanner.close();
	}

	public void writeLine(FileWriter writer) throws IOException
	{
		writer.write("" + TimeUtil.et2str(etStart).substring(TimeUtil.et2str(etStart).indexOf("T")+1) + ",");
		writer.write("" + TimeUtil.et2str(et).substring(TimeUtil.et2str(et).indexOf("T")+1) + ",");
		writer.write("" + TimeUtil.et2str(etEnd).substring(TimeUtil.et2str(etEnd).indexOf("T")+1) + ",");
		writer.write("" + geoFields[0] + ",");
		writer.write("" + geoFields[1] + ",");
		writer.write("" + geoFields[2] + ",");
		writer.write("" + geoFields[3] + ",");
		writer.write("" + geoFields[4] + ",");
		writer.write("" + geoFields[5] + ",");
		for (int i = 0; i < NIRS3.bandCentersLength; i++)
			writer.write("" + spectrum[i] + ",");
		writer.write("\n");
	}

}