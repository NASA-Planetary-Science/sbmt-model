package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.jhuapl.sbmt.core.io.BasicFileReader;

/**
 * Reads in NIRS3 files from Hayabusa2, so those values can be stored in a NIRS3Spectrum object
 * @author steelrj1
 *
 */
public class NIRS3SpectrumReader extends BasicFileReader
{
	List<NIRS3SpectrumData> spectra;

    public NIRS3SpectrumReader(String filename)
    {
        super(filename);
        spectra = new ArrayList<NIRS3SpectrumData>();
    }

    @Override
    public void read()
    {
        try
        {
            Scanner scanner=new Scanner(new File(filename));
            NIRS3SpectrumData data = new NIRS3SpectrumData();
            while (scanner.hasNextLine())
            {
            	data.readLine(filename, scanner.nextLine());
            	spectra.add(data);
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
