package edu.jhuapl.sbmt.model.deimos;

import java.io.IOException;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;
import edu.jhuapl.sbmt.model.phobos.PhobosImage;

import nom.tam.fits.FitsException;

public class DeimosImage extends PhobosImage
{
    public DeimosImage(ImageKeyInterface key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }
}
