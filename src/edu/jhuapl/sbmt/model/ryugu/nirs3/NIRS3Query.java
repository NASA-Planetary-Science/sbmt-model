package edu.jhuapl.sbmt.model.ryugu.nirs3;

import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;

// This must be final because it is a singleton with a clone method.
public final class NIRS3Query extends FixedListQuery
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
    public NIRS3Query clone()
    {
        return getInstance();
    }

    @Override
    public String getDataPath()
    {
        return rootPath + "/spectra";
    }

    @Override
    public ISearchResultsMetadata runQuery(SearchMetadata queryMetadata)
    {
        // TODO Auto-generated method stub
        return super.runQuery(queryMetadata);
    }

//    @Override
//    public List<List<String>> runFixedListQuery(ImageSource imageSource,
//            String dataFolderOffRoot, String dataListFileName)
//    {
//        return super.runFixedListQuery(imageSource, dataFolderOffRoot,
//                dataListFileName);
//
//        String spectrumListPrefix = "";
//
////        if (multiSource)
////        {
////            if (imageSource == ImageSource.GASKELL)
////                imageListPrefix = "sumfiles";
////            if (imageSource == ImageSource.CORRECTED)
////                imageListPrefix = "sumfiles-corrected";
////            else if (imageSource == ImageSource.CORRECTED_SPICE)
//                //spectrumListPrefix = "infofiles-corrected";
////        }
//
//        List<List<String>> result = getResultsFromFileListOnServer(rootPath + "/" + spectrumListPrefix + "/spectrumlist.txt", rootPath + "/spectra/", getGalleryPath(), searchString);
//
//        return result;
//    }

}
