package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OVIRSSpectrumReader extends BasicFileReader
{
    double boresightLatDeg;
    double boresightLonDeg;
    double et;
    double[] calibratedRadiance;
    double[] calibratedRadianceUncertainty;

    public OVIRSSpectrumReader(String filename)
    {
        super(filename);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void read()
    {
        try
        {
            calibratedRadiance=new double[OVIRS.bandCentersLength];
            calibratedRadianceUncertainty=new double[OVIRS.bandCentersLength];
            DataInputStream stream=new DataInputStream(new FileInputStream(new File(filename)));
            byte[] junk=new byte[4];
            stream.readFully(junk);
            byte[] sclkStr=new byte[16];
            stream.readFully(sclkStr);
            et=Double.valueOf(new String(sclkStr));
            boresightLatDeg=stream.readDouble();
            boresightLonDeg=stream.readDouble();
            byte[] data=new byte[OVIRS.bandCentersLength*Double.BYTES];
            stream.readFully(data);
            ByteBuffer buffer=ByteBuffer.wrap(data);
            buffer.asDoubleBuffer().get(calibratedRadiance);

            byte[] data2=new byte[OVIRS.bandCentersLength*Double.BYTES];
            stream.readFully(data2);
            ByteBuffer buffer2=ByteBuffer.wrap(data2);
            buffer2.asDoubleBuffer().get(calibratedRadianceUncertainty);
            stream.close();
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
