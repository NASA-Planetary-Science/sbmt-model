package edu.jhuapl.sbmt.model.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkFeatureEdges;
import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkImageMapToColors;
import vtk.vtkImageMask;
import vtk.vtkImageReslice;
import vtk.vtkLookupTable;
import vtk.vtkPNGWriter;
import vtk.vtkPointData;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataNormals;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTexture;
import vtk.vtkXMLPolyDataReader;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.Image.ImageKey;

public class PerspectiveImageVTKRenderEngine implements IVTKRenderEngine
{
    private PerspectiveImage image;
    protected vtkTexture imageTexture;
    private vtkPolyData frustumPolyData;
    private vtkActor frustumActor;
    private vtkActor footprintActor;
    private List<vtkProp> footprintActors = new ArrayList<vtkProp>();
    private vtkPolyDataNormals normalsFilter;
    private vtkImageData rawImage;
    private vtkImageData displayedImage;
    private final vtkPolyData[] shiftedFootprint = new vtkPolyData[1];
    private double[][] spacecraftPositionAdjusted;
    private double[][] frustum1Adjusted;
    private double[][] frustum2Adjusted;
    private double[][] frustum3Adjusted;
    private double[][] frustum4Adjusted;
    private int currentSlice;
    private int imageDepth;
    private int imageHeight;
    private int imageWidth;
    private SmallBodyModel smallBodyModel;
    private vtkPolyData[] footprint = new vtkPolyData[1];
    private vtkFloatArray textureCoords;
    protected double[] maxFrustumDepth;
    protected double[] minFrustumDepth;
    private vtkImageCanvasSource2D maskSource;
    private Frustum[] frusta = new Frustum[1];

    public PerspectiveImageVTKRenderEngine(PerspectiveImage image)
    {
        this.image = image;

        smallBodyModel = image.getSmallBodyModel();
        imageDepth = image.getImageDepth();
        imageWidth = image.getImageWidth();
        imageHeight = image.getImageHeight();
        footprint[0] = new vtkPolyData();
        shiftedFootprint[0] = new vtkPolyData();
        maxFrustumDepth=new double[imageDepth];
        minFrustumDepth=new double[imageDepth];
    }

    public void getPointingInformation()
    {
        spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
        frustum1Adjusted = image.getFrustum1Adjusted();
        frustum2Adjusted = image.getFrustum2Adjusted();
        frustum3Adjusted = image.getFrustum3Adjusted();
        frustum4Adjusted = image.getFrustum4Adjusted();
        currentSlice = image.getCurrentSlice();
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#getProps()
     */
    @Override
    public List<vtkProp> getProps()
    {
        System.out.println("PerspectiveImageVTKRenderEngine: getProps: getting props");
        //        System.out.println("getProps()");
        if (footprintActor == null)
        {
            System.out.println(
                    "PerspectiveImageVTKRenderEngine: getProps: generating footprint");
            loadFootprint();

            imageTexture = new vtkTexture();
            imageTexture.InterpolateOn();
            imageTexture.RepeatOff();
            imageTexture.EdgeClampOn();
            imageTexture.SetInputData(getDisplayedImage());

            vtkPNGWriter writer = new vtkPNGWriter();
            writer.SetFileName("fit.png");
            writer.SetInputData(displayedImage);
            writer.Write();

            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
            footprintMapper.SetInputData(shiftedFootprint[0]);
//            System.out.println(
//                    "PerspectiveImageVTKRenderEngine: getProps: shiftedfootprint " + shiftedFootprint[0]);
            footprintMapper.Update();

            footprintActor = new vtkActor();
            footprintActor.SetMapper(footprintMapper);
            footprintActor.SetTexture(imageTexture);
//            System.out.println(
//                    "PerspectiveImageVTKRenderEngine: getProps: image texture " + imageTexture);
//            System.out.println(
//                    "PerspectiveImageVTKRenderEngine: getProps: footprint " + footprintActor);
            vtkProperty footprintProperty = footprintActor.GetProperty();
            footprintProperty.LightingOff();

            footprintActors.add(footprintActor);
        }

        if (frustumActor == null)
        {
            frustumActor = new vtkActor();

            calculateFrustum();

            vtkProperty frustumProperty = frustumActor.GetProperty();
            frustumProperty.SetColor(0.0, 1.0, 0.0);
            frustumProperty.SetLineWidth(2.0);
            frustumActor.VisibilityOff();

            footprintActors.add(frustumActor);
        }

        // for offlimb
        if (image.getOfflimb() != null)
        {
            vtkActor offLimbActor = image.getOfflimb().getOffLimbActor();
            vtkActor offLimbBoundaryActor = image.getOfflimb().getOffLimbBoundaryActor();
            if (offLimbActor == null) {
                image.getOfflimb().loadOffLimbPlane();
                if (footprintActors.contains(offLimbActor))
                    footprintActors.remove(offLimbActor);
                footprintActors.add(offLimbActor);
                if (footprintActors.contains(offLimbBoundaryActor))
                    footprintActors.remove(offLimbBoundaryActor);
                footprintActors.add(offLimbBoundaryActor);
            }
        }


        return footprintActors;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#calculateFrustum()
     */
    @Override
    public void calculateFrustum()
    {
        System.out.println(
                "PerspectiveImageVTKRenderEngine: calculateFrustum: calculating frustum");
        //        System.out.println("recalculateFrustum()");
        frustumPolyData = new vtkPolyData();

        vtkPoints points = new vtkPoints();
        vtkCellArray lines = new vtkCellArray();

        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(2);


        double maxFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]) + image.getSmallBodyModel().getBoundingBoxDiagonalLength();
        double[] origin = spacecraftPositionAdjusted[currentSlice];
        double[] UL = {origin[0]+frustum1Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum1Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum1Adjusted[currentSlice][2]*maxFrustumRayLength};
        double[] UR = {origin[0]+frustum2Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum2Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum2Adjusted[currentSlice][2]*maxFrustumRayLength};
        double[] LL = {origin[0]+frustum3Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum3Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum3Adjusted[currentSlice][2]*maxFrustumRayLength};
        double[] LR = {origin[0]+frustum4Adjusted[currentSlice][0]*maxFrustumRayLength, origin[1]+frustum4Adjusted[currentSlice][1]*maxFrustumRayLength, origin[2]+frustum4Adjusted[currentSlice][2]*maxFrustumRayLength};

        double minFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]) - image.getSmallBodyModel().getBoundingBoxDiagonalLength();
        maxFrustumDepth[currentSlice]=maxFrustumRayLength;  // a reasonable approximation for a max bound on the frustum depth
        minFrustumDepth[currentSlice]=minFrustumRayLength;  // a reasonable approximation for a min bound on the frustum depth



        points.InsertNextPoint(spacecraftPositionAdjusted[currentSlice]);
        points.InsertNextPoint(UL);
        points.InsertNextPoint(UR);
        points.InsertNextPoint(LL);
        points.InsertNextPoint(LR);

        idList.SetId(0, 0);
        idList.SetId(1, 1);
        lines.InsertNextCell(idList);
        idList.SetId(0, 0);
        idList.SetId(1, 2);
        lines.InsertNextCell(idList);
        idList.SetId(0, 0);
        idList.SetId(1, 3);
        lines.InsertNextCell(idList);
        idList.SetId(0, 0);
        idList.SetId(1, 4);
        lines.InsertNextCell(idList);

        frustumPolyData.SetPoints(points);
        frustumPolyData.SetLines(lines);


        vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
        frusMapper.SetInputData(frustumPolyData);

        frustumActor.SetMapper(frusMapper);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#setDisplayedImageRange(edu.jhuapl.saavtk.util.IntensityRange)
     */
    @Override
    public void setDisplayedImageRange(IntensityRange range)
    {
        System.out.println(
                "PerspectiveImageVTKRenderEngine: setDisplayedImageRange: ");
        if (range == null || image.getDisplayedRange().min != range.min || image.getDisplayedRange().max != range.max)
        {
            //            displayedRange[currentSlice] = range != null ? range : new IntensityRange(0, 255);
            if (range != null)
                image.setDisplayedImageRange(range);    //TODO confirm this is right
//                displayedRange[currentSlice] = range;

            float minValue = image.getMinValue();
            float maxValue = image.getMaxValue();
            float dx = (maxValue-minValue)/255.0f;
            float min = minValue + image.getDisplayedRange().min*dx;
            float max = minValue + image.getDisplayedRange().max*dx;

            // Update the displayed image
            vtkLookupTable lut = new vtkLookupTable();
            lut.SetTableRange(min, max);
            lut.SetValueRange(0.0, 1.0);
            lut.SetHueRange(0.0, 0.0);
            lut.SetSaturationRange(0.0, 0.0);
            //lut.SetNumberOfTableValues(402);
            lut.SetRampToLinear();
            lut.Build();

            // for 3D images, take the current slice
            vtkImageData image2D = rawImage;
            if (imageDepth > 1)
            {
                vtkImageReslice slicer = new vtkImageReslice();
                slicer.SetInputData(rawImage);
                slicer.SetOutputDimensionality(2);
                slicer.SetInterpolationModeToNearestNeighbor();
                slicer.SetOutputSpacing(1.0, 1.0, 1.0);
                slicer.SetResliceAxesDirectionCosines(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);

                slicer.SetOutputOrigin(0.0, 0.0, (double)currentSlice);
                slicer. SetResliceAxesOrigin(0.0, 0.0, (double)currentSlice);

                slicer.SetOutputExtent(0, imageWidth-1, 0, imageHeight-1, 0, 0);

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

            if (displayedImage == null)
                displayedImage = new vtkImageData();
            vtkImageData maskFilterOutput = maskFilter.GetOutput();
            displayedImage.DeepCopy(maskFilterOutput);

            System.out.println(
                    "PerspectiveImageVTKRenderEngine: setDisplayedImageRange: displayed image is " + displayedImage);

            maskFilter.Delete();
            mapToColors.Delete();
            lut.Delete();
            mapToColorsOutput.Delete();
            maskSourceOutput.Delete();
            maskFilterOutput.Delete();

//            vtkPNGWriter writer = new vtkPNGWriter();
//            writer.SetFileName("fit.png");
//            writer.SetInputData(displayedImage);
//            writer.Write();

        }
        // for offlimb
//        System.out.println(
//                "PerspectiveImageVTKRenderEngine: setDisplayedImageRange: image is " + image.getOfflimb());
        if (image.getOfflimb() != null)
        {
            vtkTexture offLimbTexture = image.getOfflimb().getOffLimbTexture();
            if (offLimbTexture==null)
                offLimbTexture=new vtkTexture();
            vtkImageData image2=new vtkImageData();
            image2.DeepCopy(getDisplayedImage());
            offLimbTexture.SetInputData(image2);
            offLimbTexture.Modified();
        }

        this.image.firePropertyChange();
//        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);

    }

    public vtkPolyData getFootprint(int defaultSlice)
    {
        System.out.println(
                "PerspectiveImageVTKRenderEngine: getFootprint: getting footprint");
        System.out.println("PerspectiveImageVTKRenderEngine: getFootprint: frustum 1 adjusted " + frustum1Adjusted[defaultSlice][0] + " " + frustum1Adjusted[defaultSlice][1]);
        System.out.println("PerspectiveImageVTKRenderEngine: getFootprint: frustum 2 adjusted " + frustum2Adjusted[defaultSlice][0] + " " + frustum2Adjusted[defaultSlice][1]);
        System.out.println("PerspectiveImageVTKRenderEngine: getFootprint: frustum 3 adjusted " + frustum3Adjusted[defaultSlice][0] + " " + frustum3Adjusted[defaultSlice][1]);
        System.out.println("PerspectiveImageVTKRenderEngine: getFootprint: frustum 4 adjusted " + frustum4Adjusted[defaultSlice][0] + " " + frustum4Adjusted[defaultSlice][1]);
        System.out.println("PerspectiveImageVTKRenderEngine: getFootprint: sc position " + spacecraftPositionAdjusted[defaultSlice][0] + " " + spacecraftPositionAdjusted[defaultSlice][1]);
        return smallBodyModel.computeFrustumIntersection(spacecraftPositionAdjusted[defaultSlice],
                frustum1Adjusted[defaultSlice], frustum3Adjusted[defaultSlice], frustum4Adjusted[defaultSlice], frustum2Adjusted[defaultSlice]);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#loadFootprint()
     */
    @Override
    public void loadFootprint()
    {
        System.out.println(
                "PerspectiveImageVTKRenderEngine: loadFootprint: loading footprint");
        ImageKey key = image.getKey();
        if (image.isGenerateFootprint())
        {
            vtkPolyData tmp = null;

            if (!image.getFootprintGenerated()[currentSlice])
            {
                if (image.useDefaultFootprint())
                {
                    int defaultSlice = image.getDefaultSlice();
                    if (image.getFootprintGenerated()[defaultSlice] == false)
                    {
                        footprint[defaultSlice] = getFootprint(defaultSlice);
                        if (footprint[defaultSlice] == null)
                            return;

                        // Need to clear out scalar data since if coloring data is being shown,
                        // then the color might mix-in with the image.
                        footprint[defaultSlice].GetCellData().SetScalars(null);
                        footprint[defaultSlice].GetPointData().SetScalars(null);

                        image.setFootprintGenerated(true);
//                        footprintGenerated[defaultSlice] = true;
                    }

                    tmp = footprint[defaultSlice];

                }
                else
                {
                    tmp = smallBodyModel.computeFrustumIntersection(spacecraftPositionAdjusted[currentSlice],
                            frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice], frustum4Adjusted[currentSlice], frustum2Adjusted[currentSlice]);
                    if (tmp == null)
                        return;

                    // Need to clear out scalar data since if coloring data is being shown,
                    // then the color might mix-in with the image.
                    tmp.GetCellData().SetScalars(null);
                    tmp.GetPointData().SetScalars(null);
                }


                //                vtkPolyDataWriter writer=new vtkPolyDataWriter();
                //                writer.SetInputData(tmp);
                //                writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
                //               writer.SetFileTypeToBinary();
                //                writer.Write();

                footprint[currentSlice].DeepCopy(tmp);

                image.setFootprintGenerated(true, currentSlice);
//                footprintGenerated[currentSlice] = true;
            }


            vtkPointData pointData = footprint[currentSlice].GetPointData();
            pointData.SetTCoords(textureCoords);
            PolyDataUtil.generateTextureCoordinates(getFrustum(), imageWidth, imageHeight, footprint[currentSlice]);
            for (int i=0; i< 100; i++)
                System.out.println(
                    "PerspectiveImageVTKRenderEngine: loadFootprint: texturecoords " + textureCoords.GetComponent(0, i));
            pointData.Delete();
        }
        else
        {
            int resolutionLevel = smallBodyModel.getModelResolution();

            String footprintFilename = null;
            File file = null;

            if (key.source == ImageSource.SPICE || key.source == ImageSource.CORRECTED_SPICE)
                footprintFilename = key.name + "_FOOTPRINT_RES" + resolutionLevel + "_PDS.VTP";
            else
                footprintFilename = key.name + "_FOOTPRINT_RES" + resolutionLevel + "_GASKELL.VTP";

            file = FileCache.getFileFromServer(footprintFilename);

            if (file == null || !file.exists())
            {
                System.out.println("Warning: " + footprintFilename + " not found");
                return;
            }

            vtkXMLPolyDataReader footprintReader = new vtkXMLPolyDataReader();
            footprintReader.SetFileName(file.getAbsolutePath());
            footprintReader.Update();

            vtkPolyData footprintReaderOutput = footprintReader.GetOutput();
            footprint[currentSlice].DeepCopy(footprintReaderOutput);
        }


        shiftedFootprint[0].DeepCopy(footprint[currentSlice]);
        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[0], image.getOffset());
        System.out.println(
                "PerspectiveImageVTKRenderEngine: loadFootprint: footprint is " + footprint[currentSlice]);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#generateBoundary()
     */
    @Override
    public vtkPolyData generateBoundary()
    {
        System.out.println(
                "PerspectiveImageVTKRenderEngine: generateBoundary: generating boundary");
        loadFootprint();

        if (footprint[currentSlice].GetNumberOfPoints() == 0)
            return null;

        vtkFeatureEdges edgeExtracter = new vtkFeatureEdges();
        edgeExtracter.SetInputData(footprint[currentSlice]);
        edgeExtracter.BoundaryEdgesOn();
        edgeExtracter.FeatureEdgesOff();
        edgeExtracter.NonManifoldEdgesOff();
        edgeExtracter.ManifoldEdgesOff();
        edgeExtracter.Update();

        vtkPolyData boundary = new vtkPolyData();
        vtkPolyData edgeExtracterOutput = edgeExtracter.GetOutput();
        boundary.DeepCopy(edgeExtracterOutput);

        return boundary;
    }



    public void computeCellNormals()
    {
        System.out.println(
                "PerspectiveImageVTKRenderEngine: computeCellNormals: ");
        if (image.isNormalsGenerated() == false)
        {
            normalsFilter.SetInputData(footprint[currentSlice]);
            normalsFilter.SetComputeCellNormals(1);
            normalsFilter.SetComputePointNormals(0);
            //normalsFilter.AutoOrientNormalsOn();
            //normalsFilter.ConsistencyOn();
            normalsFilter.SplittingOff();
            normalsFilter.Update();

            if (footprint != null && footprint[currentSlice] != null)
            {
                vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
                footprint[currentSlice].DeepCopy(normalsFilterOutput);
                image.setNormalsGenerated(true);
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#getRawImage()
     */
    @Override
    public vtkImageData getRawImage()
    {
        return rawImage;
    }

    public vtkImageData getDisplayedImage()
    {
        return displayedImage;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#getFrustum()
     */
    @Override
    public Frustum getFrustum()
    {
        return getFrustum(currentSlice);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.image.IVTKRenderEngine#getFrustum(int)
     */
    @Override
    public Frustum getFrustum(int slice)
    {
        if (image.useDefaultFootprint())
        {
            int defaultSlice = image.getDefaultSlice();
            if (frusta[defaultSlice] == null)
                frusta[defaultSlice] = new Frustum(spacecraftPositionAdjusted[defaultSlice], frustum1Adjusted[defaultSlice], frustum3Adjusted[defaultSlice], frustum4Adjusted[defaultSlice], frustum2Adjusted[defaultSlice]);
            return frusta[defaultSlice];
        }

        if (frusta[slice] == null)
            frusta[slice] = new Frustum(spacecraftPositionAdjusted[slice], frustum1Adjusted[slice], frustum3Adjusted[slice], frustum4Adjusted[slice], frustum2Adjusted[slice]);
        return frusta[slice];
    }

    public Frustum[] getFrusta()
    {
        return frusta;
    }

    public vtkActor getFrustumActor()
    {
        return frustumActor;
    }

    public void setRawImage(vtkImageData rawImage)
    {
        this.rawImage = rawImage;
    }

    public double getMaxFrustumDepth(int slice)
    {
        return maxFrustumDepth[slice];
    }

    public void setMaxFrustumDepth(int slice, double value)
    {
        maxFrustumDepth[slice] = value;
    }

    public double getMinFrustumDepth(int slice)
    {
        return minFrustumDepth[slice];
    }

    public void setMinFrustumDepth(int slice, double value)
    {
        minFrustumDepth[slice] = value;
    }

    public vtkActor getFootprintActor()
    {
        return footprintActor;
    }

    public vtkTexture getTexture()
    {
        return imageTexture;
    }

    public vtkPolyData getFrustumPolyData()
    {
        return frustumPolyData;
    }

    /**
     * The shifted footprint is the original footprint shifted slightly in the
     * normal direction so that it will be rendered correctly and not obscured
     * by the asteroid.
     * @return
     */
//    @Override
    public vtkPolyData getShiftedFootprint()
    {
        return shiftedFootprint[0];
    }

    public vtkPolyData getShiftedFootprint(int index)
    {
        return shiftedFootprint[index];
    }

    public vtkPolyData[] getFootprint()
    {
        return footprint;
    }

    /**
     * The original footprint whose cells exactly overlap the original asteroid.
     * If rendered as is, it would interfere with the asteroid.
     * Note: this is made public in this class for the benefit of backplane
     * generators, which use it.
     * @return
     */
//    @Override
    public vtkPolyData getUnshiftedFootprint()
    {
        return footprint[currentSlice];
    }

    public vtkImageCanvasSource2D getMaskSource()
    {
        return maskSource;
    }

    public void setMaskSource(vtkImageCanvasSource2D maskSource)
    {
        this.maskSource = maskSource;
    }

    public void reset()
    {
        shiftedFootprint[0] = new vtkPolyData();
        textureCoords = new vtkFloatArray();
        normalsFilter = new vtkPolyDataNormals();
    }

    public void resetFrustaAndFootprints(int nslices)
    {
        frusta = new Frustum[nslices];
        footprint = new vtkPolyData[nslices];
    }

    public vtkFloatArray getTextureCoords()
    {
        return textureCoords;
    }

    public vtkPolyDataNormals getNormalsFilter()
    {
        return normalsFilter;
    }

}
