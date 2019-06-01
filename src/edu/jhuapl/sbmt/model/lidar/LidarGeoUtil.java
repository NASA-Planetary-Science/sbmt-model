package edu.jhuapl.sbmt.model.lidar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialFitter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;

import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Point3D;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.util.TimeUtil;
import edu.jhuapl.sbmt.util.gravity.Gravity;

/**
 * Collection of geometric utility methods useful for working with lidar data.
 * <P>
 * Most of the methods in this class originated from the object (prior to
 * 2019May24) edu.jhuapl.smbt.model.lidar.LidarSearchDataCollection. The naming
 * of that class was not intuitive. That class also violated the separation of
 * principles. That class has since been refactored - part of which is here.
 *
 * @author lopeznr1
 */
public class LidarGeoUtil
{
	/**
	 * Run gravity program on specified track and return potential, acceleration,
	 * and elevation as function of distance and time.
	 *
	 * @param aTrack
	 * @throws Exception
	 */
	public static void getGravityDataForTrack(LidarTrackManager aManager, LidarTrack aTrack, List<Double> aPotentialL,
			List<Double> aAccelerationL, List<Double> aElevationL, List<Double> aDistanceL, List<Double> aTimeL)
			throws Exception
	{
		if (aTrack.getNumberOfPoints() <= 0)
			throw new IOException();

		// Run the gravity program
		Vector3D tmpVect = aManager.getTranslation(aTrack);
		List<double[]> xyzPointList = new ArrayList<double[]>();
		for (LidarPoint aLP : aTrack.getPointList())
		{
			double[] target = aLP.getTargetPosition().toArray();
			target = aManager.transformLidarPoint(tmpVect, target);
			xyzPointList.add(target);
		}

		PolyhedralModel tmpSmallBodyModel = aManager.refSmallBodyModel;
		List<Point3D> accelerationVector = new ArrayList<Point3D>();
		Gravity.getGravityAtPoints(xyzPointList, tmpSmallBodyModel.getDensity(), tmpSmallBodyModel.getRotationRate(),
				tmpSmallBodyModel.getReferencePotential(), tmpSmallBodyModel.getSmallBodyPolyData(), aElevationL,
				aAccelerationL, accelerationVector, aPotentialL);

		double[] fittedLinePoint = new double[3];
		double[] fittedLineDirection = new double[3];
		fitLineToTrack(aManager, aTrack, fittedLinePoint, fittedLineDirection);
		for (LidarPoint aLP : aTrack.getPointList())
		{
			double[] point = aLP.getTargetPosition().toArray();
			point = aManager.transformLidarPoint(tmpVect, point);
			double dist = distanceOfClosestPointOnLineToStartOfLine(point, aTrack, fittedLinePoint, fittedLineDirection);
			aDistanceL.add(dist);
			aTimeL.add(aLP.getTime());
		}
	}

	// TODO: Add javadoc
	public static double getOffsetScale(LidarTrackManager aTrackManager)
	{
		PolyhedralModel tmpSmallBodyModel = aTrackManager.refSmallBodyModel;

		BodyViewConfig tmpBodyViewConfig = (BodyViewConfig) tmpSmallBodyModel.getConfig();
		if (tmpBodyViewConfig.lidarOffsetScale <= 0.0)
			return tmpSmallBodyModel.getBoundingBoxDiagonalLength() / 1546.4224133453388;

		return tmpBodyViewConfig.lidarOffsetScale;
	}

	/**
	 * This function takes a lidar track, fits a plane through it and reorients
	 * the track into a new coordinate system such that the fitted plane is the
	 * XY plane in the new coordinate system, and saves the reoriented track and
	 * the transformation to a file.
	 *
	 * @param aTrack The Track of interest.
	 * @param outfile - save reoriented track to this file
	 * @param rotationMatrixFile - save transformation matrix to this file
	 * @throws IOException
	 */
	public static void reprojectedTrackOntoFittedPlane(LidarTrackManager aManager, LidarTrack aTrack, File outfile,
			File rotationMatrixFile) throws IOException
	{
		// Need at least 2 points to define a plane
		if (aTrack.getNumberOfPoints() <= 1)
			return;

		double[] pointOnPlane = new double[3];
		RealMatrix planeOrientation = new Array2DRowRealMatrix(3, 3);

		fitPlaneToTrack(aManager, aTrack, pointOnPlane, planeOrientation);
		planeOrientation = new LUDecomposition(planeOrientation).getSolver().getInverse();

		FileWriter fstream = new FileWriter(outfile);
		BufferedWriter out = new BufferedWriter(fstream);

		String newline = System.getProperty("line.separator");

		Vector3D tmpVect = aManager.getTranslation(aTrack);
		for (LidarPoint aLP : aTrack.getPointList())
		{
			double[] target = aManager.transformLidarPoint(tmpVect, aLP.getTargetPosition().toArray());

			target[0] = target[0] - pointOnPlane[0];
			target[1] = target[1] - pointOnPlane[1];
			target[2] = target[2] - pointOnPlane[2];

			target = planeOrientation.operate(target);

			out.write(TimeUtil.et2str(aLP.getTime()) + " " + target[0] + " " + target[1] + " " + target[2] + newline);
		}

		out.close();

		// Also save out the orientation matrix.
		fstream = new FileWriter(rotationMatrixFile);
		out = new BufferedWriter(fstream);

		double[][] data = planeOrientation.getData();
		out.write(data[0][0] + " " + data[0][1] + " " + data[0][2] + " " + pointOnPlane[0] + "\n");
		out.write(data[1][0] + " " + data[1][1] + " " + data[1][2] + " " + pointOnPlane[1] + "\n");
		out.write(data[2][0] + " " + data[2][1] + " " + data[2][2] + " " + pointOnPlane[2] + "\n");

		out.close();
	}

	// TODO: Add javadoc
	private static double distanceOfClosestPointOnLineToStartOfLine(double[] point, LidarTrack aTrack,
			double[] fittedLinePoint, double[] fittedLineDirection)
	{
		if (aTrack.getNumberOfPoints() <= 1)
			return 0.0;

		double[] pnear = new double[3];
		double[] dist = new double[1];
		MathUtil.nplnpt(fittedLinePoint, fittedLineDirection, point, pnear, dist);

		return MathUtil.distanceBetween(pnear, fittedLinePoint);
	}

	/**
	 * It is useful to fit a line to the track. The following function computes
	 * the parameters of such a line, namely, a point on the line and a vector
	 * pointing in the direction of the line. Note that the returned
	 * fittedLinePoint is the point on the line closest to the first point of the
	 * track.
	 */
	private static void fitLineToTrack(LidarTrackManager aManager, LidarTrack aTrack, double[] fittedLinePoint,
			double[] fittedLineDirection)
	{
		// Need at least 2 points to define a line
		if (aTrack.getNumberOfPoints() <= 1)
			return;

		Vector3D tmpVect = aManager.getTranslation(aTrack);
		try
		{
			LidarPoint firstPt = aTrack.getPointList().get(0);
			double t0 = firstPt.getTime();

			double[] lineStartPoint = new double[3];
			for (int j = 0; j < 3; ++j)
			{
				PolynomialFitter fitter = new PolynomialFitter(new LevenbergMarquardtOptimizer());
				for (LidarPoint aLP : aTrack.getPointList())
				{
					double[] target = aManager.transformLidarPoint(tmpVect, aLP.getTargetPosition().toArray());
					fitter.addObservedPoint(1.0, aLP.getTime() - t0, target[j]);
				}

				PolynomialFunction fitted = new PolynomialFunction(fitter.fit(new double[2]));
				fittedLineDirection[j] = fitted.getCoefficients()[1];
				lineStartPoint[j] = fitted.value(0.0);
			}
			MathUtil.vhat(fittedLineDirection, fittedLineDirection);

			// Set the fittedLinePoint to the point on the line closest to first
			// track point as this makes it easier to do distance computations
			// along the line.
			double[] dist = new double[1];
			double[] target = aManager.transformLidarPoint(tmpVect, firstPt.getTargetPosition().toArray());
			MathUtil.nplnpt(lineStartPoint, fittedLineDirection, target, fittedLinePoint, dist);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * It is useful to fit a plane to the track. The following function computes
	 * the parameters of such a plane, namely, a point on the plane and a vector
	 * pointing in the normal direction of the plane. Note that the returned
	 * pointOnPlane is the point on the plane closest to the centroid of the
	 * track.
	 */
	private static void fitPlaneToTrack(LidarTrackManager aManager, LidarTrack aTrack, double[] pointOnPlane,
			RealMatrix planeOrientation)
	{
		// Need at least 2 points to define a plane
		int numPts = aTrack.getNumberOfPoints();
		if (numPts <= 1)
			return;

		Vector3D tmpVect = aManager.getTranslation(aTrack);
		try
		{
			double[] centroid = getCentroidOfTrack(aManager, aTrack);

			// subtract out the centroid from the track
			double[][] points = new double[3][numPts];
			int tmpIdx = 0;
			for (LidarPoint aLP : aTrack.getPointList())
			{
				double[] target = aManager.transformLidarPoint(tmpVect, aLP.getTargetPosition().toArray());
				points[0][tmpIdx] = target[0] - centroid[0];
				points[1][tmpIdx] = target[1] - centroid[1];
				points[2][tmpIdx] = target[2] - centroid[2];
				tmpIdx++;
			}
			RealMatrix pointMatrix = new Array2DRowRealMatrix(points, false);

			// Now do SVD on this matrix
			SingularValueDecomposition svd = new SingularValueDecomposition(pointMatrix);
			RealMatrix u = svd.getU();

			planeOrientation.setSubMatrix(u.copy().getData(), 0, 0);

			pointOnPlane[0] = centroid[0];
			pointOnPlane[1] = centroid[1];
			pointOnPlane[2] = centroid[2];
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// TODO: Add javadoc
	private static double[] getCentroidOfTrack(LidarTrackManager aManager, LidarTrack aTrack)
	{
		Vector3D tmpVect = aManager.getTranslation(aTrack);

		double[] centroid = { 0.0, 0.0, 0.0 };
		for (LidarPoint aLP : aTrack.getPointList())
		{
			double[] target = aManager.transformLidarPoint(tmpVect, aLP.getTargetPosition().toArray());
			centroid[0] += target[0];
			centroid[1] += target[1];
			centroid[2] += target[2];
		}

		int trackSize = aTrack.getNumberOfPoints();
		if (trackSize > 0)
		{
			centroid[0] /= trackSize;
			centroid[1] /= trackSize;
			centroid[2] /= trackSize;
		}

		return centroid;
	}

}
