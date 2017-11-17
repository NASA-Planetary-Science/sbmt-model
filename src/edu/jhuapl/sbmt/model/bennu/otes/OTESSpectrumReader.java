package edu.jhuapl.sbmt.model.bennu.otes;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OTESSpectrumReader extends BasicFileReader
{
    String sourceFileName;
    double et;
    double[] calibratedRadiance;

    public OTESSpectrumReader(String filename)
    {
        super(filename);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void read()
    {
        try
        {
            calibratedRadiance=new double[OTES.bandCenters.length];
            DataInputStream stream=new DataInputStream(new FileInputStream(new File(filename)));
            byte[] sourceFileNameBytes=new byte[36];    // Ray stores the filename as 36 bytes at the beginning of the header
            stream.readFully(sourceFileNameBytes);
            sourceFileName=new String(sourceFileNameBytes);
            et=stream.readDouble();
            //byte[] data=new byte[OTES.bandCenters.length*Double.BYTES];
            //stream.readFully(data);
            //ByteBuffer buffer=ByteBuffer.wrap(data);
            //buffer.asDoubleBuffer().get(calibratedRadiance);
            //for (int i=0; i<calibratedRadiance.length; i++)
            //    System.out.println(i+" "+OTES.bandCenters[i]+" "+calibratedRadiance[i]);
            for (int i=0; i<OTES.bandCenters.length; i++)
                calibratedRadiance[i]=stream.readDouble();

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
