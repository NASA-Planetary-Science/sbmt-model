package edu.jhuapl.sbmt.model.phobos.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.SaavtkItemManager;

public class MEGANECollection extends SaavtkItemManager<MEGANEFootprint> implements PropertyChangeListener
{
	private List<MEGANEFootprint> footprints;

	public MEGANECollection()
	{

	}

	public MEGANECollection(List<MEGANEFootprint> footprints)
	{
		this.footprints = footprints;
	}

	public void setFootprints(List<MEGANEFootprint> footprints)
	{
		this.footprints = footprints;
		setAllItems(footprints);
	}

	@Override
	public List<vtkProp> getProps()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// TODO Auto-generated method stub

	}

	public boolean isFootprintMapped(MEGANEFootprint footprint)
	{
		return false;
	}

}
