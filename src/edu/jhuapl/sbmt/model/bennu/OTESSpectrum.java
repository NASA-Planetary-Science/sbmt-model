package edu.jhuapl.sbmt.model.bennu;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;

public class OTESSpectrum extends BasicSpectrum
{
    boolean footprintGenerated=false;
    File infoFile;

    public OTESSpectrum(String filename, SmallBodyModel smallBodyModel,
            SpectralInstrument instrument) throws IOException
    {
        super(filename, smallBodyModel, instrument);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
        throw new IOException("Not implemented.");
    }

    protected String getInfoFileServerPath()
    {
        return getServerPath()+getInfoFilePathRelativeToSpectrumFile();
    }

    protected String getInfoFilePathRelativeToSpectrumFile()
    {
        return "/../infofiles/"+FilenameUtils.getBaseName(getServerPath())+".info";
    }


    @Override
    public void generateFootprint()
    {
        if (!footprintGenerated)
        {

        }
    }

    protected Frustum readPointingFromInfoFile()
    {
        infoFile=FileCache.getFileFromServer(getInfoFileServerPath());
        System.out.println(infoFile+" "+getInfoFileServerPath());
        //
        Frustum frustum=null;
        return frustum;
    }


}