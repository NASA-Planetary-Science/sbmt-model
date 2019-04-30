package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.jhuapl.sbmt.model.image.BasicFileReader;

public class NIRS3SpectrumReader extends BasicFileReader
{

	List<NIRS3SpectrumData> spectra;

    public NIRS3SpectrumReader(String filename)
    {
        super(filename);
        spectra = new ArrayList<NIRS3SpectrumData>();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void read()
    {
        try
        {
            Scanner scanner=new Scanner(new File(filename));
//            String nextLine = scanner.nextLine();
//            System.out.println("NIRS3SpectrumReader: read: next line " + line);
            while (scanner.hasNextLine())
            {
            	spectra.add(new NIRS3SpectrumData(new File(filename).getName(), scanner.nextLine()));
            }
            scanner.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void main(String[] args)
    {
        new NIRS3SpectrumReader("/Users/steelrj1/Desktop/sample_data/nirs3/20180907_nirs_l2c.csv").read();
    }

}
