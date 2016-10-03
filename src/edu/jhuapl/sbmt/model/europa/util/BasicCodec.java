package edu.jhuapl.sbmt.model.europa.util;

public class BasicCodec {
	public static int longToInt(long v)
	{
		return (int)v;
	}
	public static long intToLong(int v)
	{
		long result = 0;
		
		int b1 = (int)((v>>24)&0xff);
		int b2 = (int)((v>>16)&0xff);
		int b3 = (int)((v>>8)&0xff);
		int b4 = (int)(v&0xff);
		
		result = (long)(((b1<<24) | (b2<<16) | (b3<<8) | b4)&0xFFFFFFFFL);
		
		return result;
	}
	//
	//	This method adds a long number to an array of bytes (data). The number is
	//	encoded into nBits. This method does not check to see if the information
	//	contained in the long number can be stored in nBits of data. It assumes the
	//	caller only wants nBits of data to be transfered to the byte array (data).
	//
	//	inputs:
	//		number is the long value to be encoded into the byte array
	//		nBits is the number of bits to use for the encoding
	//		currentBit provides the location within the byte array(data) where the information
	//			is to be encoded.
	//		data is the byte array where the data is to be encoded.
	//
	//	return value:
	//		void
	//
	public static void addNumber(long number, int nBits, int currentBit, byte data[])
	{
		if (data != null)
		{
			int lastByte = (currentBit + nBits)/8;
			
			if (lastByte < data.length)
			{
				for (int bit = 0; bit < nBits; bit++){
					int dstBitPosition = ((bit+currentBit)%8);
					byte bitValue = (byte)((number>>(nBits-bit-1))&1);
					
					data[(bit+currentBit)/8] |= (byte)((bitValue<<dstBitPosition)&0xff);
				}
			}
		}
	}
	//
	//	This method gets a long number from an array of bytes (data). (nBits) of data are taken from
	//	the data array and encoded into the returned long number.
	//
	//	inputs:
	//		nBits is the number of bits to use for the encoding
	//		startBit provides the location within the byte array(data) where the information
	//			is encoded.
	//		data is the byte array where the data is encoded.
	//
	//	return value:
	//		long - a long integer with nBits of data copied from the byte array (data[])
	//
	public static long getNumber(int nBits, int startBit, byte data[])
	{
		long result = 0;
		
		int lastByte = (startBit + nBits)/8;
		if (lastByte < data.length)
		{	
			for (int bit =0; bit < nBits; bit++){
				int byteNumber = (bit + startBit)/8;
				int srcBitPosition = ((bit + startBit)%8);
				
				byte bitValue = (byte)((data[byteNumber]>>srcBitPosition)&1);
				
				int dstBitPosition = nBits - bit - 1;
				
				result |= (bitValue<<dstBitPosition);
			}
		}
		
		return result;
	}
	//
	//	This method determines the minimum number of bits needed to store the provided long
	//	integer (number).
	//
	//	inputs:
	//		value is the number that we want to know the minimum bits for 
	//
	//	return value:
	//		int - minimum number of bits to store numeric value
	//
	//	for example: the number seven is encoded in 8 bits as 00000111, but only the last three
	//	are non-zero bits, so the answer should be three.
	//
	public static int getMinimumBits(long value)
	{
		int result = 0;
		
		String bitStr = getBitString(value);
		result = 64 - bitStr.indexOf('1');
		
		return result;
	}
	public static int getMinimumBits(long values[])
	{
		int result = 0;
		
		for (int i = 0; i < values.length; i++){
			String bitStr = getBitString(values[i]);
			int nBits = 64 - bitStr.indexOf('1');
			if (i == 0)
			{
				result = nBits;
			}
			else if (nBits > result)
			{
				result = nBits;
			}
		}
		
		return result;
	}
	public static String getBitString(long value)
	{
		String result = "";
		
		for (long bit = 63; bit >= 0; bit--){
			long bitValue = ((value>>bit)&1);
			result += (bitValue==1) ? '1' : '0';
		}

		return result;
	}
	public static String getNiceBitString(long value)
	{
		String result = "";
		
		for (long bit = 63; bit >= 0; bit--){
			long bitValue = ((value>>bit)&1);
			result += (bitValue==1) ? '1' : '0';
			if ((bit%8) == 0)
				result += " ";
		}

		return result;
	}
	public static String getBitString(int value)
	{
		String result = "";
		
		for (long bit = 31; bit >= 0; bit--){
			long bitValue = ((value>>bit)&1);
			result += (bitValue==1) ? '1' : '0';
		}

		return result;
	}
	public static String getNiceBitString(int value)
	{
		String result = "";
		
		for (long bit = 31; bit >= 0; bit--){
			long bitValue = ((value>>bit)&1);
			result += (bitValue==1) ? '1' : '0';
			if ((bit%8) == 0)
				result += " ";
		}

		return result;
	}
	public static String getBitString(byte value)
	{
		String result = "";
		
		for (long bit = 7; bit >= 0; bit--){
			long bitValue = ((value>>bit)&1);
			result += (bitValue==1) ? '1' : '0';
		}

		return result;
	}
	public static String getByteBitString(long value)
	{
		String result = "";
		
		for (long bit = 63; bit >= 0; bit--){
			long bitValue = ((value>>bit)&1);
			result += (bitValue==1) ? '1' : '0';
			if ((bit%8) == 0)
				result += ' ';
		}

		return result;
	}
	public static void runTests()
	{
		long value;
		int minBits;
		String s;
		
		value = 7;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
		
		value = 16384;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
		
		value = 32767;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
		
		value = 32768;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
		
		value = -1;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
		
		value = -10;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
		
		value = -32767;
		minBits = BasicCodec.getMinimumBits(value);
		s = BasicCodec.getByteBitString(value);
		
		System.out.println(value+" = "+s+" min bits = "+minBits);
	}
	public static void main(String args[])
	{
		runTests();
	}
}
