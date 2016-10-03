package edu.jhuapl.sbmt.model.europa;

import javax.swing.ListModel;


public interface AreaCalculationCollection extends ListModel
{
    public String getName();

    public Integer getCurrentIndex();

    public void setCurrentIndex(Integer index);

    public void setCurrentTrajectory(String trajectoryName);

    public void put(Integer index, AreaCalculation areaCalculation);

    public Integer getLength();

    public AreaCalculation getValue(Integer index);

    public AreaCalculation getCurrentValue();
}
