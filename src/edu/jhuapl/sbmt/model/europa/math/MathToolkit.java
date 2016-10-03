package edu.jhuapl.sbmt.model.europa.math;

public class MathToolkit {
	public static double EPS3 = 0.0010;
	public static double EPS4 = 0.00010;
	public static double EPS5 = 0.000010;
	public static double EPS6 = 0.0000010;
	public static double EPS7 = 0.00000010;
	public static double EPS8 = 0.000000010;
	public static double EPS9 = 0.0000000010;
	public static double Pi(){ return 3.141592653589793;}
	public static double roundOff(double x, int places)
	{
		double factor = Math.pow(10.0, (double)places);
		return Math.floor(0.50 + x * factor)/factor;
	}
	public static double fractionalPart(double x)
	{
		return (x - Math.floor(x));
	}
	public static boolean Between(double v, double x1, double x2)
	{
		if (v > MathToolkit.Max(x1,x2)) return false;
		if (v < MathToolkit.Min(x1,x2)) return false;
		return true;
	}
	public static boolean Between(double v, double x1, double x2, double allowance)
	{
		if (v > (MathToolkit.Max(x1,x2)+allowance)) return false;
		if (v < (MathToolkit.Min(x1,x2)-allowance)) return false;
		return true;
	}
	public static boolean Between(int i, int i1, int i2)
	{
		boolean result = false;
		
		if ((i <= Math.max(i1,i2)) && (i >= Math.min(i1, i2)))
		{
			result = true;
		}
		
		return result;
	}
	public static boolean isSame(double x, double y, double tolerance)
	{
		boolean result = false;
		
		double delta = Math.abs(x - y);
		if (delta < tolerance)
			result = true;
		
		return result;
	}
	public static double Abs(double x)
	{
		return (x >= 0.0) ? x : -x;
	}
	public static int Min(int i1, int i2)
	{
		return (i1 < i2) ? i1 : i2;
	}
	public static int Max(int i1, int i2)
	{
		return (i1 > i2) ? i1 : i2;
	}
	public static int MinInt(int i1, int i2)
	{
		return (i1 < i2) ? i1 : i2;
	}
	public static int MaxInt(int i1, int i2)
	{
		return (i1 > i2) ? i1 : i2;
	}
	public static double Max(double x1, double x2)
	{
		return (x1 > x2) ? x1 : x2;
	}
	public static boolean IsMax(double x1, double x2, double x3)
	{
		if ((x1 >= x2) && (x1 >= x3))
		{
			return true;
		}
		return false;
	}
	public static boolean IsMin(double x1, double x2, double x3)
	{
		if ((x1 <= x2) && (x1 <= x3))
		{
			return true;
		}
		return false;
	}
	public static boolean IsMax(double x1, double x2, double x3, double x4)
	{
		if ((x1 >= x2) && (x1 >= x3) && (x1 >= x4))
		{
			return true;
		}
		return false;
	}
	public static boolean IsMin(double x1, double x2, double x3, double x4)
	{
		if ((x1 <= x2) && (x1 <= x3) && (x1 <= x4))
		{
			return true;
		}
		return false;
	}
	public static double MaxAbs(double x1, double x2)
	{
		return MathToolkit.Max(MathToolkit.Abs(x1), MathToolkit.Abs(x2));
	}
	public static double MinAbs(double x1, double x2)
	{
		return MathToolkit.Min(MathToolkit.Abs(x1), MathToolkit.Abs(x2));
	}
	public static double Max(double x1, double x2, double x3)
	{
		double result = x1;

		if (x2 > result)
			result = x2;
		if (x3 > result)
			result = x3;

		return result;
	}
	public static double Min(double x1, double x2, double x3)
	{
		double result = x1;

		if (x2 < result)
			result = x2;
		if (x3 < result)
			result = x3;

		return result;
	}
	public static double Min(double x1, double x2)
	{
		return (x1 < x2) ? x1 : x2;
	}
	public static double Rad2Deg(double x)
	{
		return (x * 57.295779513082325226);
	}
	public static double Deg2Rad(double x)
	{
		return (x * 0.017453292519943294);
	}
	public static double toRadians(double x){ return Deg2Rad(x);}
	public static double toDegrees(double x){ return Rad2Deg(x);}
	public static double PythagoreanTheorem2D(double a, double b)
	{
		return Math.sqrt(a*a + b*b);
	}
	public static double PythagoreanTheorem3D(double a, double b, double c)
	{
		return Math.sqrt(a*a + b*b + c*c);
	}
	public static double CircularArcLength(double radius, double thetaDeg)
	{
		return (radius * thetaDeg);
	}
	public static double PlaneLawOfCosines(double a, double b, double gammaRad)
	{
		return (a*a + b*b - 2*a*b*Math.cos(gammaRad));
	}
	//	Spherical law of cosines takes a little explaining:
	//	To picture a spherical triangle imagine a sphere with a line drawn
	//	on the surface from the north pole down to the equator at 0 degrees
	//	longitude, then another line from the north pole to the equator at
	//	45 degrees longitude, and one more line connecting the two ends at
	//	the equator. You end up with three sides and three angles. If you
	//	take the sides as lower-case a,b,c...and the angle opposite to side
	//	"a" as angle "A", the angle opposite side "b" is angle "B", and the
	//	same for side "c" and angle "C", then you have a well defined spherical
	//	triangle. This method answers the question: given sides "a" and "c"
	//	and the angle between them, calculate the arc length of side "b"
	//	A tricky, but extremely important point concerning spherical triangles:
	//	since the sides a,b,c are traced on the surface of a sphere they can be
	//	thought of as angular distances rather than ordinary lengths.
	//	arguments are in RADIANS
	public static double SphericalLawOfCosines(double a, double c, double B)
	{
		double arg = Math.sin(a)*Math.sin(c) + Math.cos(a)*Math.cos(c)*Math.cos(B);
		//	TODO: check for arg = PI/2.0
		return Math.acos(arg);
	}
	//	everything defined the same as above
	public static double SphericalLawOfSines(double a, double A)
	{
		return Math.sin(A)/Math.sin(a);
	}
	public static double ZeroTo360(double x)
	{
		double result = x;

		while (result < 0.0) result += 360.0;
		while (result > 360.0) result -= 360.0;

		return result;
	}
	public static int Sign(double x)
	{
		return (x < 0.0) ? -1 : 1;
	}
	public static boolean SameSign(double x1, double x2)
	{
		if (Sign(x1) == Sign(x2))
			return true;
		return false;
	}
	public static double SafeACos(double x)
	{
		if (x >= 1.0) return 0.0;
		if (x <= -1.0) return Pi();
		return Math.acos(x);
	}
	public static void SinCos(double x, double snx, double csx)
	{
		snx = Math.sin(x);
		csx = Math.cos(x);
	}
	public static double Sigmoid(double x)
	{
		return (1.0 / (1.0 + Math.exp(-x)));
	}
	public static int Pow2(int n)
	{
		int result = 1;

		for (int i = 1; i <= n; i++){
			result = 2 * result;
		}

		return result;
	}

	public static boolean PowerOf2(int n, int m)
	{
		boolean result = false;

		if (n == 1)
		{
			result = true;
			m = 0;
		}
		else
		{
			int i = 0;
			int p2 = 1;

			while ((n > p2) && !result){
				i++;

				p2 = MathToolkit.Pow2(i);

				if (p2 == n)
				{
					result = true;
					m = i;
				}
			}
		}

		return result;
	}

	//----------------------------------------------------------------
	//This computes an in-place complex-to-complex FFT
	//x and y are the real array of 2^m points.
	//dir =  1 gives forward transform
	//dir = -1 gives reverse transform

	// Formula: forward
	//			  N-1
	//			  ---
	//		  1   \          - j k 2 pi n / N
	//  X(n) = ---   >   x(k) e                    = forward transform
	//		  N   /                                n=0..N-1
	//			  ---
	//			  k=0
	//
	//  Formula: reverse
	//			  N-1
	//			  ---
	//			  \          j k 2 pi n / N
	//  X(n) =       >   x(k) e                    = inverse transform
	//			  /                                n=0..N-1
	//			  ---
	//			  k=0
	//
	public static int FFT(int dir,int m,double x[])
	{
		int result = 0;
		
		int nn,i,i1,j,k,i2,l,l1,l2;
		double c1,c2,tx,t1,t2,u1,u2,z;

		// Calculate the number of points
		nn = 1;
		for (i=0;i<m;i++)
			nn *= 2;

		// Do the bit reversal
		i2 = nn >> 1;
		j = 0;

		for (i=0;i<nn-1;i++){
			if (i < j)
			{
				tx = x[i];
				x[i] = x[j];
				x[j] = tx;
			}
			k = i2;

			while (k <= j){
				j -= k;
				k >>= 1;
			}

			j += k;
		}

		// Compute the FFT
		c1 = -1.0;
		c2 = 0.0;
		l2 = 1;
		for (l=0;l<m;l++){
			l1 = l2;
			l2 <<= 1;
			u1 = 1.0;
			u2 = 0.0;

			for (j=0;j<l1;j++){
				for (i=j;i<nn;i+=l2){
					i1 = i + l1;
					t1 = u1 * x[i1];
					t2 = u2 * x[i1];
					x[i1] = x[i] - t1;
					x[i] += t1;
				}

				z =  u1 * c1 - u2 * c2;
				u2 = u1 * c2 + u2 * c1;
				u1 = z;
			}

			c2 = Math.sqrt((1.0 - c1) / 2.0);
			if (dir == 1)
				c2 = -c2;

			c1 = Math.sqrt((1.0 + c1) / 2.0);
		}

		// Scaling for forward transform
		if (dir == 1)
		{
			for (i=0;i<nn;i++){
				x[i] /= (double)nn;
			}
		}
		
		result = 1;

		return result;
	}
}
