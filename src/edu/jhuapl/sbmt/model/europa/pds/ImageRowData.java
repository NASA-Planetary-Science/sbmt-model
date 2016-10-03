package edu.jhuapl.sbmt.model.europa.pds;


import edu.jhuapl.sbmt.model.europa.util.BitArray;
import edu.jhuapl.sbmt.model.europa.util.Compressor;

public class ImageRowData {
	BitArray rowArray = null;
	int rowLength = -1;
	long cache[] = null;
	int cacheStart = -1;
	int cacheLength = 1024;
	public static int noDataValue = -888888888;
	boolean compressed = true;
	public ImageRowData(BitArray ba, int rowLen)
	{
		rowArray = ba;
		cache = new long[cacheLength];
		rowLength = rowLen;
	}
	public ImageRowData(BitArray ba, int rowLen, int cacheSize)
	{
		rowArray = ba;
		cacheLength = cacheSize;
		cache = new long[cacheLength];
		rowLength = rowLen;
	}
	public ImageRowData(int numbers[])
	{
		rowLength = numbers.length;
		cache = new long[rowLength];
		for (int i = 0; i < rowLength; i++) cache[i] = numbers[i];
		cacheStart = 0;
		cacheLength = rowLength;
		compressed = false;
	}
	public long getRowValue(int col)
	{
		long result = noDataValue;

		if (compressed)
		{
			result = getCompressedRowValue(col);
		}
		else
		{
			result = cache[col];
		}

		return result;
	}
	synchronized long getCompressedRowValue(int col)
	{
		long result = noDataValue;

		if ((col >= 0) && (col < rowLength))
		{
			if (((col >= cacheStart) && (col < (cacheStart + cacheLength))) && (cacheStart >= 0))
			{

			}
			else
			{
				long values[] = Compressor.decompressNumbers(rowArray);

				cacheStart = col - cacheLength/2;
				if (cacheStart < 0) cacheStart = 0;
				if ((cacheStart + cacheLength - 1) >= rowLength) cacheStart = rowLength - cacheLength;
				for (int i = 0; i < cacheLength; i++){
					cache[i] = values[i + cacheStart];
				}

				values = null;
			}
			result = cache[col - cacheStart];
		}

		return result;
	}
	public long getCompressedLength()
	{
		long result = 0;

		if (rowArray != null)
		{
			result = rowArray.getPaddedLength();
		}

		return result;
	}
}
