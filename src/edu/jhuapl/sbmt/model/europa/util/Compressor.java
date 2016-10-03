package edu.jhuapl.sbmt.model.europa.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Vector;

public class Compressor {
	public static int firstThreshold = 8;
	public static int lastThreshold = 24;
	public static int thresholdIncr = 2;
	public static int firstRepeatLength = 3;
	public static int lastRepeatLength = 12;
	public static int repeatIncr = 2;
	public static int longBitCount = 6;
	public Compressor()
	{
	}
	public static int getBestStrips(Vector<Long> deltaVec, Vector<Integer> bitCountVec)
	{
		int result = firstThreshold;
		
		int i, j, nBits, minBits=0, nStrips, bestThreshold=18, bestRepeatLength=3;
		
		for (i = firstThreshold; i <= lastThreshold; i+=thresholdIncr){
			for (j = firstRepeatLength; j <= lastRepeatLength; j+=repeatIncr){
				Vector<CompStrip> strips = new Vector<CompStrip>();
				nStrips = CompStrip.getStrips(deltaVec, bitCountVec, strips, i, j);
				nBits = Compressor.getCompressedLength(nStrips, deltaVec, strips);
				//System.out.println("Threshold = "+i+"  total bits = "+nBits);
				if ((i == firstThreshold) && (j == firstRepeatLength))
				{
					minBits = nBits;
					bestThreshold = i;
					bestRepeatLength = j;
				}
				else
				{
					if (nBits < minBits)
					{
						minBits = nBits;
						bestThreshold = i;
						bestRepeatLength = j;
					}
				}
			}
		}
		
		result = ((bestRepeatLength&0xff)<<8) | (bestThreshold&0xff);
		
		//System.out.println("Best threshold = "+result+"  best repeat length = "+bestRepeatLength+"  total bits = "+minBits);
		
		return result;
	}
	public static int[] predictionAdjustment(int x[])
	{
		int results[] = new int[x.length];
		
		int i, n, predict;
		
		n = x.length;
		for (i = 0; i < n; i++){
			if (i == 0)
			{
				results[i] = x[i];
			}
			else if (i > 0)
			{
				results[i] = x[i] + (x[i] - x[i-1]);
			}
			else if (i > 1)
			{
				double d1 = x[i - 1] - x[i-2];
				double d2 = x[i] - x[i-1];
				double xEst = (double)x[i];
				if (d2 > d1)
					xEst += d2;
				else
					xEst += (d2 + d1)*0.50;
				predict = (int)(0.50 + xEst);
				results[i] = x[i] - predict;
			}
		}
		
		return results;
	}
	public static int[] preconditionNumbers(int x[])
	{
		int results[] = new int[x.length];
		
		int i, n;
		
		n = x.length;
		
		int leftNdx = 0;
		int rightNdx = n/2 + ((n%2)==0 ? 0 : 1);
		
		for (i = 0; i < n; i++){
			if ((i%2) == 0)
			{
				results[leftNdx++] = x[i];
			}
			else
			{
				if (i == (n-1))
				{
					results[rightNdx++] = x[i] - x[i-1];
				}
				else
				{
					results[rightNdx++] = x[i] - (int)(0.50 + ((double)x[i-1]+(double)x[i+1])*0.50);
				}
			}
		}
		
		return results;
	}
	public static long[] decompressNumbers(BitArray ba)
	{
		long results[] = null;
		
		if (ba != null)
		{
			int nBits, n, i, j, signBit=0;
			int stripLen, restoredCount, stripBits, repeatFlag, stripLenBits;
			long firstVal, val, delta, offset=0;
			
			//
			//	read off the number of bits needed for the length of the array
			//
			nBits = (int)ba.getNumber(longBitCount, offset);
			offset += longBitCount;
			
			//
			//	read off the length of the array
			//
			n = (int)ba.getNumber(nBits, offset);
			offset += nBits;
			
			if (n > 0)
			{
				results = new long[n];
				//
				//	read the sign of the first value in the array
				//
				signBit = (int)ba.getNumber(1, offset);
				offset += 1;
				
				//
				//	read the number of bits needed for the first value in the array
				//
				nBits = (int)ba.getNumber(longBitCount, offset);
				offset += longBitCount;
				
				//
				//	read the first value in the array
				//
				firstVal = ba.getNumber(nBits, offset);
				offset += nBits;
				
				//
				//	now we have the sign bit and the absolute value of the first value in the array
				//	if the sign bit is one then the first value was a negative number
				//
				if (signBit == 1)
					firstVal = -firstVal;
				
				if (n > 1)
				{
					//
					//	now we get the number of bits needed to encode the largest strip length
					//
					stripLenBits = (int)ba.getNumber(longBitCount, offset);
					offset += longBitCount;
					
					restoredCount = 0;
					results[restoredCount++] = val = firstVal;
					
					while (restoredCount < n){
						
						stripLen = (int)ba.getNumber(stripLenBits, offset);
						offset += stripLenBits;
						
						stripBits = (int)ba.getNumber(longBitCount, offset);
						offset += longBitCount;
						
						repeatFlag = (int)ba.getNumber(1, offset);
						offset += 1;
						
						if (repeatFlag == 1)
						{
							signBit = (int)ba.getNumber(1, offset);
							offset += 1;
							
							delta = (int)ba.getNumber(stripBits, offset);
							offset += stripBits;
							
							for (j = 0; (j < stripLen) && (restoredCount < n); j++){
								val += ((signBit == 1) ? -delta : delta);
								results[restoredCount++] = val;
							}
						}
						else
						{
							for (i = 0; (i < stripLen) && (restoredCount < n); i++){
								
								signBit = (int)ba.getNumber(1, offset);
								offset += 1;
								
								delta = ba.getNumber(stripBits, offset);
								offset += stripBits;
								
								val += ((signBit == 1) ? -delta : delta);
								results[restoredCount++] = val;
							}
						}
					}
				}
			}
		}
		
		return results;
	}
	public static int getCompressedLength(int maxStripLength, Vector<Long> deltaVec, Vector<CompStrip> strips)
	{
		int result = 0;
		
		int stripLenBits = BitArray.getMinimumBits(maxStripLength);
		//result.addNumber(stripLenBits, 5);
		result += 5;
		
		int nStrips = strips.size();
		
		for (int i = 0; i < nStrips; i++){
			int stripLen = strips.get(i).getLength();
			int iStart = strips.get(i).startIndex;
			int iStop = strips.get(i).stopIndex;
			int stripBits = strips.get(i).nBits;
			
			result += stripLenBits;
			result += 5;
			//result.addNumber(stripLen, stripLenBits);
			//result.addNumber(stripBits, 5);
			
			int repeatFlag = (strips.get(i).repeatStrip ? 1 : 0);
			//result.addNumber(repeatFlag, 1);
			result += 1;
			
			if (strips.get(i).repeatStrip)
			{
				long delta = deltaVec.get(iStart);//numbers[j+1] - numbers[j];
				int signBit = 0;
				if (delta < 0)
				{
					signBit = 1;
					delta = -delta;
				}
				//result.addNumber(signBit, 1);
				//result.addNumber(delta, stripBits);
				result += 1;
				result += stripBits;
			}
			else
			{	
				for (int j = iStart; j <= iStop; j++){
					//
					//	the values in the deltaVec data structure are offset 1 to the right from the numbers array
					//
					long delta = deltaVec.get(j);//numbers[j+1] - numbers[j];
					int signBit = 0;
					if (delta < 0)
					{
						signBit = 1;
						delta = -delta;
					}
					//result.addNumber(signBit, 1);
					//result.addNumber(delta, stripBits);
					result += 1;
					result += stripBits;
				}
			}
		}
		
		return result;
	}
	public static BitArray compressFile(String fileStr)
	{
		BitArray result = null;
		
		int i, n;
		
		byte fileBytes[] = FileUtils.readEntireFile(fileStr);
		if ((fileBytes != null) && (fileBytes.length > 0))
		{
			n = fileBytes.length;
			long numbers[] = new long[n];
			
			for (i = 0; i < n; i++){
				numbers[i] = fileBytes[i];
			}
			
			result = compressNumbers(numbers);
		}
		
		return result;
	}
	public static void main(String[] args)
	{
		/*
		int i, n;
		
		n = 64;
		int origLength = 64 * 4;
		int array[] = new int[n];
		
		int val = -22;
		int incr = 27;
		
		for (i = 0; i < 32; i++){
			array[i] = val;
			val += incr;
			if ((i%20) == 0)
				incr = 17;
			if ((i%30) == 0)
				incr = 9;
		}
		incr = -37;
		for (i = 32; i < n; i++){
			array[i] = val;
			val += incr;
			if ((i%40) == 0)
				incr = 5;
			if ((i%50) == 0)
				incr = 12;
		}
		
		BitArray ba = Compressor.compressNumbers(array, false);
		int outArray[] = Compressor.decompressNumbers(ba);
		
		int compLength = ba.getLength();
		
		System.out.println("Compressed length is "+compLength+"  original length is "+origLength);
		if (outArray != null)
		{
			System.out.println("Output array length = "+outArray.length);
			if (outArray.length == 64)
			{
				for (i = 0; i < n; i++){
					System.out.println(""+i+" Original = "+array[i]+"   Expanded = "+outArray[i]);
				}
			}
		}
		else
		{
			System.out.println("Output array is null");
		}
		*/
		
		try {
			//String fileStr = "E:\\transfer\\EDR\\FRT0001961C_07_SC164S_EDR0.IMG";
			//String fileStr = "E:\\transfer\\EDR\\FRT00003CAB_07_SC168S_EDR0.IMG";
			//String fileStr = "E:\\transfer\\EDR\\FRT00003CAB_07_SC168L_EDR0.IMG";
			String fileStr = "E:\\transfer\\EDR\\FRT0001961C_07_SC164L_EDR0.IMG";
			RandomAccessFile raf = new RandomAccessFile(fileStr, "r");
			DataOutputStream outStream = new DataOutputStream(new FileOutputStream(fileStr+ ".jam"));
			if ((raf != null) && (outStream != null))
			{
				int i, ndx=0;
				int recLength = 1280;
				int iLength = recLength/2;
				int fileSize = (int)(new File(fileStr).length());
				int offset = 0;
				
				byte rowData[] = new byte[recLength];
				int nums[] = new int[iLength];
				long compNums[] = new long[iLength];
				int lastNums[] = new int[iLength];
				
				for (i = 0; i < iLength; i++) lastNums[i] = 0;
				
				while (offset < fileSize){
					raf.read(rowData);
					for (i = 0; i < iLength; i++){
						nums[i] = ((rowData[i*2]&0xff)<<8) | (rowData[i*2+1]&0xff);
						compNums[i] = nums[i] - lastNums[i];
					}
					BitArray ba = Compressor.compressNumbers(compNums);
					outStream.writeInt(ba.getPaddedLength());
					outStream.write(ba.data, 0, ba.getPaddedLength());
					
					for (i = 0; i < iLength; i++)
						lastNums[i] = nums[i];
					
					offset += recLength;
					ndx++;
					if ((ndx%500) == 0)
					{
						System.out.println("Done with "+ndx);
					}
				}
				
				
				raf.close();
				outStream.close();
			}
		}
		catch (Exception ex)
		{
			System.out.println(ex);
			ex.printStackTrace();
		}
	}
	public static BitArray compressNumbers(long numbers[])
	{
		BitArray result = null;
		
		int i, j, nBits;
		long signBit, firstVal, deltaCount;
		
		Vector<Long> deltaVec = new Vector<Long>();
		
		long n = numbers.length;
		
		//
		//	create a new bit array to hold the compressed results
		//
		result = new BitArray();
		
		//
		//	store the size of the array of numbers that we're compressing
		//
		nBits = BitArray.getMinimumBits(n);
		if (nBits == 0)
			nBits = 1;
		result.addNumber(nBits, longBitCount);
		result.addNumber(n, nBits);
		
		//
		//	store the minimum value in the array (that we're compressing)
		//
		if (n > 0)
		{
			signBit = 0;
			firstVal = numbers[0];
			if (firstVal < 0)
			{
				signBit = 1;
				firstVal = -firstVal;
			}
			nBits = BitArray.getMinimumBits(firstVal);
			result.addNumber(signBit, 1);
			result.addNumber(nBits, longBitCount);
			result.addNumber(firstVal, nBits);
			
			if (n > 1)
			{
				long maxDelta, delta;
				
				maxDelta = 0;
	
				//
				//	loop thru the numbers and determine the maximum delta
				//
				for (i = 0; i < n; i++){
					if (i > 0)
					{
						delta = numbers[i] - numbers[i-1];
						deltaVec.add(new Long(delta));
						
						if (delta < 0) delta = -delta;
						if (delta > maxDelta) maxDelta = delta;
					}
				}
				
				deltaCount = deltaVec.size();
				
				Vector<Integer> bitCountVec = new Vector<Integer>();
				
				for (i = 0; i < deltaCount; i++){
					bitCountVec.add(BitArray.getMinimumBits(Math.abs(deltaVec.get(i))));
				}
				
				Vector<CompStrip> strips = new Vector<CompStrip>();
				
				int bestThreshold = 18;
				int bestRepeatLen = 32;
				
				int maxStripLength = CompStrip.getStrips(deltaVec, bitCountVec, strips, bestThreshold, bestRepeatLen);
				
				//int numBits = 0;
				
				int stripLenBits = BitArray.getMinimumBits(maxStripLength);
				result.addNumber(stripLenBits, longBitCount);
				
				//numBits += 5;
				
				int nStrips = strips.size();
				
				for (i = 0; i < nStrips; i++){
					int stripLen = strips.get(i).getLength();
					int iStart = strips.get(i).startIndex;
					int iStop = strips.get(i).stopIndex;
					int stripBits = strips.get(i).nBits;
					
					result.addNumber(stripLen, stripLenBits);
					result.addNumber(stripBits, longBitCount);
					//numBits += stripLenBits;
					//numBits += 5;
					
					int repeatFlag = (strips.get(i).repeatStrip ? 1 : 0);
					result.addNumber(repeatFlag, 1);
					//numBits += 1;
					
					if (strips.get(i).repeatStrip)
					{
						delta = deltaVec.get(iStart);//numbers[j+1] - numbers[j];
						signBit = 0;
						if (delta < 0)
						{
							signBit = 1;
							delta = -delta;
						}
						result.addNumber(signBit, 1);
						result.addNumber(delta, stripBits);
						//numBits += 1;
						//numBits += stripBits;
					}
					else
					{	
						for (j = iStart; j <= iStop; j++){
							//
							//	the values in the deltaVec data structure are offset 1 to the right from the numbers array
							//
							delta = deltaVec.get(j);//numbers[j+1] - numbers[j];
							signBit = 0;
							if (delta < 0)
							{
								signBit = 1;
								delta = -delta;
							}
							result.addNumber(signBit, 1);
							result.addNumber(delta, stripBits);
							//numBits += 1;
							//numBits += stripBits;
						}
					}
				}
				
				//System.out.println("Actual bits = "+numBits);
			}
		}
		
		return result;
	}
}
