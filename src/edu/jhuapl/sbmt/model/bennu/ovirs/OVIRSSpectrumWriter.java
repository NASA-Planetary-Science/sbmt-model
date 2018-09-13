package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.sbmt.model.image.BasicFileWriter;

public class OVIRSSpectrumWriter extends BasicFileWriter
{
    Vector3D boresightIntercetp;
    double et;
    double[] xData;
    double[] yData;

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
            xData = ovirs.getxData();
            yData = ovirs.getSpectrum();

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

//    public double[] getCalibratedRadiance()
//    {
//        return calibratedRadiance;
//    }
//
//    public double[] getCalibratedRadianceUncertainty()
//    {
//        return calibratedRadianceUncertainty;
//    }
//
//    public double getEt()
//    {
//        return et;
//    }
//
//    public double getBoresightLatDeg()
//    {
//        return boresightLatDeg;
//    }
//
//    public double getBoresightLonDeg()
//    {
//        return boresightLonDeg;
//    }
//
//    public boolean boresightIsOnSurface()
//    {
//        return boresightLatDeg!=-999 & boresightLonDeg!=-999;
//    }

}
