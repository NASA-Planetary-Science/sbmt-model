package edu.jhuapl.sbmt.model.europa;

import edu.jhuapl.sbmt.model.europa.math.V3;
import edu.jhuapl.sbmt.model.europa.pds.PDSCodec;
import edu.jhuapl.sbmt.model.europa.projection.UV;

public class InstrumentPixel {
	public V3 surfIntercept;
	public UV surfPt;
	public int pixelRGB = 0;
	public float incAngleDeg = 180.0f;
	public float emsAngleDeg = 180.0f;
	public float phaseAngleDeg = 180.0f;
	public float pixelSpacingKm = -1.0f;
	public float hourAngle = 180.0f;
	public float score = 0.0f;
	public static int InstrumentPixelRecordLength = 40;
	public InstrumentPixel(){}
	public void getPixelRecord(byte data[], int offset)
	{
		//PDSCodec.encodePCReal(surfIntercept.X1(), offset, 64, data);
		//offset += 8;
		//PDSCodec.encodePCReal(surfIntercept.X2(), offset, 64, data);
		//offset += 8;
		//PDSCodec.encodePCReal(surfIntercept.X3(), offset, 64, data);
		//offset += 8;
		PDSCodec.encodePCReal(surfPt.lat, offset, 64, data);
		offset += 8;
		PDSCodec.encodePCReal(surfPt.lon, offset, 64, data);
		offset += 8;
		PDSCodec.encodePCReal(incAngleDeg, offset, 32, data);
		offset += 4;
		PDSCodec.encodePCReal(emsAngleDeg, offset, 32, data);
		offset += 4;
		PDSCodec.encodePCReal(pixelSpacingKm, offset, 32, data);
		offset += 4;
		PDSCodec.encodePCReal(score, offset, 32, data);
		offset += 4;
	}
	public void fromPixelRecord(byte data[], int offset)
	{
		//surfIntercept.v[0] = PDSCodec.decodePCReal(data, 64, offset);
		//offset += 8;
		//surfIntercept.v[1] = PDSCodec.decodePCReal(data, 64, offset);
		//offset += 8;
		//surfIntercept.v[2] = PDSCodec.decodePCReal(data, 64, offset);
		//offset += 8;
		surfPt.lat = PDSCodec.decodePCReal(data, 64, offset);
		offset += 8;
		surfPt.lon = PDSCodec.decodePCReal(data, 64, offset);
		offset += 8;
		incAngleDeg = (float)PDSCodec.decodePCReal(data, 32, offset);
		offset += 4;
		emsAngleDeg = (float)PDSCodec.decodePCReal(data, 32, offset);
		offset += 4;
		pixelSpacingKm = (float)PDSCodec.decodePCReal(data, 32, offset);
		offset += 4;
		score = (float)PDSCodec.decodePCReal(data, 32, offset);
		offset += 4;
	}
	public static InstrumentPixel createFromRecord(byte data[], int offset)
	{
		InstrumentPixel result = new InstrumentPixel();
		result.fromPixelRecord(data, offset);
		return result;
	}
}
