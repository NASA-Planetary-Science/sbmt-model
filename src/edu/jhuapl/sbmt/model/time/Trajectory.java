package edu.jhuapl.sbmt.model.time;

import java.util.ArrayList;


public interface Trajectory
{

    public int getCellId();

    public void setCellId(int cellId);

    public String getName();

    public void setName(String name);

    public int getId();

    public void setId(int id);

    public ArrayList<Double> getX();

    public void setX(ArrayList<Double> x);

    public ArrayList<Double> getY();

    public void setY(ArrayList<Double> y);

    public ArrayList<Double> getZ();

    public void setZ(ArrayList<Double> z);

}
