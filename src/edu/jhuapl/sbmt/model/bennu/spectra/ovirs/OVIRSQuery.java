package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

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
 * Query definition class for OVIRS
 * @author steelrj1
 *
 * This must be final because it is a singleton with a clone() method.
 *
 */
public final class OVIRSQuery extends SpectrumDataQuery
{
	private static OVIRSQuery instance = new OVIRSQuery();
	private static String SEARCHSCRIPT = "searchovirs.php";
	private static final Key<OVIRSQuery> OVIRSQUERY_KEY = Key.of("ovirsQuery");
	protected static String OVIRSSPECTRATABLEBASENAME = "_ovirsspectra_";
	protected static String OVIRSCUBESTABLEBASENAME = "_ovirscubes_";
	private String searchString;

	public static OVIRSQuery getInstance()
	{
		return instance;
	}

	private OVIRSQuery()
	{
		super(DataQuerySourcesMetadata.of("/bennu/shared/ovirs/l3/SA16l3escireff",
				"/bennu/shared/ovirs/l3/SA16l3escireff/spectra", "", "", "", PointingSource.SPICE, "spectrumlist.txt"));
	}

	@Override
	public OVIRSQuery clone()
	{
		return getInstance();
	}

	@Override
	public FetchedResults runQuery(ISearchMetadata queryMetadata) throws QueryException
	{
		FixedMetadata metadata = queryMetadata.getMetadata();
		String searchString = metadata.get(DatabaseSearchMetadata.SEARCH_STRING);
		TreeSet<Integer> cubeList = metadata.get(SpectraDatabaseSearchMetadata.CUBE_LIST);
		FetchedResults results = null;

		String modelName = metadata.get(SpectraDatabaseSearchMetadata.MODEL_NAME);
		String dataType = metadata.get(SpectraDatabaseSearchMetadata.DATA_TYPE);
		spectraTableName = "bennu_" + modelName + OVIRSSPECTRATABLEBASENAME + dataType;
		cubeTableName = "bennu_" + modelName + OVIRSCUBESTABLEBASENAME + dataType;

		HashMap<String, String> dbSearchArgsMap = convertSearchParamsToDBArgsMap(metadata);

		boolean tableExists = DatabaseDataQuery.checkForDatabaseTable(spectraTableName);
		if (!tableExists)
			throw new QueryException("Table Does Not Exist", Severity.ERROR, QueryExceptionReason.DB_TABLE_NOT_FOUND);

		if (searchString != null)
		{
			dbSearchArgsMap = new HashMap<>();
			dbSearchArgsMap.put("searchString", searchString);
		} else
		{
			if (cubeList != null && cubeList.size() > 0)
			{
				String cubesStr = "";
				int size = cubeList.size();
				int count = 0;
				for (Integer i : cubeList)
				{
					cubesStr += "" + i;
					if (count < size - 1)
						cubesStr += ",";
					++count;
				}
				dbSearchArgsMap.put(CUBES, cubesStr);
			}

		}
		return doQuery(SEARCHSCRIPT, constructUrlArguments(dbSearchArgsMap));
	}

	protected void changeDataPathToFullPath(List<String> result, int index)
	{
		super.changeDataPathToFullPath(result, index);
	}

	@Override
	public FetchedResults fallbackQuery(ISearchMetadata queryMetadata) throws QueryException
	{
		ISearchMetadata searchMetadata = DataQuerySourcesMetadata.of("/bennu/shared/ovirs/l3/SA16l3escireff",
				"/bennu/shared/ovirs/l3/SA16l3escireff/spectra", "", "", "", PointingSource.SPICE, "spectrumlist.txt");
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
		// TODO Finish defining this
		InstanceGetter.defaultInstanceGetter().register(OVIRSQUERY_KEY, (metadata) ->
		{

			OVIRSQuery query = new OVIRSQuery();
			return query;

		}, OVIRSQuery.class, query ->
		{

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
