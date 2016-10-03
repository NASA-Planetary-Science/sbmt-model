package edu.jhuapl.sbmt.model.europa.math;

import edu.jhuapl.sbmt.model.europa.projection.GeometricConstants;

public class VectorOps {
	public static boolean Aligned(V3 v1, V3 v2)
	{
		V3 v1u = VectorOps.Unit(v1);
		V3 v2u = VectorOps.Unit(v2);
		V3 vSum = VectorOps.Add(v1u, v2u);
		double m = VectorOps.Mag(vSum);

		if (MathToolkit.Between(m, 2.0 - GeometricConstants.EPS10, 2.0 + GeometricConstants.EPS10))
			return true;
		return false;
	}
	public static boolean RoughlyAligned(V3 v1, V3 v2, double eps)
	{
		V3 v1u = VectorOps.Unit(v1);
		V3 v2u = VectorOps.Unit(v2);
		V3 vSum = VectorOps.Add(v1u, v2u);
		double m = VectorOps.Mag(vSum);

		if (MathToolkit.Between(m, 2.0 - eps, 2.0 + eps))
			return true;
		return false;
	}
	public static V3 Subtract(V3 v1, V3 v2)
	{
		V3 result = new V3();

		result.create(v1.X1() - v2.X1(), v1.X2() - v2.X2(), v1.X3() - v2.X3());

		return result;
	}
	public static void Subtract(V3 v1, V3 v2, V3 result)
	{
		result.create(v1.X1() - v2.X1(), v1.X2() - v2.X2(), v1.X3() - v2.X3());
	}
	public static V3 Add(V3 v1, V3 v2)
	{
		V3 result = new V3();

		result.create(v1.X1() + v2.X1(), v1.X2() + v2.X2(), v1.X3() + v2.X3());


		return result;
	}
	public static void Add(V3 v1, V3 v2, V3 result)
	{
		result.create(v1.X1() + v2.X1(), v1.X2() + v2.X2(), v1.X3() + v2.X3());
	}
	public static V3 Scale(V3 v, double scaleFactor)
	{
		V3 result = new V3();

		result.setX1(v.X1()*scaleFactor);
		result.setX2(v.X2()*scaleFactor);
		result.setX3(v.X3()*scaleFactor);

		return result;
	}
	public static void Scale(V3 v, double scaleFactor, V3 result)
	{
		result.setX1(v.X1()*scaleFactor);
		result.setX2(v.X2()*scaleFactor);
		result.setX3(v.X3()*scaleFactor);
	}
	public static void ChangeScale(V3 result, double scaleFactor)
	{
		result.setX1(result.X1()*scaleFactor);
		result.setX2(result.X2()*scaleFactor);
		result.setX3(result.X3()*scaleFactor);
	}
	public static V3 Cross(V3 v1, V3 v2)
	{
		V3 result = new V3();

		result.setX1(v1.get(1) * v2.get(2) - v1.get(2) * v2.get(1));
		result.setX2(v1.get(2) * v2.get(0) - v1.get(0) * v2.get(2));
		result.setX3(v1.get(0) * v2.get(1) - v1.get(1) * v2.get(0));

		return result;
	}
	public static void Cross(V3 v1, V3 v2, V3 result)
	{
		result.v[0] = (v1.v[1] * v2.v[2] - v1.v[2] * v2.v[1]);
		result.v[1] = (v1.v[2] * v2.v[0] - v1.v[0] * v2.v[2]);
		result.v[2] = (v1.v[0] * v2.v[1] - v1.v[1] * v2.v[0]);
	}
	public static double Mag(V3 v)
	{
		//return Math.sqrt(v.X1()*v.X1() + v.X2()*v.X2() + v.X3()*v.X3());
		double v1max = Math.max(Math.abs(v.X1()),Math.abs(v.X2()));
		v1max = Math.max(v1max, Math.abs(v.X3()));


		/*
		If the vector is zero, return zero; otherwise normalize first.
		Normalizing helps in the cases where squaring would cause overflow
		or underflow.  In the cases where such is not a problem it not worth
		it to optimize further.
		*/

		if ( v1max == 0.0 )
		{
			return ( 0.0 );
		}
		else
		{
			double tmp0     =  v.X1()/v1max;
			double tmp1     =  v.X2()/v1max;
			double tmp2     =  v.X3()/v1max;

			double normSqr  =  tmp0*tmp0 + tmp1*tmp1 + tmp2*tmp2;

			return (  v1max * Math.sqrt( normSqr )  );
		}
	}
	public static double MagSquared(V3 v)
	{
		return (v.X1()*v.X1() + v.X2()*v.X2() + v.X3()*v.X3());
	}
	public static double Dot(V3 v1, V3 v2)
	{
		return v1.X1()*v2.X1() + v1.X2()*v2.X2() + v1.X3()*v2.X3();
	}
	public static double UnitDot(V3 v1, V3 v2)
	{
		V3 v1u = VectorOps.Unit(v1);
		V3 v2u = VectorOps.Unit(v2);

		return (v1u.X1()*v2u.X1() + v1u.X2()*v2u.X2() + v1u.X3()*v2u.X3());
	}
	public static V3 Unit(V3 v)
	{
		V3 result = new V3();

		double m = VectorOps.Mag(v);
		if (m > 0.0)
		{
			result.create(v.X1()/m, v.X2()/m, v.X3()/m);
		}

		return result;
	}
	public static void MakeUnit(V3 v)
	{
		double m = VectorOps.Mag(v);
		if (m > 0.0)
		{
			v.create(v.X1()/m, v.X2()/m, v.X3()/m);
		}
	}
	public static void Unit(V3 vIn, V3 vOut)
	{
		double m = 1.0/VectorOps.Mag(vIn);
		if (m > 0.0)
		{
			vOut.v[0] = vIn.v[0]*m;
			vOut.v[1] = vIn.v[1]*m;
			vOut.v[2] = vIn.v[2]*m;
		}
	}
	public static double AngularSepNoChecking(V3 v1, V3 v2)
	{
		double result = 0.0;

		V3 vtemp;
		vtemp = new V3();

		if ( VectorOps.Dot(v1,v2) > 0.0 )
		{
			vtemp.setX1(v1.v[0] - v2.v[0]);
			vtemp.setX2(v1.v[1] - v2.v[1]);
			vtemp.setX3(v1.v[2] - v2.v[2]);

			result = 2.00 * Math.asin (0.50 * VectorOps.Mag(vtemp));
		}
		else if ( VectorOps.Dot(v1,v2) < 0.0 )
		{
			vtemp.setX1(v1.v[0] + v2.v[0]);
			vtemp.setX2(v1.v[1] + v2.v[1]);
			vtemp.setX3(v1.v[2] + v2.v[2]);

			result = GeometricConstants.PI - 2.00 * Math.asin (0.50 * VectorOps.Mag(vtemp));
		}
		else
		{
			result = GeometricConstants.PIOVER2;
		}

		return result;
	}
	public static double AngularSep(V3 v1, V3 v2)
	{
		double result = 0.0;

		V3 u1, u2, vtemp;
		u1 = new V3();
		u2 = new V3();
		vtemp = new V3();
		double dmag1, dmag2;

		VectorOps.Unit(v1, u1);
		dmag1 = VectorOps.Mag(v1);

		if ( dmag1 == 0.0 )
		{
			return result;
		}

		VectorOps.Unit(v2, u2);
		dmag2 = VectorOps.Mag(v2);

		if ( dmag2 == 0.0 )
		{
			return result;
		}

		if ( VectorOps.Dot(u1,u2) > 0.0 )
		{
			vtemp.setX1(u1.v[0] - u2.v[0]);
			vtemp.setX2(u1.v[1] - u2.v[1]);
			vtemp.setX3(u1.v[2] - u2.v[2]);

			result = 2.00 * Math.asin (0.50 * VectorOps.Mag(vtemp));
		}
		else if ( VectorOps.Dot(u1,u2) < 0.0 )
		{
			vtemp.setX1(u1.v[0] + u2.v[0]);
			vtemp.setX2(u1.v[1] + u2.v[1]);
			vtemp.setX3(u1.v[2] + u2.v[2]);

			result = GeometricConstants.PI - 2.00 * Math.asin (0.50 * VectorOps.Mag(vtemp));
		}
		else
		{
			result = GeometricConstants.PIOVER2;
		}


		return result;
	}
	public static V3 Negative(V3 v)
	{
		V3 result = new V3(v);

		result.setX1(-result.X1());
		result.setX2(-result.X2());
		result.setX3(-result.X3());

		return result;
	}
	public static void Negative(V3 v, V3 result)
	{
		result.setX1(-v.X1());
		result.setX2(-v.X2());
		result.setX3(-v.X3());
	}
	public static V3 XRotation(double angleRad, V3 v)
	{
		V3 result = new V3();

		result.setX1(v.X1());
		result.setX2(Math.cos(angleRad)*v.X2() + Math.sin(angleRad)*v.X3());
		result.setX3(-Math.sin(angleRad)*v.X2() + Math.cos(angleRad)*v.X3());

		return result;
	}
	public static V3 YRotation(double angleRad, V3 v)
	{
		V3 result = new V3();

		result.setX1(Math.cos(angleRad)*v.X1() - Math.sin(angleRad)*v.X3());
		result.setX2(v.X2());
		result.setX3(Math.sin(angleRad)*v.X1() + Math.cos(angleRad)*v.X3());

		return result;
	}
	public static V3 ZRotation(double angleRad, V3 v)
	{
		V3 result = new V3();

		result.setX1(Math.cos(angleRad)*v.X1() + Math.sin(angleRad)*v.X2());
		result.setX2(-Math.sin(angleRad)*v.X1() + Math.cos(angleRad)*v.X2());
		result.setX3(v.X3());

		return result;
	}
	public static V3 ArbitraryRotate(V3 p, double thetaRad, V3 r)
	{
		V3 result = new V3();

		double cost, sint, omc;
		V3 ru = VectorOps.Unit(r);
		double rx = ru.v[0], ry = ru.v[1], rz = ru.v[2];
		double px = p.v[0], py = p.v[1], pz = p.v[2];
		//double qx, qy, qz;

		cost = Math.cos(thetaRad);
		sint = Math.sin(thetaRad);
		omc = 1.0 - cost;

		result.v[0] = (cost + omc * rx * rx) * px +
			(omc * rx * ry - rz * sint) * py +
			(omc * rx * rz + ry * sint) * pz;

		result.v[1] = (omc * rx * ry + rz * sint) * px +
			(cost + omc * ry * ry) * py +
			(omc * ry * rz - rx * sint) * pz;

		result.v[2] = (omc * rx * rz - ry * sint) * px +
			(omc * ry * rz + rx * sint) * py +
			(cost + omc * rz * rz) * pz;

		return result;
	}
	public static void ArbitraryRotate(V3 p, double thetaRad, V3 r, V3 result)
	{
		double cost, sint, omc;
		V3 ru = new V3();
		VectorOps.Unit(r, ru);
		//double rx = ru.m_v[0], ry = ru.m_v[1], rz = ru.m_v[2];
		//double px = p.m_v[0], py = p.m_v[1], pz = p.m_v[2];
		//double qx, qy, qz;

		cost = Math.cos(thetaRad);
		sint = Math.sin(thetaRad);
		omc = 1.0 - cost;

		result.v[0] = (cost + omc * ru.v[0] * ru.v[0]) * p.v[0] +
			(omc * ru.v[0] * ru.v[1] - ru.v[2] * sint) * p.v[1] +
			(omc * ru.v[0] * ru.v[2] + ru.v[1] * sint) * p.v[2];

		result.v[1] = (omc * ru.v[0] * ru.v[1] + ru.v[2] * sint) * p.v[0] +
			(cost + omc * ru.v[1] * ru.v[1]) * p.v[1] +
			(omc * ru.v[1] * ru.v[2] - ru.v[0] * sint) * p.v[2];

		result.v[2] = (omc * ru.v[0] * ru.v[2] - ru.v[1] * sint) * p.v[0] +
			(omc * ru.v[1] * ru.v[2] + ru.v[0] * sint) * p.v[1] +
			(cost + omc * ru.v[2] * ru.v[2]) * p.v[2];
	}
	//	given a vector "v" and a plane defined by it's normal vector "n"
	//	produces a vector that is the projection of "v" in the plane defined
	//	by "n"
	public static V3 ArbitraryProjection(V3 v, V3 n)
	{
		V3 result = new V3();

		V3 nu = VectorOps.Unit(n);

		double nx = nu.X1(), ny = nu.X2(), nz = nu.X3();
		double vx = v.X1(), vy = v.X2(), vz = v.X3();
		double qx, qy, qz;

		qx = (1.0 - (nx * nx)) * vx;
		qx += (-nx * ny) * vy;
		qx += (-nx * nz) * vz;

		qy = (-nx * ny) * vx;
		qy += (1.0 - (ny * ny)) * vy;
		qy += (-ny * nz) * vz;

		qz = (-nx * nz) * vx;
		qz += (-ny * nz) * vy;
		qz += (1.0 - (nz * nz)) * vz;

		result.create(qx, qy, qz);

		return result;
	}
	public static void ArbitraryProjection(V3 v, V3 n, V3 vOut)
	{
		V3 nu = VectorOps.Unit(n);

		//double nx = nu.X1(), ny = nu.X2(), nz = nu.X3();

		vOut.v[0] = (1.0 - (nu.v[0] * nu.v[0])) * v.v[0];
		vOut.v[0] += (-nu.v[0] * nu.v[1]) * v.v[1];
		vOut.v[0] += (-nu.v[0] * nu.v[2]) * v.v[2];

		vOut.v[1] = (-nu.v[0] * nu.v[1]) * v.v[0];
		vOut.v[1] += (1.0 - (nu.v[1] * nu.v[1])) * v.v[1];
		vOut.v[1] += (-nu.v[1] * nu.v[2]) * v.v[2];

		vOut.v[2] = (-nu.v[0] * nu.v[2]) * v.v[0];
		vOut.v[2] += (-nu.v[1] * nu.v[2]) * v.v[1];
		vOut.v[2] += (1.0 - (nu.v[2] * nu.v[2])) * v.v[2];
	}
	public static void NormalVector(double p1[], double p2[], double p3[], double n[])
	{
		double d;//, v1[3], v2[3];

		double v1[] = new double[3];
		double v2[] = new double[3];

		v1[0] = p3[0] - p1[0];
		v1[1] = p3[1] - p1[1];
		v1[2] = p3[2] - p1[2];
		v2[0] = p3[0] - p2[0];
		v2[1] = p3[1] - p2[1];
		v2[2] = p3[2] - p2[2];

		// calculate the cross product of the two vectors
		n[0] = v1[1] * v2[2] - v2[1] * v1[2];
		n[1] = v1[2] * v2[0] - v2[2] * v1[0];
		n[2] = v1[0] * v2[1] - v2[0] * v1[1];

		// normalize the vector
		d = ( n[0] * n[0] + n[1] * n[1] + n[2] * n[2] );
		// try to catch very small vectors
		if (d < (double)0.00000001)
		{
			d = (double)100000000.0;
		}
		else
		{
			d = (double)1.0 / Math.sqrt(d);
		}

		n[0] *= d;
		n[1] *= d;
		n[2] *= d;
	}
	public static void LinearlyInterpolate(V3 v1, V3 v2, double t, V3 v3)
	{
		v3.setX1(v1.X1() + t * (v2.X1() - v1.X1()));
		v3.setX2(v1.X2() + t * (v2.X2() - v1.X2()));
		v3.setX3(v1.X3() + t * (v2.X3() - v1.X3()));
	}
	public static void Bisector(V3 v1, V3 v2, V3 bisector)
	{
		V3 v = new V3(v2.X1() - v1.X1(), v2.X2() - v1.X2(), v2.X3() - v1.X3());
		v = VectorOps.Scale(v, 0.5000);
		bisector.create(v1.X1() + v.X1(), v1.X2() + v.X2(), v1.X3() + v.X3());
	}
	public static double MiniCone(V3 inV1, V3 inV2, V3 centerVec)
	{
		double angularExtent = 0.0;

		double angle1, angle2;
		V3 u1 = new V3(inV1);
		V3 u2 = new V3(inV2);

		VectorOps.MakeUnit(u1);
		VectorOps.MakeUnit(u2);
		VectorOps.Bisector(u1, u2, centerVec);
		VectorOps.MakeUnit(centerVec);
		angle1 = VectorOps.AngularSep(centerVec, u1);
		angle2 = VectorOps.AngularSep(centerVec, u2);
		angularExtent = (angle1 > angle2) ? angle1 : angle2;

		return angularExtent;
	}
	public static boolean MiniCone(V3 inV1, V3 inV2, V3 inV3, V3 centerVec, double angularExtent)
	{
		boolean result = false;

		V3 u12, u13, b12, b13, crss, u12Perp, u13Perp;
		V3 u1 = new V3();
		V3 u2 = new V3();
		V3 u3 = new V3();
		u12 = new V3();
		u13 = new V3();
		crss = new V3();
		u12Perp = new V3();
		u13Perp = new V3();
		b12 = new V3();
		b13 = new V3();

		u1.copy(inV1);
		u2.copy(inV2);
		u3.copy(inV3);

		VectorOps.MakeUnit(u1);
		VectorOps.MakeUnit(u2);
		VectorOps.MakeUnit(u3);

		VectorOps.Subtract(u2,u1,u12);
		VectorOps.MakeUnit(u12);
		VectorOps.Subtract(u3,u1,u13);
		VectorOps.MakeUnit(u13);

		VectorOps.Cross(u12, u13, crss);
		VectorOps.MakeUnit(crss);
		VectorOps.ArbitraryRotate(u12, GeometricConstants.PIOVER2, crss, u12Perp);
		VectorOps.MakeUnit(u12Perp);
		VectorOps.ArbitraryRotate(u13, -GeometricConstants.PIOVER2, crss, u13Perp);
		VectorOps.MakeUnit(u13Perp);

		VectorOps.Bisector(u1, u2, b12);
		VectorOps.MakeUnit(b12);
		VectorOps.Bisector(u1, u3, b13);
		VectorOps.MakeUnit(b13);

		u12Perp = VectorOps.Add(b12, u12Perp);
		u13Perp = VectorOps.Add(b13, u13Perp);

		if (IntersectionTest(b12, u12Perp, b13, u13Perp, centerVec))
		{
			VectorOps.MakeUnit(centerVec);
			double angle1 = VectorOps.AngularSep(centerVec, u1);
			double angle2 = VectorOps.AngularSep(centerVec, u2);
			double angle3 = VectorOps.AngularSep(centerVec, u3);
			angularExtent = angle1;
			if (angle2 > angularExtent) angularExtent = angle2;
			if (angle3 > angularExtent) angularExtent = angle3;
			result = true;
		}

		return result;
	}
	public static boolean IntersectionTest(V3 p1, V3 end1, V3 p2, V3 end2, V3 vIntersect)
	{
		boolean result = false;

		V3 d1 = VectorOps.Subtract(end1, p1);
		V3 d2 = VectorOps.Subtract(end2, p2);

		V3 dCross = VectorOps.Cross(d1,d2);
		double dCrossMag = VectorOps.Mag(dCross);
		if (dCrossMag > GeometricConstants.EPS10)
		{
			double dsq = dCrossMag * dCrossMag;
			V3 p2Minus1 = VectorOps.Subtract(p2, p1);
			V3 p2Minus1CrossD2 = VectorOps.Cross(p2Minus1,d2);
			double t1 = VectorOps.Dot(p2Minus1CrossD2, dCross)/dsq;
			V3 p2Minus1CrossD1 = VectorOps.Cross(p2Minus1,d1);
			double t2 = VectorOps.Dot(p2Minus1CrossD1,dCross)/dsq;

			d1 = VectorOps.Scale(d1, t1);
			V3 i1 = VectorOps.Add(p1, d1);
			d2 = VectorOps.Scale(d2, t2);
			V3 i2 = VectorOps.Add(p2, d2);
			vIntersect.create(0.50*(i1.X1()+i2.X1()), 0.50*(i1.X2()+i2.X2()), 0.50*(i1.X3()+i2.X3()));
			result = true;
		}

		return result;
	}
	public static V3 multiply(double rotMat[][], V3 v)
	{
		V3 result = new V3();

		multiply(rotMat, v, result);

		return result;
	}
	public static void multiply(double rotMat[][], V3 v, V3 result)
	{
		result.setX1(rotMat[0][0]*v.X1() + rotMat[0][1]*v.X2() + rotMat[0][2]*v.X3());
		result.setX2(rotMat[1][0]*v.X1() + rotMat[1][1]*v.X2() + rotMat[1][2]*v.X3());
		result.setX3(rotMat[2][0]*v.X1() + rotMat[2][1]*v.X2() + rotMat[2][2]*v.X3());
	}
	public static double azangle(V3 v){  //azimuthal angle in degrees
		double result=Math.atan2(v.X2(),v.X1())*GeometricConstants.RA2DE;
		return result;
	}
	public static double polarangle(V3 v){  //polar angle in degrees
		double result=Math.acos(v.X3()/VectorOps.Mag(v))*GeometricConstants.RA2DE;
		return result;
	}
	public static double latangle(V3 v){  //latitude angle in degrees
		double result=90.-VectorOps.polarangle(v);
		return result;
	}

}
