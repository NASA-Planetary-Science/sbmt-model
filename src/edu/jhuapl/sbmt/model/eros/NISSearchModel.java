package edu.jhuapl.sbmt.model.eros;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.Maps;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;

public class NISSearchModel extends BaseSpectrumSearchModel
{
    public static Map<String,String> nisFileToObservationTimeMap=Maps.newHashMap();
    static Map<String,Vector3D> nisFileToSunPositionMap=Maps.newHashMap();

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

    public static Vector3D getToSunUnitVector(String fileName)    // file name is taken relative to /project/nearsdc/data/NIS/2000
    {
        return nisFileToSunPositionMap.get(fileName);
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

        getColoringModel().setRedMaxVal(0.05);
        getColoringModel().setGreenMaxVal(0.05);
        getColoringModel().setBlueMaxVal(0.05);

        getColoringModel().setRedIndex(1);
        getColoringModel().setGreenIndex(25);
        getColoringModel().setBlueIndex(50);
    }

    @Override
    public void setSpectrumRawResults(List<BasicSpectrum> spectrumRawResults)
    {
//        List<BasicSpectrum> matchedImages=Lists.newArrayList();
//        if (matchedImages.size() > 0)
//            fileExtension = FilenameUtils.getExtension(matchedImages.get(0).getFullPath());
//        for (BasicSpectrum res : spectrumRawResults)
//        {
//            String path = NisQuery.getNisPath(res);
////            matchedImages.add(path);
////        }
////
////
//////        String[] formattedResults = new String[spectrumRawResults.size()];
////        for (BasicSpectrum str : matchedImages)
////        {
//            String str = path;
//            String[] tokens = str.split("/");
//            String filename = tokens[4];
//            String strippedFileName=str.replace("/NIS/2000/", "");
//            String detailedTime=nisFileToObservationTimeMap.get(strippedFileName);
//            List<String> result = new ArrayList<String>();
//            result.add(str);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//            try
//            {
//                result.add(""+sdf.parse(detailedTime).getTime());
//            }
//            catch (ParseException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//            String fileNum=str.substring(16,25);
//            result.add(fileNum);
//            result.add(str.substring(5,9));
//            result.add(str.substring(10, 13));
//
//            this.results.add(result);
//
//
//        }

//        // add the results to the list
//        int i=0;
//        for (String str : matchedImages)
//        {
//            System.out.println("NISSearchModel: setSpectrumRawResults: str " + str);
//            String fileNum=str.substring(16,25);
//            String strippedFileName=str.replace("/NIS/2000/", "");
//            String detailedTime=nisFileToObservationTimeMap.get(strippedFileName);
//            formattedResults[i] = new String(
//                    fileNum
//                    + ", day: " + str.substring(10, 13) + "/" + str.substring(5, 9)+" ("+detailedTime+")"
//                    );
//
//            ++i;
//        }
//
//        for (String res : formattedResults)
//        {
//            List<String> result = new ArrayList<String>();
//            result.add(res);
//            System.out.println("NISSearchModel: setSpectrumRawResults: adding " + res);
//            this.results.add(result);
//        }
    	this.results = spectrumRawResults;
        super.setSpectrumRawResults(results);
//        fireResultsChanged();
//        fireResultsCountChanged(this.results.size());
    }
}
