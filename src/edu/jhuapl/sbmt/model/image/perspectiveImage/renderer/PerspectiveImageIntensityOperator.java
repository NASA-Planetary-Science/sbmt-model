package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import com.google.common.base.Preconditions;

import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkImageMapToColors;
import vtk.vtkImageMask;
import vtk.vtkImageReslice;
import vtk.vtkLookupTable;

import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.core.rendering.PerspectiveImage;

public class PerspectiveImageIntensityOperator
{

	// Always use accessors to use this field -- even within this class!
	private IntensityRange[] displayedRange = null;
	PerspectiveImage image;
	vtkImageData rawImage;
	private vtkImageData displayedImage;

	public PerspectiveImageIntensityOperator(PerspectiveImage image)
	{
		this.image = image;

	}

	public vtkImageData getImageWithDisplayedRange(IntensityRange range, boolean offlimb, vtkImageCanvasSource2D maskSource)
	{
		this.rawImage = image.getRawImage();
		int currentSlice = image.getCurrentSlice();
		float minValue = image.getMinValue();
		float maxValue = image.getMaxValue();
		float dx = (maxValue - minValue) / 255.0f;

		float min = minValue;
		float max = maxValue;
		if (!offlimb)
		{
			IntensityRange displayedRange = getDisplayedRange(currentSlice);
			min = minValue + displayedRange.min * dx;
			max = minValue + displayedRange.max * dx;
		}
		else
		{
			IntensityRange offLimbDisplayedRange = image.getOfflimbPlaneHelper().getOffLimbDisplayedRange();
			min = minValue + offLimbDisplayedRange.min * dx;
			max = minValue + offLimbDisplayedRange.max * dx;
		}

		// Update the displayed image
		vtkLookupTable lut = new vtkLookupTable();
		lut.SetTableRange(min, max);
		lut.SetValueRange(0.0, 1.0);
		lut.SetHueRange(0.0, 0.0);
		lut.SetSaturationRange(0.0, 0.0);
		// lut.SetNumberOfTableValues(402);
		lut.SetRampToLinear();
		lut.Build();

		// for 3D images, take the current slice
		vtkImageData image2D = rawImage;
		if (image.getImageDepth() > 1)
		{
			vtkImageReslice slicer = new vtkImageReslice();
			slicer.SetInputData(rawImage);
			slicer.SetOutputDimensionality(2);
			slicer.SetInterpolationModeToNearestNeighbor();
			slicer.SetOutputSpacing(1.0, 1.0, 1.0);
			slicer.SetResliceAxesDirectionCosines(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);

			slicer.SetOutputOrigin(0.0, 0.0, (double) currentSlice);
			slicer.SetResliceAxesOrigin(0.0, 0.0, (double) currentSlice);

			slicer.SetOutputExtent(0, image.getImageWidth() - 1, 0, image.getImageHeight() - 1, 0, 0);

			slicer.Update();
			image2D = slicer.GetOutput();
		}

		vtkImageMapToColors mapToColors = new vtkImageMapToColors();
		mapToColors.SetInputData(image2D);
		mapToColors.SetOutputFormatToRGBA();
		mapToColors.SetLookupTable(lut);
		mapToColors.Update();

		vtkImageData mapToColorsOutput = mapToColors.GetOutput();
		vtkImageData maskSourceOutput = maskSource.GetOutput();

		vtkImageMask maskFilter = new vtkImageMask();
		maskFilter.SetImageInputData(mapToColorsOutput);
		maskFilter.SetMaskInputData(maskSourceOutput);
		maskFilter.Update();

		vtkImageData maskFilterOutput = maskFilter.GetOutput();
		mapToColors.Delete();
		lut.Delete();
		mapToColorsOutput.Delete();
		maskSourceOutput.Delete();
		maskFilter.Delete();
		return maskFilterOutput;
	}

	/**
	 * This getter lazily initializes the range field as necessary to ensure
	 * this returns a valid, non-null range as long as the argument is in range
	 * for this image.
	 *
	 * @param slice
	 *            the number of the slice whose displayed range to return.
	 */
	public IntensityRange getDisplayedRange(int slice)
	{
		int nslices = image.getImageDepth();

		Preconditions.checkArgument(slice < nslices);

		if (displayedRange == null)
		{
			displayedRange = new IntensityRange[nslices];
		}
		if (displayedRange[slice] == null)
		{
			displayedRange[slice] = new IntensityRange(0, 255);
		}

		return displayedRange[slice];
	}

	/**
	 * Set the displayed image range of the currently selected slice of the
	 * image. As a side-effect, this method also MAYBE CREATES the displayed
	 * image.
	 *
	 * @param range
	 *            the new displayed range of the image. If null is passed,
	 */
	public void setDisplayedImageRange(IntensityRange range, vtkImageCanvasSource2D maskSource)
	{
		int currentSlice = image.getCurrentSlice();
		if (rawImage != null)
		{
			if (rawImage.GetNumberOfScalarComponents() > 1)
			{
				displayedImage = rawImage;
				return;
			}
		}

		IntensityRange displayedRange = getDisplayedRange(currentSlice);
		if (range == null || displayedRange.min != range.min || displayedRange.max != range.max)
		{
			if (range != null)
			{
				this.displayedRange[currentSlice] = range;
				image.saveImageInfo();
			}

			if (rawImage != null)
			{
				vtkImageData img = getImageWithDisplayedRange(range, false, maskSource);
				if (displayedImage == null)
					displayedImage = new vtkImageData();
				displayedImage.DeepCopy(img);
			}
		}

		image.firePropertyChange(Properties.MODEL_CHANGED, null, image);
	}

	public IntensityRange getDisplayedRange()
	{
		return getDisplayedRange(image.getCurrentSlice());
	}

	/**
	 * @return the displayedImage
	 */
	public vtkImageData getDisplayedImage()
	{
		return displayedImage;
	}

//	public void propertyChange(PropertyChangeEvent evt)
//	{
//		if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
//		{
//			loadFootprint();
//			normalsGenerated = false;
//			this.minEmission = Double.MAX_VALUE;
//			this.maxEmission = -Double.MAX_VALUE;
//			this.minIncidence = Double.MAX_VALUE;
//			this.maxIncidence = -Double.MAX_VALUE;
//			this.minPhase = Double.MAX_VALUE;
//			this.maxPhase = -Double.MAX_VALUE;
//			this.minHorizontalPixelScale = Double.MAX_VALUE;
//			this.maxHorizontalPixelScale = -Double.MAX_VALUE;
//			this.minVerticalPixelScale = Double.MAX_VALUE;
//			this.maxVerticalPixelScale = -Double.MAX_VALUE;
//			this.meanHorizontalPixelScale = 0.0;
//			this.meanVerticalPixelScale = 0.0;
//
//			image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
//		}
//	}

}
