package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.model.image.BasicFileWriter;

public class OVIRSSpectrumWriter extends BasicFileWriter
{
    double boresightLatDeg;
    double boresightLonDeg;
    double et;
    double[] calibratedRadiance;
    double[] calibratedRadianceUncertainty;

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
            calibratedRadiance = ovirs.getSpectrum();
            calibratedRadianceUncertainty = ovirs.getCalibratedRadianceUncertainty();

            writer.write("# ET time");
            writer.newLine();
            writer.write("" + ovirs.getTime());
            writer.newLine();
            writer.write("# Boresight Latitude (deg), Boresight Longitude (deg)");
            writer.newLine();
            writer.write("" + ovirs.getBoresightLatDeg() + "," + ovirs.getBoresightLonDeg());
            writer.newLine();
            writer.write("## Wavelength (Band Center), Calibrated Radiance, Calibrated Radiance Uncertainty");
            writer.newLine();
            for (int i=0; i<OVIRS.bandCenters.length; i++)
            {
                writer.write(OVIRS.bandCenters[i] + "," + calibratedRadiance[i] + "," + calibratedRadianceUncertainty[i]);
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

    public double[] getCalibratedRadiance()
    {
        return calibratedRadiance;
    }

    public double[] getCalibratedRadianceUncertainty()
    {
        return calibratedRadianceUncertainty;
    }

    public double getEt()
    {
        return et;
    }

    public double getBoresightLatDeg()
    {
        return boresightLatDeg;
    }

    public double getBoresightLonDeg()
    {
        return boresightLonDeg;
    }

    public boolean boresightIsOnSurface()
    {
        return boresightLatDeg!=-999 & boresightLonDeg!=-999;
    }

}
