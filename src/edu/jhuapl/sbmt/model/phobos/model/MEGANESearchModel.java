package edu.jhuapl.sbmt.model.phobos.model;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

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

	public MEGANESearchModel(ModelManager modelManager, SmallBodyModel smallBodyModel, MEGANEDatabaseConnection dbConnection)
	{
		this.smallBodyModel = smallBodyModel;
		this.dbConnection = dbConnection;
		this.polygonModel = (PolygonModel)modelManager.getModel(ModelNames.POLYGON_STRUCTURES);
		this.circleModel = (CircleModel)modelManager.getModel(ModelNames.CIRCLE_STRUCTURES);
		this.ellipseModel = (EllipseModel)modelManager.getModel(ModelNames.ELLIPSE_STRUCTURES);
		this.selectionModel = (CircleSelectionModel)modelManager.getModel(ModelNames.CIRCLE_SELECTION);

		this.numericFilterModel = new FilterModel();
		this.nonNumericFilterModel = new FilterModel();
		this.timeWindowModel = new FilterModel();
	}

	public void removeRegion()
	{
		selectionModel.removeAllStructures();
	}

	public List<MEGANEFootprint> performSearch() throws SQLException
	{
		List<MEGANEFootprint> footprints = Lists.newArrayList();
		List<Structure> structuresToSearch = getStructuresToSearch();
		if (selectionModel.getNumItems() != 0)
		{
			Ellipse region = selectionModel.getItem(0);
			structuresToSearch.add(region);
		}
		footprints.addAll(search(structuresToSearch, getSearchString()));
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

	private List<MEGANEFootprint> getFootprintsForPlates(ImmutableList<Integer> cellIdList)
	{
		try
		{
			List<MEGANEFootprint> filteredFootprints = dbConnection.getFootprintsForFacets(cellIdList);
			return filteredFootprints;
		}
		catch (SQLException e1)
		{

			e1.printStackTrace();
			return Lists.newArrayList();
		}
	}

	private List<MEGANEFootprint> search(List<Structure> structures, String sqlString) throws SQLException
	{
		return dbConnection.getFootprintsForFacets(generateStructureIndices(structures), getSearchString());
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

	private List<MEGANEFootprint> generateStructureFootprints(List<Structure> structureFilters)
	{
		List<MEGANEFootprint> allFootprints = Lists.newArrayList();
		for (Structure structure : structureFilters)
		{
//			Structure structure = (Structure)filter.getSelectedRangeValue();
			StructureManager refManager = null;
			if (structure instanceof Polygon) refManager = polygonModel;
			else if (structure instanceof Ellipse) refManager = ellipseModel;
			vtkPolyData structureFacetInformation = getStructureFacetInformation(structure, refManager);
			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);

			allFootprints.addAll(getFootprintsForPlates(cellIdList));
		}
		return allFootprints;
	}

	public String getSearchString()
	{
		System.out.println("MEGANESearchModel: getSearchString: numeric filter model sql " + numericFilterModel.getSQLQueryString());
		System.out.println("MEGANESearchModel: getSearchString: time window sql " + timeWindowModel.getSQLQueryString());
		String queryString = "";
		if (numericFilterModel.getSQLQueryString().isEmpty()) queryString += numericFilterModel.getSQLQueryString();
		if (!numericFilterModel.getSQLQueryString().isEmpty() &&  !timeWindowModel.getSQLQueryString().isEmpty()) queryString += "&" + timeWindowModel.getSQLQueryString();
		else if (numericFilterModel.getSQLQueryString().isEmpty() && !timeWindowModel.getSQLQueryString().isEmpty()) queryString = timeWindowModel.getSQLQueryString();
		System.out.println("MEGANESearchModel: getSearchString: returning sqlString " + queryString);
		return queryString;
	}

	public List<Structure> getStructuresToSearch()
	{
		List<FilterType> structureFilters = nonNumericFilterModel.getAllItems().stream().filter(filter -> filter.getType() == Structure.class).toList();
		List<Structure> structures = Lists.newArrayList();
		for (FilterType filter : structureFilters)
		{
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

}
