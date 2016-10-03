package edu.jhuapl.sbmt.model.europa.pds;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import edu.jhuapl.sbmt.model.europa.geodesic.Ellipsoid;
import edu.jhuapl.sbmt.model.europa.math.V3;
import edu.jhuapl.sbmt.model.europa.math.VectorOps;
import edu.jhuapl.sbmt.model.europa.projection.GeometricConstants;
import edu.jhuapl.sbmt.model.europa.projection.IJ;
import edu.jhuapl.sbmt.model.europa.projection.MapProjection;
import edu.jhuapl.sbmt.model.europa.projection.UV;
import edu.jhuapl.sbmt.model.europa.projection.XY;
import edu.jhuapl.sbmt.model.europa.util.BlockFile;
import edu.jhuapl.sbmt.model.europa.util.Pair;

public class PDSDEM {
	public String imageFilename = "";
	public String labelFilename = "";
	public String sourceImageFile = "";
	public enum ImageTypes { Unknown, Elevation, ColorImage, ShadedTexture, PermanentShadowTexture, ShadowBoundaryHeight, EclipseTexture}
	public int recordLength = 0;
	public int labelRecords = 0;
	public int labelOffset = 0;
	public int recordBytes = 0;
	public int nLines = 0;
	public int nSamples = 0;
	public int bitsPerSample = 0;
	public int nBands=1;
	public boolean isDEM = false;
	//public UV mapCenter = null;
	public double centerLatDeg = 0.0;
	public double centerLonDeg = 0.0;
	public double minLat = 0.0;
	public double minLon = 0.0;
	public double maxLat = 0.0;
	public double maxLon = 0.0;
	public double scaleFactor = 1.0;
	public double heightOffset = 0.0;
	public double mapScale = 0.0;
	public double mapResolution = 0.0;
	public double bodyRadius = 1.0;
	public PDSTypes.UnitTypes verticalUnits = PDSTypes.UnitTypes.Unknown;
	public MapProjection.ProjectionTypes projType = MapProjection.ProjectionTypes.Unknown;
	public PDSTypes.SampleTypes sampleType = PDSTypes.SampleTypes.Unknown;
	public PDSTypes.BandStorageTypes bandStorage = PDSTypes.BandStorageTypes.Unknown;
	public ImageTypes type = ImageTypes.Unknown;
	public String targetStr = "";
	public String imageType = "";
	public int bufferSize = 4096;//256*1024*1024;
	public boolean isValidDEM = false;
	public boolean isTopo = false;
	public int ancillary = 0;
	public double minValue = 1.0e12;
	public double maxValue = -1.0e12;
	public Vector<String> labelLines = null;

	public static double noDataValue = -1.0e12;

	BlockFile blkFile = null;
	byte rawData2[] = new byte[2];
	byte rawData4[] = new byte[4];

	CompressedImage compImg = null;
	int uncompImg[] = null;
	boolean wantCache = false;

	public PDSDEM(){}
	public PDSDEM(String imageStr, int buffSize)
	{
		bufferSize = buffSize;
		imageFilename = imageStr;
		try {
			blkFile = new BlockFile(imageStr, bufferSize, labelOffset);
		}
		catch (IOException ioEx)
		{
			System.out.println("Caught exception in PDSDEM(1): "+ioEx);
			ioEx.printStackTrace();
			blkFile = null;
		}
	}
	public PDSDEM(String imageStr)
	{
		imageFilename = imageStr;
		try {
			blkFile = new BlockFile(imageStr, bufferSize, labelOffset);
			//int fileSize = blkFile.getFileSize();
			//pCache = new PixCache((int)(2.0*Math.sqrt(fileSize)));
		}
		catch (IOException ioEx)
		{
			System.out.println("Caught exception in PDSDEM(2): "+ioEx);
			ioEx.printStackTrace();
			blkFile = null;
		}
	}
	public void copyFrom(PDSDEM dem)
	{

	}
	public void closeFile() throws IOException
	{
		if (blkFile != null)
			blkFile.closeFile();
	}
	public void setImageData(byte data[])
	{
		if (blkFile == null)
			blkFile = new BlockFile();

		blkFile.setBlockData(data);
	}
	public byte[] getImageData()
	{
		return blkFile.getBlockData();
	}
	public boolean isDEM(){ return isValidDEM;}
	public boolean isTopography(){ return isTopo;}
	public boolean isGlobal()
	{
		boolean result = false;

		if ((maxLat - minLat) >= (180.0 - GeometricConstants.EPS5))
		{
			if ((maxLon - minLon) >= (360.0 - GeometricConstants.EPS5))
			{
				result = true;
			}
		}

		return result;
	}
	public String getTargetBody(){ return targetStr;}
	public MapProjection.ProjectionTypes getProjectionType()
	{
		return projType;
	}
	public void getPixelCoordinates(int row, int col, UV result)
	{
		switch (projType){
			case PolarStereographic:
			{
				double X = (col - nSamples/2)/mapResolution;
				double Y = (row - nLines/2)/mapResolution;

				double R = Math.sqrt(X*X + Y*Y);

				double LAT, LON;

				LAT = 90.0 - 2.0*Math.atan(R*GeometricConstants.PI/360.0) * 180.0/GeometricConstants.PI;// (northern hemisphere)
				LON = Math.atan2(X,Y) * 180.0/GeometricConstants.PI;

				if (centerLatDeg < 0.0)
				{
					LAT = -90.0 + 2.0*Math.atan(R*GeometricConstants.PI/360.0) * 180.0/GeometricConstants.PI;// (southern hemisphere)
					Y = -Y;
					LON = Math.atan2(X,Y) * 180.0/GeometricConstants.PI;
				}

				while (LON < 0.0) LON += 360.0;

				result.lat = GeometricConstants.DE2RA * LAT;
				result.lon = GeometricConstants.DE2RA * LON;
			}
			break;
			case ObliqueStereographic:
			{
				double obliqueScaleFactor = bodyRadius / mapScale;
				double X = (col - (double)(nSamples/2))/obliqueScaleFactor;
				double Y = ((double)(nLines/2) - row)/obliqueScaleFactor;

				double cosLat1 = Math.cos(centerLatDeg * GeometricConstants.DE2RA);
				double sinLat1 = Math.sin(centerLatDeg * GeometricConstants.DE2RA);

				// EQ: 20-18, 21-15
				double rho = Math.sqrt(X*X + Y*Y);
				if (rho > GeometricConstants.EPS10)
				{
					double c = 2.0 * Math.atan(rho / 2.0);
					double sinC = Math.sin(c);
					double cosC = Math. cos(c);

					// EQ: 20-14, 20-15
					result.lat = Math.asin( cosC*sinLat1 + (Y*sinC*cosLat1/rho) );
					double yComponent = X*sinC;
					double xComponent = (rho*cosLat1*cosC - Y*sinLat1*sinC);
					result.lon = GeometricConstants.DE2RA * centerLonDeg + Math.atan2(yComponent, xComponent);
				}
				else
				{
					result.lat = centerLatDeg * GeometricConstants.DE2RA;
					result.lon = centerLonDeg * GeometricConstants.DE2RA;
				}
			}
			break;
			case SimpleCylindrical:
			{
				result.lat = GeometricConstants.DE2RA * (maxLat - ((double)row / mapResolution));
				result.lon = GeometricConstants.DE2RA * (minLon + ((double)col / mapResolution));
			}
			break;
			case EquiRectangular:
			{
				double X = (double)(col - ((double)nSamples/2.0)) * mapScale;
				double Y = (double)(((double)nLines/2.0) - row) * mapScale;
				result.lat = Y/bodyRadius + centerLatDeg*GeometricConstants.DE2RA;
				result.lon = centerLonDeg * GeometricConstants.DE2RA + X/(bodyRadius*Math.cos(centerLatDeg*GeometricConstants.DE2RA));
				//result.x = bodyRadius * (lonDeg - centerLonDeg) * GeometricConstants.DE2RA * Math.cos(centerLatDeg * GeometricConstants.DE2RA)/mapScale + (double)nSamples/2.0;
				//result.y = (double)nLines/2.0 - bodyRadius * (latDeg - centerLatDeg) * GeometricConstants.DE2RA/mapScale;
			}
			break;
		}
	}
	public void getPixelCoordinates(XY xy, UV result)
	{
		switch (projType){
			case PolarStereographic:
			{
				double X = (xy.x - 0.50 * (double)nSamples)/mapResolution;
				double Y = (xy.y - 0.50 * (double)nLines)/mapResolution;

				double R = Math.sqrt(X*X + Y*Y);

				double LAT, LON;

				LAT = 90.0 - 2.0*Math.atan(R*GeometricConstants.PI/360.0) * 180.0/GeometricConstants.PI;// (northern hemisphere)
				LON = Math.atan2(X,Y) * 180.0/GeometricConstants.PI;

				if (centerLatDeg < 0.0)
				{
					LAT = -90.0 + 2.0*Math.atan(R*GeometricConstants.PI/360.0) * 180.0/GeometricConstants.PI;// (southern hemisphere)
					Y = -Y;
					LON = Math.atan2(X,Y) * 180.0/GeometricConstants.PI;
				}

				while (LON < 0.0) LON += 360.0;

				result.lat = GeometricConstants.DE2RA * LAT;
				result.lon = GeometricConstants.DE2RA * LON;
			}
			break;
			case ObliqueStereographic:
			{
				double obliqueScaleFactor = bodyRadius / mapScale;
				double X = (xy.x - (double)(nSamples/2))/obliqueScaleFactor;
				double Y = ((double)(nLines/2) - xy.y)/obliqueScaleFactor;

				double cosLat1 = Math.cos(centerLatDeg * GeometricConstants.DE2RA);
				double sinLat1 = Math.sin(centerLatDeg * GeometricConstants.DE2RA);

				// EQ: 20-18, 21-15
				double rho = Math.sqrt(X*X + Y*Y);
				if (rho > GeometricConstants.EPS10)
				{
					double c = 2.0 * Math.atan(rho / 2.0);
					double sinC = Math.sin(c);
					double cosC = Math. cos(c);

					// EQ: 20-14, 20-15
					result.lat = Math.asin( cosC*sinLat1 + (Y*sinC*cosLat1/rho) );
					double yComponent = X*sinC;
					double xComponent = (rho*cosLat1*cosC - Y*sinLat1*sinC);
					result.lon = GeometricConstants.DE2RA * centerLonDeg + Math.atan2(yComponent, xComponent);
				}
				else
				{
					result.lat = centerLatDeg * GeometricConstants.DE2RA;
					result.lon = centerLonDeg * GeometricConstants.DE2RA;
				}
			}
			break;
			case SimpleCylindrical:
			{
				result.lat = GeometricConstants.DE2RA * (maxLat - ((double)xy.y / mapResolution));
				result.lon = GeometricConstants.DE2RA * (minLon + ((double)xy.x / mapResolution));
			}
			break;
			case EquiRectangular:
			{
				double X = (double)(xy.x - ((double)nSamples/2.0)) * mapScale;
				double Y = (double)(((double)nLines/2.0) - xy.y) * mapScale;
				result.lat = Y/bodyRadius + centerLatDeg*GeometricConstants.DE2RA;
				result.lon = centerLonDeg * GeometricConstants.DE2RA + X/(bodyRadius*Math.cos(centerLatDeg*GeometricConstants.DE2RA));
				//result.x = bodyRadius * (lonDeg - centerLonDeg) * GeometricConstants.DE2RA * Math.cos(centerLatDeg * GeometricConstants.DE2RA)/mapScale + (double)nSamples/2.0;
				//result.y = (double)nLines/2.0 - bodyRadius * (latDeg - centerLatDeg) * GeometricConstants.DE2RA/mapScale;
			}
			break;
		}
	}
	public double getPixelValue(int row, int col)
	{
		double result = noDataValue;

		if (row < 0) row = 0;
		if (row >= nLines) row = nLines - 1;
		if (col < 0) col = 0;
		if (col >= nSamples) col = nSamples - 1;

		if ((row == 3916) && (col == 3718))
		{
			result = noDataValue;
		}

		if (!wantCache)
		{
			if ((blkFile != null) && (row >= 0) && (col >= 0) && (row < nLines) && (col < nSamples))
			{
				float fValue=0.0f;
				int   iValue=0;

				int offset = (row * nSamples + col) * (bitsPerSample / 8);
				if (offset < blkFile.getFileSize())
				{
					iValue = pullValue(offset);
				}

				if (sampleType == PDSTypes.SampleTypes.PC_REAL)
				{
					fValue = Float.intBitsToFloat(iValue);
					result = scaleFactor * (double)fValue + heightOffset;
				}
				else
				{
					result = scaleFactor * (double)iValue + heightOffset;
				}
			}
		}
		else
		{
			if (uncompImg != null)
			{
				result = scaleFactor * (double)uncompImg[row*nSamples+col] + heightOffset;
			}
			else
			{
				result = scaleFactor * (double)compImg.getSample(row, col) + heightOffset;
			}
		}

		return result;
	}
	public boolean getPixelValue(int row, int col, PDSSample sample)
	{
		boolean result = false;

		int bpp = bitsPerSample/8;
		int off = bpp * (row * nSamples + col);

		byte data[] = new byte[bpp];

		try {
			pullDataFromFile(off, data);
			sample.createSample(data, bitsPerSample, 0, sampleType);
			result = true;
		}
		catch (IOException ioEx)
		{
			System.out.println(ioEx);
			ioEx.printStackTrace();
		}

		return result;
	}
	public boolean getPixelValue(XY xy, PDSSample sample)
	{
		boolean result = false;

		int row1 = (int)Math.floor(xy.y);
		int row2 = row1 + 1;

		if ((row1 >= 0) && (row1 < (nLines-1)))
		{
			int col1 = (int)Math.floor(xy.x);
			int col2 = col1 + 1;
			if ((col1 >= 0) && (col1 < (nSamples-1)))
			{
				PDSSample h11 = new PDSSample();
				PDSSample h12 = new PDSSample();
				PDSSample h21 = new PDSSample();
				PDSSample h22 = new PDSSample();

				boolean gotH11 = getPixelValue(row1, col1, h11);
				if (gotH11)
					sample.copy(h11);

				boolean gotH12 = getPixelValue(row1, col2, h12);
				boolean gotH21 = getPixelValue(row2, col1, h21);
				boolean gotH22 = getPixelValue(row2, col2, h22);

				if (gotH11 && gotH12 && gotH21 && gotH22)
				{
					double d11 = h11.dData;
					double d12 = h12.dData;
					double d21 = h21.dData;
					double d22 = h22.dData;

					double hTop = d11 + (xy.x - col1)*(d12 - d11);
					double hBot = d21 + (xy.x - col1)*(d22 - d21);
					sample.dData = hTop + (xy.y - (double)row1)*(hBot - hTop);
					result = true;
				}
				else if (gotH11)
				{
					sample.copy(h11);
					result = true;
				}
			}
			else if (col1 == (nSamples-1))
			{
				PDSSample h11 = new PDSSample();
				PDSSample h21 = new PDSSample();

				boolean gotH11 = getPixelValue(row1, col1, h11);
				boolean gotH21 = getPixelValue(row2, col1, h21);

				double d11 = h11.dData;
				double d21 = h21.dData;

				if (gotH11 && gotH21)
				{
					sample.dData = d11 + (xy.y - (double)row1)*(d21 - d11);
					result = true;
				}
			}
		}
		else if (row1 == (nLines-1))
		{
			int col1 = (int)Math.floor(xy.x);
			int col2 = col1 + 1;

			if ((col1 >= 0) && (col1 < (nSamples-1)))
			{
				PDSSample h11 = new PDSSample();
				PDSSample h12 = new PDSSample();

				boolean gotH11 = getPixelValue(row1, col1, h11);
				boolean gotH12 = getPixelValue(row1, col2, h12);
				if (gotH11 && gotH12)
				{
					sample.dData = h11.dData + (xy.x - (double)col1)*(h12.dData - h11.dData);
					result = true;
				}
			}
			else if (col1 == (nSamples-1))
			{
				PDSSample h11 = new PDSSample();
				boolean gotH11 = getPixelValue(row1, col1, h11);
				if (gotH11)
				{
					sample.copy(h11);
					result = true;
				}
			}
		}

		sample.fData = (float)sample.dData;
		sample.iData = (int)(0.50 + sample.dData);
		sample.lData = (long)(0.50 + sample.dData);

		return result;
	}
	boolean pullDataFromFile(int offset, byte data[]) throws IOException
	{
		return blkFile.readBytes(offset, data);
	}
	int pullValue(int offset)
	{
		int iValue = 0;

		try {
			switch (bitsPerSample){
				case 8: iValue = blkFile.readByte(offset); break;
				case 16:
					{
						blkFile.readBytes(offset, rawData2);
						iValue = getRawValue(rawData2[0], rawData2[1]);
					}
					break;
				case 32:
					{
						//iValue = blkFile.readInt(offset);
						blkFile.readBytes(offset, rawData4);
						iValue = ((rawData4[3]&0xff)<<24) | ((rawData4[2]&0xff)<<16) | ((rawData4[1]&0xff)<<8) | (rawData4[0]&0xff);
					}
					break;
			}
		}
		catch (Exception ex)
		{
			System.out.println("Caught exception in PDSDEM.getGridValue: "+ex);
			ex.printStackTrace();
		}

		return iValue;
	}
	public double getPixelValue(RandomAccessFile raf, int row, int col)
	{
		double result = noDataValue;

		if ((row >= 0) && (col >= 0) && (row < nLines) && (col < nSamples))
		{
			float fValue=0.0f;
			int   iValue=0;

			int offset = (row * nSamples + col) * (bitsPerSample / 8);
			try {
				raf.seek(offset);

				switch (bitsPerSample){
					case 8:
						{
							iValue = raf.readByte();
						}
						break;
					case 16:
						{
							rawData2[0] = raf.readByte();
							rawData2[1] = raf.readByte();
							iValue = getRawValue(rawData2[0], rawData2[1]);
						}
						break;
					case 32:
						{
							rawData4[0] = raf.readByte();
							rawData4[1] = raf.readByte();
							rawData4[2] = raf.readByte();
							rawData4[3] = raf.readByte();
							iValue = ((rawData4[3]&0xff)<<24) | ((rawData4[2]&0xff)<<16) | ((rawData4[1]&0xff)<<8) | (rawData4[0]&0xff);
						}
						break;
				}
			}
			catch (IOException ex)
			{
				System.out.println("Caught exception in PDSDEM.getPixelValue: "+ex);
				ex.printStackTrace();
			}

			if (sampleType == PDSTypes.SampleTypes.PC_REAL)
			{
				fValue = Float.intBitsToFloat(iValue);
				result = scaleFactor * (double)fValue + heightOffset;
			}
			else
			{
				result = scaleFactor * (double)iValue + heightOffset;
			}
		}

		return result;
	}
	public double getPixelValue(XY xy)
	{
		double result = noDataValue;

		int row1 = (int)Math.floor(xy.y);
		int row2 = row1 + 1;

		if ((row1 >= 0) && (row1 < (nLines-1)))
		{
			int col1 = (int)Math.floor(xy.x);
			int col2 = col1 + 1;
			if ((col1 >= 0) && (col1 < (nSamples-1)))
			{
				double h11 = getPixelValue(row1, col1);
				double h12 = getPixelValue(row1, col2);
				double h21 = getPixelValue(row2, col1);
				double h22 = getPixelValue(row2, col2);

				double hTop = h11 + (xy.x - col1)*(h12 - h11);
				double hBot = h21 + (xy.x - col1)*(h22 - h21);
				result = hTop + (xy.y - row1)*(hBot - hTop);
			}
			else if (col1 == (nSamples-1))
			{
				double h11 = getPixelValue(row1, col1);
				double h21 = getPixelValue(row2, col1);
				result = h11 + (xy.y - row1)*(h21 - h11);
			}
		}
		else if (row1 == (nLines-1))
		{
			int col1 = (int)Math.floor(xy.x);
			int col2 = col1 + 1;

			if ((col1 >= 0) && (col1 < (nSamples-1)))
			{
				double h11 = getPixelValue(row1, col1);
				double h12 = getPixelValue(row1, col2);

				result = h11 + (xy.x - col1)*(h12 - h11);
			}
			else if (col1 == (nSamples-1))
			{
				result = getPixelValue(nLines-1, nSamples-1);
			}
		}

		return result;
	}
	public double getPixelValue(RandomAccessFile raf, XY xy)
	{
		double result = noDataValue;

		int row1 = (int)Math.floor(xy.y);
		int row2 = row1 + 1;

		if ((row1 >= 0) && (row1 < (nLines-1)))
		{
			int col1 = (int)Math.floor(xy.x);
			int col2 = col1 + 1;
			if ((col1 >= 0) && (col1 < (nSamples-1)))
			{
				double h11 = getPixelValue(raf, row1, col1);
				double h12 = getPixelValue(raf, row1, col2);
				double h21 = getPixelValue(raf, row2, col1);
				double h22 = getPixelValue(raf, row2, col2);

				double hTop = h11 + (xy.x - col1)*(h12 - h11);
				double hBot = h21 + (xy.x - col1)*(h22 - h21);
				result = hTop + (xy.y - row1)*(hBot - hTop);
			}
			else if (col1 == (nSamples-1))
			{
				double h11 = getPixelValue(raf, row1, col1);
				double h21 = getPixelValue(raf, row2, col1);
				result = h11 + (xy.y - row1)*(h21 - h11);
			}
		}
		else if (row1 == (nLines-1))
		{
			int col1 = (int)Math.floor(xy.x);
			int col2 = col1 + 1;

			if ((col1 >= 0) && (col1 < (nSamples-1)))
			{
				double h11 = getPixelValue(raf, row1, col1);
				double h12 = getPixelValue(raf, row1, col2);

				result = h11 + (xy.x - col1)*(h12 - h11);
			}
			else if (col1 == (nSamples-1))
			{
				result = getPixelValue(raf, nLines-1, nSamples-1);
			}
		}

		return result;
	}
	public int getRawValue(byte b1, byte b2)
	{
		int result = 0;

		if (sampleType == PDSTypes.SampleTypes.MSB_UNSIGNED_INTEGER)
		{
			result = ( ((b1&0xff)<<8) | (b2&0xff) );
		}
		else if (sampleType == PDSTypes.SampleTypes.MSB_INTEGER)
		{
			short sValue = (short)( ((b1&0xff)<<8) | (b2&0xff) );
			result = sValue;
		}
		else if (sampleType == PDSTypes.SampleTypes.LSB_UNSIGNED_INTEGER)
		{
			result = ( ((b2&0xff)<<8) | (b1&0xff) );
		}
		else
		{
			short sValue = (short)( ((b2&0xff)<<8) | (b1&0xff) );
			result = sValue;
		}

		return result;
	}
	public void getGridValue(int row, int col, Pair<Double,UV> result)
	{
		getPixelCoordinates(row, col, result.second);
		result.first = getPixelValue(row, col);
	}

	public void getGridValue(IJ ij, Pair<Double,UV> result)
	{
		getGridValue(ij.j, ij.i, result);
	}

	public void getMapValue(double latDeg, double lonDeg, Pair<Double,XY> result)
	{
		getPixelFromCoordinates(latDeg, lonDeg, result.second);
		result.first = getPixelValue( (int)(0.50 + result.second.y), (int)(0.50 + result.second.x));
		//result.first = getPixelValue( (int)result.second.y, (int)result.second.x);
	}
	public void getMapValue(UV uv, Pair<Double,XY> result)
	{
		getMapValue(uv.lat*GeometricConstants.RA2DE, uv.lon*GeometricConstants.RA2DE, result);
	}
	public void getPixelFromCoordinates(UV uv, XY result)
	{
		getPixelFromCoordinates(uv.lat*GeometricConstants.RA2DE, uv.lon*GeometricConstants.RA2DE, result);
	}
	public void getPixelFromCoordinates(double latDeg, double lonDeg, XY result)
	{
		switch (projType){
			case PolarStereographic:
			{
				//
				// from the MOLA website, these formulas are essentially MOLA's
				// stereographic implementation
				//X = (I - N/2 - 0.5)/RES
				//Y = (J - N/2 - 0.5)/RES
				//
				//R = SQRT(X^2 + Y^2)
				//
				//LON = ATAN2(X,Y) * 180/PI
				//LAT = 90 - 2*ATAN(R*PI/360) * 180/PI (northern hemisphere)
				//LAT = -90 + 2*ATAN(R*PI/360) * 180/PI (southern hemisphere)
				//

				double R = (360.0/GeometricConstants.PI) * Math.tan((GeometricConstants.PI/360.0)*(90.0 - latDeg));
				if (centerLatDeg < 0.0)
					R = (360.0/GeometricConstants.PI) * Math.tan((GeometricConstants.PI/360.0)*(90.0 + latDeg));

				double Y = R * Math.cos(lonDeg*GeometricConstants.DE2RA);
				double X = R * Math.sin(lonDeg*GeometricConstants.DE2RA);

				if (centerLatDeg < 0.0)
					Y = -Y;

				result.x = mapResolution * X + (double)(nSamples/2);
				result.y =  mapResolution * Y + (double)(nLines/2);
			}
			break;
			case ObliqueStereographic:
			{
				double sinLat1 = Math.sin(centerLatDeg * GeometricConstants.DE2RA);
				double cosLat1 = Math.cos(centerLatDeg * GeometricConstants.DE2RA);
				double obliqueScaleFactor = bodyRadius / mapScale;

				double lat = latDeg * GeometricConstants.DE2RA;
				double lon = lonDeg * GeometricConstants.DE2RA;
				double lon0 = centerLonDeg * GeometricConstants.DE2RA;

				double cosLat = Math.cos(lat);
				double sinLat = Math.sin(lat);
				double cosLonMinusLon0 = Math.cos(lon-lon0);

				// EQ: 21-2, 21-3, 21-4
				double k = 2 / (1 + sinLat1*sinLat + cosLat1*cosLat*cosLonMinusLon0);
				double X = k * cosLat * Math.sin(lon - lon0);
				double Y = k * (cosLat1*sinLat - sinLat1*cosLat*cosLonMinusLon0);

				// Convert from model coordinates to world coordinates
				// Note that the var scaleFactor is composed of the parameters R and k0
				result.x = X * obliqueScaleFactor + (double)nSamples*0.50;
				result.y = (Y * obliqueScaleFactor) + (double)nLines*0.50;
				result.y = (double)nLines - result.y;
			}
			break;
			case SimpleCylindrical:
			{
				//while (lonDeg > 360.0) lonDeg -= 360.0;
				//while (lonDeg < 0.0) lonDeg += 360.0;
				result.x = mapResolution * (lonDeg - minLon);
				if (result.x < 0.0)
					result.x = mapResolution * ((lonDeg+360.0) - minLon);
				else if (result.x > nSamples)
					result.x = mapResolution * ((lonDeg-360.0) - minLon);
				result.y = mapResolution * (maxLat - latDeg);
			}
			break;
			case EquiRectangular:
			{
				//while (lonDeg > 360.0) lonDeg -= 360.0;
				//while (lonDeg < 0.0) lonDeg += 360.0;
				//double X = (double)(col - ((double)nSamples/2.0)) * mapScale;
				//double Y = (double)(((double)nLines/2.0) - row) * mapScale;
				//result.lat = Y/bodyRadius + centerLatDeg*GeometricConstants.DE2RA;
				//result.lon = centerLonDeg * GeometricConstants.DE2RA + X/(bodyRadius*Math.cos(centerLatDeg*GeometricConstants.DE2RA));
				result.x = bodyRadius * (lonDeg - centerLonDeg) * GeometricConstants.DE2RA * Math.cos(centerLatDeg * GeometricConstants.DE2RA)/mapScale + (double)nSamples/2.0;
				result.y = (double)nLines/2.0 - bodyRadius * (latDeg - centerLatDeg) * GeometricConstants.DE2RA/mapScale;
			}
			break;
		}
	}
	public boolean isInside(UV uv)
	{
		boolean result = false;

		double lat = uv.lat * GeometricConstants.RA2DE;
		if ((lat <= maxLat) && (lat >= minLat))
		{
			XY xy = new XY();
			getPixelFromCoordinates(uv, xy);
			int row = (int)(0.50 + xy.y);
			int col = (int)(0.50 + xy.x);
			if ((row >= 0) && (row < nLines)&& (col >= 0) && (col < nSamples))
			{
				result = true;
			}
		}

		return result;
	}
	public boolean areCoordinatesInside(double latDeg, double lonDeg)
	{
		boolean result = false;

		XY xy = new XY();
		getPixelFromCoordinates(latDeg, lonDeg, xy);
		if ((xy.x >= 0.0) && (xy.x < nSamples))
		{
			if ((xy.y >= 0.0) && (xy.y < nLines))
			{
				result = true;
			}
		}

		return result;
	}
	public float[] getBoxValues(int startRow, int startCol, int boxWidth, int boxHeight)
	{
		float results[] = null;

		if ((startRow >= 0) && (startCol >= 0) && (boxWidth > 0) && (boxHeight > 0) &&
			((startRow+boxHeight) <= nLines) && ((startCol+boxWidth) <= nSamples))
		{
			results = new float[boxWidth*boxHeight];

			int ndx = 0;

			for (int row = 0; row < boxHeight; row++){
				for (int col = 0; col < boxWidth; col++){
					results[ndx++] = (float)getPixelValue(row+startRow, col+startCol);
				}
			}
		}

		return results;
	}
	public UV[] getBoxCoordinates(int startRow, int startCol, int boxWidth, int boxHeight)
	{
		UV results[] = null;

		if ((startRow >= 0) && (startCol >= 0) && (boxWidth > 0) && (boxHeight > 0) &&
			((startRow+boxHeight) <= nLines) && ((startCol+boxWidth) <= nSamples))
		{
			results = new UV[boxWidth*boxHeight];

			int ndx = 0;

			for (int row = 0; row < boxHeight; row++){
				for (int col = 0; col < boxWidth; col++){
					getPixelCoordinates(row+startRow, col+startCol, results[ndx++]);
				}
			}
		}

		return results;
	}
	public double getPixelSpacingMeters()
	{
		double result = 0.0;
		UV corner1UV = new UV();
		UV corner2UV = new UV();
		V3 corner1Vec = new V3();
		V3 corner2Vec = new V3();

		getPixelCoordinates(0, 0, corner1UV);
		getPixelCoordinates(nLines-1, nSamples-1, corner2UV);

		Ellipsoid.latLongToV3(corner1UV, 1.0, corner1Vec);
		Ellipsoid.latLongToV3(corner2UV, 1.0, corner2Vec);

		switch (projType){
			case PolarStereographic:
			case ObliqueStereographic:
			case EquiRectangular:
			{
				double cornerAngle = VectorOps.AngularSep(corner1Vec, corner2Vec);
				double d = Math.sqrt((double)nLines*(double)nLines + (double)nSamples*(double)nSamples);
				double pixelAngle = cornerAngle / d;
				double r = bodyRadius * GeometricConstants.Kilometers2Meters;

				result = (pixelAngle * r);
			}
			break;
			case SimpleCylindrical:
			{
				result = 2.0 * GeometricConstants.PI * bodyRadius * GeometricConstants.Kilometers2Meters / (360.0 * mapResolution);
			}
			break;
		}

		return result;
	}
	public double getAngularSpacing()
	{
		double result = 0.0;

		switch (projType){
			case PolarStereographic:
			case ObliqueStereographic:
			case EquiRectangular:
			{
				UV corner1UV = new UV();
				UV corner2UV = new UV();
				V3 corner1Vec = new V3();
				V3 corner2Vec = new V3();

				getPixelCoordinates(0, 0, corner1UV);
				getPixelCoordinates(nLines-1, nSamples-1, corner2UV);

				Ellipsoid.latLongToV3(corner1UV, 1.0, corner1Vec);
				Ellipsoid.latLongToV3(corner2UV, 1.0, corner2Vec);

				double cornerAngle = VectorOps.AngularSep(corner1Vec, corner2Vec);
				result = (cornerAngle / Math.sqrt((double)nLines*(double)nLines + (double)nSamples*(double)nSamples));
			}
			break;
			case SimpleCylindrical:
			{
				result = GeometricConstants.DE2RA / mapResolution;
			}
			break;
		}

		return result;
	}
}
