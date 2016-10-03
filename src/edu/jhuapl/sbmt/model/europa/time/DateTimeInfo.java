package edu.jhuapl.sbmt.model.europa.time;

import edu.jhuapl.sbmt.model.europa.util.FileUtils;

public class DateTimeInfo {
	public int dtYear=0;
	public int dtMonth=0;		// 0-11
	public int dtDay=0;		// 1-31
	public int dtHour=0;		// 0-23
	public int dtMinute=0;	// 0-59
	public double dtSecond=0.0;	// 0-59 plus decimal value
	public int dtDoy=0;

	public static double secondsPerDay = 86400.0;
	public static int []daysInMonths =
		{31,28,31,30,31,30,31,31,30,31,30,31};
	public static String []shortDaysOfWeek = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
	public static String []daysOfWeek = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
	public static String []monthNames = {"January","February","March","April","May","June","July","August",
		"September","October","November","December"};
	public static String []shortMonths = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

	public DateTimeInfo()
	{
	}
	public DateTimeInfo(int year, int month, int day, int hour, int minute, double secs)
	{
		dtYear = year;
		dtMonth = month;
		dtDay = day;
		dtHour = hour;
		dtMinute = minute;
		dtSecond = secs;
	}
	public DateTimeInfo(int year, int doy, int hour, int minute, double secs)
	{
		int month = 0;
		int day = doy;

		int []days = {31,28,31,30,31,30,31,31,30,31,30,31};

		if (isLeapYear(year)) days[1] = 29;

		for (int i = 0; i < 12; i++){
			if (day <= days[i])
			{
				month = i;
				break;
			}
			else
			{
				day -= days[i];
			}
		}

		dtYear = year;
		dtMonth = month;
		dtDay = day;
		dtHour = hour;
		dtMinute = minute;
		dtSecond = secs;
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
	public static int getDOY(int year, int month, int day)
	{
		int result = 0;

		int i;

		for (i = 0; i < month; i++){
			if (DateTimeInfo.isLeapYear(year) && (i == 1))
				result += 29;
			else
				result += DateTimeInfo.daysInMonths[i];
		}

		result += day;

		return result;
	}
	public static int getDay(int year, int doy)
	{
		int result = -1;

		int total = 0;

		for (int month = 0; (month < 12) && (result < 0); month++){
			int dayCount = 0;
			if (DateTimeInfo.isLeapYear(year) && (month == 1))
				dayCount = 29;
			else
				dayCount = DateTimeInfo.daysInMonths[month];

			for (int day = 1; (day <= dayCount) && (result < 0); day++){
				total++;
				if (total == doy)
				{
					result = day;
				}
			}
		}

		return result;
	}
	public static int getMonth(int year, int doy)
	{
		int result = -1;

		int total = 0;

		for (int month = 0; (month < 12) && (result < 0); month++){
			int dayCount = 0;
			if (DateTimeInfo.isLeapYear(year) && (month == 1))
				dayCount = 29;
			else
				dayCount = DateTimeInfo.daysInMonths[month];

			for (int day = 1; (day <= dayCount) && (result < 0); day++){
				total++;
				if (total == doy)
				{
					result = month;
				}
			}
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
	public int getDayOfYear()
	{
		return getDOY(dtYear, dtMonth, dtDay);
	}
	public String getFullDateTimeString()
	{
		String doyStr = FileUtils.formatInteger(getDayOfYear(),3);
		String hourStr = FileUtils.formatInteger(dtHour, 2);
		String minStr = FileUtils.formatInteger(dtMinute, 2);
		int milliSeconds = (int)(0.50 + 1000.0 * (dtSecond - (int)dtSecond));
		String secStr = FileUtils.formatInteger((int)dtSecond, 2) + "." + FileUtils.formatInteger(milliSeconds,3);

		String result = new String(dtYear+"-"+doyStr+"T"+hourStr+":"+minStr+":"+secStr);

		return result;
	}
	public String getNumericDateString()
	{
		String result = new String("");
		result += dtYear;
		result += FileUtils.formatInteger(dtMonth+1,2);
		result += FileUtils.formatInteger(dtDay,2);

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
								dti.dtDay = getDay(year, doy);
								dti.dtMonth = getMonth(year, doy);

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
							dti.dtMonth = getMonth(year, doy);

							result = true;
						}
					}
				}
			}
		}

		return result;
	}
	public static boolean isValidTimeString(String s)
	{
		boolean result = false;

		int year, hours, minutes, doy;
		double seconds=1.0e12;

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

								if ((year > 1500) && (year < 3000))
								{
									int maxDays = 365;
									if (isLeapYear(year))
										maxDays = 366;
									if ((doy >= 1) && (doy <= maxDays))
									{
										if ((hours >= 0) && (hours < 24))
										{
											if ((minutes >= 0) && (minutes < 60))
											{
												if ((seconds >= 0.0) && (seconds < 60.0))
												{
													result = true;
												}
											}
										}
									}
								}
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

							if ((year > 1500) && (year < 3000))
							{
								if ((month >= 1) && (month < 13))
								{
									int maxDays = daysInMonths[month-1];
									if ((month == 2) && isLeapYear(year))
										maxDays++;

									if ((day >= 1) && (day <= maxDays))
									{
										if ((hours >= 0) && (hours < 24))
										{
											if ((minutes >= 0) && (minutes < 60))
											{
												if ((seconds >= 0.0) && (seconds < 60.0))
												{
													result = true;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return result;
	}
}
