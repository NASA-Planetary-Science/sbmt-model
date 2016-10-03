package edu.jhuapl.sbmt.model.europa.util;

public class BitArray {
	public byte data[] = null;
	int blockSize = 256;
	int currentBit = 0;
	public BitArray()
	{
		initialize();
	}
	public BitArray(int size)
	{
		if (size > 0)
		{
			blockSize = size;
			while ((blockSize%4) != 0) blockSize++;
		}
		initialize();
	}
	public BitArray(byte array[])
	{
		data = array;
	}
	void initialize()
	{
		data = new byte[blockSize];
		for (int i = 0; i < data.length; i++)
			data[i] = 0;
		currentBit = 0;
	}
	public void clear()
	{
		data = null;
		currentBit = 0;
	}
	public int getLength()
	{
		int result = 1 + (currentBit/8);
		return result;
	}
	public int getPaddedLength()
	{
		int result = getLength();
		
		while ((result%8) != 0) result++;
		
		return result;
	}
	public BitArray getSubArray(int offset, int length)
	{
		BitArray result = null;
		
		byte subData[] = new byte[length];
		
		for (int i = 0; i < length; i++){
			subData[i] = data[offset + i];
		}
		
		result = new BitArray(subData);
		
		return result;
	}
	void expandArray()
	{
		if (data == null)
		{
			initialize();
		}
		
		int len = data.length + blockSize;
		while ((len%8) != 0) len++;
		
		byte array[] = new byte[len];
		
		for (int i = 0; i < array.length; i++)
			array[i] = 0;
		
		for (int i = 0; i < data.length; i++)
			array[i] = data[i];
		
		data = array;
	}
	public void addNumber(long number, int nBits)
	{
		if (data == null)
			initialize();
		
		int lastByte = (currentBit + nBits)/8;
		
		while (lastByte >= data.length)
			expandArray();
		
		for (int bit = 0; bit < nBits; bit++){
			int dstBitPosition = ((bit+currentBit)%8);
			byte bitValue = (byte)((number>>(nBits-bit-1))&1);
			
			data[(bit+currentBit)/8] |= (byte)((bitValue<<dstBitPosition)&0xff);
			//System.out.println(dstBitPosition+"-"+bitValue);
		}
		
		currentBit += nBits;
	}
	public long getNumber(int nBits, long bitOffset)
	{
		long result = 0;
		
		byte bitValue = 0;
		long lastByte = (bitOffset + nBits)/8;
		if (lastByte < data.length)
		{	
			for (int bit =0; bit < nBits; bit++){
				long byteNumber = (bit + bitOffset)/8;
				long srcBitPosition = ((bit + bitOffset)%8);
				
				try {
					bitValue = (byte)((data[(int)byteNumber]>>srcBitPosition)&1);
				}
				catch(Exception ex)
				{
					System.out.println(ex);
					ex.printStackTrace();
				}
				
				//System.out.println(byteNumber+"-"+srcBitPosition+"-"+bitValue);
				
				int dstBitPosition = nBits - bit - 1;
				
				result |= ((long)bitValue<<dstBitPosition);
			}
		}
		
		return result;
	}
	/*
	public static int getMinimumBits(int number)
	{
		int result = 0;
		
		int val = 1;
		int pow = 1;
		for (int i = 0; (i < 31) && (result==0); i++, pow++){
			val *= 2;
			if (number <= (val-1))
			{
				result = pow;
			}
		}
		
		return result;
	}
	*/
	public static int getMinimumBits(long value)
	{
		int result = 0;
		
		String bitStr = getBitString(value);
		if (bitStr.contains("1"))
			result = 64 - bitStr.indexOf('1');
		
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
	/*
	public static int getMinimumBits(long number)
	{
		int result = 0;
		
		long val = 1;
		int pow = 1;
		for (int i = 0; (i < 63) && (result==0); i++, pow++){
			val *= 2;
			if (number <= (val-1))
			{
				result = pow;
			}
		}
		
		return result;
	}
	*/
	public static void printBits(BitArray ba, int startByte, int stopByte)
	{
		for (int b = startByte; b <= stopByte; b++){
			System.out.print(BasicCodec.getBitString(ba.data[b]) + " ");
		}
		System.out.println("");
	}
	public static void printBitArray(BitArray ba, int nBits)
	{
		String s = "";
		int nBytes = ba.currentBit/8 + 1;
		int ndx = 0;
		
		for (int i = 0; i < nBytes; i++){
			for (int j = 0; j < 8; j++){
				byte b = ba.data[i];
				int bitValue = (b>>j)&1;
				
				if ((ndx%nBits)==0)
					s += " ";
				s += (bitValue == 1) ? '1' : '0';
				ndx++;
			}
		}
		System.out.println("Bit stream="+s);
	}
	public static void printIntegerBits(int num)
	{
		String s = "";
		
		for (int bit = 31; bit >= 0; bit--){
			int bitValue = ((num>>bit)&1);
			s += bitValue;
			if ((bit%8) == 0)
			{
				s += " ";
			}
		}
		System.out.println(num+" = "+s);
	}
	public static void testBitArray()
	{
		int i, nBits, currentBit;
		long num;
		BitArray ba = new BitArray();
		
		int numbers[] = {11,13,7,31,8,9,21,23,22,29,1};
		
		nBits = 5;
		for (i = 0; i < numbers.length; i++)
			ba.addNumber(numbers[i], nBits);
		
		currentBit = 0;
		for (i = 0; i < numbers.length; i++){
			num = ba.getNumber(nBits, currentBit);
			currentBit += nBits;
			System.out.println("Number out = "+num+" should be "+numbers[i]);
		}
		
		System.out.println("Minimum Bits for 31 = "+getMinimumBits(31));
		printIntegerBits(31);
		System.out.println("Minimum Bits for 32 = "+getMinimumBits(32));
		printIntegerBits(32);
		System.out.println("Minimum Bits for 1 = "+getMinimumBits(1));
		printIntegerBits(1);
		System.out.println("Minimum Bits for 7 = "+getMinimumBits(7));
		printIntegerBits(7);
		System.out.println("Minimum Bits for 65 = "+getMinimumBits(65));
		printIntegerBits(65);
		System.out.println("Minimum Bits for 255 = "+getMinimumBits(255));
		printIntegerBits(255);
		System.out.println("Minimum Bits for 256 = "+getMinimumBits(256));
		printIntegerBits(256);
		System.out.println("Minimum Bits for 16 = "+getMinimumBits(16));
		printIntegerBits(16);
		System.out.println("Minimum Bits for 0 = "+getMinimumBits(0));
		//printIntegerBits(0);
	}
	public static void main(String[] args)
	{
		BitArray.testBitArray();
	}
}
