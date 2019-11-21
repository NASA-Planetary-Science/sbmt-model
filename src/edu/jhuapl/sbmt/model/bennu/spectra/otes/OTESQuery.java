package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;


/**
 * Query definition class for OTES
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class OTESQuery extends FixedListQuery<BasicSpectrum>
{
    private static OTESQuery instance=new OTESQuery();


    public static OTESQuery getInstance()
    {
        return instance;
    }

    private OTESQuery()
    {
        super();
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
    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
    {
        // TODO Auto-generated method stub
        return super.runQuery(queryMetadata);
    }
}
