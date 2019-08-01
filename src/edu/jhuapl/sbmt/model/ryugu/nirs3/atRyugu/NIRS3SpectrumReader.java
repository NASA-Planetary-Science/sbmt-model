package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.jhuapl.sbmt.model.image.BasicFileReader;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.util.TimeUtil;

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

    class NIRS3SpectrumData
    {
    	double et;
    	double[] spectrum;

    	public NIRS3SpectrumData(String filename, String line)
    	{
    		spectrum = new double[NIRS3.bandCentersLength];
    		Scanner scanner = new Scanner(line).useDelimiter(",");
    		scanner.next(); // skip the UTC start
    		String midTime = scanner.next(); // get the UTC mid time
    		//craft the time from the filename + the midTime
    		String midTimeComplete = filename.substring(0, 4) + "-" + filename.substring(4,6) + "-" + filename.substring(6,8) + "T" + midTime;
    		et = TimeUtil.str2et(midTimeComplete);
    		scanner.next(); // skip the UTC end
    		scanner.next();  //skip the other geo fields
    		scanner.next();
    		scanner.next();
    		scanner.next();
    		scanner.next();
    		scanner.next();
    		for (int i = 9; i < NIRS3.bandCentersLength+9; i++)
    			spectrum[i-9] = Double.parseDouble(scanner.next());
    		scanner.close();
    	}

    	public double[] getSpectrum()
    	{
    		return spectrum;
    	}

    	public double getEt()
    	{
    		return et;
    	}

    }
}
