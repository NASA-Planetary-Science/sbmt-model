package edu.jhuapl.sbmt.model.bennu;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.BasicSpectrum;
import edu.jhuapl.sbmt.model.spectrum.SpectralInstrument;

public class OTESSpectrum extends BasicSpectrum
{

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

    protected String getLocalInfoFilePath()
    {
        return getFullPath()+getInfoFilePathRelativeToSpectrumFile();
    }



}