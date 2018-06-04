package edu.jhuapl.sbmt.model.rosetta;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkActor;
import vtk.vtkCell;
import vtk.vtkCellArray;
import vtk.vtkFeatureEdges;
import vtk.vtkIdList;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageConstantPad;
import vtk.vtkImageData;
import vtk.vtkImageToPolyDataFilter;
import vtk.vtkImageTranslateExtent;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.FitsException;

public class OsirisImage extends PerspectiveImage
{
    public OsirisImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
        offLimbVisibility=true;
    }

    boolean offLimbVisibility;

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        // Flip image along y axis and rotate it. Only needed for NAC images.
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        if (keyFile.getName().startsWith("N"))
        {
            ImageDataUtil.flipImageYAxis(rawImage);
            ImageDataUtil.rotateImage(rawImage, 180.0);
        }

        if (key.name.contains("67P"))
        {
            return;
        }
        else // for lutetia
        {
            // If image is smaller than 2048x2048 we need to extend it to that size.
            // Therefore, the following pads the images with zero back to
            // original size. The vtkImageTranslateExtent first translates the cropped image
            // to its proper position in the original and the vtkImageConstantPad then pads
            // it with zero to size 2048x2048.
            int[] dims = rawImage.GetDimensions();
            if (dims[0] == 2048 && dims[1] == 2048)
                return;

            // Currently this correction only works with NAC images of size 1024x1024.
            // Other images don't align well with the shape model using this shift amount.
            int xshift = 559;
            int yshift = 575;

            vtkImageTranslateExtent translateExtent = new vtkImageTranslateExtent();
            translateExtent.SetInputData(rawImage);
            translateExtent.SetTranslation(xshift, yshift, 0);
            translateExtent.Update();

            vtkImageConstantPad pad = new vtkImageConstantPad();
            pad.SetInputConnection(translateExtent.GetOutputPort());
            pad.SetOutputWholeExtent(0, 2047, 0, 2047, 0, 0);
            pad.Update();

            vtkImageData padOutput = pad.GetOutput();
            rawImage.DeepCopy(padOutput);

            // shift origin back to zero
            rawImage.SetOrigin(0.0, 0.0, 0.0);
        }
    }

    @Override
    protected int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        ImageKey key = getKey();
        return FileCache.getFileFromServer(key.name + ".FIT").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        return null;
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/infofiles/"
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/sumfiles/"
        + keyFile.getName() + ".SUM";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        int filter = Integer.parseInt(getFilterName());

        switch(filter)
        {
        case 12:
            return 1;
        case 16:
            return 2;
        case 18:
            return 3;
        case 22:
            return 4;
        case 23:
            return 5;
        case 24:
            return 6;
        case 27:
            return 7;
        case 28:
            return 8;
        case 41:
            return 9;
        case 51:
            return 10;
        case 54:
            return 11;
        case 61:
            return 12; // Everything below here was added on 12/24/2015, nothing above was touched for backwards compatibility
        case 13:
            return 13;
        case 15:
            return 14;
        case 17:
            return 15;
        case 31:
            return 16;
        case 71:
            return 17;
        case 82:
            return 18;
        case 84:
            return 19;
        case 87:
            return 20;
        case 88:
            return 21; // Everything below here was added on 3/27/2016, nothing above was touched for backwards compatibility
        case 21:
            return 22;
        }

        return 0;
    }

    @Override
    public String getFilterName()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String filename = keyFile.getName();

        return filename.substring(filename.length()-2, filename.length());
    }


    private String getCameraNameFromNumber(int num)
    {
        String name = null;
        if (num == 1)
            name = "NAC";
        else if (num == 2)
            name = "WAC";

        return name;
    }

    @Override
    public String getCameraName()
    {
        return getCameraNameFromNumber(getCamera());
    }

    @Override
    public int getCamera()
    {
        // Return the following:
        // 1 for NAC
        // 2 for WAC
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        if (keyFile.getName().startsWith("N"))
        {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    vtkPolyData offLimbPlane=null;
    vtkActor offLimbActor;
    vtkTexture offLimbTexture;
    vtkPolyData offLimbBoundary=null;
    vtkActor offLimbBoundaryActor;
    double offLimbFootprintDepth;

    public double getOffLimbPlaneDepth()
    {
        return offLimbFootprintDepth;
    }

    public void setOffLimbPlaneDepth(double footprintDepth)
    {
        this.offLimbFootprintDepth=footprintDepth;
        loadOffLimbPlane(footprintDepth);
    }


    protected void loadOffLimbPlane()
    {
        double[] spacecraftPosition=new double[3];
        double[] focalPoint=new double[3];
        double[] upVector=new double[3];
        this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        this.offLimbFootprintDepth=new Vector3D(spacecraftPosition).getNorm();
        loadOffLimbPlane(offLimbFootprintDepth);
    }

    protected void loadOffLimbPlane(double footprintDepth)
    {

        double[] spacecraftPosition=new double[3];
        double[] focalPoint=new double[3];
        double[] upVector=new double[3];
        this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        final double fov=this.getMaxFovAngle();

        int res=(int)Math.sqrt(getFootprint(getDefaultSlice()).GetNumberOfPoints());
        int[] resolution=new int[]{res,res};

        int szMax=Math.max(resolution[0], resolution[1]);
        int szW=szMax;//(int)(aspect*szMax);
        int szH=szMax;

        Vector3D lookVec=new Vector3D(focalPoint).subtract(new Vector3D(spacecraftPosition));
        // rotation to align points with camera view, given that camera is at spacecraft position
        Vector3D upVec=new Vector3D(upVector);
        Rotation lookRot=new Rotation(Vector3D.MINUS_K, lookVec.normalize());
        Rotation upRot=new Rotation(lookRot.applyTo(Vector3D.PLUS_J), upVec.normalize());
        Vector3D scPos=new Vector3D(spacecraftPosition);
        if (minFrustumDepth[getCurrentSlice()]==0)
            minFrustumDepth[getCurrentSlice()]=0;
        if (maxFrustumDepth[getCurrentSlice()]==0)
            maxFrustumDepth[getCurrentSlice()]=scPos.getNorm()+getSmallBodyModel().getBoundingBoxDiagonalLength();
        double maxRayDepth=(minFrustumDepth[getCurrentSlice()]+maxFrustumDepth[getCurrentSlice()]);
        double ffac=maxRayDepth*Math.tan(Math.toRadians(fov/2));

        vtkImageCanvasSource2D imageSource=new vtkImageCanvasSource2D();
        imageSource.SetScalarTypeToUnsignedChar();
        imageSource.SetNumberOfScalarComponents(3);
        imageSource.SetExtent(-szW/2, szW/2, -szH/2, szH/2, 0, 0);
        for (int i=-szW/2; i<=szW/2; i++)
            for (int j=-szH/2; j<=szH/2; j++)
            {
                Vector3D ray=new Vector3D((double)i/((double)szW/2)*ffac,(double)j/((double)szW/2)*ffac,-maxRayDepth);
                ray=upRot.applyTo(lookRot.applyTo(ray));//upRot.applyInverseTo(lookRot.applyInverseTo(ray.normalize()));
                Vector3D rayEnd=ray.add(scPos);
                //
                vtkIdList ids=new vtkIdList();
                getSmallBodyModel().getCellLocator().FindCellsAlongLine(scPos.toArray(), rayEnd.toArray(), 1e-12, ids);
                if (ids.GetNumberOfIds()>0)
                    imageSource.SetDrawColor(0,0,0);
                else
                    imageSource.SetDrawColor(255,255,255);
                imageSource.DrawPoint(i, j);
            }
        imageSource.Update();
        vtkImageData imageData=imageSource.GetOutput();

        vtkImageToPolyDataFilter imageConverter=new vtkImageToPolyDataFilter();
        imageConverter.SetInputData(imageData);
        imageConverter.SetOutputStyleToPixelize();
        imageConverter.Update();
        vtkPolyData tempImagePolyData=imageConverter.GetOutput();

        vtkCellArray cells=new vtkCellArray();
        for (int c=0; c<tempImagePolyData.GetNumberOfCells(); c++)
        {
            double[] rgb=tempImagePolyData.GetCellData().GetScalars().GetTuple3(c);
            if (rgb[0]>0 || rgb[1]>0 || rgb[2]>0)
            {
                vtkCell cell=tempImagePolyData.GetCell(c);
                cells.InsertNextCell(cell.GetPointIds());
            }
        }
        vtkPolyData imagePolyData=new vtkPolyData();
        imagePolyData.SetPoints(tempImagePolyData.GetPoints());
        imagePolyData.SetPolys(cells);
        double sfac=footprintDepth*Math.tan(Math.toRadians(fov/2));
        for (int i=0; i<imagePolyData.GetNumberOfPoints(); i++)
        {
            Vector3D pt=new Vector3D(imagePolyData.GetPoint(i));
            pt=pt.subtract(new Vector3D((double)szW/2,(double)szH/2,0));
            pt=new Vector3D(pt.getX()/((double)szW/2)*sfac,pt.getY()/((double)szH/2)*sfac,-footprintDepth);
            pt=scPos.add(upRot.applyTo(lookRot.applyTo(pt)));
            imagePolyData.GetPoints().SetPoint(i, pt.toArray());
        }

/*        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
        writer.SetFileTypeToBinary();
        writer.SetInputData(imagePolyData);
        writer.Write();*/

/*        vtkAppendPolyData piAppendFilter=new vtkAppendPolyData();
        piAppendFilter.AddInputData(imagePolyData);
        piAppendFilter.AddInputData(getSmallBodyModel().getSmallBodyPolyData());
        piAppendFilter.Update();

        vtkPolyDataWriter piWriter=new vtkPolyDataWriter();
        piWriter.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
        piWriter.SetFileTypeToBinary();
        piWriter.SetInputData(piAppendFilter.GetOutput());
        piWriter.Write();*/

        offLimbPlane=new vtkPolyData();
        offLimbPlane.DeepCopy(imagePolyData);
        PolyDataUtil.generateTextureCoordinates(getFrustum(), getImageWidth(), getImageHeight(), offLimbPlane);

        if (getDisplayedImage()!=null)
        {
            //        for (int i=image.GetExtent()[0]; i<=image.GetExtent()[1]; i++)
            //            for (int j=image.GetExtent()[2]; j<=image.GetExtent()[3]; j++)
            //                image.SetScalarComponentFromDouble(i, j, 0, 3, 0.7*255);    // set alpha manually per pixel; is there a faster way to do this?
            if (offLimbTexture==null)
            {
                offLimbTexture = new vtkTexture();
                offLimbTexture.InterpolateOn();
                offLimbTexture.RepeatOff();
                offLimbTexture.EdgeClampOn();
            }
            //offLimbTexture.SetBlendingMode(3);
            this.setDisplayedImageRange(super.getDisplayedRange());

            vtkPolyDataMapper offLimbMapper=new vtkPolyDataMapper();
            offLimbMapper.SetInputData(offLimbPlane);

            if (offLimbActor==null)
                offLimbActor=new vtkActor();
            offLimbActor.SetMapper(offLimbMapper);
            offLimbActor.SetTexture(offLimbTexture);

            vtkFeatureEdges edgeFilter=new vtkFeatureEdges();
            edgeFilter.SetInputData(offLimbPlane);
            edgeFilter.BoundaryEdgesOn();
            edgeFilter.ManifoldEdgesOff();
            edgeFilter.FeatureEdgesOff();
            edgeFilter.Update();
            offLimbBoundary=new vtkPolyData();
            offLimbBoundary.DeepCopy(edgeFilter.GetOutput());
            vtkPolyDataMapper boundaryMapper=new vtkPolyDataMapper();
            boundaryMapper.SetInputData(offLimbBoundary);

            if (offLimbBoundaryActor==null)
                offLimbBoundaryActor=new vtkActor();
            offLimbBoundaryActor.SetMapper(boundaryMapper);
            offLimbBoundaryActor.GetProperty().SetColor(0, 0, 1);
            offLimbBoundaryActor.GetProperty().SetLineWidth(1);

            offLimbActor.SetVisibility(offLimbVisibility?1:0);
            offLimbBoundaryActor.SetVisibility(offLimbVisibility?1:0);

        }

    }

    @Override
    public List<vtkProp> getProps()
    {
        List<vtkProp> props=super.getProps();
        if (offLimbVisibility)
        {
            if (offLimbActor==null)
                loadOffLimbPlane();
            if (props.contains(offLimbActor))
                props.remove(offLimbActor);
            props.add(offLimbActor);
            if (props.contains(offLimbBoundaryActor))
                props.remove(offLimbBoundaryActor);
            props.add(offLimbBoundaryActor);
        }
        return props;
    }

    public boolean offLimbFootprintIsVisible()
    {
        return offLimbVisibility;
    }

    public void setOffLimbFootprintVisibility(boolean visible)
    {
        offLimbVisibility=visible;
        if (offLimbActor==null)
            loadOffLimbPlane();

        if (visible)
        {
            offLimbActor.VisibilityOn();
            offLimbBoundaryActor.VisibilityOn();
        }
        else
        {
            offLimbActor.VisibilityOff();
            offLimbBoundaryActor.VisibilityOff();
        }

        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    @Override
    public void setDisplayedImageRange(IntensityRange range)
    {
        super.setDisplayedImageRange(range);
        if (offLimbTexture==null)
            offLimbTexture=new vtkTexture();
        vtkImageData image=new vtkImageData();
        image.DeepCopy(getDisplayedImage());
        offLimbTexture.SetInputData(image);
        offLimbTexture.Modified();
    }

    public void setOffLimbFootprintAlpha(double alpha)  // between 0-1
    {
/*        vtkImageData image=offLimbTexture.GetImageDataInput(0);
        for (int i=image.GetExtent()[0]; i<=image.GetExtent()[1]; i++)
            for (int j=image.GetExtent()[2]; j<=image.GetExtent()[3]; j++)
                image.SetScalarComponentFromDouble(i, j, 0, 3, value*255);    // set alpha manually per pixel; is there a faster way to do this?
        offLimbTexture.Modified();*/
        if (offLimbActor==null)
            loadOffLimbPlane();
        offLimbActor.GetProperty().SetOpacity(alpha);
        //offLimbBoundaryActor.GetProperty().SetOpacity(alpha);
    }

}
