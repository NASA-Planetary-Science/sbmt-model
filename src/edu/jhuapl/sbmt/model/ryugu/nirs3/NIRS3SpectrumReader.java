package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.io.IOException;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class NIRS3SpectrumReader extends BasicFileReader
{
    String sourceFileName;
    double et;
    double[] calibratedRadiance;

    public NIRS3SpectrumReader(String filename)
    {
        super(filename);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void read()
    {
        try
        {
            calibratedRadiance=new double[NIRS3.bandCenters.length];

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
        new NIRS3SpectrumReader("/Users/zimmemi1/sbmt/hayabusa2/nirs3/l2c/earth/hyb2_nirs3_20151207a.fit").read();
    }

}
