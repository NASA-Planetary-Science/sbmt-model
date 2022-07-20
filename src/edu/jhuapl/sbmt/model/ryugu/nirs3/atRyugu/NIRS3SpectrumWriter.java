package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.util.BasicFileWriter;

/**
 * Writes the NIRS3 spectrum out to a human readable form for sharing.
 * @author steelrj1
 *
 */
public class NIRS3SpectrumWriter extends BasicFileWriter
{
	private NIRS3Spectrum nirs3;

    public NIRS3SpectrumWriter(String filename, NIRS3Spectrum nirs3)
    {
        super(filename);
        this.nirs3 = nirs3;
    }

    @Override
    public void write()
    {
    	NIRS3SpectrumData data = new NIRS3SpectrumData();
    	data.setGeoFields(nirs3.getGeoFields());
    	data.setSpectrum(nirs3.getSpectrum());
    	data.setUtcEnd(nirs3.getUtcEnd());
    	data.setUtcMid(nirs3.getUtcMid());
    	data.setUtcStart(nirs3.getUtcStart());
    	data.et = nirs3.getTime();

    	FileWriter writer;
		try
		{
			writer = new FileWriter(new File(filename));
			data.writeLine(writer);
	    	writer.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
}
