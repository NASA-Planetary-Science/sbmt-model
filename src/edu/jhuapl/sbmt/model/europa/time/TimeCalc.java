package edu.jhuapl.sbmt.model.europa.time;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import edu.jhuapl.sbmt.model.europa.util.FileUtils;
import edu.jhuapl.sbmt.model.europa.util.Pair;

public class TimeCalc {
	String currFolder = ".";
	Vector<File> tlsFiles = new Vector<File>();
	String tlsFile = "";
	int fileNumber = -1;
	double deltaTA = 32.184;
	Vector<Pair<Double,Integer>> leapSecondPairs = new Vector<Pair<Double,Integer>>();
	public static final String monthStrings[] = {"JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG",
		"SEP","OCT","NOV","DEC"};
	public static final double secondsPerDay = 86400.0;
	public static final double secondsPerHour = 3600.0;
	public static final double minutesPerHour = 60.0;
	public static final double secondsPerMinute = 60.0;
	public static final double secondsPerHalfDay = 43200.0;
	public static final double millisecondsPerSecond = 1000.0;
	public static final int []daysInMonths =
		{31,28,31,30,31,30,31,31,30,31,30,31};
	//DateTimeInfo dti = new DateTimeInfo();
	private static TimeCalc singleton = null;
	public static Object timeLock = new Object();
	TimeCalc()
	{
		updateLeapSecondFileList();
		loadLeapSeconds();
	}
	public static TimeCalc getSingleton()
	{
		synchronized(timeLock){
			if (singleton == null)
			{
				singleton = new TimeCalc();
			}
		}

		return singleton;
	}
	public static double utc2ET(String utcStr)
	{
		double result = -1.0e9;

		DateTimeInfo dti = new DateTimeInfo();

		TimeCalc tc = TimeCalc.getSingleton();

		if (fromString(utcStr, dti))
		{
			double leapTime = tc.calculateLeapTime(dti.dtYear, dti.dtMonth, dti.dtDay);
			double leapSecs = 0.0;
			for (int i = 0; i < tc.leapSecondPairs.size(); i++){
				if (leapTime > tc.leapSecondPairs.get(i).first)
				{
					leapSecs = tc.leapSecondPairs.get(i).second;
				}
			}

			//System.out.println("Leap seconds = "+Double.toString(leapSecs));

			result = leapTime + leapSecs + tc.deltaTA + (dti.dtHour * 3600.0) + dti.dtMinute*60.0 + dti.dtSecond;
		}

		return result;
	}
	public static String et2UTC(double et)
	{
		String result = "";

		DateTimeInfo localDTI = new DateTimeInfo();

		TimeCalc tc = TimeCalc.getSingleton();

		tc.et2DTI(et, localDTI);

		result = FileUtils.formatInteger(localDTI.dtYear,4)+"-"+FileUtils.formatInteger(localDTI.dtDoy,3)+"T"+
				FileUtils.formatInteger(localDTI.dtHour,2)+":"+FileUtils.formatInteger(localDTI.dtMinute,2)+":"+
				FileUtils.formatDouble(localDTI.dtSecond,2,3);

		return result;
	}
	public static boolean isDayBoundary(double et, double tolerance)
	{
		boolean result = false;

		DateTimeInfo localDTI = new DateTimeInfo();

		TimeCalc tc = TimeCalc.getSingleton();

		tc.et2DTI(et, localDTI);

		double timeOfDay = (double)localDTI.dtHour * 3600.0 + (double)localDTI.dtMinute*60.0 + localDTI.dtSecond;
		if (Math.abs(timeOfDay) < tolerance)
		{
			result = true;
		}

		return result;
	}
	public static String et2UTC(double et, String fmtStr)
	{
		String result = "";

		DateTimeInfo localDTI = new DateTimeInfo();

		TimeCalc tc = TimeCalc.getSingleton();

		tc.et2DTI(et, localDTI);

		if ((fmtStr != null) && (fmtStr.compareToIgnoreCase("ISOC") == 0))
		{
			result = FileUtils.formatInteger(localDTI.dtYear,4)+"-"+FileUtils.formatInteger(localDTI.dtMonth+1,2)+"-"+
					FileUtils.formatInteger(localDTI.dtDay,2)+"T"+
					FileUtils.formatInteger(localDTI.dtHour,2)+":"+FileUtils.formatInteger(localDTI.dtMinute,2)+":"+
					FileUtils.formatDouble(localDTI.dtSecond,2,3);
		}
		else
		{
			result = FileUtils.formatInteger(localDTI.dtYear,4)+"-"+FileUtils.formatInteger(localDTI.dtDoy,3)+"T"+
					FileUtils.formatInteger(localDTI.dtHour,2)+":"+FileUtils.formatInteger(localDTI.dtMinute,2)+":"+
					FileUtils.formatDouble(localDTI.dtSecond,2,3);
		}

		return result;
	}
	void et2DTI(double et, DateTimeInfo dti)
	{
		int hour=0, minute=0, year=2000, doy=1;
		double seconds=0.0;

		int leapSecs = 0;
		for (int i = 0; i < leapSecondPairs.size(); i++){
			if (et > leapSecondPairs.get(i).first)
			{
				leapSecs = leapSecondPairs.get(i).second;
			}
		}

		boolean finished = false;
		while (!finished){
			//System.out.println("LS = "+Double.toString(leapSecs));

			double dt = et + secondsPerHalfDay - leapSecs - deltaTA;

			//System.out.println("dt = "+Double.toString(dt));

			double totalDays = Math.abs(dt/secondsPerDay);
			int iDays = (int)totalDays;
			double reder = totalDays - iDays;
			int dayCount = 0;

			if (dt >= 0.0)
			{
				year = 2000;
				doy = 1;

				int day;
				boolean complete = false;
				while (!complete){
					for (day = 1; day <= daysInYear(year); day++){
						if (dayCount == iDays)
						{
							complete = true;
							doy = day;
						}
						dayCount++;
					}
					if (!complete)
					{
						year++;
					}
				}

				double dHour = (reder * secondsPerDay)/secondsPerHour;
				hour = (int)dHour;
				double dMinutes = (dHour - hour)* minutesPerHour;
				minute = (int)dMinutes;
				seconds = (dMinutes - minute) * secondsPerMinute;
			}
			else
			{
				year = 1999;
				doy=daysInYear(year);

				int day;

				boolean complete = false;
				while (!complete){
					for (day = daysInYear(year); day >= 1; day--){
						if (dayCount == iDays)
						{
							complete = true;
							doy = day;
						}
						dayCount++;
					}
					if (!complete)
					{
						year--;
					}
				}

				double dSecs = secondsPerDay*(1.0 - reder);
				double dHour = dSecs/secondsPerHour;
				hour = (int)dHour;
				double dMinutes = (dHour - (double)hour)*minutesPerHour;
				minute = (int)dMinutes;
				seconds = (dMinutes - minute)*secondsPerMinute;
			}

			int m=0;
			int days = 0;
			int daysThisMonth;
			int month = 0;
			int day = 1;

			for (m = 0; m < 12; m++){
				if (m == 1)
				{
					if (isLeapYear(year))
						daysThisMonth = 29;
					else
						daysThisMonth = 28;
				}
				else
				{
					daysThisMonth = daysInMonths[m];
				}

				if ((days + daysThisMonth) >= doy)
				{
					month = m;
					day = doy - days;
					break;
				}
				else
				{
					days += daysThisMonth;
				}
			}

			double lt = calculateLeapTime(year, month, day);

			int ls = 0;
			for (int i = 0; i < leapSecondPairs.size(); i++){
				if (lt > leapSecondPairs.get(i).first)
				{
					ls = leapSecondPairs.get(i).second;
				}
			}
			if (ls != leapSecs)
			{
				leapSecs = ls;
			}
			else
			{
				finished = true;
				dti.dtYear = year;
				dti.dtMonth = month;
				dti.dtDay = day;
				dti.dtDoy = doy;
				dti.dtHour = hour;
				dti.dtMinute = minute;
				dti.dtSecond = seconds;
			}
		}
	}
	void loadLeapSeconds()
	{
		Vector<String> lines = new Vector<String>();

		int i, n;
		boolean beginData = false;

		loadDefaultLines(lines);

		File f = new File(tlsFile);
		if (f.exists())
		{
			try {
				FileUtils.readAsciiFile(tlsFile, lines);
			}
			catch (IOException ioEx)
			{
			}
		}

		n = lines.size();
		for (i = 0; i < n; i++){
			if (lines.get(i).startsWith("\\begindata"))
			{
				beginData = true;
			}
			else if (lines.get(i).startsWith("\\begintext"))
			{
				beginData = false;
			}
			else if (beginData)
			{
				String line = lines.get(i);
				if (line.startsWith("DELTET/DELTA_T_A"))
				{
					//System.out.println(line);
					String str = FileUtils.getStringAfter(line, "=");
					if (str.length() > 0)
					{
						str = str.trim();
						deltaTA = Double.parseDouble(str);
					}
				}
				else if (line.startsWith("DELTET/K"))
				{
					//System.out.println(line);
				}
				else if (line.startsWith("DELTET/EB"))
				{
					//System.out.println(line);
				}
				else if (line.startsWith("DELTET/M"))
				{
					//System.out.println(line);
				}
				else if (line.startsWith("DELTET/DELTA_AT"))
				{
					String str = FileUtils.getStringAfter(line, "(");

					Pair<Double,Integer> p = getLeapSecondPair(str);
					leapSecondPairs.add(p);

					line = lines.get(++i);
					while (!line.endsWith(")")){
						//System.out.println(line);
						//FileUtils.splitOnChar(line, ',', parts);
						p = getLeapSecondPair(line);
						leapSecondPairs.add(p);
						line = lines.get(++i);
					}
					str = FileUtils.getStringBefore(line, ")");
					p = getLeapSecondPair(str);
					leapSecondPairs.add(p);
					//System.out.println(line);
				}
			}
		}
	}
	void loadDefaultLines(Vector<String> lines)
	{
		lines.add("\\begindata");
		lines.add("DELTET/DELTA_T_A       =   32.184");
		lines.add("DELTET/K               =    1.657D-3");
		lines.add("DELTET/EB              =    1.671D-2");
		lines.add("DELTET/M               = (  6.239996D0   1.99096871D-7 )");

		lines.add("DELTET/DELTA_AT        = ( 10,   @1972-JAN-1");
		lines.add("11,   @1972-JUL-1");
		lines.add("12,   @1973-JAN-1");
		lines.add("                13,   @1974-JAN-1");
		lines.add("        14,   @1975-JAN-1");
		lines.add("15,   @1976-JAN-1");
		lines.add("16,   @1977-JAN-1");
		lines.add("17,   @1978-JAN-1");
		lines.add("18,   @1979-JAN-1");
		lines.add("19,   @1980-JAN-1");
		lines.add("20,   @1981-JUL-1");
		lines.add("21,   @1982-JUL-1");
		lines.add("22,   @1983-JUL-1");
		lines.add("23,   @1985-JUL-1");
		lines.add("24,   @1988-JAN-1");
		lines.add("25,   @1990-JAN-1");
		lines.add("26,   @1991-JAN-1");
		lines.add("27,   @1992-JUL-1");
		lines.add("28,   @1993-JUL-1");
		lines.add("29,   @1994-JUL-1");
		lines.add("30,   @1996-JAN-1");
		lines.add("31,   @1997-JUL-1");
		lines.add("32,   @1999-JAN-1");
		lines.add("33,   @2006-JAN-1");
		lines.add("34,   @2009-JAN-1");
		lines.add("35,   @2012-JUL-1 )");
		lines.add("\\begintext");

	}
	Pair<Double,Integer> getLeapSecondPair(String s)
	{
		Pair<Double,Integer> result = null;

		Vector<String> parts = new Vector<String>();
		FileUtils.splitOnChar(s, ',', parts);
		if (parts.size() == 2)
		{
			int numSecs = Integer.parseInt(parts.get(0).trim());
			String dateStr = FileUtils.getStringAfter(parts.get(1), "@");
			FileUtils.splitOnChar(dateStr, '-', parts);
			if (parts.size() == 3)
			{
				int year = Integer.parseInt(parts.get(0));
				String monthStr = parts.get(1);
				int dayOfMonth = Integer.parseInt(parts.get(2).trim());
				int month = 0;

				boolean found = false;

				for (int i = 0; (i < monthStrings.length) && !found; i++){
					if (monthStr.compareToIgnoreCase(monthStrings[i]) == 0)
					{
						found = true;
						month = i;
					}
				}

				if (found)
				{
					result = new Pair<Double,Integer>();
					result.first = new Double(calculateLeapTime(year, month, dayOfMonth));
					result.second = new Integer(numSecs);
				}
			}
		}

		return result;
	}
	double calculateLeapTime(int year, int month, int day)
	{
		double result = -1.0e9;

		double deltaDays = 0;
		if (year >= 2000)
		{
			for (int y = 2000; y < year; y++){
				deltaDays += daysInYear(y);
			}

			for (int m = 0; m < month; m++){
				if (m == 1)
				{
					if (isLeapYear(year))
						deltaDays += 29;
					else
						deltaDays += 28;
				}
				else
				{
					deltaDays += daysInMonths[m];
				}
			}

			//
			//	J2000 actually starts at noon on January 1, 2000...so subtract half day from the year 2000
			//
			deltaDays -= 0.50;

			result = deltaDays * secondsPerDay + (double)(day-1)*secondsPerDay;
		}
		else if (year < 2000)
		{
			for (int y = 1999; y > year; y--){
				deltaDays += daysInYear(y);
			}

			int daysInThisYear = daysInYear(year);

			int dayCount = 0;

			for (int m = 0; m < month; m++){
				if (m == 1)
				{
					if (isLeapYear(year))
						dayCount += 29;
					else
						dayCount += 28;
				}
				else
				{
					dayCount += daysInMonths[m];
				}
			}

			dayCount += (day - 1);

			deltaDays += daysInThisYear - dayCount;

			//
			//	J2000 actually starts at noon on January 1, 2000...so add half a day for the year 2000
			//
			deltaDays += 0.50;

			result = -deltaDays * secondsPerDay;
		}

		return result;
	}
	public static int daysInYear(int year)
	{
		int result = 365;
		if (DateTimeInfo.isLeapYear(year))
			result++;
		return result;
	}
	public static boolean isLeapYear(int year)
	{
		boolean result = false;

		//
		//	Generally every 4 years is a leap year with a few exceptions
		//
		if ((year%4) == 0)
		{
			//
			//	Centurial years in general are not leap years, unless they
			//	are evenly divisible by 400 (i.e. the year 2000)
			//
			if ((year%100) == 0)
			{
				if ((year%400) == 0)
				{
					result = true;
				}
			}
			else
			{
				result = true;
			}
		}

		return result;
	}
	public void updateLeapSecondFileList()
	{
		int i, n;

		fileNumber = -1;
		leapSecondPairs.clear();

		tlsFiles.clear();
		File defFolder = new File(".");
		currFolder = defFolder.getAbsolutePath();

		//System.out.println("Current folder = "+currFolder);

		Vector<File> files = new Vector<File>();

		FileUtils.getFilesInFolder(currFolder, files);

		n = files.size();

		for (i = 0; i < n; i++){
			String fileStr = files.get(i).getName();
			if ((fileStr != null) && (fileStr.length() > 0))
			{
				String upFileStr = fileStr.toUpperCase();
				if (upFileStr.startsWith("NAIF") && upFileStr.endsWith(".TLS"))
				{
					tlsFiles.add(files.get(i));
					int fileNum = getFileNumber(fileStr);
					//System.out.println(files.get(i).getAbsolutePath()+"  # = "+fileNum);
					if (fileNum > fileNumber)
					{
						fileNumber = fileNum;
						tlsFile = files.get(i).getAbsolutePath();
					}
				}
			}
		}
	}
	int getFileNumber(String fileStr)
	{
		int result = 0;

		String preExt = FileUtils.getFilenameBeforeExtension(fileStr);
		String numStr = preExt.substring(4);
		result = Integer.parseInt(numStr);

		return result;
	}
	public static boolean fromString(String s, DateTimeInfo dti)
	{
		boolean result = false;

		int year, hours, minutes, doy;
		double seconds;

		String args[] = s.split("-");
		if (args.length == 2)
		{
			if (FileUtils.isInteger(args[0]))
			{
				year = Integer.parseInt(args[0]);

				String args2[] = args[1].split("T");
				if (args2.length == 2)
				{
					if (FileUtils.isInteger(args2[0]))
					{
						doy = Integer.parseInt(args2[0]);
						String args3[] = args2[1].split(":");
						if (args3.length == 3)
						{
							if (FileUtils.isInteger(args3[0]) && FileUtils.isInteger(args3[1]) && FileUtils.isDouble(args3[2]))
							{
								hours = Integer.parseInt(args3[0]);
								minutes = Integer.parseInt(args3[1]);
								seconds = Double.parseDouble(args3[2]);

								dti.dtYear = year;
								dti.dtDoy = doy;
								dti.dtHour = hours;
								dti.dtMinute = minutes;
								dti.dtSecond = seconds;

								int days = 0;
								for (int m = 0; m < 12; m++){
									if (m == 1)
									{
										int daysThisMonth = 28;
										if (isLeapYear(year))
											daysThisMonth++;
										if ((days + daysThisMonth) < doy)
										{
											days += daysThisMonth;
											dti.dtMonth = m;
										}
									}
									else if ((days + daysInMonths[m]) < doy)
									{
										days += daysInMonths[m];
										dti.dtMonth = m;
									}
								}

								dti.dtMonth++;

								dti.dtDay = dti.dtDoy - days;

								result = true;
							}
						}
					}
				}
			}
		}
		else if (args.length == 3)
		{
			year = Integer.parseInt(args[0]);
			int month = Integer.parseInt(args[1]);

			String args2[] = args[2].split("T");
			if (args2.length == 2)
			{
				if (FileUtils.isInteger(args2[0]))
				{
					int day = Integer.parseInt(args2[0]);
					doy = DateTimeInfo.getDOY(year, month-1, day);
					String args3[] = args2[1].split(":");
					if (args3.length == 3)
					{
						if (FileUtils.isInteger(args3[0]) && FileUtils.isInteger(args3[1]) && FileUtils.isDouble(args3[2]))
						{
							hours = Integer.parseInt(args3[0]);
							minutes = Integer.parseInt(args3[1]);
							seconds = Double.parseDouble(args3[2]);

							dti.dtYear = year;
							dti.dtDoy = doy;
							dti.dtHour = hours;
							dti.dtMinute = minutes;
							dti.dtSecond = seconds;
							dti.dtDay = day;
							dti.dtMonth = month - 1;

							result = true;
						}
					}
				}
			}
		}

		return result;
	}
//	public static void main(String args[])
//	{
//		testTimeCalc();
//	}
//	public static void testTimeCalc()
//	{
//		//double t = 428500867.234;
//		//TimeCalc tc = TimeCalc.getSingleton();
//		//System.out.println(tc.et2UTC(t, "ISOC"));
//		/*
//		2012-245T12:01:00.050 = 399772927.232609
//		1981-03-22T12:01:00.050 = -592617488.764378
//		1995-245T12:01:00.050 = -136684678.767397
//		1996-245T12:01:00.050 = -105148677.767393
//		2002-09-22T12:01:00.050 = 85968124.232384
//		1992-09-22T12:01:00.050 = -229564680.767620
//		1999-01-22T12:01:00.050 = -29721475.765466
//		2000-12-22T12:01:00.050 = 30758524.233661
//		*/
//
//		/*
//		int i;
//		double t;
//		String s;
//
//		String utcArray[] = {"2012-245T12:01:00.050", "1981-03-22T12:01:00.050", "1995-245T12:01:00.050", "1996-245T12:01:00.050",
//				"2002-09-22T12:01:00.050", "1992-09-22T12:01:00.050", "1999-01-22T12:01:00.050", "2000-12-22T12:01:00.050",
//				"1994-06-30T23:59:57.000"};
//		double resultsFromSpice[] = {399772927.232609, -592617488.764378, -136684678.767397, -105148677.767393,
//				85968124.232384, -229564680.767620, -29721475.765466, 30758524.233661, -173707142.815886};
//
//		//TimeCalc tc = new TimeCalc();
//
//		String centerTime = TimeCalc.et2UTC(3.97286781492619e8);
//		System.out.println("Center time = "+centerTime);
//		*/
//
//		/*
//		for (i = 0; i < utcArray.length; i++){
//			t = TimeCalc.utc2ET(utcArray[i]);
//			System.out.println("utc2ET: Input value="+utcArray[i]+"  Output value="+Double.toString(t)+"  Correct value="+
//					Double.toString(resultsFromSpice[i])+"  Difference="+Double.toString(resultsFromSpice[i]-t));
//			s = TimeCalc.et2UTC(t);
//			System.out.println("et2UTC: Input value="+Double.toString(t)+"  ISOD value="+s+"  ISOC value="+
//					TimeCalc.et2UTC(t, "ISOC"));
//			System.out.println("");
//			System.out.println("");
//		}
//		*/
//
//		int i, n;
//		Vector<MoonData> mdVec = MoonData.processCSV("C:\\data\\april_2017_opportunity.csv");
//
//		n = mdVec.size();
//		System.out.println("Vector size = "+n);
//
//		for (i = 0; i < n; i++){
//			double t1 = mdVec.get(i).et;
//			String utc1 = mdVec.get(i).domUTC;
//			double t2 = TimeCalc.utc2ET(utc1);
//			String utc2 = TimeCalc.et2UTC(t1, "ISOC");
//
//			System.out.println("1: "+utc1+ " = " + Double.toString(t1)+"  ISOD = "+mdVec.get(i).doyUTC);
//			System.out.println("2: "+utc2+ " = " + Double.toString(t2)+"  ISOD = "+TimeCalc.et2UTC(t1));
//			System.out.println();
//		}
//	}
}
