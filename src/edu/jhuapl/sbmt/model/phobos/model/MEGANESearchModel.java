package edu.jhuapl.sbmt.model.phobos.model;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
import edu.jhuapl.sbmt.model.phobos.controllers.MEGANEDatabaseConnection;
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
	FilterModel<Double> numericFilterModel;
	FilterModel<Object> nonNumericFilterModel;
	FilterModel<LocalDateTime> timeWindowModel;
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

	public List<MEGANEFootprint> performSearch(Function<String, Void> statusUpdater) throws SQLException
	{
		List<MEGANEFootprint> footprints = Lists.newArrayList();
		List<Structure> structuresToSearch = getStructuresToSearch();
		if (!structuresToSearch.isEmpty())
		{
			List<String> structuresMetadata = structuresToSearch.stream().map(struct -> { return struct.getName(); } ).toList();
			metadata.put(" Structs ", structuresMetadata);
		}
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
		footprints.addAll(search(structuresToSearch, statusUpdater));
		return footprints;
	}

	private vtkPolyData getStructureFacetInformation(Structure structure, StructureManager refManager)
	{
		Task tmpTask = new SilentTask();
		vtkPolyData tmpPolyData = PlateUtil.formUnifiedStructurePolyData(tmpTask, refManager, List.of(structure));
		return tmpPolyData;
	}

	private List<MEGANEFootprint> search(List<Structure> structures, Function<String, Void> statusUpdater) throws SQLException
	{
		return dbConnection.getFootprintsForFacets2(generateStructureIndices(structures), getFacetObsSearchString(), getObservingGeometrySearchString(), statusUpdater);
	}

	private ImmutableList<Integer> generateStructureIndices(List<Structure> structureFilters)
	{
		List<MEGANEFootprint> allFootprints = Lists.newArrayList();
		HashSet<Integer> indices = new HashSet<Integer>();
		for (Structure structure : structureFilters)
		{
			StructureManager refManager = null;
			if (structure instanceof Polygon) refManager = polygonModel;
			else if (structure instanceof Ellipse) refManager = ellipseModel;
			vtkPolyData structureFacetInformation = getStructureFacetInformation(structure, refManager);
			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);
			indices.addAll(cellIdList);
		}
		return ImmutableList.copyOf(indices);
	}

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
			String parts[] = range.get().split(" ");
			Double lowValue = Double.parseDouble(parts[2]);
			Double highValue = Double.parseDouble(parts[4]);
			signalRange = Pair.of(lowValue, highValue);
		}
		return signalRange;
	}

	private String getFacetObsSearchString()
	{
		queryString = "";
		List<String> queryElements = parameterTableMap.get("FacetObs");
		List<String> actualQueries = numericFilterModel.getSQLQueryString();
		nonNumericFilterModel.getSQLQueryString().stream().filter(item -> item.contains("integrationTime")).findFirst().ifPresent(intTime -> actualQueries.add(intTime));

		Iterator<String> queryElementsIterator = queryElements.iterator();
		while (queryElementsIterator.hasNext())
		{
			String element = queryElementsIterator.next();
			List<String> matchingQuery = actualQueries.stream().filter(query -> query.contains(element)).toList();

			for (String str : matchingQuery)
			{
				if (!queryString.isEmpty()) queryString += " AND ";
				queryString += str;
			}

		}
		metadata.put(" Facet Constraints", List.of(queryString));
		return queryString;
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
		return queryString;
	}

	public List<Structure> getStructuresToSearch()
	{
		List<FilterType<Object>> structureFilters = nonNumericFilterModel.getAllItems().stream().filter(filter -> filter.getType().getSimpleName().equals("Structure")).toList();
		List<Structure> structures = Lists.newArrayList();
		for (FilterType filter : structureFilters)
		{
			if (filter.isEnabled() && ((Structure)filter.getSelectedRangeValue() != null))
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
	public FilterModel<LocalDateTime> getTimeWindowModel()
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
