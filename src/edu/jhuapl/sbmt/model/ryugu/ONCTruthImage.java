package edu.jhuapl.sbmt.model.ryugu;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import vtk.vtkImageData;
import vtk.vtkProp;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;

import nom.tam.fits.FitsException;

public class ONCTruthImage extends ONCImage
{

    public ONCTruthImage(ImageKeyInterface key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // There is a -90 degree rotation in the data.
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

        status += df.format(pickPosition[1]);
        status += ", ";
        status += df.format(getImageWidth() - pickPosition[0]);
        status += ")";


     // Append raw pixel value information
        status += ", Raw Value = ";
        if(rawImage == null)
        {
            status += "Unavailable";
        }
        else
        {
            int ip0 = (int)Math.round(pickPosition[0]);
            int ip1 = (int)Math.round(pickPosition[1]);
            if (!rawImage.GetScalarTypeAsString().contains("char"))
            {
                float[] pixelColumn = ImageDataUtil.vtkImageDataToArray1D(rawImage, imageHeight-1-ip0, ip1);
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
