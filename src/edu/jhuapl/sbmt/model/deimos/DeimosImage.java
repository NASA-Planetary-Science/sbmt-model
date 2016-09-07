package edu.jhuapl.sbmt.model.deimos;

import java.io.IOException;

import nom.tam.fits.FitsException;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.phobos.PhobosImage;

public class DeimosImage extends PhobosImage
{
    public DeimosImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }
}
