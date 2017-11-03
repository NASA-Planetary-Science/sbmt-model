package edu.jhuapl.sbmt.model.ryugu;

import java.io.IOException;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

public class ONCImage extends PerspectiveImage
{

    public ONCImage(ImageKey key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected int[] getMaskSizes()
    {
        // Cribbed the body of this method from AmicaImage at the time ONCImage was created.
        String filename = getFitFileFullPath();

        try
        {
            Fits f = new Fits(filename);
            BasicHDU h = f.getHDU(0);

            int startH = h.getHeader().getIntValue("START_H");
            int startV = h.getHeader().getIntValue("START_V");
            int lastH  = h.getHeader().getIntValue("LAST_H");
            int lastV  = h.getHeader().getIntValue("LAST_V");

            f.getStream().close();

            return new int[]{startV, 1023-lastH, 1023-lastV, startH};
        }
        catch (FitsException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // Should never reach here
        return new int[]{0, 0, 0, 0};
    }

}
