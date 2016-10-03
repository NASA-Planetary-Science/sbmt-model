package edu.jhuapl.sbmt.model.europa.pds;


public class PDSCodec {
	public static long decodeMSBSigned(byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[0];
				result =  b1;
			}
			break;
			case 16:
			{
				byte b1 = data[0];
				byte b2 = data[1];
				result = (short)((b1&0xff)<<8) | (b2&0xff);
			}
			break;
			case 32:
			{
				byte b1 = data[0];
				byte b2 = data[1];
				byte b3 = data[2];
				byte b4 = data[3];
				result = (int)((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
			}
			break;
			case 64:
			{
				byte b1 = data[0];
				byte b2 = data[1];
				byte b3 = data[2];
				byte b4 = data[3];
				byte b5 = data[4];
				byte b6 = data[5];
				byte b7 = data[6];
				byte b8 = data[7];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static long decodeMSBSigned(int offset, byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[offset+0];
				result =  b1;
			}
			break;
			case 16:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				result = (short)((b1&0xff)<<8) | (b2&0xff);
			}
			break;
			case 32:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				byte b3 = data[offset+2];
				byte b4 = data[offset+3];
				result = (int)((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
			}
			break;
			case 64:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				byte b3 = data[offset+2];
				byte b4 = data[offset+3];
				byte b5 = data[offset+4];
				byte b6 = data[offset+5];
				byte b7 = data[offset+6];
				byte b8 = data[offset+7];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static void encodeSignedMSB(long value, int offset, int bits, byte data[])
	{
		switch (bits){
			case 8:
			{
				byte val = (byte)value;
				data[offset] = val;
			}
			break;
			case 16:
			{
				short val = (short)value;
				data[offset] = (byte)((val>>8)&0xff);
				data[offset+1] = (byte)(val&0xff);
			}
			break;
			case 32:
			{
				int val = (int)value;
				data[offset] = (byte)((val>>24)&0xff);
				data[offset+1] = (byte)((val>>16)&0xff);
				data[offset+2] = (byte)((val>>8)&0xff);
				data[offset+3] = (byte)(val&0xff);
			}
			break;
			case 64:
			{
				data[offset] = (byte)((value>>56)&0xff);
				data[offset+1] = (byte)((value>>48)&0xff);
				data[offset+2] = (byte)((value>>40)&0xff);
				data[offset+3] = (byte)((value>>32)&0xff);
				data[offset+4] = (byte)((value>>24)&0xff);
				data[offset+5] = (byte)((value>>16)&0xff);
				data[offset+6] = (byte)((value>>8)&0xff);
				data[offset+7] = (byte)(value&0xff);
			}
			break;
		}
	}
	public static void main(String args[])
	{
		byte data[] = new byte[8];

		byte outByte;
		short outShort;
		int outInt;
		long outLong;

		boolean problem = false;

		for (int bValue = -127; (bValue < 128) && !problem; bValue++){
			byte b = (byte)bValue;
			//System.out.println("Trying: "+b);
			PDSCodec.encodeSignedLSB(b, 0, 8, data);
			outByte = (byte)PDSCodec.decodeLSBSigned(data, 8);
			if (outByte != b)
			{
				System.out.println("byte values are not equal: "+bValue+" != "+outByte);
				problem = true;
			}
		}

		if (!problem)
			System.out.println("Finished byte values with no problems");

		for (int bValue = 0; (bValue < 255) && !problem; bValue++){
			//byte b = (byte)bValue;
			//System.out.println("Trying: "+b);
			PDSCodec.encodeUnsignedLSB(bValue, 0, 8, data);
			outByte = (byte)PDSCodec.decodeLSBSigned(data, 8);
			outInt = outByte&0xff;
			if (outInt != bValue)
			{
				System.out.println("unsigned byte values are not equal: "+bValue+" != "+outByte);
				problem = true;
			}
		}

		if (!problem)
			System.out.println("Finished unsigned byte values with no problems");

		for (int sValue = -32768; (sValue < 32768) && !problem; sValue++){
			short s = (short)sValue;
			PDSCodec.encodeSignedLSB(s, 0, 16, data);
			outShort = (short)PDSCodec.decodeLSBSigned(data, 16);
			if (outShort != s)
			{
				System.out.println("lsb short values are not equal: "+sValue+" != "+outShort);
				problem = true;
			}
		}

		if (!problem)
			System.out.println("Finished short values with no problems");

		for (long iValue = -2147483648; (iValue <= 2147483647) && !problem; iValue += 1000){
			int i = (int)iValue;
			PDSCodec.encodeSignedLSB(i, 0, 32, data);
			outInt = (int)PDSCodec.decodeLSBSigned(data, 32);
			if (outInt != i)
			{
				System.out.println("lsb int values are not equal: "+iValue+" != "+outInt);
				problem = true;
			}
		}

		if (!problem)
		{
			System.out.println("Finished with no problems");
		}
	}
	public static long decodeLSBSigned(byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[0];
				result =  b1;
			}
			break;
			case 16:
			{
				byte b1 = data[1];
				byte b2 = data[0];
				result = (short)((b1&0xff)<<8) | (b2&0xff);
			}
			break;
			case 32:
			{
				byte b1 = data[3];
				byte b2 = data[2];
				byte b3 = data[1];
				byte b4 = data[0];
				result = (int)((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
			}
			break;
			case 64:
			{
				byte b1 = data[7];
				byte b2 = data[6];
				byte b3 = data[5];
				byte b4 = data[4];
				byte b5 = data[3];
				byte b6 = data[2];
				byte b7 = data[1];
				byte b8 = data[0];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static long decodeLSBSigned(int offset, byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[offset+0];
				result =  b1;
			}
			break;
			case 16:
			{
				byte b1 = data[offset+1];
				byte b2 = data[offset+0];
				result = (short)((b1&0xff)<<8) | (b2&0xff);
			}
			break;
			case 32:
			{
				byte b1 = data[offset+3];
				byte b2 = data[offset+2];
				byte b3 = data[offset+1];
				byte b4 = data[offset+0];
				result = (int)((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
			}
			break;
			case 64:
			{
				byte b1 = data[offset+7];
				byte b2 = data[offset+6];
				byte b3 = data[offset+5];
				byte b4 = data[offset+4];
				byte b5 = data[offset+3];
				byte b6 = data[offset+2];
				byte b7 = data[offset+1];
				byte b8 = data[offset+0];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static void encodeSignedLSB(long value, int offset, int bits, byte data[])
	{
		switch (bits){
			case 8:
			{
				byte val = (byte)value;
				data[offset] = val;
			}
			break;
			case 16:
			{
				short val = (short)value;
				data[offset+1] = (byte)((val>>8)&0xff);
				data[offset] = (byte)(val&0xff);
			}
			break;
			case 32:
			{
				int val = (int)value;
				data[offset+3] = (byte)((val>>24)&0xff);
				data[offset+2] = (byte)((val>>16)&0xff);
				data[offset+1] = (byte)((val>>8)&0xff);
				data[offset] = (byte)(val&0xff);
			}
			break;
			case 64:
			{
				data[offset+7] = (byte)((value>>56)&0xff);
				data[offset+6] = (byte)((value>>48)&0xff);
				data[offset+5] = (byte)((value>>40)&0xff);
				data[offset+4] = (byte)((value>>32)&0xff);
				data[offset+3] = (byte)((value>>24)&0xff);
				data[offset+2] = (byte)((value>>16)&0xff);
				data[offset+1] = (byte)((value>>8)&0xff);
				data[offset] = (byte)(value&0xff);
			}
			break;
		}
	}
	public static long decodeUnsignedMSB(byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[0];
				result =  (long)((b1&0xff)&0xFFFFFFFFL);
			}
			break;
			case 16:
			{
				byte b1 = data[0];
				byte b2 = data[1];
				result = (long)((((b1&0xff)<<8) | (b2&0xff))&0xFFFFFFFFL);
			}
			break;
			case 32:
			{
				byte b1 = data[0];
				byte b2 = data[1];
				byte b3 = data[2];
				byte b4 = data[3];
				int v = ((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
				result = intToLong(v);
			}
			break;
			case 64:
			{
				byte b1 = data[0];
				byte b2 = data[1];
				byte b3 = data[2];
				byte b4 = data[3];
				byte b5 = data[4];
				byte b6 = data[5];
				byte b7 = data[6];
				byte b8 = data[7];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static long decodeUnsignedMSB(int offset, byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[offset+0];
				result =  (long)((b1&0xff)&0xFFFFFFFFL);
			}
			break;
			case 16:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				result = (long)((((b1&0xff)<<8) | (b2&0xff))&0xFFFFFFFFL);
			}
			break;
			case 32:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				byte b3 = data[offset+2];
				byte b4 = data[offset+3];
				int v = ((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
				result = intToLong(v);
			}
			break;
			case 64:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				byte b3 = data[offset+2];
				byte b4 = data[offset+3];
				byte b5 = data[offset+4];
				byte b6 = data[offset+5];
				byte b7 = data[offset+6];
				byte b8 = data[offset+7];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static void encodeUnsignedMSB(long value, int offset, int bits, byte data[])
	{
		switch (bits){
			case 8:
			{
				data[offset] = (byte)(value&0xff);
			}
			break;
			case 16:
			{
				data[offset] = (byte)((value>>8)&0xff);
				data[offset+1] = (byte)(value&0xff);
			}
			break;
			case 32:
			{
				data[offset] = (byte)((value>>24)&0xff);
				data[offset+1] = (byte)((value>>16)&0xff);
				data[offset+2] = (byte)((value>>8)&0xff);
				data[offset+3] = (byte)(value&0xff);
			}
			break;
			case 64:
			{
				data[offset] = (byte)((value>>56)&0xff);
				data[offset+1] = (byte)((value>>48)&0xff);
				data[offset+2] = (byte)((value>>40)&0xff);
				data[offset+3] = (byte)((value>>32)&0xff);
				data[offset+4] = (byte)((value>>24)&0xff);
				data[offset+5] = (byte)((value>>16)&0xff);
				data[offset+6] = (byte)((value>>8)&0xff);
				data[offset+7] = (byte)(value&0xff);
			}
			break;
		}
	}
	public static long decodeUnsignedLSB(byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[0];
				result =  (long)((b1&0xff)&0xFFFFFFFFL);
			}
			break;
			case 16:
			{
				byte b1 = data[1];
				byte b2 = data[0];
				result = (long)((((b1&0xff)<<8) | (b2&0xff))&0xFFFFFFFFL);
			}
			break;
			case 32:
			{
				byte b1 = data[3];
				byte b2 = data[2];
				byte b3 = data[1];
				byte b4 = data[0];
				int v = ((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
				result = intToLong(v);
			}
			break;
			case 64:
			{
				byte b1 = data[7];
				byte b2 = data[6];
				byte b3 = data[5];
				byte b4 = data[4];
				byte b5 = data[3];
				byte b6 = data[2];
				byte b7 = data[1];
				byte b8 = data[0];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static long decodeUnsignedLSB(int offset, byte data[], int bits)
	{
		long result = 0;

		switch (bits){
			case 8:
			{
				byte b1 = data[offset];
				result =  (long)((b1&0xff)&0xFFFFFFFFL);
			}
			break;
			case 16:
			{
				byte b1 = data[offset+1];
				byte b2 = data[offset+0];
				result = (long)((((b1&0xff)<<8) | (b2&0xff))&0xFFFFFFFFL);
			}
			break;
			case 32:
			{
				byte b1 = data[offset+3];
				byte b2 = data[offset+2];
				byte b3 = data[offset+1];
				byte b4 = data[offset+0];
				int v = ((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
				result = intToLong(v);
			}
			break;
			case 64:
			{
				byte b1 = data[offset+7];
				byte b2 = data[offset+6];
				byte b3 = data[offset+5];
				byte b4 = data[offset+4];
				byte b5 = data[offset+3];
				byte b6 = data[offset+2];
				byte b7 = data[offset+1];
				byte b8 = data[offset+0];

				result = ((b1&0xff)<<56) | ((b2&0xff)<<48) | ((b3&0xff)<<40) | ((b4&0xff)<<32) | ((b5&0xff)<<24) | ((b6&0xff)<<16) | ((b7&0xff)<<8) | (b8&0xff);
			}
			break;
		}

		return result;
	}
	public static void encodeUnsignedLSB(long value, int offset, int bits, byte data[])
	{
		switch (bits){
			case 8:
			{
				data[offset] = (byte)(value&0xff);
			}
			break;
			case 16:
			{
				data[offset + 1] = (byte)((value>>8)&0xff);
				data[offset] = (byte)(value&0xff);
			}
			break;
			case 32:
			{
				data[offset + 3] = (byte)((value>>24)&0xff);
				data[offset + 2] = (byte)((value>>16)&0xff);
				data[offset + 1] = (byte)((value>>8)&0xff);
				data[offset] = (byte)(value&0xff);
			}
			break;
			case 64:
			{
				data[offset+7] = (byte)((value>>56)&0xff);
				data[offset+6] = (byte)((value>>48)&0xff);
				data[offset+5] = (byte)((value>>40)&0xff);
				data[offset+4] = (byte)((value>>32)&0xff);
				data[offset+3] = (byte)((value>>24)&0xff);
				data[offset+2] = (byte)((value>>16)&0xff);
				data[offset+1] = (byte)((value>>8)&0xff);
				data[offset] = (byte)(value&0xff);
			}
			break;
		}
	}
	public static double decodeReal(byte data[], int bits, int offset)
	{
		double result = 0.0;

		switch (bits){
			case 32:
			{
				byte b1 = data[offset+0];
				byte b2 = data[offset+1];
				byte b3 = data[offset+2];
				byte b4 = data[offset+3];
				int iBits = ((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
				result = (double)Float.intBitsToFloat(iBits);
			}
			break;
			case 64:
			{
				long lBits = 0l;

				for (int i = 0; i < 8; i++){
					long b = data[offset+i];
					int shift = 56 - i*8;
					lBits |= ((b&0xff)<<shift);
				}

				result = Double.longBitsToDouble(lBits);
			}
			break;
		}

		return result;
	}
	public static void encodeReal(double value, int offset, int bits, byte data[])
	{
		int i = 0;

		switch (bits){
			case 32:
			{
				int intBits = Float.floatToIntBits((float)value);

				data[offset + i++] = (byte)((intBits>>24)&0xff);
				data[offset + i++] = (byte)((intBits>>16)&0xff);
				data[offset + i++] = (byte)((intBits>>8)&0xff);
				data[offset + i++] = (byte)(intBits&0xff);
			}
			break;
			case 64:
			{
				long longBits = Double.doubleToLongBits(value);

				data[offset + i++] = (byte)((longBits>>56)&0xff);
				data[offset + i++] = (byte)((longBits>>48)&0xff);
				data[offset + i++] = (byte)((longBits>>40)&0xff);
				data[offset + i++] = (byte)((longBits>>32)&0xff);
				data[offset + i++] = (byte)((longBits>>24)&0xff);
				data[offset + i++] = (byte)((longBits>>16)&0xff);
				data[offset + i++] = (byte)((longBits>>8)&0xff);
				data[offset + i++] = (byte)(longBits&0xff);
			}
			break;
		}

	}
	public static double decodePCReal(byte data[], int bits, int offset)
	{
		double result = 0;

		switch (bits){
			case 32:
			{
				byte b1 = data[offset+3];
				byte b2 = data[offset+2];
				byte b3 = data[offset+1];
				byte b4 = data[offset+0];
				int iBits = ((b1&0xff)<<24) | ((b2&0xff)<<16) | ((b3&0xff)<<8) | (b4&0xff);
				result = (double)Float.intBitsToFloat(iBits);
			}
			break;
			case 64:
			{
				long lBits = 0l;

				for (int i = 7; i >= 0; i--){
					long b = data[offset+i];
					int shift = i*8;
					lBits |= ((b&0xff)<<shift);
				}

				result = Double.longBitsToDouble(lBits);
			}
			break;
		}

		return result;
	}
	public static void encodePCReal(double value, int offset, int bits, byte data[])
	{
		switch (bits){
			case 32:
			{
				int intBits = Float.floatToIntBits((float)value);

				data[offset + 3] = (byte)((intBits>>24)&0xff);
				data[offset + 2] = (byte)((intBits>>16)&0xff);
				data[offset + 1] = (byte)((intBits>>8)&0xff);
				data[offset] = (byte)(intBits&0xff);
			}
			break;
			case 64:
			{
				long longBits = Double.doubleToLongBits(value);

				data[offset + 7] = (byte)((longBits>>56)&0xff);
				data[offset + 6] = (byte)((longBits>>48)&0xff);
				data[offset + 5] = (byte)((longBits>>40)&0xff);
				data[offset + 4] = (byte)((longBits>>32)&0xff);
				data[offset + 3] = (byte)((longBits>>24)&0xff);
				data[offset + 2] = (byte)((longBits>>16)&0xff);
				data[offset + 1] = (byte)((longBits>>8)&0xff);
				data[offset] = (byte)(longBits&0xff);
			}
			break;
		}
	}
	public static int longToInt(long v)
	{
		int result = 0;

		int b1 = (int)((v>>24)&0xff);
		int b2 = (int)((v>>16)&0xff);
		int b3 = (int)((v>>8)&0xff);
		int b4 = (int)(v&0xff);

		result = (b1<<24) | (b2<<16) | (b3<<8) | b4;

		return result;
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
	public static long calculateOffset(PDSDEM img, long row, long column, long band)
	{
		long result = 0;

		long bytesPerPixel = img.bitsPerSample/8;

		switch (img.bandStorage){
			case SAMPLE_INTERLEAVED:
			{
				result = (img.nBands * img.nSamples * row + column * img.nBands + band) * bytesPerPixel;
			}
			break;
			case BAND_INTERLEAVED_BY_LINE:
			{
				result = (img.nBands * img.nSamples * row + band * img.nSamples + column) * bytesPerPixel;
			}
			break;
			case Unknown:
			case BAND_SEQUENTIAL:
			{
				result = (band * img.nLines * img.nSamples + row * img.nSamples + column) * bytesPerPixel;
			}
			break;
		}

		result += img.labelOffset;

		return result;
	}
	public static long calculateOffset(PDSTypes.BandStorageTypes bandType, int bitsPerSample, int nLines, int nSamples, int nBands, int row, int column, int band)
	{
		long result = 0;

		long bytesPerPixel = bitsPerSample/8;

		switch (bandType){
			case SAMPLE_INTERLEAVED:
			{
				result = (nBands * nSamples * row + column * nBands + band) * bytesPerPixel;
			}
			break;
			case BAND_INTERLEAVED_BY_LINE:
			{
				result = (nBands * nSamples * row + band * nSamples + column) * bytesPerPixel;
			}
			break;
			case Unknown:
			case BAND_SEQUENTIAL:
			{
				result = (band * nLines * nSamples + row * nSamples + column) * bytesPerPixel;
			}
			break;
		}

		return result;
	}
}
