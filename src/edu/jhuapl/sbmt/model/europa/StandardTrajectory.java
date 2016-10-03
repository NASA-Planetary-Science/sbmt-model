package edu.jhuapl.sbmt.model.europa;

import java.util.ArrayList;

public class StandardTrajectory implements Trajectory
{
    private int cellId;
    private String name;
    private int id;
    private ArrayList<Double> x;
    private ArrayList<Double> y;
    private ArrayList<Double> z;

    public int getCellId()
    {
        return cellId;
    }

    public void setCellId(int cellId)
    {
        this.cellId = cellId;
    }


    public String getName()
    {
        return name;
    }


    public void setName(String name)
    {
        this.name = name;
    }


    public int getId()
    {
        return id;
    }


    public void setId(int id)
    {
        this.id = id;
    }


    public ArrayList<Double> getX()
    {
        return x;
    }


    public void setX(ArrayList<Double> x)
    {
        this.x = x;
    }


    public ArrayList<Double> getY()
    {
        return y;
    }


    public void setY(ArrayList<Double> y)
    {
        this.y = y;
    }


    public ArrayList<Double> getZ()
    {
        return z;
    }


    public void setZ(ArrayList<Double> z)
    {
        this.z = z;
    }

    public StandardTrajectory()
    {
        super();
        this.x = new ArrayList<Double>();
        this.y = new ArrayList<Double>();
        this.z = new ArrayList<Double>();
    }
}