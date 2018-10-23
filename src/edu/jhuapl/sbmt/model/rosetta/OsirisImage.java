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
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;
import edu.jhuapl.sbmt.util.ImageDataUtil;

import nom.tam.fits.FitsException;

public class OsirisImage extends PerspectiveImage
{
    public OsirisImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
        offLimbVisibility=true;
        offLimbBoundaryVisibility = true;
    }

    boolean offLimbVisibility;
    private boolean offLimbBoundaryVisibility;


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


    /**
     * Set the distance of the off-limb plane from the camera position, along its look vector.
     * The associated polydata doesn't need to be regenerated every time this method is called since the body's shadow in frustum coordinates does not change with depth along the look axis.
     * The call to loadOffLimbPlane here does actually re-create the polydata, which should be unnecessary, and needs to be fixed in a future release.
     * @param footprintDepth
     */
    public void setOffLimbPlaneDepth(double footprintDepth)
    {
        this.offLimbFootprintDepth=footprintDepth;
        loadOffLimbPlane(footprintDepth);
    }

    /**
     * No-argument entry point into the off-limb geometry-creation implementation.
     *
     * I'm not sure if this method is entirely necessary, but I think I wrote it to avoid a null value for offLimbFootprintDepth during image loading (in case the footprint depth hasn't been set yet) -- zimmemi1
     */
    protected void loadOffLimbPlane()
    {

        double[] spacecraftPosition=new double[3];
        double[] focalPoint=new double[3];
        double[] upVector=new double[3];
        this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        this.offLimbFootprintDepth=new Vector3D(spacecraftPosition).getNorm();
        loadOffLimbPlane(offLimbFootprintDepth);
    }

    /**
     * Core off-limb geometry creation happens here.
     *
     *  Steps are:
     *   (1) Discretize the view frustum of the camera, choosing a "macro-pixel" size for constructing off-limb geometry
     *
     *   (2) For each off-limb pixel shoot a ray from the camera position toward the camera back-plane
     *      + If the ray hits the body, record "true" in this position;
     *      + otherwise record "false"
     *
     *   (3) The resulting black & white 2D image captures the "shadow" of the body against the sky, from the viewpoint of the camera
     *      + Choose an initial off-limb "depth" (presently the camera-origin distance)
     *      + Project off-limb macro-pixels (marked "false") to this depth
     *      + Compile these into a polydata for visualization -- this is the "off-limb geometry"
     *
     *   (4) Construct a vtkTexture from the original raw image, and map it to the off-limb geometry
     *
     *   (5) Extract edges of the off-limb geometry for visualization (presently shown as red lines)
     *
     * @param footprintDepth
     */
    public void loadOffLimbPlane(double footprintDepth)
    {
        // Step (1): Discretize the view frustum into macro-pixels, from which geometry will later be derived

        // (1a) get camera parameters
        double[] spacecraftPosition=new double[3];
        double[] focalPoint=new double[3];
        double[] upVector=new double[3];
        this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        final double fov=this.getMaxFovAngle();

        // (1b) guess at a resolution for the macro-pixels; these will be used to create quadrilateral cells (i.e. what will eventually be the off-limb geometry) in the camera view-plane
        int res=(int)Math.sqrt(getFootprint(getDefaultSlice()).GetNumberOfPoints());    // for now just grab the number of points in the on-body footprint; assuming this is roughly planar we apply sqrt to get an approximate number of points on a "side" of the on-body geometry, within the rectangular frustuma
        int[] resolution=new int[]{res,res};    // cast to int and store s- and t- resolutions; NOTE: s and t can be thought of as respectively "horizontal" and "vertical" when viewing the image in the "Properties..." pane (t-hat cross s-hat = look direction in a righthanded coordinate system)
        // allow for later possibility of unequal macro-pixel resolutions; take the highest resolution
        int szMax=Math.max(resolution[0], resolution[1]);
        int szW=szMax;//(int)(aspect*szMax);
        int szH=szMax;


        // Step (2): Shoot rays from the camera position toward each macro-pixel & record which ones don't hit the body (these will comprise the off-limb geometry)

        // (2a) determine ray-cast depth; currently implemented as camera-to-origin distance plus body bounding-box diagonal length -- that way rays will always extend from the camera position past the entire body
        Vector3D scPos=new Vector3D(spacecraftPosition);
        if (minFrustumDepth[getCurrentSlice()]==0)
            minFrustumDepth[getCurrentSlice()]=0;
        if (maxFrustumDepth[getCurrentSlice()]==0)
            maxFrustumDepth[getCurrentSlice()]=scPos.getNorm()+getSmallBodyModel().getBoundingBoxDiagonalLength();
        double maxRayDepth=(minFrustumDepth[getCurrentSlice()]+maxFrustumDepth[getCurrentSlice()]);
        double ffac=maxRayDepth*Math.tan(Math.toRadians(fov/2));    // this is the scaling factor in the plane perpendicular to the boresight, which maps unit vectors from the camera position onto the chosen max depth, in frustum coordinates, thus forming a ray

        // (2b) figure out rotations lookRot and upRot, which transform frustum (s,t) coordinates into a direction in 3D space, pointing in the implied direction from the 3D camera position:
        //    (A) lookRot * negative z-hat = look-hat               [ transform negative z-hat from 3D space into the look direction ; this is the boresight of the camera ]
        //    (B) upRot * (lookRot * y-hat) = up-hat = t-hat        [ first transform y-hat from 3D space into the "boresight frame" (it was perpendicular to z-hat before, so will now be perpendicular to the boresight direction) and second rotate that vector around the boresight axis to align with camera up, i.e. t-hat
        // NOTE: t-hat cross s-hat = look-hat, thus completing the frustum coordinate system
        // NOTE: given two scalar values -1<=s<=1 and -1<=t<=1, the corresponding ray extends (in 3D space) from the camera position to upRot*lookRot*
        Vector3D lookVec=new Vector3D(focalPoint).subtract(new Vector3D(spacecraftPosition));
        Vector3D upVec=new Vector3D(upVector);
        Rotation lookRot=new Rotation(Vector3D.MINUS_K, lookVec.normalize());
        Rotation upRot=new Rotation(lookRot.applyTo(Vector3D.PLUS_J), upVec.normalize());

        // (2c) use a vtkImageCanvasSource2D to represent the macro-pixels, with an unsigned char color type "true = (0, 0, 0) = ray hits surface" and "false = (255, 255, 255) = ray misses surface"... this might seem backwards but 0-values can be thought of as forming the "shadow" of the body against the sky as viewed by the camera
        // NOTE: this could be done more straightforwardly (and possibly more efficiently) just by using a java boolean[][] array... I think the present implementation is a hangover from prior experimentation with a vtkPolyDataSilhouette filter
        vtkImageCanvasSource2D imageSource=new vtkImageCanvasSource2D();
        imageSource.SetScalarTypeToUnsignedChar();
        imageSource.SetNumberOfScalarComponents(3);
        imageSource.SetExtent(-szW/2, szW/2, -szH/2, szH/2, 0, 0);
        for (int i=-szW/2; i<=szW/2; i++)
            for (int j=-szH/2; j<=szH/2; j++)
            {
                double s=(double)i/((double)szW/2)*ffac;
                double t=(double)j/((double)szW/2)*ffac;
                Vector3D ray=new Vector3D(s,t,-maxRayDepth);  // ray construction starts from s,t coordinates each on the interval [-1 1]
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

        // (3) process the resulting black & white 2D image, which captures the "shadow" of the body against the sky, from the viewpoint of the camera

        // (3a) Actually create some polydata from the imagedata... pixels in the image-data become pairs of triangles (each pair forming a quad)
        vtkImageToPolyDataFilter imageConverter=new vtkImageToPolyDataFilter();
        imageConverter.SetInputData(imageData);
        imageConverter.SetOutputStyleToPixelize();
        imageConverter.Update();
        vtkPolyData tempImagePolyData=imageConverter.GetOutput();   // NOTE: the output of vtkImageToPolyDataFilter is in pixel coordinates, e.g. 0 to width-1 and 0 to height-1, probably with origin at the top-left of the image

        // (3b) create a new cell array to hold "white" cells, i.e. "off-limb" cells, from the "temporary" polydata representation of the silhouette
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

        // (3c) assemble a "final" polydata from the new cell array and points of the temporary silhouette (this could eventually be run through some sort of cleaning filter to get rid of orphaned points)
        vtkPolyData imagePolyData=new vtkPolyData();
        imagePolyData.SetPoints(tempImagePolyData.GetPoints());
        imagePolyData.SetPolys(cells);
        double sfac=footprintDepth*Math.tan(Math.toRadians(fov/2)); // scaling factor that "fits" the polydata into the frustum at the given footprintDepth (in the s,t plane perpendicular to the boresight)
        // make sure all points are reset with the correct transformation (though on-body cells have been culled, topology of the remaining cells doesn't need to be touched; just the respective points need to be unprojected into 3d space)
        for (int i=0; i<imagePolyData.GetNumberOfPoints(); i++)
        {
            Vector3D pt=new Vector3D(imagePolyData.GetPoint(i));            // here's a pixel coordinate
            pt=pt.subtract(new Vector3D((double)szW/2,(double)szH/2,0));    // move the origin to the center of the image, so values range from [-szW/2 szW/2] and [szH/2 szH/2]
            pt=new Vector3D(pt.getX()/((double)szW/2)*sfac,pt.getY()/((double)szH/2)*sfac,-footprintDepth); // a number of things happen here; first a conversion to s,t-coordinates on [-1 1] in each direction, then scaling of the s,t-coordinates to fit the physical dimensions of the frustum at the chosen depth, and finally translation along the boresight axis to the chosen depth
            pt=scPos.add(upRot.applyTo(lookRot.applyTo(pt)));               // transform from (s,t) coordinates into the implied 3D direction vector, with origin at the camera's position in space; depth along the boresight was enforced on the previous line
            imagePolyData.GetPoints().SetPoint(i, pt.toArray());        // overwrite the old (pixel-coordinate) point with the new (3D cartesian) point
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

        // keep a reference to a copy of the polydata
        offLimbPlane=new vtkPolyData();
        offLimbPlane.DeepCopy(imagePolyData);
        PolyDataUtil.generateTextureCoordinates(getFrustum(), getImageWidth(), getImageHeight(), offLimbPlane); // generate (u,v) coords; individual components lie on the interval [0 1]; https://en.wikipedia.org/wiki/UV_mapping

        // now, if there is an "active" image, cf. PerspectiveImage class", then map it to the off-limb polydata
        if (getDisplayedImage()!=null)
        {
            //        for (int i=image.GetExtent()[0]; i<=image.GetExtent()[1]; i++)
            //            for (int j=image.GetExtent()[2]; j<=image.GetExtent()[3]; j++)
            //                image.SetScalarComponentFromDouble(i, j, 0, 3, 0.7*255);    // set alpha manually per pixel; is there a faster way to do this?
            if (offLimbTexture==null)
            {
                // create the texture first
                offLimbTexture = new vtkTexture();
                offLimbTexture.InterpolateOn();
                offLimbTexture.RepeatOff();
                offLimbTexture.EdgeClampOn();
            }
            //offLimbTexture.SetBlendingMode(3);
            this.setDisplayedImageRange(super.getDisplayedRange()); // match off-limb image intensity range to that of the on-body footprint; the "this" method call also takes care of syncing the off-limb vtkTexture object with the displayed raw image, above and beyond what the parent class has to do for the on-body geometry

            // setup off-limb mapper and actor
            vtkPolyDataMapper offLimbMapper=new vtkPolyDataMapper();
            offLimbMapper.SetInputData(offLimbPlane);
            if (offLimbActor==null)
                offLimbActor=new vtkActor();
            offLimbActor.SetMapper(offLimbMapper);
            offLimbActor.SetTexture(offLimbTexture);

            // generate off-limb edge geometry, with mapper and actor
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

            // set initial visibilities
            offLimbActor.SetVisibility(offLimbVisibility?1:0);
            offLimbBoundaryActor.SetVisibility(offLimbBoundaryVisibility?1:0);

        }

    }

    @Override
    /**
     * This is called by a Renderer to get vtkActors to show in the 3D window.
     *
     * In the case of OsirisImage getProps() checks for (1) off-limb visibility, and if visible (2) checks if off-limb geometry has been generated (3) if not creates it by calling loadOffLimbPlane(), and (4) replaces actor references held by the Renderer with the latest (possibly updated) ones contained here.
     */
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

    /**
     * Set visibility of the off-limb footprint
     *
     * Checks if offLimbActor has been instantiated; if not then call loadOffLimbPlane() before showing/hiding actors.
     *
     * @param visible
     */
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

    /**
     * Set visibility of the off-limb footprint boundary
     *
     * Checks if offLimbActor has been instantiated; if not then call loadOffLimbPlane() before showing/hiding actors.
     *
     * @param visible
     */
    public void setOffLimbBoundaryVisibility(boolean visible)
    {

        offLimbVisibility=visible;
        if (offLimbActor==null)
            loadOffLimbPlane();

        if (visible)
        {
            offLimbBoundaryActor.VisibilityOn();
        }
        else
        {
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
