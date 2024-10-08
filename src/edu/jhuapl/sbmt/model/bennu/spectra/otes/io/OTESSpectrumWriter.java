package edu.jhuapl.sbmt.model.bennu.spectra.otes.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.core.io.BasicFileWriter;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSpectrum;

/**
 * Writes the OTES spectrum out to a human readable form for sharing.
 * @author steelrj1
 *
 */
public class OTESSpectrumWriter extends BasicFileWriter
{
    OTESSpectrum otes;

    public OTESSpectrumWriter(String filename, OTESSpectrum otes)
    {
        super(filename);
        this.otes = otes;
    }

    @Override
    public void write()
    {
        try
        {
            double[] calibratedRadiance = otes.getSpectrum();
            Double[] waveNumber = otes.getxData();
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
            writer.write("# ET time");
            writer.newLine();
            writer.write(""+otes.getTime());
            writer.newLine();
            writer.write("## Wavenumber, Calibrated Radiance");
            writer.newLine();
            //TODO needs to be updated to handle L3 data as well
            for (int i=0; i<calibratedRadiance.length; i++)
            {
                writer.write(waveNumber[i] + "," + calibratedRadiance[i]);
                writer.newLine();
            }
            writer.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
