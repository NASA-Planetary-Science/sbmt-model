package edu.jhuapl.sbmt.model.europa.pds;

public class PDSTypes {
	public enum ProjectionTypes { Unknown, SimpleCylindrical, PolarStereographic, ObliqueStereographic, EquiRectangular}
	public enum UnitTypes { Unknown, Kilometers, Meters}
	public enum SampleTypes { Unknown, LSB_INTEGER, MSB_INTEGER, LSB_UNSIGNED_INTEGER, MSB_UNSIGNED_INTEGER, PC_REAL, REAL}
	public enum BandStorageTypes { Unknown, SAMPLE_INTERLEAVED, BAND_INTERLEAVED_BY_LINE, BAND_SEQUENTIAL}
	
	public static String bandStorageKey = "BAND_STORAGE_TYPE";
	public static String bandCountKey = "BANDS";
	public static String sampleBitsKey = "SAMPLE_BITS";
	public static String imageLinesKey = "LINES";
	public static String imageSamplesKey = "LINE_SAMPLES";
	public static String sampleTypeKey = "SAMPLE_TYPE";
	public static String imageDescKey = "DESCRIPTION";
	
	public static SampleTypes getSampleType(String s)
	{
		SampleTypes result = SampleTypes.Unknown;
		
		if (s.compareToIgnoreCase("MSB_INTEGER") == 0)
			result = SampleTypes.MSB_INTEGER;
		else if (s.compareToIgnoreCase("LSB_INTEGER") == 0)
			result = SampleTypes.LSB_INTEGER;
		else if (s.compareToIgnoreCase("LSB_UNSIGNED_INTEGER") == 0)
			result = SampleTypes.LSB_UNSIGNED_INTEGER;
		else if (s.compareToIgnoreCase("MSB_UNSIGNED_INTEGER") == 0)
			result = SampleTypes.MSB_UNSIGNED_INTEGER;
		else if (s.compareToIgnoreCase("PC_REAL") == 0)
			result = SampleTypes.PC_REAL;
		else if (s.compareToIgnoreCase("REAL") == 0)
			result = SampleTypes.REAL;
		
		return result;
	}
	public static String getSampleTypeString(SampleTypes type)
	{
		String result = new String("Unknown");
		
		switch (type){
			case MSB_INTEGER: result = "MSB_INTEGER"; break;
			case MSB_UNSIGNED_INTEGER: result = "MSB_UNSIGNED_INTEGER"; break;
			case LSB_INTEGER: result = "LSB_INTEGER"; break;
			case LSB_UNSIGNED_INTEGER: result = "LSB_UNSIGNED_INTEGER"; break;
			case PC_REAL: result = "PC_REAL"; break;
			case REAL: result = "REAL"; break;
		}
		
		return result;
	}
	public static BandStorageTypes getBandType(String s)
	{
		BandStorageTypes result = BandStorageTypes.Unknown;
		
		if (s.compareToIgnoreCase("LINE_INTERLEAVED") == 0)
			result = BandStorageTypes.BAND_INTERLEAVED_BY_LINE;
		else if (s.compareToIgnoreCase("SAMPLE_INTERLEAVED") == 0)
			result = BandStorageTypes.SAMPLE_INTERLEAVED;
		else if (s.compareToIgnoreCase("BAND_SEQUENTIAL") == 0)
			result = BandStorageTypes.BAND_SEQUENTIAL;
		
		return result;
	}
	public static String getBandStorageString(BandStorageTypes type)
	{
		String result = new String("Unknown");
		
		switch (type){
			case BAND_INTERLEAVED_BY_LINE: result = "LINE_INTERLEAVED"; break;
			case SAMPLE_INTERLEAVED: result = "SAMPLE_INTERLEAVED"; break;
			case BAND_SEQUENTIAL: result = "BAND_SEQUENTIAL"; break;
		}
		
		return result;
	}
	public static int getSampleTypeCode(SampleTypes type)
	{
		int result = 0;
		
		switch (type){
			case LSB_INTEGER: result = 1; break;
			case MSB_INTEGER: result = 2; break;
			case LSB_UNSIGNED_INTEGER: result = 3; break;
			case MSB_UNSIGNED_INTEGER: result = 4; break;
			case PC_REAL: result = 5; break;
			case REAL: result = 6; break;
		}
		
		return result;
	}
	public static SampleTypes getSampleTypeFromCode(int code)
	{
		SampleTypes result = SampleTypes.Unknown;
		
		switch (code){
			case 1: result = SampleTypes.LSB_INTEGER; break;
			case 2: result = SampleTypes.MSB_INTEGER; break;
			case 3: result = SampleTypes.LSB_UNSIGNED_INTEGER; break;
			case 4: result = SampleTypes.MSB_UNSIGNED_INTEGER; break;
			case 5: result = SampleTypes.PC_REAL; break;
			case 6: result = SampleTypes.REAL; break;
		}
		
		return result;
	}
	public static int getBandStorageCode(BandStorageTypes type)
	{
		int result = 0;
		
		switch (type){
			case SAMPLE_INTERLEAVED: result = 1; break;
			case BAND_INTERLEAVED_BY_LINE: result = 2; break;
			case BAND_SEQUENTIAL: result = 3; break;
		}
		
		return result;
	}
	public static BandStorageTypes getBandStorageTypeFromCode(int code)
	{
		BandStorageTypes result = BandStorageTypes.Unknown;
		
		switch (code){
			case 1: result = BandStorageTypes.SAMPLE_INTERLEAVED; break;
			case 2: result = BandStorageTypes.BAND_INTERLEAVED_BY_LINE; break;
			case 3: result = BandStorageTypes.BAND_SEQUENTIAL; break;
		}
		
		return result;
	}
	public static boolean isIntegerType(SampleTypes type)
	{
		boolean result = false;
		
		if ((type == SampleTypes.MSB_INTEGER) || (type == SampleTypes.MSB_UNSIGNED_INTEGER) ||
			(type == SampleTypes.LSB_INTEGER)|| (type == SampleTypes.LSB_UNSIGNED_INTEGER))
		{
			result = true;
		}
		
		return result;
	}
}