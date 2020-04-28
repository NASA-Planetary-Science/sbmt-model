package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;

import edu.jhuapl.sbmt.client.SbmtSpectrumModelFactory;
import edu.jhuapl.sbmt.query.ISearchResultsMetadata;
import edu.jhuapl.sbmt.query.QueryBase;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.database.DatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.database.SpectraDatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.database.SpectrumPhpQuery;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;

import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.SettableMetadata;


/**
 * Query definition class for OTES
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class OTESQuery extends SpectrumPhpQuery //DatabaseQueryBase //FixedListQuery<BasicSpectrum>
{
    private static OTESQuery instance=new OTESQuery();


    public static OTESQuery getInstance()
    {
        return instance;
    }

    private OTESQuery()
    {
        super("", "", "");
    }

    @Override
    public OTESQuery clone() {
        return getInstance();
    }

    @Override
    public String getDataPath()
    {
        return "/bennu/shared/otes/";   //see constructor above for rootPath
    }

//    @Override
//    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
//    {
//        // TODO Auto-generated method stub
//        return super.runQuery(queryMetadata);
//    }

    @Override
    public ISearchResultsMetadata<BasicSpectrum> runQuery(SearchMetadata queryMetadata)
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
        spectraTableName = "bennu_" + modelName + "_otesspectra_" + dataType;
        cubeTableName = "bennu_" + modelName + "_otescubes_" + dataType;
        List<List<String>> results = null;

        double minIncidence = Math.min(fromIncidence, toIncidence);
        double maxIncidence = Math.max(fromIncidence, toIncidence);
        double minEmission = Math.min(fromEmission, toEmission);
        double maxEmission = Math.max(fromEmission, toEmission);
        double minPhase = Math.min(fromPhase, toPhase);
        double maxPhase = Math.max(fromPhase, toPhase);

        try
        {
        	boolean tableExists = QueryBase.checkForDatabaseTable(spectraTableName);
            if (!tableExists) throw new RuntimeException("Database table " + spectraTableName + " is not available now;\n Please contact SBMT Support for assistance");

        	if (searchString != null)
            {
                HashMap<String, String> args = new HashMap<>();
                args.put("searchString", searchString);
                results = doQuery("searchotes.php", constructUrlArguments(args));
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
//	            for (int i=0; i<4; ++i)
//	            {
//	                if (polygonTypes.contains(i))
//	                    args.put("polygonType"+i, "1");
//	                else
//	                    args.put("polygonType"+i, "0");
//	            }
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
	            results = doQuery("searchotes.php", constructUrlArguments(args));
	            System.out.println("OTESQuery: runQuery: number of results " + results.size());
        	}

        }
        catch (RuntimeException re)
        {
        	throw re;
        }
        catch (Exception e)
        {
            e.printStackTrace();
//            results = getResultsFromFileListOnServer(getDataPath() + "/nisTimes.txt", getDataPath(), getGalleryPath(), "");

        }

//        List<BasicSpectrum> tempResults = new ArrayList<BasicSpectrum>();
        OTESSearchResult tempResults = new OTESSearchResult();
        try
        {
        	for (List<String> res : results)
        	{
//        		System.out.println("OTESQuery: runQuery: res is " + res);
//        		String path = NisQuery.getNisPath(res);
            	BasicSpectrum spectrum = SbmtSpectrumModelFactory.createSpectrum(res.get(3), SpectrumInstrumentFactory.getInstrumentForName("OTES"));
//            	String str = path;
//                String[] tokens = str.split("/");
//                String filename = tokens[4];
//                String strippedFileName=str.replace("/NIS/2000/", "");
//                String detailedTime = NISSearchModel.nisFileToObservationTimeMap.get(strippedFileName);
//                List<String> result = new ArrayList<String>();
//                result.add(str);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//                spectrum.setDateTime(new DateTime(sdf.parse(detailedTime).getTime()));
////            	spectrum.setMetadata(spec);	//TODO is this needed for NIS?
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

    class OTESSearchResult implements ISearchResultsMetadata<BasicSpectrum>
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
}
