package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;

import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.QueryBase;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.database.DatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.database.SpectraDatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.database.SpectrumPhpQuery;
import edu.jhuapl.sbmt.query.fixedlist.FixedListQuery;
import edu.jhuapl.sbmt.spectrum.SbmtSpectrumModelFactory;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;

import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.SettableMetadata;

/**
 * Query definition class for OVIRS
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */public final class OVIRSQuery extends SpectrumPhpQuery
{
    private static OVIRSQuery instance=new OVIRSQuery();


    public static OVIRSQuery getInstance()
    {
        return instance;
    }

    private OVIRSQuery()
    {
        super("", "", "");
    }

    @Override
    public OVIRSQuery copy()
    {
        return getInstance();
    }

    @Override
    public String getDataPath()
    {
    	return "/bennu/shared/ovirs/";
//        return rootPath + "/spectra";
    }

    @Override
    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
    {
    	List<List<String>> results = null;
    	int pathIndex = 3;
    	try
        {
	        FixedMetadata metadata = queryMetadata.getMetadata();
	        double fromIncidence = metadata.get(DatabaseSearchMetadata.FROM_INCIDENCE);
	        double toIncidence = metadata.get(DatabaseSearchMetadata.TO_INCIDENCE);
	        double fromEmission = metadata.get(DatabaseSearchMetadata.FROM_EMISSION);
	        double toEmission = metadata.get(DatabaseSearchMetadata.TO_EMISSION);
	        String searchString = metadata.get(DatabaseSearchMetadata.SEARCH_STRING);
	        double fromPhase = metadata.get(DatabaseSearchMetadata.FROM_PHASE);
	        double toPhase = metadata.get(DatabaseSearchMetadata.TO_PHASE);
	        double startDistance = metadata.get(DatabaseSearchMetadata.FROM_DISTANCE);
	        double stopDistance = metadata.get(DatabaseSearchMetadata.TO_DISTANCE);
	        DateTime startDate = new DateTime(metadata.get(DatabaseSearchMetadata.START_DATE));
	        DateTime stopDate = new DateTime(metadata.get(DatabaseSearchMetadata.STOP_DATE));
	//        List<Integer> polygonTypes = metadata.get(DatabaseSearchMetadata.POLYGON_TYPES);
	        TreeSet<Integer> cubeList = metadata.get(SpectraDatabaseSearchMetadata.CUBE_LIST);
	        String modelName = metadata.get(SpectraDatabaseSearchMetadata.MODEL_NAME);
	        String dataType = metadata.get(SpectraDatabaseSearchMetadata.DATA_TYPE);
	        spectraTableName = "bennu_" + modelName + "_ovirspectra_" + dataType;
	        cubeTableName = "bennu_" + modelName + "_ovirscubes_" + dataType;


	        double minIncidence = Math.min(fromIncidence, toIncidence);
	        double maxIncidence = Math.max(fromIncidence, toIncidence);
	        double minEmission = Math.min(fromEmission, toEmission);
	        double maxEmission = Math.max(fromEmission, toEmission);
	        double minPhase = Math.min(fromPhase, toPhase);
	        double maxPhase = Math.max(fromPhase, toPhase);


        	boolean tableExists = QueryBase.checkForDatabaseTable(spectraTableName);
            if (!tableExists) throw new RuntimeException("Database table " + spectraTableName + " is not available now;\n Please contact SBMT Support for assistance");

        	if (searchString != null)
            {
                HashMap<String, String> args = new HashMap<>();
                args.put("searchString", searchString);
                results = doQuery("searchovirs.php", constructUrlArguments(args));
            }
        	else
        	{

	            double minScDistance = Math.min(startDistance, stopDistance);
	            double maxScDistance = Math.max(startDistance, stopDistance);

	            HashMap<String, String> args = new HashMap<>();
	            args.put("startDate", String.valueOf(startDate.getMillis()));
	            args.put("stopDate", String.valueOf(stopDate.getMillis()));
	            args.put("minScDistance", String.valueOf(minScDistance));
	            args.put("maxScDistance", String.valueOf(maxScDistance));
	            args.put("minIncidence", String.valueOf(minIncidence));
	            args.put("maxIncidence", String.valueOf(maxIncidence));
	            args.put("minEmission", String.valueOf(minEmission));
	            args.put("maxEmission", String.valueOf(maxEmission));
	            args.put("minPhase", String.valueOf(minPhase));
	            args.put("maxPhase", String.valueOf(maxPhase));
	            args.put("spectraTableName", spectraTableName);
	            args.put("cubeTableName", cubeTableName);

	            if (cubeList != null && cubeList.size() > 0)
	            {
	                String cubesStr = "";
	                int size = cubeList.size();
	                int count = 0;
	                for (Integer i : cubeList)
	                {
	                    cubesStr += "" + i;
	                    if (count < size-1)
	                        cubesStr += ",";
	                    ++count;
	                }
	                args.put("cubes", cubesStr);
	            }



	            results = doQuery("searchovirs.php", constructUrlArguments(args));
	            System.out.println("OVIRSQuery: runQuery: number of results " + results.size());
        	}

        }
        catch (RuntimeException re)
        {
        	try {
        		pathIndex = 0;
	            FixedListQuery query = getFixedListQuery();
	            results = query.runQuery(queryMetadata).getResultlist();
        	}
        	catch (Exception e)
        	{
        		throw re;
        	}
        }
        catch (Exception e)
        {
            e.printStackTrace();

        }

        OVIRSSearchResult tempResults = new OVIRSSearchResult();
        try
        {
        	for (List<String> res : results)
        	{
            	BasicSpectrum spectrum = SbmtSpectrumModelFactory.createSpectrum(res.get(pathIndex),
            								SpectrumInstrumentFactory.getInstrumentForName("OVIRS"));
            	tempResults.addResult(spectrum);
        	}
        }
        catch (Exception e)
        {
       	 	System.out.println("SpectrumStandardSearch: search: " + e.getLocalizedMessage());
       	 	e.printStackTrace();
        }
        return tempResults;
    }

    // Convert the 0th element of the result (the path to the data)
    // with the full path, but only if the result does not already have
    // a full path.
    protected void changeDataPathToFullPath(List<String> result)
    {
        String fullPath = result.get(3);
//        if (!fullPath.startsWif("/"))
//        {
            result.set(3, getDataPath() + "" + fullPath);
//        }
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

    @Override
    public Metadata store()
    {
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
        return configMetadata;
    }

    @Override
    public void retrieve(Metadata source)
    {

    }

//    @Override
//    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
//    {
//        // TODO Auto-generated method stub
//        return super.runQuery(queryMetadata);
//    }

//    @Override
//    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
//    {
//        String spectrumListPrefix = "";
//
//        List<List<String>> serverResult = getResultsFromFileListOnServer(rootPath + "/" + spectrumListPrefix + "/spectrumlist.txt", rootPath + "/spectra/", getGalleryPath());
//
//        OVIRSSearchResult searchResult = new OVIRSSearchResult();
//
//        for (List<String> res : serverResult)
//        {
//        	IBasicSpectrumRenderer renderer = null;
//			try
//			{
//				renderer = SbmtSpectrumModelFactory.createSpectrumRenderer(res.get(0), SpectrumInstrumentFactory.getInstrumentForName("OVIRS"));
//			}
//			catch (IOException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        	searchResult.addResult(renderer.getSpectrum());
//        }
//
//        return searchResult;
//    }
//
//    class OVIRSSearchResult implements ISearchResultsMetadata<BasicSpectrum>
//    {
//    	List<BasicSpectrum> results = new ArrayList<BasicSpectrum>();
//
//    	public void addResult(BasicSpectrum spectrum)
//    	{
//    		results.add(spectrum);
//    	}
//
//		@Override
//		public FixedMetadata getMetadata()
//		{
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public List<BasicSpectrum> getResultlist()
//		{
//			return results;
//		}
//    }

}
