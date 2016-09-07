package edu.jhuapl.sbmt.model.eros;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import com.google.common.collect.Lists;

import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;

public class NISStatisticsCollection extends AbstractModel implements PropertyChangeListener
{
    List<vtkProp> props=Lists.newArrayList();
    List<NISStatistics> stats=Lists.newArrayList();

    public void addStatistics(NISStatistics stats)
    {
        this.stats.add(stats);
    }

    public enum SpectrumOrdering
    {
        TH_MEAN,TH_VARIANCE,TH_SKEWNESS,TH_KURTOSIS;
    }



    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {

    }

    @Override
    public List<vtkProp> getProps()
    {
        return props;
    }

}
