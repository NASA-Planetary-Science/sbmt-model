package edu.jhuapl.sbmt.model.europa.math;

public class V3F {
	public float v[] = new float[3];
	public V3F()
	{
		v[0] = v[1] = v[2] = 0.0f;
	}
	public V3F(float input[])
	{
		if (input.length >= 3)
		{
			v[0] = input[0];
			v[1] = input[1];
			v[2] = input[2];
		}
	}
	public V3F(V3F input)
	{
		copy(input);
	}
	public V3F(float x, float y, float z)
	{
		set(x,y,z);
	}
	public void create(float x, float y, float z)
	{
		set(x,y,z);
	}
	public void copy(V3F input)
	{
		v[0] = input.v[0];
		v[1] = input.v[1];
		v[2] = input.v[2];
	}
	public float X1(){ return v[0];}
	public float X2(){ return v[1];}
	public float X3(){ return v[2];}
	public void setX1(float x){ v[0] = x;}
	public void setX2(float x){ v[1] = x;}
	public void setX3(float x){ v[2] = x;}
	public void set(float x, float y, float z)
	{
		v[0] = x;
		v[1] = y;
		v[2] = z;
	}
	public float get(int i)
	{
		if (i == 0) return v[0];
		if (i == 1) return v[1];
		if (i == 2) return v[2];
		return 0.0f;
	}
	public void get(float data[])
	{
		if (data.length >= 3)
		{
			data[0] = v[0];
			data[1] = v[1];
			data[2] = v[2];
		}
	}
}
