package edu.jhuapl.sbmt.model.phobos.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.SaavtkItemManager;

public class MEGANECollection extends SaavtkItemManager<MEGANEFootprint> implements PropertyChangeListener
{

	public MEGANECollection()
	{
		// TODO Auto-generated constructor stub
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
