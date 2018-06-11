package edu.jhuapl.sbmt.model.bennu.otes;

import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.SearchResultsMetadata;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;

// This must be final because it is a singleton with a clone() method.
public final class OTESQuery extends FixedListQuery
{
    private static OTESQuery instance=new OTESQuery();


    public static OTESQuery getInstance()
    {
        return instance;
    }

    private OTESQuery()
    {
        super("/earth/osirisrex/otes");
    }

    @Override
    public OTESQuery clone() {
        return getInstance();
    }

    @Override
    public String getDataPath()
    {
        return rootPath + "/spectra";   //see constructor above for rootPath
    }

    @Override
    public SearchResultsMetadata runQuery(SearchMetadata queryMetadata)
    {
        // TODO Auto-generated method stub
        return super.runQuery(queryMetadata);
    }


//    public List<List<String>> runQuery(
//            String... locations)
//    {
//        String spectrumListPrefix = "";
//
//        List<List<String>> result = getResultsFromFileListOnServer(rootPath + "/" + spectrumListPrefix + "/spectrumlist.txt", rootPath + "/spectra/", getGalleryPath());
//
//        return result;
//    }

}
