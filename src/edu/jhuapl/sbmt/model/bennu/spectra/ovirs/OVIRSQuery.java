package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhuapl.sbmt.client.SbmtSpectrumModelFactory;
import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.spectrum.model.rendering.IBasicSpectrumRenderer;

import crucible.crust.metadata.impl.FixedMetadata;

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
        super();
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
    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
    {
        String spectrumListPrefix = "";

        List<List<String>> serverResult = getResultsFromFileListOnServer(rootPath + "/" + spectrumListPrefix + "/spectrumlist.txt", rootPath + "/spectra/", getGalleryPath());

        OVIRSSearchResult searchResult = new OVIRSSearchResult();

        for (List<String> res : serverResult)
        {
        	IBasicSpectrumRenderer renderer = null;
			try
			{
				renderer = SbmtSpectrumModelFactory.createSpectrumRenderer(res.get(0), SpectrumInstrumentFactory.getInstrumentForName("OVIRS"));
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	searchResult.addResult(renderer.getSpectrum());
        }

        return searchResult;
    }

    class OVIRSSearchResult implements ISearchResultsMetadata<BasicSpectrum>
    {
    	List<BasicSpectrum> results = new ArrayList<BasicSpectrum>();

    	public void addResult(BasicSpectrum spectrum)
    	{
    		results.add(spectrum);
    	}

		@Override
		public FixedMetadata getMetadata()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<BasicSpectrum> getResultlist()
		{
			return results;
		}
    }

}
