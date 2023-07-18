package edu.jhuapl.sbmt.model.phobos;

import edu.jhuapl.sbmt.core.pointing.PointingSource;
import edu.jhuapl.sbmt.query.v2.DataQuerySourcesMetadata;
import edu.jhuapl.sbmt.query.v2.FetchedResults;
import edu.jhuapl.sbmt.query.v2.ISearchMetadata;
import edu.jhuapl.sbmt.query.v2.QueryException;
import edu.jhuapl.sbmt.spectrum.query.SpectrumDataQuery;

// This must be final because it is a singleton with a clone() method.
public final class MEGANEQuery extends SpectrumDataQuery
{
    private static MEGANEQuery instance=new MEGANEQuery();


    public static MEGANEQuery getInstance()
    {
        return instance;
    }

    private MEGANEQuery()
    {
        super(DataQuerySourcesMetadata.of("/phobos/shared/megane", "/phobos/shared/megane/spectra",
        		"", "", "", PointingSource.SPICE, "spectrumlist.txt"));
    }

    @Override
    public MEGANEQuery clone() {
        return getInstance();
    }

//    @Override
//    public String getDataPath()
//    {
//        return rootPath + "/spectra";   //see constructor above for rootPath
//    }

    @Override
    public FetchedResults runQuery(ISearchMetadata queryMetadata) throws QueryException
    {
    	return null;
//        // TODO Auto-generated method stub
//        return super.runQuery(queryMetadata);
    }

	@Override
	public String getRootPath()
	{
		// TODO Auto-generated method stub
		return null;
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
