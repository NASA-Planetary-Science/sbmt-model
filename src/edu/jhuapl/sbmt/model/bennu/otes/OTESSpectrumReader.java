package edu.jhuapl.sbmt.model.bennu.otes;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class OTESSpectrumReader extends BasicFileReader
{
//    String sourceFileName;
    double sclk;
    double[] yValues;
    double[] xValues;
    int numberEntries = 0;

    public OTESSpectrumReader(String filename, int numberEntries)
    {
        super(filename);
        this.numberEntries = numberEntries;
        // TODO Auto-generated constructor stub
    }

    @Override
    public void read()
    {
        try
        {
            xValues=new double[numberEntries];
            yValues=new double[numberEntries];
//            System.out.println("OTESSpectrumReader: read: reading " + filename);
//            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
//            String line = reader.readLine();
//            line = reader.readLine();
//            System.out.println("OTESSpectrumReader: read: sclk line is " + line);
//            sclk = Double.parseDouble(line);
//            line = reader.readLine();
//            for (int i=0; i<numberEntries; i++)
//            {
//                line = reader.readLine();
//                String[] parts = line.split(",");
//                xValues[i] = Double.parseDouble(parts[0]);
//                yValues[i] = Double.parseDouble(parts[1]);
//            }
//            reader.close();

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

    public double[] getXAxis()
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
