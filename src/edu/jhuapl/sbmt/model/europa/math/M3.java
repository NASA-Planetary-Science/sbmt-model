package edu.jhuapl.sbmt.model.europa.math;

//
//	The purpose of this class is to encapsulate the data and operations of a
//	rotation matrix. The all-static MatrixOps class provides mathematical methods
//	that operate on rotation matrices. Nothing prevents someone from using this
//	class as a generic 3x3 matrix.
//

public class M3 {
	public double m[][] = new double[3][3];
	public M3()
	{
		m[0][0] = m[0][1] = m[0][2] = 0.0;
		m[1][0] = m[1][1] = m[1][2] = 0.0;
		m[2][0] = m[2][1] = m[2][2] = 0.0;
	}
	public M3(
			double m11, double m12, double m13,
			double m21, double m22, double m23,
			double m31, double m32, double m33)
	{
		m[0][0] = m11;
		m[0][1] = m12;
		m[0][2] = m13;
		m[1][0] = m21;
		m[1][1] = m22;
		m[1][2] = m23;
		m[2][0] = m31;
		m[2][1] = m32;
		m[2][2] = m33;
	}
	public M3(double data[][])
	{
		m[0][0] = data[0][0];
		m[0][1] = data[0][1];
		m[0][2] = data[0][2];
		m[1][0] = data[1][0];
		m[1][1] = data[1][1];
		m[1][2] = data[1][2];
		m[2][0] = data[2][0];
		m[2][1] = data[2][1];
		m[2][2] = data[2][2];
	}
	public M3(M3 m)
	{
		copy(m.m);
	}
	public M3(double row1[], double row2[], double row3[])
	{
		create(row1, row2, row3);
	}
	public void create(double row1[], double row2[], double row3[])
	{
		m[0][0] = row1[0];
		m[0][1] = row1[1];
		m[0][2] = row1[2];
		m[1][0] = row2[0];
		m[1][1] = row2[1];
		m[1][2] = row2[2];
		m[2][0] = row3[0];
		m[2][1] = row3[1];
		m[2][2] = row3[2];
	}
	public void create(
			double m11, double m12, double m13,
			double m21, double m22, double m23,
			double m31, double m32, double m33)
	{
		m[0][0] = m11;
		m[0][1] = m12;
		m[0][2] = m13;
		m[1][0] = m21;
		m[1][1] = m22;
		m[1][2] = m23;
		m[2][0] = m31;
		m[2][1] = m32;
		m[2][2] = m33;
	}
	public void create(double mat[][])
	{
		copy(mat);
	}
	public void copy(double mat[][])
	{
		m[0][0] = mat[0][0];
		m[0][1] = mat[0][1];
		m[0][2] = mat[0][2];
		m[1][0] = mat[1][0];
		m[1][1] = mat[1][1];
		m[1][2] = mat[1][2];
		m[2][0] = mat[2][0];
		m[2][1] = mat[2][1];
		m[2][2] = mat[2][2];
	}
	public double m11(){ return m[0][0];}
	public double m12(){ return m[0][1];}
	public double m13(){ return m[0][2];}
	public double m21(){ return m[1][0];}
	public double m22(){ return m[1][1];}
	public double m23(){ return m[1][2];}
	public double m31(){ return m[2][0];}
	public double m32(){ return m[2][1];}
	public double m33(){ return m[2][2];}

	public void set11(double value){ m[0][0] = value;}
	public void set12(double value){ m[0][1] = value;}
	public void set13(double value){ m[0][2] = value;}
	public void set21(double value){ m[1][0] = value;}
	public void set22(double value){ m[1][1] = value;}
	public void set23(double value){ m[1][2] = value;}
	public void set31(double value){ m[2][0] = value;}
	public void set32(double value){ m[2][1] = value;}
	public void set33(double value){ m[2][2] = value;}
	public void fromRowVectors(V3 row1, V3 row2, V3 row3)
	{
		m[0][0] = row1.X1();
		m[0][1] = row1.X2();
		m[0][2] = row1.X3();
		
		m[1][0] = row2.X1();
		m[1][1] = row2.X2();
		m[1][2] = row2.X3();
		
		m[2][0] = row3.X1();
		m[2][1] = row3.X2();
		m[2][2] = row3.X3();
	}
	public void fromColumnVectors(V3 col1, V3 col2, V3 col3)
	{
		m[0][0] = col1.X1();
		m[1][0] = col1.X2();
		m[2][0] = col1.X3();
		
		m[0][1] = col2.X1();
		m[1][1] = col2.X2();
		m[2][1] = col2.X3();
		
		m[0][2] = col3.X1();
		m[1][2] = col3.X2();
		m[2][2] = col3.X3();
	}
}
