package edu.jhuapl.sbmt.model.rosetta;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkContourFilter;
import vtk.vtkFloatArray;
import vtk.vtkGenericCell;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageConstantPad;
import vtk.vtkImageData;
import vtk.vtkImageTranslateExtent;
import vtk.vtkLine;
import vtk.vtkMatrix4x4;
import vtk.vtkPointData;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataWriter;
import vtk.vtkProp;
import vtk.vtkRenderWindow;
import vtk.vtkRenderer;
import vtk.vtkTexture;
import vtk.vtkTransform;
import vtk.vtkTransformFilter;
import vtk.vtkTriangle;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.gui.jogl.vtksbmtJoglCanvas;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.PolyDataUtil;
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
    }

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

    vtkPolyData offLimbPlane;
    vtkFloatArray offLimbTextureCoords;
    vtkTexture offLimbTexture;

    JFrame frame=new JFrame();
    vtksbmtJoglCanvas canvas;
    vtkRenderWindow renderWindow;
    int szW,szH;
    int observerId;
    int cnt=0;

    void test()
    {
        System.out.println("!");
/*        try
        {
            cnt++;
            if (cnt<3)
                return;
            BufferedImage image=new BufferedImage(szW, szH, BufferedImage.TYPE_INT_RGB);
            canvas.getComponent().paint(image.getGraphics());
            File outputFile=new File("/Users/zimmemi1/Desktop/test.png");
            System.out.println(outputFile);
            ImageIO.write(image, "png", outputFile);
            renderWindow.RemoveObserver(observerId);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
    }

    public void loadOffLimbPlane()
    {
        final Vector3D origin=new Vector3D(spacecraftPositionAdjusted[getCurrentSlice()]);
        Vector3D ul=new Vector3D(frustum1Adjusted[getCurrentSlice()]);
        Vector3D ur=new Vector3D(frustum3Adjusted[getCurrentSlice()]);
        Vector3D lr=new Vector3D(frustum4Adjusted[getCurrentSlice()]);
        Vector3D ll=new Vector3D(frustum2Adjusted[getCurrentSlice()]);
        Vector3D farCenter=new Vector3D(PolyDataUtil.computeFrustumFarPlaneCenter(origin.toArray(), ul.toArray(), ur.toArray(), lr.toArray(), ll.toArray()));
        final double depth=0;//origin.getNorm()*2;
        int res=(int)Math.sqrt(getFootprint(getDefaultSlice()).GetNumberOfPoints())*2;
        int[] resolution=new int[]{res,res};
        offLimbPlane=PolyDataUtil.generateFrustumPlane(origin.toArray(), ul.toArray(), ur.toArray(), lr.toArray(), ll.toArray(), depth, resolution);

        //double[] bounds=offLimbPlane.GetBounds();
        //double[] center=offLimbPlane.GetCenter();
        vtkPolyDataMapper mapper=new vtkPolyDataMapper();
        mapper.SetInputData(getSmallBodyModel().getSmallBodyPolyData());

        vtkActor actor=new vtkActor();
        actor.SetMapper(mapper);
        actor.GetProperty().SetColor(0,0,0);

        Vector3D midptlft=(ll.add(origin)).add(ul.add(origin)).scalarMultiply(1./2.);
        Vector3D midptbot=(ll.add(origin)).add(lr.add(origin)).scalarMultiply(1./2.);
        //Vector3D axis=farCenter.subtract(origin);
        //Vector3D lftRay=midptlft.subtract(origin);
        //Vector3D botRay=midptbot.subtract(origin);
        //Vector3D upVec=ur.subtract(lr);
        //double fovh=2*Math.toDegrees(Math.acos(lftRay.dotProduct(axis)/lftRay.getNorm()/axis.getNorm()));
        //double fovv=2*Math.toDegrees(Math.acos(botRay.dotProduct(axis)/botRay.getNorm()/axis.getNorm()));
        double aspect=Math.abs(midptlft.subtract(farCenter).getNorm()/midptbot.subtract(farCenter).getNorm());  // w/h

        double[] spacecraftPosition = new double[3];
        double[] focalPoint = new double[3];
        double[] upVector = new double[3];
        this.getCameraOrientation(spacecraftPosition, focalPoint, upVector);
        double fov=this.getMaxFovAngle();


        int szMax=Math.max(resolution[0], resolution[1]);
        szW=(int)(aspect*szMax);
        szH=szMax;

        canvas=new vtksbmtJoglCanvas();
        canvas.setSize(szW,szH);

        renderWindow=canvas.getRenderWindow();
        renderWindow.SetSize(new int[]{szW,szH});

        final vtkRenderer renderer=canvas.getRenderer();
        renderer.SetBackground(1,1,1);
        renderer.AddActor(actor);
        renderer.ResetCamera();
        renderer.GetActiveCamera().SetPosition(origin.toArray());
        renderer.GetActiveCamera().SetFocalPoint(farCenter.toArray());
        renderer.GetActiveCamera().SetClippingRange(0.1,depth);
        renderer.GetActiveCamera().SetViewAngle(fov);
        renderer.GetActiveCamera().SetViewUp(upVector);
        //renderWindow.OffScreenRenderingOn();
        renderWindow.AlphaBitPlanesOn();

        frame.add(canvas.getComponent());
        frame.setUndecorated(true);
        frame.setSize(szW,szH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        //observerId=renderWindow.AddObserver("RenderEvent", this, "test");
        canvas.getComponent().addGLEventListener(new GLEventListener()
        {

            @Override
            public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
                    int arg4)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void init(GLAutoDrawable arg0)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void dispose(GLAutoDrawable arg0)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void display(GLAutoDrawable arg0)
            {
                if (frame==null || !canvas.isWindowSet())
                    return;

                BufferedImage image=new BufferedImage(szW, szH, BufferedImage.TYPE_INT_RGB);
                frame.paint(image.getGraphics());
                vtkUnsignedCharArray array=new vtkUnsignedCharArray();
                canvas.getRenderWindow().GetRGBACharPixelData(0, 0, szW-1, szH-1, 1, array);
                int m=0;
                for (int i=0; i<szW; i++)
                    for (int j=0; j<szH; j++)
                    {
                        double[] rgba=array.GetTuple4(m);
                        int r=(int)rgba[0];
                        int g=(int)rgba[1];
                        int b=(int)rgba[2];
                        image.setRGB(i, j, new Color(r,g,b).getRGB());
                        m++;
                    }

                File outputFile=new File("/Users/zimmemi1/Desktop/test.png");
                try
                {
                    ImageIO.write(image, "png", outputFile);
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                vtkImageCanvasSource2D imageSource=new vtkImageCanvasSource2D();
                imageSource.SetScalarTypeToUnsignedChar();
                imageSource.SetNumberOfScalarComponents(4);
                imageSource.SetExtent(0, szW-1, 0, szH-1, 0, 0);
                int n=0;
                for (int j=0; j<szH; j++)
                    for (int i=0; i<szW; i++)
                    {
                        double[] rgba=array.GetTuple4(n);
                        imageSource.SetDrawColor(rgba);
                        imageSource.DrawPoint(i, j);
                        n++;
                    }
                imageSource.Update();

                /*                vtkPNGWriter writer=new vtkPNGWriter();
                writer.SetInputData(imageSource.GetOutput());
                writer.SetFileName("/Users/zimmemi1/Desktop/test.png");
                writer.Write();*/

                vtkContourFilter contourFilter=new vtkContourFilter();
                contourFilter.SetInputData(imageSource.GetOutput());
                contourFilter.SetValue(0, 255);
                contourFilter.Update();
                vtkPolyData contour=contourFilter.GetOutput();

                vtkPoints points=new vtkPoints();
                vtkCellArray cells=new vtkCellArray();
                int originId=points.InsertNextPoint(0,0,depth);
                for (int i=0; i<contour.GetNumberOfCells(); i++)
                {
                    vtkLine line=(vtkLine)contour.GetCell(i);
                    int[] ids=new int[2];
                    for (int j=0; j<2; j++)
                    {
                        Vector3D pt=new Vector3D(line.GetPoints().GetPoint(j)); // convert to 3d
                        ids[j]=points.InsertNextPoint(pt.toArray());
                    }
                    vtkTriangle tri=new vtkTriangle();
                    tri.GetPointIds().SetId(0, originId);
                    tri.GetPointIds().SetId(1, ids[0]);
                    tri.GetPointIds().SetId(2, ids[1]);
                    cells.InsertNextCell(tri);
                }
                vtkPolyData projectedHull=new vtkPolyData();
                projectedHull.SetPoints(points);
                projectedHull.SetPolys(cells);

/*                double[] bounds_data=getSmallBodyModel().getSmallBodyPolyData().GetBounds();
                double[] center_data=getSmallBodyModel().getSmallBodyPolyData().GetCenter();
                double[] bounds_contour=contour.GetBounds();
                double[] center_contour=contour.GetCenter();
                double trans_x=0., trans_y=0., trans_z=0., ratio_x=0., ratio_y=0.;

                ratio_x = (bounds_data[1]-bounds_data[0])/(bounds_contour[1]-bounds_contour[0]);
                ratio_y = (bounds_data[3]-bounds_data[2])/(bounds_contour[3]-bounds_contour[2]);*/


//                vtkMatrix4x4 cameraMat=renderer.GetActiveCamera().GetModelViewTransformMatrix();//.GetCameraLightTransformMatrix();
//                cameraMat.Invert();
//                vtkTransform transform=new vtkTransform();
//                transform.SetMatrix(cameraMat);
//                transform.Update();
                vtkMatrix4x4 matrix=renderer.GetActiveCamera().GetCameraLightTransformMatrix();
                //matrix.Invert();
                vtkTransform transform=new vtkTransform();
                transform.SetMatrix(matrix);
                transform.Update();
                vtkTransformFilter transformFilter=new vtkTransformFilter();
                transformFilter.SetInputData(projectedHull);
                transformFilter.SetTransform(transform);
                transformFilter.Update();
                final vtkPolyData transformedProjectedHull=transformFilter.GetPolyDataOutput();    // this polydata is now represented in the coordinate system where the camera is at 0,0,1 looking at focal point 0,0,0 and up vector 0,1,0

                vtkPolyDataWriter writer=new vtkPolyDataWriter();
                writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
                writer.SetFileTypeToBinary();
                //writer.SetInputData(contour);
                writer.SetInputData(contour);
                writer.Write();

              //  frame.dispose();
              //  frame=null;
            }
        });




        /*        vtkWindowToImageFilter imageFilter=new vtkWindowToImageFilter();
        imageFilter.SetInput(canvas.getRenderWindow());
        imageFilter.ReadFrontBufferOff();
        imageFilter.Update();

        vtkPNGWriter writer=new vtkPNGWriter();
        writer.SetInputData(imageFilter.GetOutput());
        writer.SetFileName("/Users/zimmemi1/Desktop/test.png");
        writer.Write();*/


/*
        vtkContourFilter contourFilter=new vtkContourFilter();
        contourFilter.SetInputData(imageFilter.GetOutput());
        contourFilter.SetValue(0, 255);
        contourFilter.Update();

        vtkPolyData contour=contourFilter.GetOutput();
        for (int i=0; i<contour.GetNumberOfPoints(); i++)
        {
            Vector3D p=new Vector3D(contour.GetPoint(i));
            System.out.println(p.getX()+" "+p.getY()+" "+p.getZ());
        }
        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
        writer.SetFileTypeToBinary();
        writer.SetInputData(contour);
        writer.Write();
*/
        if (true)
            return;


        vtkPoints newPoints=new vtkPoints();
        BiMap<Integer,Integer> newToOldPointMap=HashBiMap.create();
        for (int i=0; i<offLimbPlane.GetNumberOfPoints(); i++)
        {
            double[] pt=offLimbPlane.GetPoint(i);
            double tol = 1e-6;
            double[] t = new double[1];
            double[] x = new double[3];
            double[] pcoords = new double[3];
            int[] subId = new int[1];
            int[] cellId = new int[1];
            vtkGenericCell cell=new vtkGenericCell();
          //  int result = getSmallBodyModel().getCellLocator().IntersectWithLine(origin, pt, tol, t, x, pcoords, subId, cellId, cell);
            int result=0;

//            getSmallBodyModel().getCellLocator()..IntersectWithLine(origin, pt, intersections, null);
            if (result==0)   // no intersection
                newToOldPointMap.put(newPoints.InsertNextPoint(pt), i);
        }
        vtkCellArray newCells=new vtkCellArray();
        for (int i=0; i<offLimbPlane.GetNumberOfCells(); i++)
        {
            vtkTriangle tri=(vtkTriangle)offLimbPlane.GetCell(i);
            boolean allPointsSurvived=true;
            for (int j=0; j<3 && allPointsSurvived; j++)
            {
                if (!newToOldPointMap.values().contains(tri.GetPointId(j)))
                    allPointsSurvived=false;
            }
            if (allPointsSurvived)
            {
                vtkTriangle newTri=new vtkTriangle();
                for (int j=0; j<3; j++)
                    newTri.GetPointIds().SetId(j, newToOldPointMap.inverse().get(tri.GetPointId(j)));
                newCells.InsertNextCell(newTri);
            }
        }

        offLimbPlane=new vtkPolyData();
        offLimbPlane.SetPoints(newPoints);
        offLimbPlane.SetPolys(newCells);

        offLimbTextureCoords=new vtkFloatArray();
        vtkPointData pointData=offLimbPlane.GetPointData();
        pointData.SetTCoords(offLimbTextureCoords);
        PolyDataUtil.generateTextureCoordinates(getFrustum(), getImageWidth(), getImageHeight(), offLimbPlane);
        pointData.Delete();

/*        vtkAppendPolyData appendFilter=new vtkAppendPolyData();
        appendFilter.AddInputData(offLimbPlane);
        appendFilter.AddInputData(getSmallBodyModel().getSmallBodyPolyData());
        appendFilter.Update();

        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
        writer.SetFileTypeToBinary();
        writer.SetInputData(appendFilter.GetOutput());
        writer.Write();*/

    }

    @Override
    public List<vtkProp> getProps()
    {
        List<vtkProp> props=super.getProps();
        if (offLimbPlane==null)
        {
            if (offLimbPlane==null)
                loadOffLimbPlane();
            vtkPolyDataMapper mapper=new vtkPolyDataMapper();
            mapper.SetInputData(offLimbPlane);



            /*vtkImageData maskImage=new vtkImageData();
            int[] extent=image.GetExtent();
            maskImage.SetExtent(extent);
            maskImage.AllocateScalars(VtkDataTypes.VTK_DOUBLE, 4);
            for (int i=extent[0]; i<extent[1]; i++)
                for (int j=extent[2]; j<extent[3]; j++)
                {
                    double r=image.GetScalarComponentAsDouble(i, j, 0, 0);
                    double g=image.GetScalarComponentAsDouble(i, j, 0, 1);
                    double b=image.GetScalarComponentAsDouble(i, j, 0, 2);
                    maskImage.SetScalarComponentFromDouble(i, j, 0, 0, r);
                    maskImage.SetScalarComponentFromDouble(i, j, 0, 1, g);
                    maskImage.SetScalarComponentFromDouble(i, j, 0, 2, b);
                    maskImage.SetScalarComponentFromDouble(i, j, 0, 3, r==0 && g==0 && b==0 ? 0 : 1);
                }
*/
/*            vtkImageData image=getDisplayedImage();
            int[] extent=image.GetExtent();
            for (int i=extent[0]; i<extent[1]; i++)
                for (int j=extent[2]; j<extent[3]; j++)
                {
                    double r=image.GetScalarComponentAsDouble(i, j, 0, 0);
                    double g=image.GetScalarComponentAsDouble(i, j, 0, 1);
                    double b=image.GetScalarComponentAsDouble(i, j, 0, 2);
                    //double y=0.2126*r+0.7152*g+0.0722*b; // cf. https://en.wikipedia.org/wiki/Luma_%28video%29
                    if (r<0.1 && g<0.1 && b<0.1)
                        image.SetScalarComponentFromDouble(i, j, 0, 3, 0);
                }*/
//            System.out.println(image.GetNumberOfScalarComponents());

            vtkImageData image=getDisplayedImage();

            offLimbTexture = new vtkTexture();
            offLimbTexture.InterpolateOn();
            offLimbTexture.RepeatOff();
            offLimbTexture.EdgeClampOn();
            offLimbTexture.SetBlendingMode(3);
            offLimbTexture.SetInputData(image);

            vtkActor actor=new vtkActor();
            actor.SetMapper(mapper);
            actor.SetTexture(offLimbTexture);
            //actor.GetProperty().set
            props.add(actor);
        }
        return props;
    }
}
