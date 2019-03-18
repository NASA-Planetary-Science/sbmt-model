package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.util.Scanner;

import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.util.TimeUtil;

public class NIRS3SpectrumData
{
	double et;
	double[] spectrum;

	public NIRS3SpectrumData(String filename, String line)
	{
		spectrum = new double[NIRS3.bandCentersLength];
		Scanner scanner = new Scanner(line).useDelimiter(",");
		scanner.next(); // skip the UTC start
		String midTime = scanner.next(); // get the UTC mid time
		//craft the time from the filename + the midTime
		String midTimeComplete = filename.substring(0, 4) + "-" + filename.substring(4,6) + "-" + filename.substring(6,8) + "T" + midTime;
		System.out.println("NIRS3SpectrumData: NIRS3SpectrumData: midtime is " + midTimeComplete);
		et = TimeUtil.str2et(midTimeComplete);
		scanner.next(); // skip the UTC end
		scanner.next();  //skip the other geo fields
		scanner.next();
		scanner.next();
		scanner.next();
		scanner.next();
		scanner.next();
		for (int i = 9; i < NIRS3.bandCentersLength+9; i++)
			spectrum[i-9] = Double.parseDouble(scanner.next());
		scanner.close();
	}

	public double[] getSpectrum()
	{
		return spectrum;
	}

	public double getEt()
	{
		return et;
	}

}
