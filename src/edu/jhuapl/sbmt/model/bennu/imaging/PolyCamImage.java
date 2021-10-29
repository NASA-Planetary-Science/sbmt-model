package edu.jhuapl.sbmt.model.bennu.imaging;

import java.io.IOException;
import java.util.List;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;

import nom.tam.fits.FitsException;


public class PolyCamImage extends MapCamImage
{
    public PolyCamImage(ImageKeyInterface key, List<SmallBodyModel> smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }
}