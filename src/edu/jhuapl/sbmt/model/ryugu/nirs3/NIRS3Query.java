package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;

import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.query.FixedListQuery;

public class NIRS3Query extends FixedListQuery
{
    private static NIRS3Query instance=new NIRS3Query();


    public static NIRS3Query getInstance()
    {
        return instance;
    }

    private NIRS3Query()
    {
        super("/earth/hayabusa2/nirs3");
    }

    @Override
    public String getDataPath()
    {
        return rootPath + "/spectra";
    }

    @Override
    public List<List<String>> runQuery(
            String type,
            DateTime startDate,
            DateTime stopDate,
            boolean sumOfProductsSearch,
            List<Integer> camerasSelected,
            List<Integer> filtersSelected,
            double startDistance,
            double stopDistance,
            double startResolution,
            double stopResolution,
            String searchString,
            List<Integer> polygonTypes,
            double fromIncidence,
            double toIncidence,
            double fromEmission,
            double toEmission,
            double fromPhase,
            double toPhase,
            TreeSet<Integer> cubeList,
            ImageSource imageSource,
            int limbType)
    {
        spectrumListPrefix = "";

//        if (multiSource)
//        {
//            if (imageSource == ImageSource.GASKELL)
//                imageListPrefix = "sumfiles";
//            if (imageSource == ImageSource.CORRECTED)
//                imageListPrefix = "sumfiles-corrected";
//            else if (imageSource == ImageSource.CORRECTED_SPICE)
                //spectrumListPrefix = "infofiles-corrected";
//        }

        List<List<String>> result = getResultsFromFileListOnServer(rootPath + "/" + spectrumListPrefix + "/spectrumlist.txt", rootPath + "/spectra/", galleryPath);

        return result;
    }

}
