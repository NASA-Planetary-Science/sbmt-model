package edu.jhuapl.sbmt.model.europa.pds;

public class PDSSample {
	public int iData;
	public long lData;
	public float fData;
	public double dData;
	public PDSTypes.SampleTypes sampleType = PDSTypes.SampleTypes.Unknown;
	public int sampleBits = 0;
	static int mask7 = makeMask(7);
	public static double noDataValue = -1.1111111e12;
	
	public PDSSample()
	{
	}
	public PDSSample(byte data[], int bitCount, int offset, PDSTypes.SampleTypes type)
	{
		createSample(data, bitCount, offset, type);
	}
	public void createSample(byte data[], int bitCount, int offset, PDSTypes.SampleTypes type)
	{
		sampleType = type;
		sampleBits = bitCount;
		
		switch (sampleType){
			case LSB_INTEGER:
			{
				lData = PDSCodec.decodeLSBSigned(data, bitCount);
				iData = (int)lData;
				fData = (float)iData;
				dData = (double)iData;
			}
			break;
			case MSB_INTEGER:
			{
				lData = PDSCodec.decodeMSBSigned(data, bitCount);
				iData = (int)lData;
				fData = (float)iData;
				dData = (double)iData;
			}
			break;
			case LSB_UNSIGNED_INTEGER:
			{
				lData = PDSCodec.decodeUnsignedLSB(data, bitCount);
				iData = (int)lData;
				fData = (float)iData;
				dData = (double)iData;
			}
			break;
			case MSB_UNSIGNED_INTEGER:
			{
				lData = PDSCodec.decodeUnsignedMSB(data, bitCount);
				iData = (int)lData;
				fData = (float)iData;
				dData = (double)iData;
			}
			break;
			case PC_REAL:
			{
				dData = PDSCodec.decodePCReal(data, bitCount, 0);
				fData = (float)dData;
				iData = (int)(0.50 + dData);
				lData = (long)(0.50 + dData);
			}
			break;
			case REAL:
			{
				dData = PDSCodec.decodeReal(data, bitCount, 0);
				fData = (float)dData;
				iData = (int)(0.50 + dData);
				lData = (long)(0.50 + dData);
			}
			break;
		}
	}
	public void copy(PDSSample samp)
	{
		sampleType = samp.sampleType;
		sampleBits = samp.sampleBits;
		
		iData = samp.iData;
		lData = samp.lData;
		fData = samp.fData;
		dData = samp.dData;
	}
	public static void main(String args[])
	{
		byte b = (byte)255;
		int i1 = (int)b;
		int i2 = (b&0xff);
		
		//System.out.println("b = "+b);
		//System.out.println("i1 = "+i1);
		//System.out.println("i2 = "+i2);
		
		printBits(b);
		printBits(i1);
		printBits(i2);
		
		i1 = 77;
		printBits(i1);
		i1 = ~i1;
		System.out.println("Twos complement");
		printBits(i1);
		i1 = ~i1;
		System.out.println("Twos complement again");
		printBits(i1);
		
		int mask24 = makeMask(24);
		System.out.println("Mask 24");
		printBits(mask24);
		
		int mask23 = makeMask(23);
		System.out.println("Mask 23");
		printBits(mask23);
		
		int mask8 = makeMask(8);
		System.out.println("Mask 8");
		printBits(mask8);
		
		int mask1 = makeMask(1);
		System.out.println("Mast 1");
		printBits(mask1);
		
		int mask0 = makeMask(0);
		System.out.println("Mast 0");
		printBits(mask0);
	}
	public static int makeMask(int bits)
	{
		int result = 0;
		
		for (int i = 0; i < bits; i++){
			result |= (1<<i);
		}
		
		return result;
	}
	public static void encodeInteger(long value, PDSTypes.SampleTypes type, int offset, int bits, byte data[])
	{
		switch (type){
			case LSB_INTEGER:
			{
				PDSCodec.encodeSignedLSB(value, offset, bits, data);
			}
			break;
			case LSB_UNSIGNED_INTEGER:
			{
				PDSCodec.encodeUnsignedLSB(value, offset, bits, data);
			}
			break;
			case MSB_INTEGER:
			{
				PDSCodec.encodeSignedMSB(value, offset, bits, data);
			}
			break;
			case MSB_UNSIGNED_INTEGER:
			{
				PDSCodec.encodeUnsignedMSB(value, offset, bits, data);
			}
			break;
		}
	}
	public static long decodeInteger(PDSTypes.SampleTypes type, int offset, int bits, byte data[])
	{
		long result = 0;
		
		switch (type){
			case LSB_INTEGER:
			{
				result = PDSCodec.decodeLSBSigned(offset, data, bits);
			}
			break;
			case LSB_UNSIGNED_INTEGER:
			{
				result = PDSCodec.decodeUnsignedLSB(offset, data, bits);
			}
			break;
			case MSB_INTEGER:
			{
				result = PDSCodec.decodeMSBSigned(offset, data, bits);
			}
			break;
			case MSB_UNSIGNED_INTEGER:
			{
				result = PDSCodec.decodeUnsignedMSB(offset, data, bits);
			}
			break;
		}
		
		return result;
	}
	public static double decodeReal(PDSTypes.SampleTypes type, int offset, int bits, byte data[])
	{
		double result = PDSSample.noDataValue;
		
		switch (type){
			case REAL:
			{
				result = PDSCodec.decodeReal(data, bits, offset);
			}
			break;
			case PC_REAL:
			{
				result = PDSCodec.decodePCReal(data, bits, offset);
			}
			break;
		}
		
		return result;
	}
	public static void encodeReal(double value, PDSTypes.SampleTypes type, int offset, int bits, byte data[])
	{
		switch (type){
			case REAL:
			{
				PDSCodec.encodeReal(value, offset, bits, data);
			}
			break;
			case PC_REAL:
			{
				PDSCodec.encodePCReal(value, offset, bits, data);
			}
			break;
		}
	}
	public static void encodeSample(PDSSample sample, int offset, byte data[])
	{
		switch (sample.sampleType){
		case LSB_INTEGER:
			{
				PDSCodec.encodeSignedLSB(sample.lData, offset, sample.sampleBits, data);
			}
			break;
			case LSB_UNSIGNED_INTEGER:
			{
				PDSCodec.encodeUnsignedLSB(sample.lData, offset, sample.sampleBits, data);
			}
			break;
			case MSB_INTEGER:
			{
				PDSCodec.encodeSignedMSB(sample.lData, offset, sample.sampleBits, data);
			}
			break;
			case MSB_UNSIGNED_INTEGER:
			{
				PDSCodec.encodeUnsignedMSB(sample.lData, offset, sample.sampleBits, data);
			}
			break;
			case REAL:
			{
				PDSCodec.encodeReal(sample.dData, offset, sample.sampleBits, data);
			}
			break;
			case PC_REAL:
			{
				PDSCodec.encodePCReal(sample.dData, offset, sample.sampleBits, data);
			}
			break;
		}
	}
	public static void printBits(byte b)
	{
		System.out.print(((b>>7)&1));
		System.out.print(((b>>6)&1));
		System.out.print(((b>>5)&1));
		System.out.print(((b>>4)&1));
		System.out.print(((b>>3)&1));
		System.out.print(((b>>2)&1));
		System.out.print(((b>>1)&1));
		System.out.print((b&1));
		System.out.println("="+(b&0xff));
	}
	public static void printBits(int b)
	{
		System.out.print(((b>>31)&1));
		System.out.print(((b>>30)&1));
		System.out.print(((b>>29)&1));
		System.out.print(((b>>28)&1));
		System.out.print(((b>>27)&1));
		System.out.print(((b>>26)&1));
		System.out.print(((b>>25)&1));
		System.out.print(((b>>24)&1));
		System.out.print(((b>>23)&1));
		System.out.print(((b>>22)&1));
		System.out.print(((b>>21)&1));
		System.out.print(((b>>20)&1));
		System.out.print(((b>>19)&1));
		System.out.print(((b>>18)&1));
		System.out.print(((b>>17)&1));
		System.out.print(((b>>16)&1));
		System.out.print(((b>>15)&1));
		System.out.print(((b>>14)&1));
		System.out.print(((b>>13)&1));
		System.out.print(((b>>12)&1));
		System.out.print(((b>>11)&1));
		System.out.print(((b>>10)&1));
		System.out.print(((b>>9)&1));
		System.out.print(((b>>8)&1));
		System.out.print(((b>>7)&1));
		System.out.print(((b>>6)&1));
		System.out.print(((b>>5)&1));
		System.out.print(((b>>4)&1));
		System.out.print(((b>>3)&1));
		System.out.print(((b>>2)&1));
		System.out.print(((b>>1)&1));
		System.out.print((b&1));
		System.out.println("="+b);
	}
	public static void printBits(short b)
	{
		System.out.print(((b>>15)&1));
		System.out.print(((b>>14)&1));
		System.out.print(((b>>13)&1));
		System.out.print(((b>>12)&1));
		System.out.print(((b>>11)&1));
		System.out.print(((b>>10)&1));
		System.out.print(((b>>9)&1));
		System.out.print(((b>>8)&1));
		System.out.print(((b>>7)&1));
		System.out.print(((b>>6)&1));
		System.out.print(((b>>5)&1));
		System.out.print(((b>>4)&1));
		System.out.print(((b>>3)&1));
		System.out.print(((b>>2)&1));
		System.out.print(((b>>1)&1));
		System.out.print((b&1));
		System.out.println("="+b);
	}
}
