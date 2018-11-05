package edu.jhuapl.sbmt.model.pointing;

import java.io.IOException;

public class ImageInfoFromMetadataIO implements InfoFileIO
{

    public ImageInfoFromMetadataIO()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void deleteAdjustedImageInfo()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void saveImageInfo()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadImageInfo() throws IOException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void saveImageInfo(String filename)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadAdjustedSumfile() throws NumberFormatException, IOException
    {
        // TODO Auto-generated method stub
        System.out.println("SumFileFromConfigIO: loadAdjustedSumfile: loading adjusted sum file ");
    }

    @Override
    public String initLocalInfoFileFullPath()
    {
        return "";
    }

}
