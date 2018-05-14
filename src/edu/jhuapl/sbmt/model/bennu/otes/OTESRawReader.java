package edu.jhuapl.sbmt.model.bennu.otes;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OTESRawReader extends BasicFileReader
{
//    String sourceFileName;
    double et;
    double[] calibratedRadiance;

    public OTESRawReader(String filename)
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


            while (true)
            {
                byte[] sclk = new byte[4];
                et = stream.read(sclk);
                if (et == -1) break;
                System.out.println("OTESSRawReader: read: et is " + ByteBuffer.wrap(sclk).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
                byte[] sclkmsec = new byte[2];
                stream.read(sclkmsec); //gets the sclk msec
//                System.out.println("OTESSpectrumReader: read: et frac sec is " + ByteBuffer.wrap(sclkmsec).getShort());
                stream.read(new byte[2]); //gets the ick
                stream.read(new byte[2]); //gets the flags

                for (int i=0; i<OTES.bandCenters.length; i++)
                {
                    byte[] val = new byte[4];
                    stream.read(val);
                    calibratedRadiance[i] = ByteBuffer.wrap(val).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
                    if (calibratedRadiance[i] > 0)
                        System.out.println("OTESRawReader: read: radiance " + calibratedRadiance[i] + " at time " + ByteBuffer.wrap(sclk).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
                }

                byte[] val2 = new byte[4];
                stream.read(val2);  //brightness temp uncertainty
                stream.read(val2);  //max brightness temp

                for (int i=0; i<OTES.bandCenters.length; i++)
                {
                    byte[] val = new byte[4];
                    stream.read(val);
//                    System.out.println("OTESRawReader: read: wavelengths " + ByteBuffer.wrap(val).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat());
                }

//                byte[] data = new byte[1];
//                int bytesRead = stream.read(data);
//                if (bytesRead == -1)
//                {
//                    break;
//                }
            }
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

    public static void main(String[] args)
    {
        new OTESRawReader("/Users/steelrj1/Desktop/otesl2calrad20170922T224426.067Zrecid6002.dat").read();
//        new OTESRawReader("/Users/steelrj1/Desktop/otesl2calrad20170925T000010.324Zrecid8003.dat").read();
    }
}

