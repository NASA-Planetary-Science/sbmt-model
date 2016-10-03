package edu.jhuapl.sbmt.model.europa.math;

public class V3 {
	public double v[] = new double[3];
	public V3()
	{
		v[0] = v[1] = v[2] = 0.0;
	}
	public V3(double input[])
	{
		if (input.length >= 3)
		{
			v[0] = input[0];
			v[1] = input[1];
			v[2] = input[2];
		}
	}
	public V3(V3 input)
	{
		copy(input);
	}
	public V3(double x, double y, double z)
	{
		set(x,y,z);
	}
	public void create(double x, double y, double z)
	{
		set(x,y,z);
	}
	public void copy(V3 input)
	{
		v[0] = input.v[0];
		v[1] = input.v[1];
		v[2] = input.v[2];
	}
	public double X1(){ return v[0];}
	public double X2(){ return v[1];}
	public double X3(){ return v[2];}
	public void setX1(double x){ v[0] = x;}
	public void setX2(double x){ v[1] = x;}
	public void setX3(double x){ v[2] = x;}
	public void set(double x, double y, double z)
	{
		v[0] = x;
		v[1] = y;
		v[2] = z;
	}
	public double get(int i)
	{
		if (i == 0) return v[0];
		if (i == 1) return v[1];
		if (i == 2) return v[2];
		return 0.0;
	}
	public void get(double data[])
	{
		if (data.length >= 3)
		{
			data[0] = v[0];
			data[1] = v[1];
			data[2] = v[2];
		}
	}
}
