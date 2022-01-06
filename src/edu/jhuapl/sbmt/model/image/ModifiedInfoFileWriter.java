package edu.jhuapl.sbmt.model.image;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import edu.jhuapl.sbmt.image2.interfaces.IPerspectiveImage;
import edu.jhuapl.sbmt.image2.interfaces.IPerspectiveImageTableRepresentable;
import edu.jhuapl.sbmt.image2.modules.pointing.offset.SpacecraftPointingDelta;
import edu.jhuapl.sbmt.image2.modules.pointing.offset.SpacecraftPointingState;

public class ModifiedInfoFileWriter<G1 extends IPerspectiveImage & IPerspectiveImageTableRepresentable> extends BasicFileWriter
{
	private SpacecraftPointingState state;
	private SpacecraftPointingDelta delta;
	public static final String FRUSTUM1 = "FRUSTUM1";
    public static final String FRUSTUM2 = "FRUSTUM2";
    public static final String FRUSTUM3 = "FRUSTUM3";
    public static final String FRUSTUM4 = "FRUSTUM4";
    public static final String BORESIGHT_DIRECTION = "BORESIGHT_DIRECTION";
    public static final String UP_DIRECTION = "UP_DIRECTION";
    public static final String NUMBER_EXPOSURES = "NUMBER_EXPOSURES";
    public static final String START_TIME = "START_TIME";
    public static final String STOP_TIME = "STOP_TIME";
    public static final String SPACECRAFT_POSITION = "SPACECRAFT_POSITION";
    public static final String SUN_POSITION_LT = "SUN_POSITION_LT";
    public static final String DISPLAY_RANGE = "DISPLAY_RANGE";
    public static final String OFFLIMB_DISPLAY_RANGE = "OFFLIMB_DISPLAY_RANGE";
    public static final String TARGET_PIXEL_COORD = "TARGET_PIXEL_COORD";
    public static final String TARGET_ROTATION = "TARGET_ROTATION";
    public static final String TARGET_ZOOM_FACTOR = "TARGET_ZOOM_FACTOR";
    public static final String APPLY_ADJUSTMENTS = "APPLY_ADJUSTMENTS";
    private boolean adjusted;
    private G1 image;

	public ModifiedInfoFileWriter(String filename, G1 image, SpacecraftPointingState state, SpacecraftPointingDelta delta, boolean adjusted)
	{
		super(filename);
		this.image = image;
		this.state = state;
		this.delta = delta;
		this.adjusted = adjusted;
	}

	@Override
	public void write()
	{
		FileOutputStream fs = null;

		// save out info file to cache with ".adjusted" appended to the name
//		boolean flatten = true;
		String suffix = adjusted? ".adjusted" : "";
		try
		{
			fs = new FileOutputStream(filename + suffix);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		OutputStreamWriter osw = new OutputStreamWriter(fs);
		BufferedWriter out = new BufferedWriter(osw);

		try
		{
			out.write(String.format("%-22s= %s\n", START_TIME, image.getEt()));
			out.write(String.format("%-22s= %s\n", STOP_TIME, image.getEt()));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", SPACECRAFT_POSITION,
					state.getSpacecraftPosition()[0], state.getSpacecraftPosition()[1], state.getSpacecraftPosition()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", BORESIGHT_DIRECTION,
					state.getBoresightDirection()[0], state.getBoresightDirection()[1], state.getBoresightDirection()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", UP_DIRECTION, state.getUpVector()[0],
					state.getUpVector()[1], state.getUpVector()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM1, state.getFrustum1()[0],
					state.getFrustum1()[1], state.getFrustum1()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM2, state.getFrustum2()[0],
					state.getFrustum2()[1], state.getFrustum2()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM3, state.getFrustum3()[0],
					state.getFrustum3()[1], state.getFrustum3()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM4, state.getFrustum4()[0],
					state.getFrustum4()[1], state.getFrustum4()[2]));
			out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", SUN_POSITION_LT, state.getSunPosition()[0],
					state.getSunPosition()[1], state.getSunPosition()[2]));

			//TODO FIX THIS
			out.write(String.format("%-22s= ( %16d , %16d )\n", DISPLAY_RANGE, image.getIntensityRange().min, image.getIntensityRange().max));
			out.write(String.format("%-22s= ( %16d , %16d )\n", OFFLIMB_DISPLAY_RANGE, image.getOfflimbIntensityRange().min,
					image.getOfflimbIntensityRange().max));

			boolean writeApplyAdustments = false;

//			if (!flatten)
			{
				if (state.getTargetPixelCoordinates()[0] != Double.MAX_VALUE && state.getBoresightDirection()[1] != Double.MAX_VALUE)
				{
					out.write(String.format("%-22s= ( %1.16e , %1.16e )\n", TARGET_PIXEL_COORD, state.getTargetPixelCoordinates()[0],
							state.getTargetPixelCoordinates()[1]));
					writeApplyAdustments = true;
				}

				if (delta.getZoomFactor() != 1.0)
				{
					out.write(String.format("%-22s= %1.16e\n", TARGET_ZOOM_FACTOR, delta.getZoomFactor()));
					writeApplyAdustments = true;
				}

				if (delta.getRotationOffset() != 0.0)
				{
					out.write(String.format("%-22s= %1.16e\n", TARGET_ROTATION, delta.getRotationOffset()));
					writeApplyAdustments = true;
				}

				// only write out user-modified offsets if the image info has been
				// modified
//				if (writeApplyAdustments)
//					out.write(String.format("%-22s= %b\n", APPLY_ADJUSTMENTS, applyFrameAdjustments));
			}
			out.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
