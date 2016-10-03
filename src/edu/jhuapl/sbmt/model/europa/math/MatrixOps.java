package edu.jhuapl.sbmt.model.europa.math;

import edu.jhuapl.sbmt.model.europa.projection.GeometricConstants;

public class MatrixOps {
	public static double trace(M3 m)
	{
		return (m.m11() + m.m22() + m.m33());
	}
	public static void AxisAngleToRotationMatrix(V3 axis, double angleRad, M3 result)
	{
		V3 e = VectorOps.Unit(axis);
		double cosPhi = Math.cos(angleRad);
		double sinPhi = Math.sin(angleRad);
		double oneMinusCosPhi = 1.0 - cosPhi;
		double e1 = e.X1();
		double e2 = e.X2();
		double e3 = e.X3();

		result.set11(cosPhi + e1*e1*oneMinusCosPhi);
		result.set12(e1*e2*oneMinusCosPhi+e3*sinPhi);
		result.set13(e1*e3*oneMinusCosPhi-e2*sinPhi);

		result.set21(e1*e2*oneMinusCosPhi-e3*sinPhi);
		result.set22(cosPhi + e2*e2*oneMinusCosPhi);
		result.set23(e2*e3*oneMinusCosPhi + e1*sinPhi);

		result.set31(e1*e3*oneMinusCosPhi + e2*sinPhi);
		result.set32(e2*e3*oneMinusCosPhi - e1*sinPhi);
		result.set33(cosPhi + e3*e3*oneMinusCosPhi);
	}
	public static boolean RotationMatrixToAxisAngle(M3 rotMat, V3 axis, Double angle)
	{
		boolean result = false;

		double cosAngle = 0.500 * (MatrixOps.trace(rotMat) - 1.0);
		if (Math.abs(cosAngle) < GeometricConstants.EPS10)
		{
			angle = GeometricConstants.PIOVER2;
		}
		else
		{
			angle = Math.acos(cosAngle);
		}

		if (Math.abs(Math.sin(angle)) > GeometricConstants.EPS10)
		{
			double twoSin = 2.0 * Math.sin(angle);

			axis.create(
				(rotMat.m23() - rotMat.m32())/twoSin,
				(rotMat.m31() - rotMat.m13())/twoSin,
				(rotMat.m12() - rotMat.m21())/twoSin
				);
			VectorOps.MakeUnit(axis);

			result = true;
		}

		return result;
	}
	public static void multiply(M3 m, V3 v, V3 result)
	{
		result.setX1(m.m11() * v.X1() + m.m12() * v.X2() + m.m13() * v.X3());
		result.setX2(m.m21() * v.X1() + m.m22() * v.X2() + m.m23() * v.X3());
		result.setX3(m.m31() * v.X1() + m.m32() * v.X2() + m.m33() * v.X3());
	}
	public static V3 multiply(M3 m, V3 v)
	{
		V3 result = new V3();

		multiply(m, v, result);

		return result;
	}
	public static M3 multiply(M3 m1, M3 m2)
	{
		M3 result = new M3();

		multiply(m1,m2,result);

		return result;
	}
	public static void multiply(M3 m1, M3 m2, M3 result)
	{
		result.set11(m1.m11()*m2.m11() + m1.m12()*m2.m21() + m1.m13()*m2.m31());
		result.set12(m1.m11()*m2.m12() + m1.m12()*m2.m22() + m1.m13()*m2.m32());
		result.set13(m1.m11()*m2.m13() + m1.m12()*m2.m23() + m1.m13()*m2.m33());

		result.set21(m1.m21()*m2.m11() + m1.m22()*m2.m21() + m1.m23()*m2.m31());
		result.set22(m1.m21()*m2.m12() + m1.m22()*m2.m22() + m1.m23()*m2.m32());
		result.set23(m1.m21()*m2.m13() + m1.m22()*m2.m23() + m1.m23()*m2.m33());

		result.set31(m1.m31()*m2.m11() + m1.m32()*m2.m21() + m1.m33()*m2.m31());
		result.set32(m1.m31()*m2.m12() + m1.m32()*m2.m22() + m1.m33()*m2.m32());
		result.set33(m1.m31()*m2.m13() + m1.m32()*m2.m23() + m1.m33()*m2.m33());
	}
	public static double determinant(M3 m)
	{
		return
			(m.m11() * (m.m22()*m.m33() - m.m23()*m.m32()) +
			m.m12() * (m.m23()*m.m31() - m.m21()*m.m33()) +
			m.m13() * (m.m21()*m.m32() - m.m22()*m.m31()));
	}
	public static M3 transpose(M3 m)
	{
		M3 result = new M3();

		transpose(m,result);

		return result;
	}
	public static void transpose(M3 m, M3 result)
	{
		result.set11(m.m11());
		result.set12(m.m21());
		result.set13(m.m31());

		result.set21(m.m12());
		result.set22(m.m22());
		result.set23(m.m32());

		result.set31(m.m13());
		result.set32(m.m23());
		result.set33(m.m33());
	}
	public static void makeTranspose(M3 mat)
	{
		M3 copyM = new M3(mat);
		MatrixOps.transpose(copyM, mat);
	}
	public static void Inverse(M3 m, M3 mInv)
	{
		double d = MatrixOps.determinant(m);
		if (Math.abs(d) > GeometricConstants.EPS10)
		{
			d = 1.0 / d;
			mInv.set11(((m.m22()*m.m33() - m.m23()*m.m32())*d));
			mInv.set12(((m.m13()*m.m32() - m.m12()*m.m33())*d));
			mInv.set13(((m.m12()*m.m23() - m.m13()*m.m22())*d));

			mInv.set21(((m.m23()*m.m31() - m.m21()*m.m33())*d));
			mInv.set22(((m.m11()*m.m33() - m.m13()*m.m31())*d));
			mInv.set23(((m.m13()*m.m21() - m.m11()*m.m23())*d));

			mInv.set31(((m.m21()*m.m32() - m.m22()*m.m31())*d));
			mInv.set32(((m.m12()*m.m31() - m.m11()*m.m32())*d));
			mInv.set33(((m.m11()*m.m22() - m.m12()*m.m21())*d));
		}
	}
}
