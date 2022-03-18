package edu.jhuapl.sbmt.model.phobos.model;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;

import vtk.vtkPolyData;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.model.structure.AbstractEllipsePolygonModel;
import edu.jhuapl.saavtk.model.structure.CircleModel;
import edu.jhuapl.saavtk.model.structure.CircleSelectionModel;
import edu.jhuapl.saavtk.model.structure.EllipseModel;
import edu.jhuapl.saavtk.model.structure.PlateUtil;
import edu.jhuapl.saavtk.model.structure.PolygonModel;
import edu.jhuapl.saavtk.structure.Ellipse;
import edu.jhuapl.saavtk.structure.Polygon;
import edu.jhuapl.saavtk.structure.Structure;
import edu.jhuapl.saavtk.structure.StructureManager;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.phobos.controllers.MEGANEController.MEGANEDatabaseConnection;
import edu.jhuapl.sbmt.query.filter.model.FilterModel;
import edu.jhuapl.sbmt.query.filter.model.FilterType;
import edu.jhuapl.sbmt.query.filter.model.RangeFilterModel;
import edu.jhuapl.sbmt.query.filter.model.TimeWindowFilterModel;

import glum.task.SilentTask;
import glum.task.Task;

public class MEGANESearchModel
{
	PolygonModel polygonModel;
	CircleModel circleModel;
	EllipseModel ellipseModel;
	SmallBodyModel smallBodyModel;
	MEGANEDatabaseConnection dbConnection;
	FilterModel numericFilterModel;
	FilterModel nonNumericFilterModel;
	FilterModel timeWindowModel;
	private AbstractEllipsePolygonModel selectionModel;
	private HashMap<String, List<String>> parameterTableMap = new HashMap<String, List<String>>();
	private String queryString = "";
	private HashMap<String, List<String>> metadata = new HashMap<String, List<String>>();

	public MEGANESearchModel(ModelManager modelManager, SmallBodyModel smallBodyModel, MEGANEDatabaseConnection dbConnection)
	{
		this.smallBodyModel = smallBodyModel;
		this.dbConnection = dbConnection;
		this.polygonModel = (PolygonModel)modelManager.getModel(ModelNames.POLYGON_STRUCTURES);
		this.circleModel = (CircleModel)modelManager.getModel(ModelNames.CIRCLE_STRUCTURES);
		this.ellipseModel = (EllipseModel)modelManager.getModel(ModelNames.ELLIPSE_STRUCTURES);
		this.selectionModel = (CircleSelectionModel)modelManager.getModel(ModelNames.CIRCLE_SELECTION);

		this.numericFilterModel = new RangeFilterModel();
		this.nonNumericFilterModel = new FilterModel();
		this.timeWindowModel = new TimeWindowFilterModel();
		parameterTableMap.put("ObservingGeometry", List.of("tdb", "lat", "lon", "alt", "normalizedAlt"));
		parameterTableMap.put("FacetObs", List.of("integrationTime", "incidence", "emission", "cosE", "projectedArea", "range"));
	}

	public void removeRegion()
	{
		selectionModel.removeAllStructures();
	}

	public List<MEGANEFootprint> performSearch() throws SQLException
	{
		List<MEGANEFootprint> footprints = Lists.newArrayList();
		List<Structure> structuresToSearch = getStructuresToSearch();
		List<String> structuresMetadata = structuresToSearch.stream().map(struct -> { return struct.getName(); } ).toList();
		metadata.put(" Structs ", structuresMetadata);
		if (selectionModel.getNumItems() != 0)
		{
			Ellipse region = selectionModel.getItem(0);
			structuresToSearch.add(region);
			metadata.put(" Region ", List.of(region.toString()));
		}
		for (FilterType type : numericFilterModel.getAllItems())
		{
			metadata.put(" " + type.toString(), List.of(""+type.getRangeMin() + "-"+type.getRangeMax()));
		}
		footprints.addAll(search(structuresToSearch));

		//save search metadata to model
//		metadata.putAll(parameterTableMap);

		return footprints;
//		if (structuresToSearch.isEmpty() && !getSearchString().isEmpty())
//			footprints.addAll(searchNonStructureParameters(getSearchString()));
//		else if (getSearchString().isEmpty() && !structuresToSearch.isEmpty())
//			footprints.addAll(generateStructureFootprints(structuresToSearch));
//		else	//combined search
//			footprints.addAll(searchAllParameters(structuresToSearch, getSearchString()));

//		 List<MEGANEFootprint> footprints = getStructureFacetInformation(region, selectionModel);

//		Task tmpTask = new SilentTask();
//		vtkPolyData structureFacetInformation = PlateUtil.formUnifiedStructurePolyData(tmpTask, selectionModel, List.of(region));
//		vtkPolyData structureFacetInformation = getStructureFacetInformation(region, selectionModel);
//		ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);
////
//		return getFootprintsForPlates(cellIdList);
	}

	private vtkPolyData getStructureFacetInformation(Structure structure, StructureManager refManager)
	{
		Task tmpTask = new SilentTask();
		vtkPolyData tmpPolyData = PlateUtil.formUnifiedStructurePolyData(tmpTask, refManager, List.of(structure));
		return tmpPolyData;
	}

//	private List<MEGANEFootprint> getFootprintsForPlates(ImmutableList<Integer> cellIdList)
//	{
//		try
//		{
//			List<MEGANEFootprint> filteredFootprints = dbConnection.getFootprintsForFacets(cellIdList);
//			return filteredFootprints;
//		}
//		catch (SQLException e1)
//		{
//
//			e1.printStackTrace();
//			return Lists.newArrayList();
//		}
//	}

	private List<MEGANEFootprint> search(List<Structure> structures) throws SQLException
	{
		return dbConnection.getFootprintsForFacets2(generateStructureIndices(structures), getFacetObsSearchString(), getObservingGeometrySearchString());
	}

//	private List<MEGANEFootprint> searchNonStructureParameters(String sqlString) throws SQLException
//	{
//		return dbConnection.getFootprintsForSQLParameters(sqlString);
//	}
//
//	private List<MEGANEFootprint> searchAllParameters(ImmutableList<Integer> cellIdList, String sqlString) throws SQLException
//	{
//		return dbConnection.getFootprintsForFacets(cellIdList, getSearchString());
//	}

	private ImmutableList<Integer> generateStructureIndices(List<Structure> structureFilters)
	{
//		Pair<Double, Double> currentSignalRange = getCurrentSignalContributionRange();
		List<MEGANEFootprint> allFootprints = Lists.newArrayList();
		HashSet<Integer> indices = new HashSet<Integer>();
		for (Structure structure : structureFilters)
		{
//			Structure structure = (Structure)filter.getSelectedRangeValue();
			StructureManager refManager = null;
			if (structure instanceof Polygon) refManager = polygonModel;
			else if (structure instanceof Ellipse) refManager = ellipseModel;
			vtkPolyData structureFacetInformation = getStructureFacetInformation(structure, refManager);
			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);
			indices.addAll(cellIdList);
//			allFootprints.addAll(getFootprintsForPlates(cellIdList));
		}
		return ImmutableList.copyOf(indices);
//		return allFootprints;
	}

//	private List<MEGANEFootprint> generateStructureFootprints(List<Structure> structureFilters)
//	{
//		List<MEGANEFootprint> allFootprints = Lists.newArrayList();
//		for (Structure structure : structureFilters)
//		{
////			Structure structure = (Structure)filter.getSelectedRangeValue();
//			StructureManager refManager = null;
//			if (structure instanceof Polygon) refManager = polygonModel;
//			else if (structure instanceof Ellipse) refManager = ellipseModel;
//			vtkPolyData structureFacetInformation = getStructureFacetInformation(structure, refManager);
//			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);
//
//			allFootprints.addAll(getFootprintsForPlates(cellIdList));
//		}
//		return allFootprints;
//	}

//	public String getSearchString()
//	{
////		List<String> searchElements = List.of(getFacetObsSearchString(), getObservingGeometrySearchString());
//		String facet = getFacetObsSearchString();
//		String obs = getObservingGeometrySearchString();
//		String join = !facet.isEmpty() && !obs.isEmpty() ? " AND " : "";
//		return facet + join + obs;
//
//	}

	public String getCurrentIntegrationTime()
	{
		String currentIntegrationTime = "";
		Optional<String> intTime = nonNumericFilterModel.getSQLQueryString().stream().filter(item -> item.contains("integrationTime")).findFirst();
		if (intTime.isPresent())
		{
			currentIntegrationTime = intTime.get();
		}
		return currentIntegrationTime;
	}

	public Pair<Double, Double> getCurrentSignalContributionRange()
	{
		Pair<Double, Double> signalRange = null;
		Optional<String> range = numericFilterModel.getSQLQueryString().stream().filter(item -> item.contains("signal")).findFirst();
		if (range.isPresent())
		{
			System.out.println("MEGANESearchModel: getCurrentSignalContributionRange: range " + range.get());
			String parts[] = range.get().split(" ");
			Double lowValue = Double.parseDouble(parts[2]);
			Double highValue = Double.parseDouble(parts[4]);
			signalRange = Pair.of(lowValue, highValue);
		}
		return signalRange;
	}

	private String getFacetObsSearchString()
	{
//		System.out.println("MEGANESearchModel: getSearchString: numeric filter model sql " + numericFilterModel.getSQLQueryString());
//		System.out.println("MEGANESearchModel: getSearchString: time window sql " + timeWindowModel.getSQLQueryString());
		queryString = "";
		List<String> queryElements = parameterTableMap.get("FacetObs");
		List<String> actualQueries = numericFilterModel.getSQLQueryString();
		nonNumericFilterModel.getSQLQueryString().stream().filter(item -> item.contains("integrationTime")).findFirst().ifPresent(intTime -> actualQueries.add(intTime));

		Iterator<String> queryElementsIterator = queryElements.iterator();
		while (queryElementsIterator.hasNext())
		{
			String element = queryElementsIterator.next();
			List<String> matchingQuery = actualQueries.stream().filter(query -> query.contains(element)).toList();
//			metadata.put(element, matchingQuery);

			for (String str : matchingQuery)
			{
				if (!queryString.isEmpty()) queryString += " AND ";
				queryString += str;
			}

		}
		metadata.put(" Facet Constraints", List.of(queryString));
//		System.out.println("MEGANESearchModel: getFacetObsSearchString: Facet obs string " + queryString);
		return queryString;


//		if (!numericFilterModel.getSQLQueryString().isEmpty()) queryString += numericFilterModel.getSQLQueryString();
//		if (!numericFilterModel.getSQLQueryString().isEmpty() &&  !timeWindowModel.getSQLQueryString().isEmpty()) queryString += " AND " + timeWindowModel.getSQLQueryString();
//		else if (numericFilterModel.getSQLQueryString().isEmpty() && !timeWindowModel.getSQLQueryString().isEmpty()) queryString = timeWindowModel.getSQLQueryString();
//		System.out.println("MEGANESearchModel: getSearchString: returning sqlString " + queryString);
//		return queryString;
	}

	private String getObservingGeometrySearchString()
	{
		queryString = "";
		List<String> queryElements = parameterTableMap.get("ObservingGeometry");
		List<String> actualQueries = numericFilterModel.getSQLQueryString();
		actualQueries.addAll(timeWindowModel.getSQLQueryString());

		Iterator<String> queryElementsIterator = queryElements.iterator();
		while (queryElementsIterator.hasNext())
		{
			String element = queryElementsIterator.next();
			List<String> matchingQuery = actualQueries.stream().filter(query -> query.contains(element)).toList();
			for (String str : matchingQuery)
			{
				if (!queryString.isEmpty()) queryString += " OR ";
				queryString += "(" + str + ")";
			}


		}
		metadata.put(" Observing Constraints", List.of(queryString));
//		List<String> actualTimeQueries = timeWindowModel.getSQLQueryString();
//		Iterator<String> queryElementsIterator2 = queryElements.iterator();
//		queryElementsIterator2 = queryElements.iterator();
//		while (queryElementsIterator.hasNext())
//		{
//			String element = queryElementsIterator2.next();
//			List<String> matchingQuery = actualTimeQueries.stream().filter(query -> query.contains(element)).toList();
//			for (String str : matchingQuery)
//			{
//				if (!queryString.isEmpty()) queryString += " OR ";
//				queryString += str;
//			}
//		}

//		System.out.println("MEGANESearchModel: getFacetObsSearchString: Obs Geometry string " + queryString);

		return queryString;
	}

	public List<Structure> getStructuresToSearch()
	{
		List<FilterType> structureFilters = nonNumericFilterModel.getAllItems().stream().filter(filter -> filter.getType() == Structure.class).toList();
		List<Structure> structures = Lists.newArrayList();
		for (FilterType filter : structureFilters)
		{
			if (filter.isEnabled())
				structures.add((Structure)filter.getSelectedRangeValue());
		}
		return structures;
	}

	/**
	 * @param dbConnection the dbConnection to set
	 */
	public void setDbConnection(MEGANEDatabaseConnection dbConnection)
	{
		this.dbConnection = dbConnection;
	}

	/**
	 * @return the numericFilterModel
	 */
	public FilterModel getNumericFilterModel()
	{
		return numericFilterModel;
	}

	/**
	 * @return the nonNumericFilterModel
	 */
	public FilterModel getNonNumericFilterModel()
	{
		return nonNumericFilterModel;
	}

	/**
	 * @return the timeWindowModel
	 */
	public FilterModel getTimeWindowModel()
	{
		return timeWindowModel;
	}

	/**
	 * @return the metadata
	 */
	public HashMap<String, List<String>> getMetadata()
	{
		return metadata;
	}

}
