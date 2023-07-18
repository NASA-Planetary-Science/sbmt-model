package edu.jhuapl.sbmt.model.eros.nis;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import edu.jhuapl.sbmt.core.pointing.PointingSource;
import edu.jhuapl.sbmt.query.database.DatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.database.SpectraDatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.v2.DataQuerySourcesMetadata;
import edu.jhuapl.sbmt.query.v2.DatabaseDataQuery;
import edu.jhuapl.sbmt.query.v2.FetchedResults;
import edu.jhuapl.sbmt.query.v2.ISearchMetadata;
import edu.jhuapl.sbmt.query.v2.QueryException;
import edu.jhuapl.sbmt.query.v2.QueryException.QueryExceptionReason;
import edu.jhuapl.sbmt.query.v2.QueryException.Severity;
import edu.jhuapl.sbmt.spectrum.query.SpectrumDataQuery;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;


/**
 * This class provides functions for querying the database for NIS
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class NisQuery extends SpectrumDataQuery
{
	private static NisQuery instance = new NisQuery();
	private String searchString;
	private static final Key<NisQuery> NISQUERY_KEY = Key.of("nisQuery");
	private static String SEARCHSCRIPT = "searchnis.php";

    public static String getNisPath(List<String> result)
    {
        if (result.size() > 1)
        {
            int id = Integer.parseInt(result.get(0).split("/")[3]);
            int year = Integer.parseInt(result.get(1));
            int dayOfYear = Integer.parseInt(result.get(2));
            return getNisPath(id, year, dayOfYear);
        }
        else
        {
            String[] components = result.get(0).split("/");
            int id = Integer.parseInt((components[4].split("N0")[1]).split("\\.")[0]);
            int year = Integer.parseInt(components[2]);
            int dayOfYear = Integer.parseInt(components[3]);
            return getNisPath(id, year, dayOfYear);
        }
    }

    public static String getNisPath(int name, int year, int dayOfYear)
    {
        String str = "/NIS/";
        str += year + "/";

        if (dayOfYear < 10)
            str += "00";
        else if (dayOfYear < 100)
            str += "0";

        str += dayOfYear + "/";

        str += "N0" + name + ".NIS";

        return str;
    }

    public static NisQuery getInstance()
    {
    	return instance;
//        if (ref == null)
//            ref = new NisQuery();
//        return ref;
    }

    @Override
    public NisQuery clone()
    {
        return getInstance();
    }

    private NisQuery()
    {
//        super(null);
        super(DataQuerySourcesMetadata.of("/NIS", "/NIS/2000",
        		"", "", "", PointingSource.SPICE, "nisTimes.txt"));
    }

    @Override
    public String getDataPath()
    {
        return "/NIS/2000";
    }

    @Override
    public FetchedResults runQuery(ISearchMetadata queryMetadata) throws QueryException
    {
        FixedMetadata metadata = queryMetadata.getMetadata();
        searchString = metadata.get(DatabaseSearchMetadata.SEARCH_STRING);
        TreeSet<Integer> cubeList = metadata.get(SpectraDatabaseSearchMetadata.CUBE_LIST);
        FetchedResults results = null;

        String modelName = metadata.get(SpectraDatabaseSearchMetadata.MODEL_NAME);
        String dataType = metadata.get(SpectraDatabaseSearchMetadata.DATA_TYPE);
        spectraTableName = "nisspectra";
        cubeTableName = "niscubes_beta2";

        HashMap<String, String> dbSearchArgsMap = convertSearchParamsToDBArgsMap(metadata);
        boolean tableExists = DatabaseDataQuery.checkForDatabaseTable(spectraTableName);
        if (!tableExists)
        	throw new QueryException("Table Does Not Exist", Severity.ERROR,
        			QueryExceptionReason.DB_TABLE_NOT_FOUND);

//        double fromIncidence = metadata.get(DatabaseSearchMetadata.FROM_INCIDENCE);
//        double toIncidence = metadata.get(DatabaseSearchMetadata.TO_INCIDENCE);
//        double fromEmission = metadata.get(DatabaseSearchMetadata.FROM_EMISSION);
//        double toEmission = metadata.get(DatabaseSearchMetadata.TO_EMISSION);
//        String searchString = metadata.get(DatabaseSearchMetadata.SEARCH_STRING);
//        double fromPhase = metadata.get(DatabaseSearchMetadata.FROM_PHASE);
//        double toPhase = metadata.get(DatabaseSearchMetadata.TO_PHASE);
//        double startDistance = metadata.get(DatabaseSearchMetadata.FROM_DISTANCE);
//        double stopDistance = metadata.get(DatabaseSearchMetadata.TO_DISTANCE);
//        DateTime startDate = new DateTime(metadata.get(DatabaseSearchMetadata.START_DATE));
//        DateTime stopDate = new DateTime(metadata.get(DatabaseSearchMetadata.STOP_DATE));
//
//        double minIncidence = Math.min(fromIncidence, toIncidence);
//        double maxIncidence = Math.max(fromIncidence, toIncidence);
//        double minEmission = Math.min(fromEmission, toEmission);
//        double maxEmission = Math.max(fromEmission, toEmission);
//        double minPhase = Math.min(fromPhase, toPhase);
//        double maxPhase = Math.max(fromPhase, toPhase);
//        double minScDistance = Math.min(startDistance, stopDistance);
//        double maxScDistance = Math.max(startDistance, stopDistance);
//
//        HashMap<String, String> args = new HashMap<>();
//        args.put("startDate", String.valueOf(startDate.getMillis()));
//        args.put("stopDate", String.valueOf(stopDate.getMillis()));
//        args.put("minScDistance", String.valueOf(minScDistance));
//        args.put("maxScDistance", String.valueOf(maxScDistance));
//        args.put("minIncidence", String.valueOf(minIncidence));
//        args.put("maxIncidence", String.valueOf(maxIncidence));
//        args.put("minEmission", String.valueOf(minEmission));
//        args.put("maxEmission", String.valueOf(maxEmission));
//        args.put("minPhase", String.valueOf(minPhase));
//        args.put("maxPhase", String.valueOf(maxPhase));


        List<Integer> polygonTypes = metadata.get(DatabaseSearchMetadata.POLYGON_TYPES);
        if (searchString != null)
        {
            dbSearchArgsMap = new HashMap<>();
            dbSearchArgsMap.put("searchString", searchString);
        }
    	else
    	{
    		for (int i=0; i<4; ++i)
            {
                if (polygonTypes.contains(i))
                	dbSearchArgsMap.put("polygonType" + i, "1");
                else
                	dbSearchArgsMap.put("polygonType" + i, "0");
            }
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
                dbSearchArgsMap.put("cubes", cubesStr);
                dbSearchArgsMap.put(CUBES, cubesStr);
            }
    	}
        return doQuery(SEARCHSCRIPT, constructUrlArguments(dbSearchArgsMap));



//        try
//        {
//        	if (searchString != null)
//            {
//                HashMap<String, String> args = new HashMap<>();
//                args.put("searchString", searchString);
//                results = doQuery("searchnis.php", constructUrlArguments(args));
//            }
//        	else
//        	{
//	            for (int i=0; i<4; ++i)
//	            {
//	                if (polygonTypes.contains(i))
//	                    args.put("polygonType"+i, "1");
//	                else
//	                    args.put("polygonType"+i, "0");
//	            }
//	            if (cubeList != null && cubeList.size() > 0)
//	            {
//	                String cubesStr = "";
//	                int size = cubeList.size();
//	                int count = 0;
//	                for (Integer i : cubeList)
//	                {
//	                    cubesStr += "" + i;
//	                    if (count < size-1)
//	                        cubesStr += ",";
//	                    ++count;
//	                }
//	                args.put("cubes", cubesStr);
//	            }
//	            results = doQuery("searchnis.php", constructUrlArguments(args));
//        	}
//
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            results = getResultsFromFileListOnServer(getDataPath() + "/nisTimes.txt", getDataPath(), getGalleryPath(), "");
//
//        }
//
//        List<BasicSpectrum> tempResults = new ArrayList<BasicSpectrum>();
////        NISSearchResult tempResults = new NISSearchResult();
////        FetchedResults tempResults = new FetchedResults("", FetchedResultsType.DATABASE, results.getFetchedData());
//        try
//        {
//        	for (List<String> res : results.getFetchedData())
//        	{
//        		String path = NisQuery.getNisPath(res);
//            	BasicSpectrum spectrum = SbmtSpectrumModelFactory.createSpectrum(path, SpectrumInstrumentFactory.getInstrumentForName("NIS"));
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
//            	tempResults.addResult(spectrum);
//        	}
//        }
//        catch (Exception e)
//        {
//       	 	System.out.println("SpectrumStandardSearch: search: " + e.getLocalizedMessage());
//       	 	e.printStackTrace();
//        }
//        return tempResults;
    }

    protected void changeDataPathToFullPath(List<String> result, int index)
    {
    	super.changeDataPathToFullPath(result, index);
    }

    @Override
    public FetchedResults fallbackQuery(ISearchMetadata queryMetadata) throws QueryException
    {
    	ISearchMetadata searchMetadata = DataQuerySourcesMetadata.of("/bennu/shared/otes/l2", "/bennu/shared/otes/l2/spectra",
        		"", "", "", PointingSource.SPICE, "spectrumlist.txt");
    	searchMetadata.setSearchString(searchString);
    	return super.fallbackQuery(searchMetadata);
    }

    /**
	 * Registers this class with the metadata system
	 *
	 *
	 */
	public static void initializeSerializationProxy()
	{
		//TODO Finish defining this
    	InstanceGetter.defaultInstanceGetter().register(NISQUERY_KEY, (metadata) -> {

    		NisQuery query = new NisQuery();
    		return query;

    	}, NisQuery.class, query -> {

    		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));

    		return result;
    	});

	}

	@Override
	public String getRootPath()
	{
		// TODO Auto-generated method stub
		return null;
	}



//    class NISSearchResult implements ISearchResultsMetadata<BasicSpectrum>
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

//    /**
//     * Run a query which searches for msi images between the specified dates.
//     * Returns a list of URL's of the fit files that match.
//     *
//     * @param startDate
//     * @param endDate
//     */
//    @Override
//    public List<List<String>> runQuery(
//            @SuppressWarnings("unused") String type,
//            DateTime startDate,
//            DateTime stopDate,
//            @SuppressWarnings("unused") boolean sumOfProductsSearch,
//            @SuppressWarnings("unused") List<Integer> camerasSelected,
//            @SuppressWarnings("unused") List<Integer> filtersSelected,
//            double startDistance,
//            double stopDistance,
//            @SuppressWarnings("unused") double startResolution,
//            @SuppressWarnings("unused") double stopResolution,
//            @SuppressWarnings("unused") String searchString,
//            List<Integer> polygonTypes,
//            double fromIncidence,
//            double toIncidence,
//            double fromEmission,
//            double toEmission,
//            double fromPhase,
//            double toPhase,
//            TreeSet<Integer> cubeList,
//            @SuppressWarnings("unused") ImageSource msiSource,
//            @SuppressWarnings("unused") int limbType)
//    {
//        List<List<String>> results = null;
//
//        double minIncidence = Math.min(fromIncidence, toIncidence);
//        double maxIncidence = Math.max(fromIncidence, toIncidence);
//        double minEmission = Math.min(fromEmission, toEmission);
//        double maxEmission = Math.max(fromEmission, toEmission);
//        double minPhase = Math.min(fromPhase, toPhase);
//        double maxPhase = Math.max(fromPhase, toPhase);
//
//        try
//        {
//            double minScDistance = Math.min(startDistance, stopDistance);
//            double maxScDistance = Math.max(startDistance, stopDistance);
//
//            HashMap<String, String> args = new HashMap<>();
//            args.put("startDate", String.valueOf(startDate.getMillis()));
//            args.put("stopDate", String.valueOf(stopDate.getMillis()));
//            args.put("minScDistance", String.valueOf(minScDistance));
//            args.put("maxScDistance", String.valueOf(maxScDistance));
//            args.put("minIncidence", String.valueOf(minIncidence));
//            args.put("maxIncidence", String.valueOf(maxIncidence));
//            args.put("minEmission", String.valueOf(minEmission));
//            args.put("maxEmission", String.valueOf(maxEmission));
//            args.put("minPhase", String.valueOf(minPhase));
//            args.put("maxPhase", String.valueOf(maxPhase));
//            for (int i=0; i<4; ++i)
//            {
//                if (polygonTypes.contains(i))
//                    args.put("polygonType"+i, "1");
//                else
//                    args.put("polygonType"+i, "0");
//            }
//            if (cubeList != null && cubeList.size() > 0)
//            {
//                String cubesStr = "";
//                int size = cubeList.size();
//                int count = 0;
//                for (Integer i : cubeList)
//                {
//                    cubesStr += "" + i;
//                    if (count < size-1)
//                        cubesStr += ",";
//                    ++count;
//                }
//                args.put("cubes", cubesStr);
//            }
//
//            results = doQuery("searchnis.php", constructUrlArguments(args));
//
///*            for (List<String> res : results)
//            {
//                String path = this.getNisPath(res);
//
//                matchedImages.add(path);
//            }*/
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//        //return matchedImages;
//        return results;
//
//        //System.err.println("Error: Not implemented. Do not call.");
//        //return null;
//    }

/*    public List<String> runSpectralQuery(
            DateTime startDate,
            DateTime stopDate,
            List<Integer> filters,
            boolean iofdbl,
            boolean cifdbl,
            double startDistance,
            double stopDistance,
            double startResolution,
            double stopResolution,
            String searchString,
            List<Integer> polygonTypes,
            double fromIncidence,
            double toIncidence,
            double fromEmission,
            double toEmission,
            double fromPhase,
            double toPhase,
            TreeSet<Integer> cubeList,
            ImageSource msiSource,
            int limbType)
    {
    }*/

//    @Override
//    protected List<List<String>> getCachedResults(
//            String pathToImageFolder,
//            String searchString
//            )
//    {
//        JOptionPane.showMessageDialog(null,
//                "Unable to perform online search for NIS data. Ignoring search parameters and listing all cached spectra.",
//                "Warning",
//                JOptionPane.WARNING_MESSAGE);
//    	// Create a map of actual files, with key the segment of the
//    	// file name that will match the output of getNisPath.
//        final List<File> fileList = getCachedFiles(pathToImageFolder);
//        final Map<String, File> filesFound = new TreeMap<>();
//        for (File file: fileList)
//        {
//        	// Format for NIS path is /NIS/YYYY/..., basically everything after the
//        	// cache directory prefix.
//            String path = file.getPath().substring(Configuration.getCacheDir().length());
//            filesFound.put(path, file);
//        }
//
//        final List<List<String>> result = new ArrayList<>();
//        SortedMap<String, List<String>> inventory = getDataInventory();
//        // Match the current inventory against the cached file map.
//        for (Entry<String, List<String>> each: inventory.entrySet())
//        {
//            List<String> res = each.getValue();
//            if (!res.get(0).contains("NIS")) continue;
//            String path = getNisPath(res);
//            if (filesFound.containsKey(path))
//                result.add(res);
//        }
//        return result;
//    }
//
//    protected void changeImagePathToFullPath(List<String> result)
//    {
//        String fullPath = result.get(1) + "/" + result.get(2) + "/" + result.get(0);
//        if (!fullPath.contains("/"))
//        {
//            result.set(0, getDataPath() + "/" + fullPath);
//        }
//    }
//
//    @Override
//    public Metadata store()
//    {
//        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
//        return configMetadata;
//    }
//
//    @Override
//    public void retrieve(Metadata source)
//    {
//
//    }
}
