package edu.jhuapl.sbmt.model.ryugu.nirs3;

import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;

/**
 * Query definition class for NIRS3
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class NIRS3Query extends FixedListQuery
{
    private static NIRS3Query instance=new NIRS3Query();


    public static NIRS3Query getInstance()
    {
        return instance;
    }

    private NIRS3Query()
    {
        super();
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
    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
    {
        // TODO Auto-generated method stub
        return super.runQuery(queryMetadata);
    }
}
