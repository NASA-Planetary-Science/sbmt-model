package edu.jhuapl.sbmt.model.ryugu.onc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import vtk.vtkImageData;
import vtk.vtkProp;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

import nom.tam.fits.FitsException;

public class ONCImage extends PerspectiveImage
{

    public ONCImage(ImageKeyInterface key, List<SmallBodyModel> smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initialize() throws FitsException, IOException
    {
        // Note this is a really bad idea in general (overriding something that is called
        // from the base class constructor), but this kind of thing is done
        // a lot in SBMT, so for now, just conforming to the pattern.
        fitFileImageExtension = 1;
        super.initialize();
    }

    @Override
    public int[] getMaskSizes()
    {
    	// Don't mask anything.
        return new int[]{0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        return FileCache.getFileFromServer(getImageFileName()).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();
        File keyFile = new File(key.getName());
        String sumFilename = keyFile.getParentFile().getParent()
        + "/infofiles/" + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImmutableMap<String, String> sumfileMap = getSumfileMap();
        String imageKey = getImageFileName().replaceFirst(".*/", "");
        String sumfileName = sumfileMap.get(imageKey);
        if (sumfileName == null) {
            throw new NullPointerException("Cannot determine correct sumfile for image " + imageKey);
        }
        String sumfileDir = new File(getImageFileName()).getParentFile().getParent();
        return FileCache.getFileFromServer(SafeURLPaths.instance().getString(sumfileDir, "sumfiles", sumfileName)).getAbsolutePath();
    }

    private String getImageFileName()
    {
        return key.getName() + ".fit";
    }

    private ImmutableMap<String, String> getSumfileMap()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        File keyFile = new File(getImageFileName());
        File mapFile = FileCache.getFileFromServer(SafeURLPaths.instance().getString(keyFile.getParentFile().getParent(), "make_sumfiles.in"));
        try (BufferedReader br = new BufferedReader(new FileReader(mapFile)))
        {
            while (br.ready())
            {
                String wholeLine = br.readLine();
                String[] line = wholeLine.split("\\s*,\\s*");
                if (line[0].equals(wholeLine))
                {
                    line = wholeLine.split("\\s\\s*");
                }
                if (line.length < 2) throw new ParseException("Cannot parse line " + String.join(" ", line) + " to get sum file/image file names", line.length > 0 ? line[0].length() : 0);
                String sumFile = line[0] + ".SUM";
                String imageFile = line[line.length - 1].replace("xx", "");
                builder.put(imageFile, sumFile);
            }
        }
        catch (IOException | ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return builder.build();
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis. For some reason we need to do
        // this so the image is displayed properly.
        ImageKeyInterface key = getKey();
        if (key.getSource().equals(ImageSource.SPICE))
            ImageDataUtil.rotateImage(rawImage, -90);
    }

    @Override
    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        // Number format
        DecimalFormat df = new DecimalFormat("#.0");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // Construct status message
        String status = "Pixel Coordinate = (";
        if (key.getSource() == ImageSource.SPICE)
        {
	        status += df.format(pickPosition[1]);
	        status += ", ";
	        status += df.format(getImageWidth() - pickPosition[0]);
	        status += ")";
        }
        else
        {
        	status += df.format(pickPosition[0]);
 	        status += ", ";
 	        status += df.format(pickPosition[1]);
 	        status += ")";
        }

     // Append raw pixel value information
        status += ", Raw Value = ";
        if(getRawImage() == null)
        {
            status += "Unavailable";
        }
        else
        {
            int ip0 = (int)Math.round(pickPosition[0]);
            int ip1 = (int)Math.round(pickPosition[1]);
            if (!getRawImage().GetScalarTypeAsString().contains("char"))
            {
                float[] pixelColumn = ImageDataUtil.vtkImageDataToArray1D(getRawImage(), imageHeight-1-ip0, ip1);
                status += pixelColumn[currentSlice];
            }
            else
            {
                status += "N/A";
            }
        }

        return status;
    }
}
