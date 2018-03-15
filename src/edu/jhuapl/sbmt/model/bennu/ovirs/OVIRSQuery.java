package edu.jhuapl.sbmt.model.bennu.ovirs;

import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;

import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.query.FixedListQuery;

//This must be final because it is a singleton with a clone method.
public final class OVIRSQuery extends FixedListQuery
{
    private static OVIRSQuery instance=new OVIRSQuery();


    public static OVIRSQuery getInstance()
    {
        return instance;
    }

    private OVIRSQuery()
    {
        super("/earth/osirisrex/ovirs");
    }

    @Override
    public OVIRSQuery clone()
    {
        return getInstance();
    }

    @Override
    public String getDataPath()
    {
        return rootPath + "/spectra";
    }

    @Override
    public List<List<String>> runQuery(
            @SuppressWarnings("unused") String type,
            @SuppressWarnings("unused") DateTime startDate,
            @SuppressWarnings("unused") DateTime stopDate,
            @SuppressWarnings("unused") boolean sumOfProductsSearch,
            @SuppressWarnings("unused") List<Integer> camerasSelected,
            @SuppressWarnings("unused") List<Integer> filtersSelected,
            @SuppressWarnings("unused") double startDistance,
            @SuppressWarnings("unused") double stopDistance,
            @SuppressWarnings("unused") double startResolution,
            @SuppressWarnings("unused") double stopResolution,
            @SuppressWarnings("unused") String searchString,
            @SuppressWarnings("unused") List<Integer> polygonTypes,
            @SuppressWarnings("unused") double fromIncidence,
            @SuppressWarnings("unused") double toIncidence,
            @SuppressWarnings("unused") double fromEmission,
            @SuppressWarnings("unused") double toEmission,
            @SuppressWarnings("unused") double fromPhase,
            @SuppressWarnings("unused") double toPhase,
            @SuppressWarnings("unused") TreeSet<Integer> cubeList,
            @SuppressWarnings("unused") ImageSource imageSource,
            @SuppressWarnings("unused") int limbType)
    {
        String spectrumListPrefix = "";

//        if (multiSource)
//        {
//            if (imageSource == ImageSource.GASKELL)
//                imageListPrefix = "sumfiles";
//            if (imageSource == ImageSource.CORRECTED)
//                imageListPrefix = "sumfiles-corrected";
//            else if (imageSource == ImageSource.CORRECTED_SPICE)
                //spectrumListPrefix = "infofiles-corrected";
//        }

        List<List<String>> result = getResultsFromFileListOnServer(rootPath + "/" + spectrumListPrefix + "/spectrumlist.txt", rootPath + "/spectra/", getGalleryPath());

        return result;
    }

}
