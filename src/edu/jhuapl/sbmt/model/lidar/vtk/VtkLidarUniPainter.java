package edu.jhuapl.sbmt.model.lidar.vtk;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkGeometryFilter;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.sbmt.gui.lidar.color.ColorProvider;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.model.lidar.LidarGeoUtil;
import edu.jhuapl.sbmt.model.lidar.LidarManager;
import edu.jhuapl.sbmt.model.lidar.feature.ConstFeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureAttr;
import edu.jhuapl.sbmt.model.lidar.feature.FeatureType;
import edu.jhuapl.sbmt.util.TimeUtil;

/**
 * Class used to render a single lidar data object and the corresponding points
 * via the VTK framework.
 * <P>
 * This class supports the following configurable state:
 * <UL>
 * <LI>Source / Target ColorProviders
 * <LI>Translation vector
 * <LI>Point size
 * <LI>Range of start / stop percent (TODO: Not intuitive)
 * </UL>
 *
 * @author lopeznr1
 */
public class VtkLidarUniPainter<G1> implements VtkLidarPainter<G1>
{
	// Reference vars
	private final LidarManager<G1> refManager;
	private final G1 refItem;

	// Attributes
	private final FeatureAttr timeFA;
	private final FeatureAttr rangeFA;
	private final FeatureAttr intensityFA;

	// Cache vars
	private ColorProvider cSourceCP;
	private ColorProvider cTargetCP;
	private Vector3D cTranslationV;
	private double cRadialOffset;

	// State vars
	private double percentBeg;
	private double percentEnd;
	private boolean isHighlightSelection;

	// VTK vars
	private vtkPolyData vSourcePD;
	private vtkPolyData vTargetPD;
	private vtkGeometryFilter vSourceGF;
	private vtkGeometryFilter vTargetGF;
	private vtkPoints vSourceP;
	private vtkPoints vTargetP;
	private vtkCellArray vSourceCA;
	private vtkCellArray vTargetCA;
	private vtkUnsignedCharArray vColorUCA;
	private vtkActor vSourceA;
	private vtkActor vTargetA;

	/**
	 * Standard Constructor
	 *
	 * @param aManager The LidarManager responsible for the specified lidar item.
	 * @param aItem The lidar item of interest.
	 * @param aVLS The {@link VtkLidarStruct} corresponding to the specified
	 * lidar item.
	 * <P>
	 * The contents of the {@link VtkLidarStruct} will be managed by this
	 * painter. Do not keep or pass on references of the contents of the
	 * {@link VtkLidarStruct}.
	 */
	public VtkLidarUniPainter(LidarManager<G1> aManager, G1 aItem, VtkLidarStruct aVLS)
	{
		refManager = aManager;
		refItem = aItem;

		timeFA = aVLS.timeFA;
		rangeFA = aVLS.rangeFA;
		intensityFA = aVLS.intensityFA;

		cSourceCP = null;
		cTargetCP = null;
		cTranslationV = Vector3D.ZERO;
		cRadialOffset = 0.0;

		percentBeg = 0.0;
		percentEnd = 1.0;
		isHighlightSelection = false;

		doInitialVtkSetup(aVLS.vSrcP, aVLS.vSrcCA, aVLS.vTgtP, aVLS.vTgtCA);
	}

	@Override
	public String getDisplayInfoStr(int aCellId, String aTitle)
	{
		aCellId = vTargetGF.GetPointMinimum() + aCellId;

		String headStr = "Lidar point";
		if (aTitle != null)
			headStr = "Lidar point " + aTitle;

		double timeVal = timeFA.getValAt(aCellId);
		String timeStr = TimeUtil.et2str(timeVal);

		double rangeVal = rangeFA.getValAt(aCellId) * 1000;

		return String.format("%s acquired at %s, ET = %f, unmodified range = %f m", headStr, timeStr, timeVal, rangeVal);
	}

	@Override
	public FeatureAttr getFeatureAttrFor(FeatureType aFeatureType)
	{
		if (aFeatureType == FeatureType.Time)
			return timeFA;
		else if (aFeatureType == FeatureType.Range)
			return rangeFA;
		else if (aFeatureType == FeatureType.Intensity)
			return intensityFA;
		else if (aFeatureType == null)
			return new ConstFeatureAttr(getNumberOfPoints(), 0.0, 1.0, 0.5);

		throw new RuntimeException("FeatureType is not supported: " + aFeatureType);
	}

	@Override
	public G1 getLidarItemForCell(int aCellId)
	{
		return refItem;
	}

	@Override
	public LidarPoint getLidarPointForCell(int aCellId)
	{
		return refManager.getLidarPointAt(refItem, aCellId);
	}

	@Override
	public List<vtkProp> getProps()
	{
		ArrayList<vtkProp> retL = new ArrayList<>();
		retL.add(vTargetA);
		if (vSourceA.GetVisibility() != 0)
			retL.add(vSourceA);

		return retL;
	}

	@Override
	public void setHighlightSelection(boolean aBool)
	{
		isHighlightSelection = aBool;
	}

	@Override
	public void setPercentageShown(double aPercentBeg, double aPercentEnd)
	{
		percentBeg = aPercentBeg;
		percentEnd = aPercentEnd;

		int numberOfPoints = getNumberOfPoints();
		int firstPointId = (int) (numberOfPoints * percentBeg);
		int lastPointId = (int) (numberOfPoints * percentEnd) - 1;
		if (lastPointId < firstPointId)
			lastPointId = firstPointId;

		vTargetGF.SetPointMinimum(firstPointId);
		vTargetGF.SetPointMaximum(lastPointId);
		vTargetGF.Update();

		vSourceGF.SetPointMinimum(firstPointId);
		vSourceGF.SetPointMaximum(lastPointId);
		vSourceGF.Update();
	}

	@Override
	public void setPointSize(double aPointSize)
	{
		double tmpPointSize = aPointSize;
		if (isHighlightSelection == true && refManager.getSelectedItems().contains(refItem) == true)
			tmpPointSize = tmpPointSize * 2;

		// Bail if no change to point size
		if (vSourceA.GetProperty().GetPointSize() == tmpPointSize)
			return;

		vSourceA.GetProperty().SetPointSize(tmpPointSize);
		vTargetA.GetProperty().SetPointSize(tmpPointSize);

		vSourceA.Modified();
		vTargetA.Modified();
	}

	@Override
	public void setShowSourcePoints(boolean aBool)
	{
		if (vSourceA != null)
			vSourceA.SetVisibility(aBool ? 1 : 0);
	}

	@Override
	public void vtkDispose()
	{
		timeFA.dispose();
		rangeFA.dispose();
		intensityFA.dispose();

		vSourcePD.Delete();
		vTargetPD.Delete();
		vSourceGF.Delete();
		vTargetGF.Delete();
		vSourceP.Delete();
		vTargetP.Delete();
		vSourceCA.Delete();
		vTargetCA.Delete();
		vColorUCA.Delete();

		vSourceA.Delete();
		vTargetA.Delete();
	}

	@Override
	public void vtkUpdateState()
	{
		// Update color state
		ColorProvider tmpSourceCP = refManager.getColorProviderSource(refItem);
		ColorProvider tmpTargetCP = refManager.getColorProviderTarget(refItem);

		boolean isStaleColor = false;
		isStaleColor |= Objects.equals(cSourceCP, tmpSourceCP) == false;
		isStaleColor |= Objects.equals(cTargetCP, tmpTargetCP) == false;
		if (isStaleColor == true)
			doUpdateStateColor(tmpSourceCP, tmpTargetCP);

		// Update translation state
		Vector3D tmpTranslationV = refManager.getTranslation(refItem);
		double tmpRadialOffset = refManager.getRadialOffset();

		boolean isStaleTranslation = Objects.equals(cTranslationV, tmpTranslationV) == false;
		isStaleTranslation |= cRadialOffset != tmpRadialOffset;
		if (isStaleTranslation == true)
			doUpdateStateTranslation(tmpTranslationV, tmpRadialOffset);
	}

	/**
	 * Helper method that performs the initial VTK setup of source/target points
	 * and colors.
	 */
	private void doInitialVtkSetup(vtkPoints aSrcP, vtkCellArray aSrcCA, vtkPoints aTgtP, vtkCellArray aTgtCA)
	{
		// VTK vars
		vSourceP = aSrcP;
		vSourceCA = aSrcCA;
		vSourcePD = new vtkPolyData();
		vSourcePD.SetPoints(vSourceP);
		vSourcePD.SetVerts(vSourceCA);

		vTargetP = aTgtP;
		vTargetCA = aTgtCA;
		vTargetPD = new vtkPolyData();
		vTargetPD.SetPoints(vTargetP);
		vTargetPD.SetVerts(vTargetCA);
		vColorUCA = new vtkUnsignedCharArray();
		vColorUCA.SetNumberOfComponents(4);
		vTargetPD.GetCellData().SetScalars(vColorUCA);

		// Cache vars
		cSourceCP = refManager.getColorProviderSource(refItem);
		cTargetCP = refManager.getColorProviderTarget(refItem);

		// Instantiate VTK vars (associated with the source points)
		int numPts = getNumberOfPoints();
		vSourceGF = VtkUtil.formGeometryFilter(vSourcePD, numPts);

		vtkPolyDataMapper pointsMapperSource = new vtkPolyDataMapper();
		pointsMapperSource.SetInputConnection(vSourceGF.GetOutputPort());

		vSourceA = new SaavtkLODActor();
		vSourceA.SetMapper(pointsMapperSource);
		((SaavtkLODActor) vSourceA).setQuadricDecimatedLODMapper(vSourceGF.GetOutputPort());

		Color tmpColorA = cSourceCP.getColor(0.0, 1.0, 0.5);
		double r = tmpColorA.getRed() / 255.0;
		double g = tmpColorA.getGreen() / 255.0;
		double b = tmpColorA.getBlue() / 255.0;
		vSourceA.GetProperty().SetColor(r, g, b);
		vSourceA.GetProperty().SetPointSize(2.0);

		// Instantiate VTK vars (associated with the target points)
		FeatureType tmpFT = cTargetCP.getFeatureType();
		FeatureAttr tmpFA = getFeatureAttrFor(tmpFT);

		double minVal = tmpFA.getMinVal();
		double maxVal = tmpFA.getMaxVal();
		for (int aIdx = 0; aIdx < numPts; aIdx++)
		{
			// Delegate to the ColorProvider for the actual color value
			double tmpVal = tmpFA.getValAt(aIdx);
			Color tmpColorB = cTargetCP.getColor(minVal, maxVal, tmpVal);
			vColorUCA.InsertNextTuple4(tmpColorB.getRed(), tmpColorB.getGreen(), tmpColorB.getBlue(),
					tmpColorB.getAlpha());
		}

		vTargetPD.GetCellData().GetScalars().Modified();
		vTargetPD.Modified();

		vTargetGF = VtkUtil.formGeometryFilter(vTargetPD, numPts);

		vtkPolyDataMapper pointsMapperTarget = new vtkPolyDataMapper();
		pointsMapperTarget.SetScalarModeToUseCellData();
		pointsMapperTarget.SetInputConnection(vTargetGF.GetOutputPort());

		vTargetA = new SaavtkLODActor();
		vTargetA.SetMapper(pointsMapperTarget);
		((SaavtkLODActor) vTargetA).setQuadricDecimatedLODMapper(vTargetGF.GetOutputPort());

		vTargetA.GetProperty().SetPointSize(2.0);
	}

	/**
	 * Helper method to update the VTK state associated with the colors of the
	 * lidar data.
	 */
	private void doUpdateStateColor(ColorProvider aSourceCP, ColorProvider aTargetCP)
	{
		// Update cache
		cSourceCP = aSourceCP;
		cTargetCP = aTargetCP;
		if (cSourceCP == null || cTargetCP == null)
			return;

		// Update the colors associated with the source points
		Color tmpColorA = aSourceCP.getColor(0.0, 1.0, 0.5);
		double r = tmpColorA.getRed() / 255.0;
		double g = tmpColorA.getGreen() / 255.0;
		double b = tmpColorA.getBlue() / 255.0;
		vSourceA.GetProperty().SetColor(r, g, b);
		vSourceA.Modified();

		// Update the colors associated with the target points
		FeatureType tmpFT = aTargetCP.getFeatureType();
		FeatureAttr tmpFA = getFeatureAttrFor(tmpFT);

		double minVal = tmpFA.getMinVal();
		double maxVal = tmpFA.getMaxVal();
		int numPts = tmpFA.getNumVals();
		for (int aIdx = 0; aIdx < numPts; aIdx++)
		{
			double tmpVal = tmpFA.getValAt(aIdx);
			Color tmpColorB = aTargetCP.getColor(minVal, maxVal, tmpVal);
			vColorUCA.SetTuple4(aIdx, tmpColorB.getRed(), tmpColorB.getGreen(), tmpColorB.getBlue(), tmpColorB.getAlpha());
		}
		vColorUCA.Modified();
	}

	/**
	 * Helper method to update the VTK state associated with the translation of
	 * the lidar data.
	 *
	 * @param aTranslationV
	 */
	private void doUpdateStateTranslation(Vector3D aTranslationV, double aRadialOffset)
	{
		// Update cache
		cTranslationV = aTranslationV;
		cRadialOffset = aRadialOffset;

		// Update the VTK state for each target lidar point position
		int numberOfPoints = getNumberOfPoints();
		for (int aIdx = 0; aIdx < numberOfPoints; aIdx++)
		{
			Vector3D tmpPos = refManager.getTargetPosition(refItem, aIdx);
			tmpPos = LidarGeoUtil.transformTarget(aTranslationV, aRadialOffset, tmpPos);

			vTargetP.SetPoint(aIdx, tmpPos.getX(), tmpPos.getY(), tmpPos.getZ());
		}
		vTargetP.Modified();
	}

	/**
	 * Helper method that returns the number of points in this object
	 */
	private int getNumberOfPoints()
	{
		return timeFA.getNumVals();
	}

}
