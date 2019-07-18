package edu.jhuapl.sbmt.model.eros;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.client.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.spectrum.model.core.AbstractSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.ISpectralInstrument;

import crucible.crust.metadata.api.Metadata;

public class NISSearchModel extends AbstractSpectrumSearchModel
{
    String fileExtension = "";
    Map<String,String> nisFileToObservationTimeMap=Maps.newHashMap();
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

    public NISSearchModel(SmallBodyViewConfig smallBodyConfig,
            ModelManager modelManager, SbmtInfoWindowManager infoPanelManager,
            PickManager pickManager, Renderer renderer,
            ISpectralInstrument instrument)
    {
    	 super(smallBodyConfig.hasHierarchicalSpectraSearch(), smallBodyConfig.hasHypertreeBasedSpectraSearch(),
         		smallBodyConfig.getHierarchicalSpectraSearchSpecification(), modelManager, pickManager,
                 renderer, instrument);

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

        setRedMaxVal(0.05);
        setGreenMaxVal(0.05);
        setBlueMaxVal(0.05);

        setRedIndex(1);
        setGreenIndex(25);
        setBlueIndex(50);
    }

    @Override
    public void setSpectrumRawResults(List<List<String>> spectrumRawResults)
    {
        List<String> matchedImages=Lists.newArrayList();
        if (matchedImages.size() > 0)
            fileExtension = FilenameUtils.getExtension(matchedImages.get(0));
        for (List<String> res : spectrumRawResults)
        {
            String path = NisQuery.getNisPath(res);
            matchedImages.add(path);
        }


//        String[] formattedResults = new String[spectrumRawResults.size()];
        for (String str : matchedImages)
        {
            String[] tokens = str.split("/");
            String filename = tokens[4];
            String strippedFileName=str.replace("/NIS/2000/", "");
            String detailedTime=nisFileToObservationTimeMap.get(strippedFileName);
            List<String> result = new ArrayList<String>();
            result.add(str);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            try
            {
                result.add(""+sdf.parse(detailedTime).getTime());
            }
            catch (ParseException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            String fileNum=str.substring(16,25);
            result.add(fileNum);
            result.add(str.substring(5,9));
            result.add(str.substring(10, 13));

            this.results.add(result);


        }

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
        super.setSpectrumRawResults(results);
        fireResultsChanged();
        fireResultsCountChanged(this.results.size());
    }

//    @Override
//    public SpectrumKey createSpectrumKey(String imagePathName, SpectralInstrument instrument)
//    {
//        SpectrumKey key = new SpectrumKey(imagePathName + ".NIS", null, null, instrument);
//        return key;
//    }

    @Override
    public String createSpectrumName(int index)
    {
        List<String> result = getSpectrumRawResults().get(index);
        if (result.size() == 2) return result.get(0);
        int name = Integer.parseInt(result.get(2));
        int year = Integer.parseInt(result.get(3));
        int doy = Integer.parseInt(result.get(4));
        String path = NisQuery.getNisPath(name, year, doy);

//        String path = NisQuery.getNisPath(getSpectrumRawResults().get(index));
        return path;
    }

    @Override
    public void populateSpectrumMetadata(String line)
    {
//      SpectraCollection collection = (SpectraCollection)getModelManager().getModel(ModelNames.SPECTRA);
//      for (int i=0; i<lines.size(); ++i)
//      {
//          OREXSearchSpec spectrumSpec = new OREXSearchSpec();
//          spectrumSpec.fromFile(lines.get(0));
//          collection.tagSpectraWithMetadata(createSpectrumName(i), spectrumSpec);
//      }
    }

	@Override
	public Metadata store()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void retrieve(Metadata source)
	{
		// TODO Auto-generated method stub

	}

}
