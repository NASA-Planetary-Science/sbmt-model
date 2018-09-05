package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class NIRS3SpectrumReader extends BasicFileReader
{
    String sourceFileName;
    double et;
    double[] spectrum;

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
            spectrum=new double[NIRS3.nirs3BandCenters.length];
            Scanner scanner=new Scanner(new File(filename));    // cf. NIRS3Preprocessor class to see how these files were written out in the first place
            et=scanner.nextDouble();
            for (int i=0; i<NIRS3.nirs3BandCenters.length; i++)
                spectrum[i]=scanner.nextDouble();
            scanner.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public double[] getCalibratedRadiance()
    {
        return spectrum;
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
