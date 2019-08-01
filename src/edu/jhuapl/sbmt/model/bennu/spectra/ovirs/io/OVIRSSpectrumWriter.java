package edu.jhuapl.sbmt.model.bennu.spectra.ovirs.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRS;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSpectrum;
import edu.jhuapl.sbmt.model.image.BasicFileWriter;

public class OVIRSSpectrumWriter extends BasicFileWriter
{
    OVIRSSpectrum ovirs;

    public OVIRSSpectrumWriter(String filename, OVIRSSpectrum ovirs)
    {
        super(filename);
        this.ovirs = ovirs;
    }

    @Override
    public void write()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
            double[] yData = ovirs.getSpectrum();

            writer.write("# Encoded SCLK time");
            writer.newLine();
            writer.write("" + ovirs.getTime());
            writer.newLine();
            writer.write("# Boresight Latitude (deg), Boresight Longitude (deg)");
            writer.newLine();
            writer.write("" + ovirs.getBoresightIntercept());
            writer.newLine();
            writer.write("## Wavelength (Band Center), Spectrum Value");
            writer.newLine();
            OVIRS ovirs = new OVIRS();
            for (int i=0; i<OVIRS.bandCentersLength; i++)
            {
                writer.write(ovirs.getBandCenters()[i] + "," + yData[i]);
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
