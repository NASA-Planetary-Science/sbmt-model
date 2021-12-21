package edu.jhuapl.sbmt.model.phobos.ui.structureSearch;

import java.util.List;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableSet;

import vtk.vtkPolyData;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.SaavtkItemManager;
import edu.jhuapl.saavtk.model.structure.CircleModel;
import edu.jhuapl.saavtk.model.structure.EllipseModel;
import edu.jhuapl.saavtk.model.structure.PlateUtil;
import edu.jhuapl.saavtk.model.structure.PolygonModel;
import edu.jhuapl.saavtk.structure.Ellipse;
import edu.jhuapl.saavtk.structure.Polygon;
import edu.jhuapl.saavtk.structure.Structure;
import edu.jhuapl.saavtk.structure.StructureManager;

import glum.task.SilentTask;
import glum.task.Task;

public class MEGANEStructureCollection extends SaavtkItemManager<Structure>
{
	PolygonModel polygonModel;
	CircleModel circleModel;
	EllipseModel ellipseModel;

	public MEGANEStructureCollection(PolygonModel polygonModel, CircleModel circleModel, EllipseModel ellipseModel)
	{
		this.polygonModel = polygonModel;
		this.circleModel = circleModel;
		this.ellipseModel = ellipseModel;
	}

	public void updateItems()
	{
		List<Structure> structures = Lists.newArrayList();
		structures.addAll(polygonModel.getAllItems());
		structures.addAll(circleModel.getAllItems());
		structures.addAll(ellipseModel.getAllItems());
		setAllItems(structures);
	}

	public vtkPolyData getStructureFacetInformation()
	{
		Structure structure = null;
		ImmutableSet<Structure> selectedItems = getSelectedItems();
		if (selectedItems.size() != 1) return null;
		structure = selectedItems.asList().get(0);
		StructureManager refManager = null;
		if (structure instanceof Polygon) refManager = polygonModel;
		else if (structure instanceof Ellipse) refManager = ellipseModel;
		//can we handle circles specifically?
		Task tmpTask = new SilentTask();
		vtkPolyData tmpPolyData = PlateUtil.formUnifiedStructurePolyData(tmpTask, refManager, selectedItems);
		return tmpPolyData;
	}

	@Override
	public List<vtkProp> getProps()
	{
		// TODO Auto-generated method stub
		return null;
	}



}
