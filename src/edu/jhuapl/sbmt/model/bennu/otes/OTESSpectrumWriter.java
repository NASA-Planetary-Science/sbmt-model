package edu.jhuapl.sbmt.model.bennu.otes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.model.image.BasicFileWriter;

public class OTESSpectrumWriter extends BasicFileWriter
{
    String sourceFileName;
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
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
            writer.write("# ET time");
            writer.newLine();
            writer.write(""+otes.getTime());
            writer.newLine();
            writer.write("## Wavelength (Band Center), Calibrated Radiance");
            writer.newLine();
            for (int i=0; i<OTES.bandCenters.length; i++)
            {
                writer.write(OTES.bandCenters[i] + "," + calibratedRadiance[i]);
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
