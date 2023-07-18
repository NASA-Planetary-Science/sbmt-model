package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import edu.jhuapl.sbmt.core.pointing.PointingSource;
import edu.jhuapl.sbmt.query.database.SpectraDatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.v2.DataQuerySourcesMetadata;
import edu.jhuapl.sbmt.query.v2.DatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.v2.FetchedResults;
import edu.jhuapl.sbmt.query.v2.ISearchMetadata;
import edu.jhuapl.sbmt.query.v2.QueryException;
import edu.jhuapl.sbmt.spectrum.query.SpectrumDataQuery;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.FixedMetadata;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

/**
 * Query definition class for NIRS3
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class NIRS3Query extends SpectrumDataQuery
{
    private static NIRS3Query instance=new NIRS3Query();
    private String searchString;
    private static final Key<NIRS3Query> NIRS3QUERY_KEY = Key.of("nirs3Query");
    private static String SEARCHSCRIPT = "searchotes.php";

    public static NIRS3Query getInstance()
    {
        return instance;
    }

    private NIRS3Query()
    {
        super(DataQuerySourcesMetadata.of("/ryugu/shared/nirs3", "/ryugu/shared/nirs3/spectra",
        		"", "", "", PointingSource.SPICE, "spectrumlist.txt"));
    }

    @Override
    public NIRS3Query clone()
    {
        return getInstance();
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
//        spectraTableName = "bennu_" + modelName + OTESSPECTRATABLEBASENAME + dataType;
//        cubeTableName = "bennu_" + modelName + OTESCUBESTABLEBASENAME + dataType;

        HashMap<String, String> dbSearchArgsMap = convertSearchParamsToDBArgsMap(metadata);
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
        return doQuery(SEARCHSCRIPT, constructUrlArguments(dbSearchArgsMap));
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
    	InstanceGetter.defaultInstanceGetter().register(NIRS3QUERY_KEY, (metadata) -> {

    		NIRS3Query query = new NIRS3Query();
    		return query;

    	}, NIRS3Query.class, query -> {

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

//    @Override
//    public String getDataPath()
//    {
//        return rootPath + "/spectra";
//    }
//
//    @Override
//    public FetchedResults runQuery(SearchMetadata queryMetadata) throws QueryException
//    {
//        // TODO Auto-generated method stub
//        return super.runQuery(queryMetadata);
//    }
}