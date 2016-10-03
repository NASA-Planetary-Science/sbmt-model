package edu.jhuapl.sbmt.model.europa.util;

import java.util.Vector;

public class CompStrip {
	public int startIndex = 0;
	public int stopIndex = 0;
	public int nBits = 0;
	public boolean repeatStrip = false;
	public static int coderVersion = 1;
	public CompStrip(int start, int stop, int bitCount)
	{
		startIndex = start;
		stopIndex = stop;
		nBits = bitCount;
	}
	public CompStrip(int start, int stop, int bitCount, boolean flag)
	{
		startIndex = start;
		stopIndex = stop;
		nBits = bitCount;
		repeatStrip = flag;
	}
	public void copy(CompStrip s)
	{
		startIndex = s.startIndex;
		stopIndex = s.stopIndex;
		nBits = s.nBits;
		repeatStrip = s.repeatStrip;
	}
	public int getLength(){ return (stopIndex - startIndex + 1);}
	public static int getStrips(Vector<Long> deltaVec, Vector<Integer> bitCountVec, Vector<CompStrip> strips)
	{
		return getStrips(deltaVec, bitCountVec, strips, 18, 6);
	}
	public static int getStrips(Vector<Long> deltaVec, Vector<Integer> bitCountVec, Vector<CompStrip> strips,
			int thresholdLength, int repeatThresholdLength)
	{
		int result = 0;
		
		int i, j, repeatLen, strippedCount=0, maxBitCount=1, bitCount;
		int n = bitCountVec.size();
		long delta;
		//int thresholdLength = 18;
		//int repeatThresholdLength = 6;
		
		strips.clear();
		
		//
		//	setting up an array of flags; true means this item has already been included in
		//	a strip, false means that it still needs to be placed in a strip
		//
		boolean stripped[] = new boolean[n];
		
		int iStart = 0;
		int iStop = 0;
		int nBits = 0;
		
		for (i = 0; i < n; i++){
			stripped[i] = false;
			if (bitCountVec.get(i) > maxBitCount)
				maxBitCount = bitCountVec.get(i);
		}
		
		for (i = 0; i < (n-1); i++){
			delta = deltaVec.get(i);
			iStart = i;
			iStop = i;
			for (j = i+1; j < n; j++){
				if (deltaVec.get(j) == delta)
				{
					iStop = j;
				}
				else
				{
					break;
				}
			}
			repeatLen = iStop - iStart;
			if (repeatLen > repeatThresholdLength)
			{
				nBits = BitArray.getMinimumBits(Math.abs(delta));
				CompStrip strip = new CompStrip(iStart, iStop, nBits, true);
				strips.add(strip);
				for (j = iStart; j <= iStop; j++){
					stripped[j] = true;
					strippedCount++;
				}
				
				i = iStop + 1;
			}
		}
		
		nBits = 1;
		
		while (strippedCount < n){
			//
			//	find all the strips that require nBits or less
			//
			iStart = 0;
			iStop = 0;
			boolean started = false;
			
			for (i = 0; i < n; i++){
				bitCount = bitCountVec.get(i);
				boolean alreadyStripped = stripped[i];
				if ((bitCount <= nBits) && !alreadyStripped)
				{
					if (!started)
					{
						iStart = i;
					}
					iStop = i;
					started = true;
	
					//
					//	in case the loop ends within a strip
					//
					if (i == (n-1))
					{
						int length = iStop - iStart + 1;
						if ((length > thresholdLength) || (nBits >= maxBitCount))
						{
							CompStrip strip = new CompStrip(iStart, iStop, nBits);
							strips.add(strip);
							for (j = iStart; j <= iStop; j++){
								stripped[j] = true;
							}
						}
					}
				}
				else
				{
					//
					//	this point (i) is not part of a strip, but if we're at the end of a
					//	strip we may want to add it to the vector of strips
					//
					if (started)
					{
						int length = iStop - iStart + 1;
						if ((length >= thresholdLength) || (nBits >= maxBitCount))
						{
							CompStrip strip = new CompStrip(iStart, iStop, nBits);
							strips.add(strip);
							for (j = iStart; j <= iStop; j++){
								stripped[j] = true;
							}
						}
					}
					started = false;
				}
			}
			
			//
			//	count all the items that have been stripped
			//
			strippedCount = 0;
			for (i = 0; i < n; i++){
				if (stripped[i]) strippedCount++;
			}
			
			//
			//	move up to the next bit level
			//
			++nBits;
			if ((nBits > maxBitCount) && (strippedCount < n))
			{
				/*
				j = 1;
				for (j = 0; j < n; j++){
					bitCount = bitCountVec.get(j);
					boolean alreadyStripped = stripped[j];
					if (!alreadyStripped)
					{
						k = 1;
					}
				}
				*/
				System.out.println("We've got a problem. We're above the maxBitCount and not everything has been stripped.");
			}
		}
		
		//
		//	the strips may be out of order so we need to sort'em
		//
		int nStrips = strips.size();
		
		boolean finished = false;
		
		while (!finished){
			finished = true;
			for (i = 0; i < (nStrips-1); i++){
				if (strips.get(i).startIndex > strips.get(i+1).startIndex)
				{
					CompStrip s = new CompStrip(strips.get(i).startIndex, strips.get(i).stopIndex, strips.get(i).nBits);
					strips.get(i).copy(strips.get(i+1));
					strips.get(i+1).copy(s);
					finished = false;
				}
			}
		}
		
		for (j = 0; j < strips.size(); j++){
			if (strips.get(j).getLength() > result) result = strips.get(j).getLength();
			if (strips.get(j).getLength() == 0)
				System.out.println("Vern, we've got a problem. Strip length is zero on strip number "+j);
		}
		
		return result;
	}
	public static int getStrips(long deltaVec[], int bitCountVec[], Vector<CompStrip> strips,
			int thresholdLength, int repeatThresholdLength)
	{
		int result = 0;
		
		int i, j, repeatLen, strippedCount=0, maxBitCount=1, bitCount;
		int n = bitCountVec.length;
		long delta;
		//int thresholdLength = 18;
		//int repeatThresholdLength = 6;
		
		strips.clear();
		
		//
		//	setting up an array of flags; true means this item has already been included in
		//	a strip, false means that it still needs to be placed in a strip
		//
		boolean stripped[] = new boolean[n];
		
		int iStart = 0;
		int iStop = 0;
		int nBits = 0;
		
		for (i = 0; i < n; i++){
			stripped[i] = false;
			if (bitCountVec[i] > maxBitCount)
				maxBitCount = bitCountVec[i];
		}
		
		for (i = 0; i < (n-1); i++){
			delta = deltaVec[i];
			iStart = i;
			iStop = i;
			for (j = i+1; j < n; j++){
				if (deltaVec[j] == delta)
				{
					iStop = j;
				}
				else
				{
					break;
				}
			}
			repeatLen = iStop - iStart;
			if (repeatLen > repeatThresholdLength)
			{
				nBits = BitArray.getMinimumBits(Math.abs(delta));
				CompStrip strip = new CompStrip(iStart, iStop, nBits, true);
				strips.add(strip);
				for (j = iStart; j <= iStop; j++){
					stripped[j] = true;
					strippedCount++;
				}
				
				i = iStop + 1;
			}
		}
		
		nBits = 1;
		
		while (strippedCount < n){
			//
			//	find all the strips that require nBits or less
			//
			iStart = 0;
			iStop = 0;
			boolean started = false;
			
			for (i = 0; i < n; i++){
				bitCount = bitCountVec[i];
				boolean alreadyStripped = stripped[i];
				if ((bitCount <= nBits) && !alreadyStripped)
				{
					if (!started)
					{
						iStart = i;
					}
					iStop = i;
					started = true;
	
					//
					//	in case the loop ends within a strip
					//
					if (i == (n-1))
					{
						int length = iStop - iStart + 1;
						if ((length > thresholdLength) || (nBits >= maxBitCount))
						{
							CompStrip strip = new CompStrip(iStart, iStop, nBits);
							strips.add(strip);
							for (j = iStart; j <= iStop; j++){
								stripped[j] = true;
							}
						}
					}
				}
				else
				{
					//
					//	this point (i) is not part of a strip, but if we're at the end of a
					//	strip we may want to add it to the vector of strips
					//
					if (started)
					{
						int length = iStop - iStart + 1;
						if ((length >= thresholdLength) || (nBits >= maxBitCount))
						{
							CompStrip strip = new CompStrip(iStart, iStop, nBits);
							strips.add(strip);
							for (j = iStart; j <= iStop; j++){
								stripped[j] = true;
							}
						}
					}
					started = false;
				}
			}
			
			//
			//	count all the items that have been stripped
			//
			strippedCount = 0;
			for (i = 0; i < n; i++){
				if (stripped[i]) strippedCount++;
			}
			
			//
			//	move up to the next bit level
			//
			++nBits;
			if ((nBits > maxBitCount) && (strippedCount < n))
			{
				/*
				j = 1;
				for (j = 0; j < n; j++){
					bitCount = bitCountVec.get(j);
					boolean alreadyStripped = stripped[j];
					if (!alreadyStripped)
					{
						k = 1;
					}
				}
				*/
				System.out.println("We've got a problem. We're above the maxBitCount and not everything has been stripped.");
			}
		}
		
		//
		//	the strips may be out of order so we need to sort'em
		//
		int nStrips = strips.size();
		
		boolean finished = false;
		
		while (!finished){
			finished = true;
			for (i = 0; i < (nStrips-1); i++){
				if (strips.get(i).startIndex > strips.get(i+1).startIndex)
				{
					CompStrip s = new CompStrip(strips.get(i).startIndex, strips.get(i).stopIndex, strips.get(i).nBits);
					strips.get(i).copy(strips.get(i+1));
					strips.get(i+1).copy(s);
					finished = false;
				}
			}
		}
		
		for (j = 0; j < strips.size(); j++){
			if (strips.get(j).getLength() > result) result = strips.get(j).getLength();
			if (strips.get(j).getLength() == 0)
				System.out.println("Vern, we've got a problem. Strip length is zero on strip number "+j);
		}
		
		return result;
	}
}
