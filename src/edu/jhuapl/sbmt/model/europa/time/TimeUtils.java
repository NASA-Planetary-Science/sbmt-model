package edu.jhuapl.sbmt.model.europa.time;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import edu.jhuapl.sbmt.model.europa.util.FileUtils;

public class TimeUtils {
	public static double secondsPerDay = 86400.0;
	public static double secondsPerHalfDay = 43200.0;
	public static double currentLeapSeconds = 34.0;
	public static double etMinusTAI = 32.184;
	public static double hoursPerDay = 24.0;
	public static double minutesPerDay = 1440.0;

	public static void main(String[] argv)
	{
		String dequax = "2008-131T23:12:00.809";
		String aequax = "2008-132T00:07:26.459";

		double tDequax = TimeUtils.utc2ETValue(dequax);
		double tAequax = TimeUtils.utc2ETValue(aequax);

		DateTimeInfo dtiD = TimeUtils.fromJ2000(tDequax);
		DateTimeInfo dtiA = TimeUtils.fromJ2000(tAequax);

		int doyDequax = dtiD.getDayOfYear();
		int doyAequax = dtiA.getDayOfYear();

		System.out.println("DEQUAX: "+dequax+"  "+Double.toString(tDequax)+" "+dtiD.dtYear+"-"+
				FileUtils.formatInteger(doyDequax,3)+"T"+dtiD.dtHour+":"+dtiD.dtMinute+":"+
				FileUtils.formatDouble(dtiD.dtSecond,3));

		System.out.println("AEQUAX: "+aequax+"  "+Double.toString(tAequax)+" "+dtiA.dtYear+"-"+
				FileUtils.formatInteger(doyAequax,3)+"T"+dtiA.dtHour+":"+dtiA.dtMinute+":"+
				FileUtils.formatDouble(dtiA.dtSecond,3));
	}

	public static DateTimeInfo getSystemTime()
	{
		String[] ids = TimeZone.getAvailableIDs(-5 * 60 * 60 * 1000);
		SimpleTimeZone edt = new SimpleTimeZone(-5 * 60 * 60 * 1000, ids[0]);
		edt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
		edt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);

		Calendar gc = new GregorianCalendar(edt);

		DateTimeInfo dti = new DateTimeInfo();

		dti.dtYear = gc.get(Calendar.YEAR);
		dti.dtMonth = gc.get(Calendar.MONTH);
		dti.dtDay = gc.get(Calendar.DAY_OF_MONTH);
		dti.dtHour = gc.get(Calendar.HOUR_OF_DAY);
		dti.dtMinute = gc.get(Calendar.MINUTE);
		dti.dtSecond = gc.get(Calendar.SECOND) + (double)gc.get(Calendar.MILLISECOND)/1000.0;

		return dti;
	}
	public static String getSystemTimestamp()
	{
		String result = "";

		DateTimeInfo dti = getSystemTime();

		result = Integer.toString(dti.dtYear) + "-" + FileUtils.formatInteger(dti.dtMonth+1,2) + "-" + FileUtils.formatInteger(dti.dtDay,2) + "T" +
				FileUtils.formatInteger(dti.dtHour,2) + ":" + FileUtils.formatInteger(dti.dtMinute,2) + ":" + FileUtils.formatDouble(dti.dtSecond,2,3);

		return result;
	}
	public static String getSystemDateStamp()
	{
		String result = "";

		DateTimeInfo dti = getSystemTime();

		result = Integer.toString(dti.dtYear) + "-" + FileUtils.formatInteger(dti.dtMonth+1,2) + "-" + FileUtils.formatInteger(dti.dtDay,2);

		return result;
	}
	public static String getSystemDateStampNoDashes()
	{
		String result = "";

		DateTimeInfo dti = getSystemTime();

		result = Integer.toString(dti.dtYear) + FileUtils.formatInteger(dti.dtMonth+1,2) + FileUtils.formatInteger(dti.dtDay,2);

		return result;
	}
	public static double utc2ETValue(String s)
	{
		double result = getJ2000Time(s); //	after this statement we have essentially UTC measured in seconds since J2000 epoch

		result += currentLeapSeconds; // after this statement result is roughly in TAI
		result += etMinusTAI; // after this statement result is in Ephemeris Time (ET)

		return result;
	}
	public static double et2ETValue(String s)
	{
		return getJ2000Time(s);
	}
	public static String et2UTCString(double et)
	{
		//double t = et - etMinusTAI - currentLeapSeconds; // this line converts from et back to UTC
		DateTimeInfo dti = fromJ2000(et);
		return dti.getFullDateTimeString();
	}
	public static DateTimeInfo et2UTCDateTime(double et)
	{
		return fromJ2000(et);
	}
	public static String et2ETString(double et)
	{
		DateTimeInfo dti = fromJ2000(et);
		return dti.getFullDateTimeString();
	}
	public static double getJ2000Time(String s)
	{
		double result = 0.0;

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

								//int secs = (int)seconds;
								//double msc = seconds - secs;
								//double usc = 0.0;

								double secondsIn12Hours = 43200.0;
								double secondsIn1Day = 86400.0;

								int lyr;
								int dyr = year - 2000;
								result = (doy - 1) * 86400.0
										+ hours * 3600.0
										+ minutes * 60.0
										+ seconds;

								if (dyr < 0)
								{
									lyr = dyr / 4;
								}
								else
								{
									lyr = (dyr + 3) / 4;
								}

								result = result + (dyr * 365 + lyr) * secondsIn1Day - secondsIn12Hours;
							}
						}
					}
				}
			}
		}

		return result;
	}
	public static double getJ2000(DateTimeInfo dti)
	{
		double result = 0.0;

		//int secs = (int)dti.dtSecond;
		//double msc = dti.dtSecond - secs;
		//double usc = 0.0;

		double secondsIn12Hours = 43200.0;
		double secondsIn1Day = 86400.0;

		int leapDays;
		int dyr = dti.dtYear - 2000;

		result = (dti.getDayOfYear() - 1) * secondsIn1Day
				+ (double)dti.dtHour * 3600.0
				+ (double)dti.dtMinute * 60.0
				+ dti.dtSecond;
				//+ ((msc * 1000.0 + usc) / 1000000.0);

		if (dyr < 0)
		{
			leapDays = dyr / 4;
		}
		else
		{
			leapDays = (dyr + 3) / 4;
		}

		result = result + (dyr * 365 + leapDays) * secondsIn1Day - secondsIn12Hours;

		return result;
	}
	public static int getDayOfWeek(DateTimeInfo dti)
	{
		int result = 0;

		double jd = getJulianDate(dti);
		int a = (int)(jd + 1.5);
		result = a%7;
		result = ((result>=0) ? result : 0);
		result = ((result<=6) ? result : 6);

		return result;
	}
	public static double getJulianDate(DateTimeInfo dtInfo)
	{
		double result = 0.0;

		int m = dtInfo.dtMonth+1;
		int y = dtInfo.dtYear;
		double d = dtInfo.dtDay;
		double h = dtInfo.dtHour;
		double min = dtInfo.dtMinute;
		double sec = dtInfo.dtSecond;

		d += (h + min/60.0 + sec/3600.0) / 24.0;

		if (m == 1 || m == 2)
		{
			y -= 1;
			m += 12;
		}

		int A = (int)((double)y/100.0);
		int B = 0;
		if (y > 1582)
		{
			B = 2 - A + (int)(A/4.0);
		}
		result = (int)(365.25 * (y + 4716)) + (int)(30.6001 * (m+1))
			+ d + B - 1524.5;

		return result;
	}
	public static DateTimeInfo setJulianDate(double jd)
	{
		DateTimeInfo result = new DateTimeInfo();

		double Z = (double)(int)(jd + 0.50);
		double F = (jd + 0.50) - Z;
		double A, alpha, B, C, D, E;

		if (Z < 2299161)
		{
			A = Z;
		}
		else
		{
			alpha = (int)((Z-1867216.25)/36524.25);
			A = Z + 1 + alpha - (int)(alpha/4.0);
		}

		B = A + 1524;
		C = (int)((B - 122.1)/365.25);
		D = (int)(365.25 * C);
		E = (int)((B - D)/30.6001);

		double DAY = B - D - (int)(30.6001 * E) + F;
		double MONTH = E - 1;
		if (E == 14 || E == 15)
			MONTH = E - 13;
		double YEAR = C - 4716;
		if (MONTH == 1 || MONTH == 2)
			YEAR = C - 4715;

		result.dtMonth = (int)(MONTH - 1);
		result.dtYear = (int)YEAR;
		result.dtDay = (int)DAY;
		result.dtHour = (int)((DAY - result.dtDay) * 24.0001);
		result.dtMinute = (int)((((DAY - result.dtDay) * 24.0001) - result.dtHour) * 60.0001);
		result.dtSecond = (((((DAY - result.dtDay) * 24.0) - result.dtHour) * 60.0) - result.dtMinute) * 60.0;

		return result;
	}
	public static DateTimeInfo fromJ2000(double j2000)
	{
		DateTimeInfo result = null;

		double secondsPerDay = 86400.0;
		double secondsPerHalfDay = 43200.0;

		int year, hour, minute, doy;
		double seconds;

		double days, hours, minutes, partialDay;

		double t = j2000 + secondsPerHalfDay - etMinusTAI - currentLeapSeconds;
		days = Math.abs(t/secondsPerDay);
		year = 2000;

		if (j2000 < 0.0)
		{
			if (t < 0.0)
			{
				year--;

				while (days > DateTimeInfo.daysInYear(year)){
					days = days - DateTimeInfo.daysInYear(year);
					year--;
				}

				double dDays = (double)DateTimeInfo.daysInYear(year) - (double)days;
				doy = (int)dDays;
				double dHours = (dDays - (double)doy)*24.0;
				hour = (int)dHours;
				double dMinutes = (dHours - (double)hour)*60.0;
				minute = (int)dMinutes;
				seconds = (dMinutes - (double)minute)*60.0;
			}
			else
			{
				year = 2000;
				doy = 1;
				double dHours = Math.abs(t)/3600.0;

				hour = (int)dHours;
				double dMinutes = (dHours - (double)hour)*60.0;
				minute = (int)dMinutes;
				seconds = (dMinutes-(double)minute)*60.0;
			}

			result = new DateTimeInfo(year, doy+1, hour, minute, seconds);
		}
		else
		{
			while (days > DateTimeInfo.daysInYear(year)){
				days = days - DateTimeInfo.daysInYear(year);
				year++;
			}

			doy = 1 + (int)days;
			partialDay = days - (int)days;
			hours = partialDay*24.0;
			hour = (int)hours;
			minutes = (hours - hour)*60.0;
			minute = (int)minutes;
			seconds = (minutes - minute)*60.0;

			result = new DateTimeInfo(year, doy, hour, minute, seconds);
		}
		return result;
	}
	public static double toJ2000(String utcStr)
	{
		double result = 0.0;

		boolean noParseError = true;

		int year, hours, minutes, doy;
		double seconds;
		DateTimeInfo dti = new DateTimeInfo();

		String s = utcStr;

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
							}
							else
							{
								noParseError = false;
							}
						}
						else
						{
							noParseError = false;
						}
					}
					else
					{
						noParseError = false;
					}
				}
				else
				{
					noParseError = false;
				}
			}
			else
			{
				noParseError = false;
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
						}
						else
						{
							noParseError = false;
						}
					}
					else
					{
						noParseError = false;
					}
				}
				else
				{
					noParseError = false;
				}
			}
			else
			{
				noParseError = false;
			}
		}

		if (noParseError)
		{
			double deltaDays = 0;

			if (dti.dtYear >= 2000)
			{
				for (year = 2000; year < dti.dtYear; year++){
					deltaDays += DateTimeInfo.daysInYear(year);
				}
				deltaDays += (double)(dti.dtDoy-1);
				deltaDays += (double)dti.dtHour/TimeUtils.hoursPerDay;
				deltaDays += (double)dti.dtMinute/TimeUtils.minutesPerDay;
				deltaDays += (double)dti.dtSecond/TimeUtils.secondsPerDay;

				//
				//	J2000 actually starts at noon on January 1, 2000...so subtract half day from the year 2000
				//
				deltaDays -= 0.50;

				result = deltaDays * TimeUtils.secondsPerDay + etMinusTAI + currentLeapSeconds;
			}
			else if (dti.dtYear < 2000)
			{
				for (year = 1999; year > dti.dtYear; year--){
					deltaDays += DateTimeInfo.daysInYear(year);
				}

				//
				//	calculate the (floating point) number of days since the beginning of the year to the date/time
				//
				double dDoy = (double)(dti.dtDoy-1) + (double)dti.dtHour/TimeUtils.hoursPerDay +
					(double)dti.dtMinute/TimeUtils.minutesPerDay +
					(double)dti.dtSecond/TimeUtils.secondsPerDay;

				//
				//	add the difference between the total days in the year and the days until the date/time
				//
				deltaDays += (double)DateTimeInfo.daysInYear(dti.dtYear) - dDoy;

				//
				//	J2000 actually starts at noon on January 1, 2000...so add half a day for the year 2000
				//
				deltaDays += 0.50;

				result = -deltaDays * TimeUtils.secondsPerDay + etMinusTAI + currentLeapSeconds;
			}
		}

		return result;
	}
	public static double getMarsSolarLongitude(double j2k)
	{
		//
		//	this formulation for determining the solar longitude on
		//	Mars from a time was found in the publication
		//	"Geophysical Research Letters" on August 15, 1997 by
		//	Michael Allison titled: Accurate analytic representations of solar
		//	time and seasons on Mars with applications to the Pathfinder/Survey missions
		//
		double deltaJD2K = TimeUtils.getJulianDate(TimeUtils.fromJ2000(j2k)) - 2451545.0;
		double M = 19.41 + (0.5240212) * deltaJD2K; //	equation 1
		double alphaFMS = 270.39 + (0.5240384) * deltaJD2K; //	equation 2

		double result = alphaFMS +
			(10.691 + 0.00000037 * deltaJD2K) * Math.sin(Math.toRadians(M)) +
			0.623 * Math.sin(Math.toRadians(2.0*M)) + 0.050 * Math.sin(Math.toRadians(3.0 * M)) +
			0.005 * Math.sin(Math.toRadians(4.0 * M)); //	equation 3

		return 360.0 * FileUtils.fractionalPart(1.0 + FileUtils.fractionalPart(result/360.0)); //	equation 8
	}
}
