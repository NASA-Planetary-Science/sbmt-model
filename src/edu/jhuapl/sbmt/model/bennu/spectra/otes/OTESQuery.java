package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import java.util.List;

import edu.jhuapl.sbmt.core.pointing.PointingSource;
import edu.jhuapl.sbmt.query.v2.DataQuerySourcesMetadata;
import edu.jhuapl.sbmt.query.v2.FetchedResults;
import edu.jhuapl.sbmt.query.v2.ISearchMetadata;
import edu.jhuapl.sbmt.query.v2.QueryException;
import edu.jhuapl.sbmt.spectrum.query.SpectrumDataQuery;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;


/**
 * Query definition class for OTES
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class OTESQuery extends SpectrumDataQuery
{
	private static OTESQuery instance = new OTESQuery();
	private static String SEARCHSCRIPT = "searchotes.php";
    private static final Key<OTESQuery> OTESQUERY_KEY = Key.of("otesQuery");
    protected static String OTESSPECTRATABLEBASENAME = "_otesspectra_";
	protected static String OTESCUBESTABLEBASENAME = "_otescubes_";
	private String searchString;

    public static OTESQuery getInstance()
    {
        return instance;
    }

    private OTESQuery()
    {
        super(DataQuerySourcesMetadata.of("/bennu/shared/otes/l2", "/bennu/shared/otes/l2/spectra",
        		"", "", "", PointingSource.SPICE, "spectrumlist.txt"));
    }

    @Override
    public OTESQuery clone() {
        return getInstance();
    }

    @Override
    public FetchedResults runQuery(ISearchMetadata queryMetadata) throws QueryException
    {
    	return fallbackQuery(queryMetadata);

//        FixedMetadata metadata = queryMetadata.getMetadata();
//        searchString = metadata.get(DatabaseSearchMetadata.SEARCH_STRING);
//        TreeSet<Integer> cubeList = null;
//        if (metadata.hasKey(SpectraDatabaseSearchMetadata.CUBE_LIST))
//        	cubeList = metadata.get(SpectraDatabaseSearchMetadata.CUBE_LIST);
//        FetchedResults results = null;
//
//        String modelName = metadata.get(SpectraDatabaseSearchMetadata.MODEL_NAME);
//        String dataType = metadata.get(SpectraDatabaseSearchMetadata.DATA_TYPE);
//        dataType = "l2";
//        spectraTableName = "bennu_" + modelName + OTESSPECTRATABLEBASENAME + dataType;
//        cubeTableName = "bennu_" + modelName + OTESCUBESTABLEBASENAME + dataType;
//
//        HashMap<String, String> dbSearchArgsMap = convertSearchParamsToDBArgsMap(metadata);
//        System.out.println("OTESQuery: runQuery: spectral table name " + spectraTableName);
//    	boolean tableExists = DatabaseDataQuery.checkForDatabaseTable(spectraTableName);
//        if (!tableExists)
//        	throw new QueryException("Table Does Not Exist", Severity.ERROR,
//        			QueryExceptionReason.DB_TABLE_NOT_FOUND);
//
//    	if (searchString != null)
//        {
//            dbSearchArgsMap = new HashMap<>();
//            dbSearchArgsMap.put("searchString", searchString);
//        }
//    	else
//    	{
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
//                dbSearchArgsMap.put(CUBES, cubesStr);
//            }
//
//    	}
//        return doQuery(SEARCHSCRIPT, constructUrlArguments(dbSearchArgsMap));
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
    	InstanceGetter.defaultInstanceGetter().register(OTESQUERY_KEY, (metadata) -> {

    		OTESQuery query = new OTESQuery();
    		return query;

    	}, OTESQuery.class, query -> {

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
}
