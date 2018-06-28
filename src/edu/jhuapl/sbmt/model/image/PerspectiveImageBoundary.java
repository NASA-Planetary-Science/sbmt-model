package edu.jhuapl.sbmt.model.image;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkGenericCell;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.Image.ImageKey;

public class PerspectiveImageBoundary extends AbstractModel implements PropertyChangeListener
{
    private vtkActor actor;
    private vtkPolyData boundary;
    private vtkPolyDataMapper boundaryMapper;
    private double[] spacecraftPosition = new double[3];
    private double[] frustum1 = new double[3];
    private double[] frustum2 = new double[3];
    private double[] frustum3 = new double[3];
    private double[] boresightDirection = new double[3];
    private double[] upVector = new double[3];
    private PerspectiveImage image;
    private SmallBodyModel smallBodyModel;
    private static vtkPolyData emptyPolyData;
    private double offset =0.003;

    public PerspectiveImageBoundary(PerspectiveImage image, SmallBodyModel smallBodyModel) throws IOException
    {
        this.image = image;
        this.smallBodyModel = smallBodyModel;

        boundary = new vtkPolyData();
        boundary.SetPoints(new vtkPoints());
        boundary.SetVerts(new vtkCellArray());

        boundaryMapper = new vtkPolyDataMapper();
        actor = new vtkActor();

        emptyPolyData = new vtkPolyData();

        update();
    }

    public void update()
    {
        spacecraftPosition = image.getSpacecraftPosition();
        Frustum frus = image.getFrustum();
        frustum1 = frus.ul;
        frustum2 = frus.lr;
        frustum3 = frus.ur;
        boresightDirection = image.getBoresightDirection();
        upVector = image.getUpVector();

        initialize();

        smallBodyModel.shiftPolyLineInNormalDirection(boundary, offset);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setOffset(double offsetnew)
    {
        this.offset=offsetnew;

        update();
    }

    public double getOffset()
    {
        return offset;
    }

    public double getDefaultOffset()
    {
        // Subclasses should redefine this if they support offset.
        return 3.0;
    }

    private void initialize()
    {
        // Using the frustum, go around the boundary of the frustum and intersect with
        // the asteroid.

        boundary.DeepCopy(emptyPolyData);
        vtkPoints points = boundary.GetPoints();
        vtkCellArray verts = boundary.GetVerts();

        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(1);

        vtksbCellLocator cellLocator = smallBodyModel.getCellLocator();

        vtkGenericCell cell = new vtkGenericCell();

        // Note it doesn't matter what image size we use, even
        // if it's not the same size as the original image. Just
        // needs to large enough so enough points get drawn.
        final int IMAGE_WIDTH = 475;
        final int IMAGE_HEIGHT = 475;

        int count = 0;

        double[] corner1 = {
                spacecraftPosition[0] + frustum1[0],
                spacecraftPosition[1] + frustum1[1],
                spacecraftPosition[2] + frustum1[2]
        };
        double[] corner2 = {
                spacecraftPosition[0] + frustum2[0],
                spacecraftPosition[1] + frustum2[1],
                spacecraftPosition[2] + frustum2[2]
        };
        double[] corner3 = {
                spacecraftPosition[0] + frustum3[0],
                spacecraftPosition[1] + frustum3[1],
                spacecraftPosition[2] + frustum3[2]
        };
        double[] vec12 = {
                corner2[0] - corner1[0],
                corner2[1] - corner1[1],
                corner2[2] - corner1[2]
        };
        double[] vec13 = {
                corner3[0] - corner1[0],
                corner3[1] - corner1[1],
                corner3[2] - corner1[2]
        };

        //double horizScaleFactor = 2.0 * Math.tan( GeometryUtil.vsep(frustum1, frustum3) / 2.0 ) / IMAGE_HEIGHT;
        //double vertScaleFactor = 2.0 * Math.tan( GeometryUtil.vsep(frustum1, frustum2) / 2.0 ) / IMAGE_WIDTH;

        double scdist = MathUtil.vnorm(spacecraftPosition);

        for (int i=0; i<IMAGE_HEIGHT; ++i)
        {
            // Compute the vector on the left of the row.
            double fracHeight = ((double)i / (double)(IMAGE_HEIGHT-1));
            double[] left = {
                    corner1[0] + fracHeight*vec13[0],
                    corner1[1] + fracHeight*vec13[1],
                    corner1[2] + fracHeight*vec13[2]
            };

            for (int j=0; j<IMAGE_WIDTH; ++j)
            {
                if (j == 1 && i > 0 && i < IMAGE_HEIGHT-1)
                {
                    j = IMAGE_WIDTH-2;
                    continue;
                }

                double fracWidth = ((double)j / (double)(IMAGE_WIDTH-1));
                double[] vec = {
                        left[0] + fracWidth*vec12[0],
                        left[1] + fracWidth*vec12[1],
                        left[2] + fracWidth*vec12[2]
                };
                vec[0] -= spacecraftPosition[0];
                vec[1] -= spacecraftPosition[1];
                vec[2] -= spacecraftPosition[2];
                MathUtil.unorm(vec, vec);

                double[] lookPt = {
                        spacecraftPosition[0] + 2.0*scdist*vec[0],
                        spacecraftPosition[1] + 2.0*scdist*vec[1],
                        spacecraftPosition[2] + 2.0*scdist*vec[2]
                };

                double tol = 1e-6;
                double[] t = new double[1];
                double[] x = new double[3];
                double[] pcoords = new double[3];
                int[] subId = new int[1];
                int[] cellId = new int[1];
                int result = cellLocator.IntersectWithLine(spacecraftPosition, lookPt, tol, t, x, pcoords, subId, cellId, cell);

                if (result > 0)
                {
                    double[] closestPoint = x;

                    //double horizPixelScale = closestDist * horizScaleFactor;
                    //double vertPixelScale = closestDist * vertScaleFactor;

                    points.InsertNextPoint(closestPoint);
                    idList.SetId(0, count);
                    verts.InsertNextCell(idList);

                    ++count;
                }
            }
        }


        PolyDataUtil.shiftPolyLineInNormalDirectionOfPolyData(
                boundary,
                smallBodyModel.getSmallBodyPolyData(),
                smallBodyModel.getCellNormals(),
                smallBodyModel.getCellLocator(),
                3.0*smallBodyModel.getMinShiftAmount());

        boundary.Modified();
        boundaryMapper.SetInputData(boundary);

        actor.SetMapper(boundaryMapper);
        actor.GetProperty().SetColor(1.0, 0.0, 0.0);
        actor.GetProperty().SetPointSize(1.0);
    }

    public void firePropertyChange()
    {
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
//      The following code seems broken and causes performance problems and issues with the colors
//      of the image list
//        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
//        {
////            System.out.println("Boundary MODEL_CHANGED event: " + evt.getSource().getClass().getSimpleName());
//            initialize();
//            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//        }
    }

    @Override
    public List<vtkProp> getProps()
    {
        List<vtkProp> props = new ArrayList<vtkProp>();
        props.add(actor);
        return props;
    }

    public ImageKey getKey()
    {
        return image.getKey();
    }

    public PerspectiveImage getImage()
    {
        return image;
    }

    public void getCameraOrientation(double[] spacecraftPosition,
            double[] focalPoint, double[] upVector)
    {
        for (int i=0; i<3; ++i)
        {
            spacecraftPosition[i] = this.spacecraftPosition[i];
            upVector[i] = this.upVector[i];
        }

        // Normalize the direction vector
        double[] direction = new double[3];
        MathUtil.unorm(boresightDirection, direction);

        int cellId = smallBodyModel.computeRayIntersection(spacecraftPosition, direction, focalPoint);

        if (cellId < 0)
        {
            BoundingBox bb = new BoundingBox(boundary.GetBounds());
            double[] centerPoint = bb.getCenterPoint();
            //double[] centerPoint = boundary.GetPoint(0);
            double distanceToCenter = MathUtil.distanceBetween(spacecraftPosition, centerPoint);

            focalPoint[0] = spacecraftPosition[0] + distanceToCenter*direction[0];
            focalPoint[1] = spacecraftPosition[1] + distanceToCenter*direction[1];
            focalPoint[2] = spacecraftPosition[2] + distanceToCenter*direction[2];
        }
    }

    public void setBoundaryColor(Color color)
    {
        double r = color.getRed();
        double g = color.getGreen();
        double b = color.getBlue();
        actor.GetProperty().SetColor(r/255.0, g/255.0, b/255.0);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public int[] getBoundaryColor()
    {
        double[] c = new double[3];
        actor.GetProperty().GetColor(c);
        return new int[] {(int) (c[0]*255.0), (int) (c[1]*255.0), (int) (c[2]*255.0)};
    }

}
