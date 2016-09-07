package edu.jhuapl.sbmt.model.bennu;

import java.io.IOException;

import nom.tam.fits.FitsException;

import edu.jhuapl.sbmt.app.SmallBodyModel;


public class PolyCamImage extends MapCamImage
{
    public PolyCamImage(ImageKey key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }
}