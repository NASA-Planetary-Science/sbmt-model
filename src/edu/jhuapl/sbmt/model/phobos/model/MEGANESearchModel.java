package edu.jhuapl.sbmt.model.phobos.model;

import java.sql.SQLException;
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

	public List<MEGANEFootprint> performSearch()
	{
		if (selectionModel.getNumItems() == 0) return null;
		Ellipse region = selectionModel.getItem(0);
		Task tmpTask = new SilentTask();
		vtkPolyData structureFacetInformation = PlateUtil.formUnifiedStructurePolyData(tmpTask, selectionModel, List.of(region));

		ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);

		try
		{
			List<MEGANEFootprint> filteredFootprints = dbConnection.getFootprintsForFacets(cellIdList);
			return filteredFootprints;
			//collection.setFootprints(filteredFootprints);
		}
		catch (SQLException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	private vtkPolyData getStructureFacetInformation(Structure structure)
	{
		StructureManager refManager = null;
		if (structure instanceof Polygon) refManager = polygonModel;
		else if (structure instanceof Ellipse) refManager = ellipseModel;
		Task tmpTask = new SilentTask();
		vtkPolyData tmpPolyData = PlateUtil.formUnifiedStructurePolyData(tmpTask, refManager, List.of(structure));
		return tmpPolyData;
	}

	private void generateStructureFootprints(List<FilterType> structureFilters)
	{
		List<MEGANEFootprint> allFootprints = Lists.newArrayList();
		for (FilterType filter : structureFilters)
		{
			Structure structure = (Structure)filter.getSelectedRangeValue();
			vtkPolyData structureFacetInformation = getStructureFacetInformation(structure);
			ImmutableList<Integer> cellIdList = smallBodyModel.getClosestCellList(structureFacetInformation);

			try
			{
				List<MEGANEFootprint> filteredFootprints = dbConnection.getFootprintsForFacets(cellIdList);
				allFootprints.addAll(filteredFootprints);
//				collection.setFootprints(filteredFootprints);
			}
			catch (SQLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public String getSearchString()
	{
		return numericFilterModel.getSQLQueryString() + "&" + timeWindowModel.getSQLQueryString();
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
