package edu.jhuapl.sbmt.model.bennu.spectra.ovirs.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OVIRSSpectrumReader extends BasicFileReader
{
    double boresightX;
    double boresightY;
    double boresightZ;
    double et;
    double sclk;
    String etString;
    double[] yValues;
    Double[] xValues;
    int numberEntries = 1393;
    int interceptFlag = 0;
    int fovFlag = 0;

    public OVIRSSpectrumReader(String filename)
    {
        super(filename);
    }

    @Override
    public void read()
    {
        try
        {
            DataInputStream stream=new DataInputStream(new FileInputStream(new File(filename)));
            byte[] sclkStr=new byte[20];
            stream.readFully(sclkStr);
            fovFlag = stream.readInt();
            interceptFlag = stream.readInt();
            boresightX = stream.readDouble();
            boresightY = stream.readDouble();
            boresightZ = stream.readDouble();

            numberEntries = stream.readInt();
            xValues=new Double[numberEntries];
            yValues=new double[numberEntries];

            for (int i=0; i<numberEntries; i++)
            {
                xValues[i] = stream.readDouble();
            }

            for (int i=0; i<numberEntries; i++)
            {
                yValues[i] = stream.readDouble();
            }

            stream.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public double[] getData()
    {
        return yValues;
    }

    public Double[] getXAxis()
    {
        return xValues;
    }

    public double getSclk()
    {
        return sclk;
    }

//    public String getSclk()
//    {
//        return etString;
//    }

    public Vector3D getBoresightIntercept()
    {
        return new Vector3D(boresightX, boresightY, boresightZ);
    }

    public boolean boresightIsOnSurface()
    {
        return interceptFlag == 0 ? false : true;
    }
}