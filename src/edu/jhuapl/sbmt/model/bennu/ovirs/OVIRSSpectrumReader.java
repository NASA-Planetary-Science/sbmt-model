package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OVIRSSpectrumReader extends BasicFileReader
{
    String sourceFileName;
    double et;

    double[] calibratedRadiance;

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
            calibratedRadiance=new double[349];
            DataInputStream stream=new DataInputStream(new FileInputStream(new File(filename)));
            byte[] sourceFileNameBytes=new byte[36];
            stream.readFully(sourceFileNameBytes);
            sourceFileName=new String(sourceFileNameBytes);
            et=stream.readDouble();
            byte[] data=new byte[OVIRS.bandCenters.length*Double.BYTES];
            stream.readFully(data);
            ByteBuffer buffer=ByteBuffer.wrap(data);
            buffer.asDoubleBuffer().get(calibratedRadiance);
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

    public double getEt()
    {
        return et;
    }

}
