package edu.jhuapl.sbmt.model.eros.nis;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.Maps;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

/**
 * NIS Search Model.  Small child class to give a concrete type to the BaseSpectrumSearchModel type.
 * @author steelrj1
 *
 */
public class NISSearchModel extends BaseSpectrumSearchModel<NISSpectrum>
{
    public static Map<String,String> nisFileToObservationTimeMap=Maps.newHashMap();
    static Map<String,Vector3D> nisFileToSunPositionMap=Maps.newHashMap();

    //TODO do we still need this?
    static
    {
        try
        {
            File nisSunFile=FileCache.getFileFromServer("/NIS/nisSunVectors.txt");

            Scanner scanner=new Scanner(nisSunFile);
            boolean found=false;
            while (scanner.hasNextLine() && !found)
            {
                String line=scanner.nextLine();
                String[] tokens=line.replaceAll(",", "").trim().split("\\s+");
                String file=tokens[0];
                String x=tokens[1];
                String y=tokens[2];
                String z=tokens[3];
                nisFileToSunPositionMap.put(file,new Vector3D(Double.valueOf(x),Double.valueOf(y),Double.valueOf(z)).normalize());
            }
            scanner.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public NISSearchModel(ModelManager modelManager, BasicSpectrumInstrument instrument)
    {
    	 super(modelManager, instrument);

        try
        {
            File nisTimesFile=FileCache.getFileFromServer("/NIS/2000/nisTimes.txt");

            Scanner scanner=new Scanner(nisTimesFile);
            boolean found=false;
            while (scanner.hasNextLine() && !found)
            {
                String line=scanner.nextLine();
                String[] tokens=line.replaceAll(",", "").trim().split("\\s+");
                String file=tokens[0];
                String time=tokens[1];
                nisFileToObservationTimeMap.put(file,time);
            }
            scanner.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
