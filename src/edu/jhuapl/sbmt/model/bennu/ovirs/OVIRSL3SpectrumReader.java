package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OVIRSL3SpectrumReader extends BasicFileReader
{
    double boresightX;
    double boresightY;
    double boresightZ;
    double et;
    String etString;
//    double[] calibratedRadiance;
//    double[] calibratedRadianceUncertainty;
    double[] yValues;
    double[] xValues;
    int numberEntries = 1393;
    int interceptFlag = 0;
    int fovFlag = 0;

    public OVIRSL3SpectrumReader(String filename)
    {
        super(filename);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void read()
    {
        try
        {
            DataInputStream stream=new DataInputStream(new FileInputStream(new File(filename)));
            byte[] sclkStr=new byte[20];
            stream.readFully(sclkStr);
            //et=Double.valueOf(new String(sclkStr));
            fovFlag = stream.readInt();
            interceptFlag = stream.readInt();
                boresightX = stream.readDouble();
                boresightY = stream.readDouble();
                boresightZ = stream.readDouble();

            numberEntries = stream.readInt();
            xValues=new double[numberEntries];
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

    public double[] getXAxis()
    {
        return xValues;
    }

    public String getSclk()
    {
        return etString;
    }

    public Vector3D getBoresightIntercept()
    {
        return new Vector3D(boresightX, boresightY, boresightZ);
    }

    public boolean boresightIsOnSurface()
    {
        return interceptFlag == 0 ? false : true;
    }

}
