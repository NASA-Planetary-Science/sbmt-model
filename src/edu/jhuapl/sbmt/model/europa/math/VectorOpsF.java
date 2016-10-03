package edu.jhuapl.sbmt.model.europa.math;

import edu.jhuapl.sbmt.model.europa.projection.GeometricConstants;

public class VectorOpsF {
	public static boolean Aligned(V3F v1, V3F v2)
	{
		V3F v1u = VectorOpsF.Unit(v1);
		V3F v2u = VectorOpsF.Unit(v2);
		V3F vSum = VectorOpsF.Add(v1u, v2u);
		float m = VectorOpsF.Mag(vSum);

		if (MathToolkit.Between(m, 2.0 - GeometricConstants.EPS10, 2.0 + GeometricConstants.EPS10))
			return true;
		return false;
	}
	public static boolean RoughlyAligned(V3F v1, V3F v2, float eps)
	{
		V3F v1u = VectorOpsF.Unit(v1);
		V3F v2u = VectorOpsF.Unit(v2);
		V3F vSum = VectorOpsF.Add(v1u, v2u);
		float m = VectorOpsF.Mag(vSum);

		if (MathToolkit.Between(m, 2.0 - eps, 2.0 + eps))
			return true;
		return false;
	}
	public static V3F Subtract(V3F v1, V3F v2)
	{
		V3F result = new V3F();

		result.create(v1.X1() - v2.X1(), v1.X2() - v2.X2(), v1.X3() - v2.X3());

		return result;
	}
	public static void Subtract(V3F v1, V3F v2, V3F result)
	{
		result.create(v1.X1() - v2.X1(), v1.X2() - v2.X2(), v1.X3() - v2.X3());
	}
	public static V3F Add(V3F v1, V3F v2)
	{
		V3F result = new V3F();

		result.create(v1.X1() + v2.X1(), v1.X2() + v2.X2(), v1.X3() + v2.X3());

		return result;
	}
	public static V3F Scale(V3F v, float scaleFactor)
	{
		V3F result = new V3F();

		result.setX1(v.X1()*scaleFactor);
		result.setX2(v.X2()*scaleFactor);
		result.setX3(v.X3()*scaleFactor);

		return result;
	}
	public static void ChangeScale(V3F result, float scaleFactor)
	{
		result.setX1(result.X1()*scaleFactor);
		result.setX2(result.X2()*scaleFactor);
		result.setX3(result.X3()*scaleFactor);
	}
	public static V3F Cross(V3F v1, V3F v2)
	{
		V3F result = new V3F();

		result.setX1(v1.get(1) * v2.get(2) - v1.get(2) * v2.get(1));
		result.setX2(v1.get(2) * v2.get(0) - v1.get(0) * v2.get(2));
		result.setX3(v1.get(0) * v2.get(1) - v1.get(1) * v2.get(0));

		return result;
	}
	public static void Cross(V3F v1, V3F v2, V3F result)
	{
		result.v[0] = (v1.v[1] * v2.v[2] - v1.v[2] * v2.v[1]);
		result.v[1] = (v1.v[2] * v2.v[0] - v1.v[0] * v2.v[2]);
		result.v[2] = (v1.v[0] * v2.v[1] - v1.v[1] * v2.v[0]);
	}
	public static float Mag(V3F v)
	{
		return (float)Math.sqrt(v.X1()*v.X1() + v.X2()*v.X2() + v.X3()*v.X3());
	}
	public static float MagSquared(V3F v)
	{
		return (v.X1()*v.X1() + v.X2()*v.X2() + v.X3()*v.X3());
	}
	public static float Dot(V3F v1, V3F v2)
	{
		return v1.X1()*v2.X1() + v1.X2()*v2.X2() + v1.X3()*v2.X3();
	}
	public static float UnitDot(V3F v1, V3F v2)
	{
		V3F v1u = VectorOpsF.Unit(v1);
		V3F v2u = VectorOpsF.Unit(v2);

		return (v1u.X1()*v2u.X1() + v1u.X2()*v2u.X2() + v1u.X3()*v2u.X3());
	}
	public static V3F Unit(V3F v)
	{
		V3F result = new V3F();

		float m = VectorOpsF.Mag(v);
		if (m > 0.0)
		{
			result.create(v.X1()/m, v.X2()/m, v.X3()/m);
		}

		return result;
	}
	public static void MakeUnit(V3F v)
	{
		float m = VectorOpsF.Mag(v);
		if (m > 0.0)
		{
			v.create(v.X1()/m, v.X2()/m, v.X3()/m);
		}
	}
	public static void Unit(V3F vIn, V3F vOut)
	{
		float m = 1.0f/VectorOpsF.Mag(vIn);
		if (m > 0.0)
		{
			vOut.v[0] = vIn.v[0]*m;
			vOut.v[1] = vIn.v[1]*m;
			vOut.v[2] = vIn.v[2]*m;
		}
	}
	public static float AngularSepNoChecking(V3F v1, V3F v2)
	{
		float result = 0.0f;

		V3F vtemp;
		vtemp = new V3F();

		if ( VectorOpsF.Dot(v1,v2) > 0.0 )
		{
			vtemp.setX1(v1.v[0] - v2.v[0]);
			vtemp.setX2(v1.v[1] - v2.v[1]);
			vtemp.setX3(v1.v[2] - v2.v[2]);

			result = (float)(2.00 * Math.asin (0.50 * VectorOpsF.Mag(vtemp)));
		}
		else if ( VectorOpsF.Dot(v1,v2) < 0.0 )
		{
			vtemp.setX1(v1.v[0] + v2.v[0]);
			vtemp.setX2(v1.v[1] + v2.v[1]);
			vtemp.setX3(v1.v[2] + v2.v[2]);

			result = (float)(GeometricConstants.PI - 2.00 * Math.asin (0.50 * VectorOpsF.Mag(vtemp)));
		}
		else
		{
			result = (float)GeometricConstants.PIOVER2;
		}

		return result;
	}
	public static float AngularSep(V3F v1, V3F v2)
	{
		float result = 0.0f;

		V3F u1, u2, vtemp;
		u1 = new V3F();
		u2 = new V3F();
		vtemp = new V3F();
		float dmag1, dmag2;

		VectorOpsF.Unit(v1, u1);
		dmag1 = VectorOpsF.Mag(v1);

		if ( dmag1 == 0.0 )
		{
			return result;
		}

		VectorOpsF.Unit(v2, u2);
		dmag2 = VectorOpsF.Mag(v2);

		if ( dmag2 == 0.0 )
		{
			return result;
		}

		if ( VectorOpsF.Dot(u1,u2) > 0.0 )
		{
			vtemp.setX1(u1.v[0] - u2.v[0]);
			vtemp.setX2(u1.v[1] - u2.v[1]);
			vtemp.setX3(u1.v[2] - u2.v[2]);

			result = (float)(2.00 * Math.asin (0.50 * VectorOpsF.Mag(vtemp)));
		}
		else if ( VectorOpsF.Dot(u1,u2) < 0.0 )
		{
			vtemp.setX1(u1.v[0] + u2.v[0]);
			vtemp.setX2(u1.v[1] + u2.v[1]);
			vtemp.setX3(u1.v[2] + u2.v[2]);

			result = (float)(GeometricConstants.PI - 2.00 * Math.asin (0.50 * VectorOpsF.Mag(vtemp)));
		}
		else
		{
			result = (float)(GeometricConstants.PIOVER2);
		}


		return result;
	}
	public static V3F Negative(V3F v)
	{
		V3F result = new V3F(v);

		result.setX1(-result.X1());
		result.setX2(-result.X2());
		result.setX3(-result.X3());

		return result;
	}
	public static V3F XRotation(float angleRad, V3F v)
	{
		V3F result = new V3F();

		result.setX1(v.X1());
		result.setX2((float)Math.cos(angleRad)*v.X2() +(float)Math.sin(angleRad)*v.X3());
		result.setX3((float)-Math.sin(angleRad)*v.X2() + (float)Math.cos(angleRad)*v.X3());

		return result;
	}
	public static V3F YRotation(float angleRad, V3F v)
	{
		V3F result = new V3F();

		result.setX1((float)Math.cos(angleRad)*v.X1() - (float)Math.sin(angleRad)*v.X3());
		result.setX2(v.X2());
		result.setX3((float)Math.sin(angleRad)*v.X1() + (float)Math.cos(angleRad)*v.X3());

		return result;
	}
	public static V3F ZRotation(float angleRad, V3F v)
	{
		V3F result = new V3F();

		result.setX1((float)Math.cos(angleRad)*v.X1() + (float)Math.sin(angleRad)*v.X2());
		result.setX2((float)-Math.sin(angleRad)*v.X1() + (float)Math.cos(angleRad)*v.X2());
		result.setX3(v.X3());

		return result;
	}
	public static V3F ArbitraryRotate(V3F p, float thetaRad, V3F r)
	{
		V3F result = new V3F();

		float cost, sint, omc;
		V3F ru = VectorOpsF.Unit(r);
		float rx = ru.v[0], ry = ru.v[1], rz = ru.v[2];
		float px = p.v[0], py = p.v[1], pz = p.v[2];
		//float qx, qy, qz;

		cost = (float)Math.cos(thetaRad);
		sint =(float) Math.sin(thetaRad);
		omc = 1.0f - cost;

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
	public static void ArbitraryRotate(V3F p, float thetaRad, V3F r, V3F result)
	{
		float cost, sint, omc;
		V3F ru = new V3F();
		VectorOpsF.Unit(r, ru);
		//float rx = ru.m_v[0], ry = ru.m_v[1], rz = ru.m_v[2];
		//float px = p.m_v[0], py = p.m_v[1], pz = p.m_v[2];
		//float qx, qy, qz;

		cost = (float)Math.cos(thetaRad);
		sint = (float)Math.sin(thetaRad);
		omc = (float)(1.0 - cost);

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
	public static V3F ArbitraryProjection(V3F v, V3F n)
	{
		V3F result = new V3F();

		V3F nu = VectorOpsF.Unit(n);

		float nx = nu.X1(), ny = nu.X2(), nz = nu.X3();
		float vx = v.X1(), vy = v.X2(), vz = v.X3();
		float qx, qy, qz;

		qx = (1.0f - (nx * nx)) * vx;
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
	public static void ArbitraryProjection(V3F v, V3F n, V3F vOut)
	{
		V3F nu = VectorOpsF.Unit(n);

		//float nx = nu.X1(), ny = nu.X2(), nz = nu.X3();

		vOut.v[0] = (1.0f - (nu.v[0] * nu.v[0])) * v.v[0];
		vOut.v[0] += (-nu.v[0] * nu.v[1]) * v.v[1];
		vOut.v[0] += (-nu.v[0] * nu.v[2]) * v.v[2];

		vOut.v[1] = (-nu.v[0] * nu.v[1]) * v.v[0];
		vOut.v[1] += (1.0f - (nu.v[1] * nu.v[1])) * v.v[1];
		vOut.v[1] += (-nu.v[1] * nu.v[2]) * v.v[2];

		vOut.v[2] = (-nu.v[0] * nu.v[2]) * v.v[0];
		vOut.v[2] += (-nu.v[1] * nu.v[2]) * v.v[1];
		vOut.v[2] += (1.0f - (nu.v[2] * nu.v[2])) * v.v[2];
	}
	public static void NormalVector(float p1[], float p2[], float p3[], float n[])
	{
		float d;//, v1[3], v2[3];

		float v1[] = new float[3];
		float v2[] = new float[3];

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
		if (d < (float)0.00000001)
		{
			d = (float)100000000.0;
		}
		else
		{
			d = (float)(1.0 / Math.sqrt(d));
		}

		n[0] *= d;
		n[1] *= d;
		n[2] *= d;
	}
	public static void LinearlyInterpolate(V3F v1, V3F v2, float t, V3F v3)
	{
		v3.setX1(v1.X1() + t * (v2.X1() - v1.X1()));
		v3.setX2(v1.X2() + t * (v2.X2() - v1.X2()));
		v3.setX3(v1.X3() + t * (v2.X3() - v1.X3()));
	}
	public static void Bisector(V3F v1, V3F v2, V3F bisector)
	{
		V3F v = new V3F(v2.X1() - v1.X1(), v2.X2() - v1.X2(), v2.X3() - v1.X3());
		v = VectorOpsF.Scale(v, 0.5000f);
		bisector.create(v1.X1() + v.X1(), v1.X2() + v.X2(), v1.X3() + v.X3());
	}
	public static float MiniCone(V3F inV1, V3F inV2, V3F centerVec)
	{
		float angularExtent = 0.0f;

		float angle1, angle2;
		V3F u1 = new V3F(inV1);
		V3F u2 = new V3F(inV2);

		VectorOpsF.MakeUnit(u1);
		VectorOpsF.MakeUnit(u2);
		VectorOpsF.Bisector(u1, u2, centerVec);
		VectorOpsF.MakeUnit(centerVec);
		angle1 = VectorOpsF.AngularSep(centerVec, u1);
		angle2 = VectorOpsF.AngularSep(centerVec, u2);
		angularExtent = (angle1 > angle2) ? angle1 : angle2;

		return angularExtent;
	}
	public static boolean MiniCone(V3F inV1, V3F inV2, V3F inV3F, V3F centerVec, float angularExtent)
	{
		boolean result = false;

		V3F u12, u13, b12, b13, crss, u12Perp, u13Perp;
		V3F u1 = new V3F();
		V3F u2 = new V3F();
		V3F u3 = new V3F();
		u12 = new V3F();
		u13 = new V3F();
		crss = new V3F();
		u12Perp = new V3F();
		u13Perp = new V3F();
		b12 = new V3F();
		b13 = new V3F();

		u1.copy(inV1);
		u2.copy(inV2);
		u3.copy(inV3F);

		VectorOpsF.MakeUnit(u1);
		VectorOpsF.MakeUnit(u2);
		VectorOpsF.MakeUnit(u3);

		VectorOpsF.Subtract(u2,u1,u12);
		VectorOpsF.MakeUnit(u12);
		VectorOpsF.Subtract(u3,u1,u13);
		VectorOpsF.MakeUnit(u13);

		VectorOpsF.Cross(u12, u13, crss);
		VectorOpsF.MakeUnit(crss);
		VectorOpsF.ArbitraryRotate(u12, (float)GeometricConstants.PIOVER2, crss, u12Perp);
		VectorOpsF.MakeUnit(u12Perp);
		VectorOpsF.ArbitraryRotate(u13, (float)(-GeometricConstants.PIOVER2), crss, u13Perp);
		VectorOpsF.MakeUnit(u13Perp);

		VectorOpsF.Bisector(u1, u2, b12);
		VectorOpsF.MakeUnit(b12);
		VectorOpsF.Bisector(u1, u3, b13);
		VectorOpsF.MakeUnit(b13);

		u12Perp = VectorOpsF.Add(b12, u12Perp);
		u13Perp = VectorOpsF.Add(b13, u13Perp);

		if (IntersectionTest(b12, u12Perp, b13, u13Perp, centerVec))
		{
			VectorOpsF.MakeUnit(centerVec);
			float angle1 = VectorOpsF.AngularSep(centerVec, u1);
			float angle2 = VectorOpsF.AngularSep(centerVec, u2);
			float angle3 = VectorOpsF.AngularSep(centerVec, u3);
			angularExtent = angle1;
			if (angle2 > angularExtent) angularExtent = angle2;
			if (angle3 > angularExtent) angularExtent = angle3;
			result = true;
		}

		return result;
	}
	public static boolean IntersectionTest(V3F p1, V3F end1, V3F p2, V3F end2, V3F vIntersect)
	{
		boolean result = false;

		V3F d1 = VectorOpsF.Subtract(end1, p1);
		V3F d2 = VectorOpsF.Subtract(end2, p2);

		V3F dCross = VectorOpsF.Cross(d1,d2);
		float dCrossMag = VectorOpsF.Mag(dCross);
		if (dCrossMag > GeometricConstants.EPS10)
		{
			float dsq = dCrossMag * dCrossMag;
			V3F p2Minus1 = VectorOpsF.Subtract(p2, p1);
			V3F p2Minus1CrossD2 = VectorOpsF.Cross(p2Minus1,d2);
			float t1 = VectorOpsF.Dot(p2Minus1CrossD2, dCross)/dsq;
			V3F p2Minus1CrossD1 = VectorOpsF.Cross(p2Minus1,d1);
			float t2 = VectorOpsF.Dot(p2Minus1CrossD1,dCross)/dsq;

			d1 = VectorOpsF.Scale(d1, t1);
			V3F i1 = VectorOpsF.Add(p1, d1);
			d2 = VectorOpsF.Scale(d2, t2);
			V3F i2 = VectorOpsF.Add(p2, d2);
			vIntersect.create(0.50f*(i1.X1()+i2.X1()), 0.50f*(i1.X2()+i2.X2()), 0.50f*(i1.X3()+i2.X3()));
			result = true;
		}

		return result;
	}
}
