package edu.jhuapl.sbmt.model.phobos;

import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;

// This must be final because it is a singleton with a clone() method.
public final class MEGANEQuery extends FixedListQuery
{
    private static MEGANEQuery instance=new MEGANEQuery();


    public static MEGANEQuery getInstance()
    {
        return instance;
    }

    private MEGANEQuery()
    {
        super(/*"/earth/osirisrex/otes"*/);
    }

    @Override
    public MEGANEQuery clone() {
        return getInstance();
    }

    @Override
    public String getDataPath()
    {
        return rootPath + "/spectra";   //see constructor above for rootPath
    }

    @Override
    public ISearchResultsMetadata runQuery(SearchMetadata queryMetadata)
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
