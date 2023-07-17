package edu.jhuapl.sbmt.model.bennu.imaging;

import java.io.IOException;

import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;

import nom.tam.fits.FitsException;


public class PolyCamImage extends MapCamImage
{
    public PolyCamImage(ImageKeyInterface key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }
}