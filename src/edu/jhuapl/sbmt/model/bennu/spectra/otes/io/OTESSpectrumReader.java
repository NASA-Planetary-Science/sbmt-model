package edu.jhuapl.sbmt.model.bennu.spectra.otes.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

/**
 * Reads in OTES files from O-REx, so those values can be stored in a OTESSpectrum object
 * @author steelrj1
 *
 */
public class OTESSpectrumReader extends BasicFileReader
{
    double sclk;
    double[] yValues;
    Double[] xValues;
    int numberEntries = 0;

    public OTESSpectrumReader(String filename, int numberEntries)
    {
        super(filename);
        this.numberEntries = numberEntries;
    }

    @Override
    public void read()
    {
        try
        {
            xValues=new Double[numberEntries];
            yValues=new double[numberEntries];
            DataInputStream stream=new DataInputStream(new FileInputStream(new File(filename)));
            sclk=stream.readDouble();
            for (int i=0; i<numberEntries; i++)
            {
                yValues[i]=stream.readDouble();
            }
            for (int i=0; i<numberEntries; i++)
            {
                xValues[i]=stream.readDouble();
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

    public static void main(String[] args)
    {
//        new OTESSpectrumReader("/Users/steelrj1/.sbmt1orex/cache/earth/osirisrex/otes/spectra/otel220170922t224426.067z_6002.1339.spect").read();
    }

}
