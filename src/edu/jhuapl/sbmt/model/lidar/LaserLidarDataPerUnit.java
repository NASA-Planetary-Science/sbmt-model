package edu.jhuapl.sbmt.model.lidar;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkGeometryFilter;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkUnsignedCharArray;
import vtk.vtkVertex;

import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.SaavtkLODActor;
import edu.jhuapl.saavtk.util.SafePaths;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.laser.LaserRawLidarFile;

public class LaserLidarDataPerUnit extends LidarDataPerUnit
{

    @Override
    void init() throws IOException
    {
        File file = FileCache.getFileFromServer(SafePaths.getString(path));

        if (file == null)
            throw new IOException(path + " could not be loaded");

        filepath = path;


        polydata = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray vert = new vtkCellArray();
        polydata.SetPoints( points );
        polydata.SetVerts( vert );
        vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
        colors.SetNumberOfComponents(4);
        polydata.GetCellData().SetScalars(colors);

        polydataSc = new vtkPolyData();
        vtkPoints pointsSc = new vtkPoints();
        vtkCellArray vertSc = new vtkCellArray();
        polydataSc.SetPoints( pointsSc );
        polydataSc.SetVerts( vertSc );
        times = new vtkDoubleArray();
        ranges = new vtkDoubleArray();

        // Variables to keep track of intensities
        double minIntensity = Double.POSITIVE_INFINITY;
        double maxIntensity = Double.NEGATIVE_INFINITY;
        List<Double> intensityList = new LinkedList<Double>();

        LaserRawLidarFile lidarFile=new LaserRawLidarFile(file.getAbsolutePath());
        Iterator<LidarPoint> it=lidarFile.iterator();
        int cnt=0;
        while (it.hasNext())
        {
            LidarPoint pt=it.next();
            int id=points.InsertNextPoint(pt.getTargetPosition().toArray());
            vtkVertex v=new vtkVertex();
            v.GetPointIds().SetId(0, id);;
            vert.InsertNextCell(v);
            ranges.InsertNextValue(pt.getSourcePosition().subtract(pt.getTargetPosition()).getNorm());
            // Add to list and keep track of min/max encountered so far
            double irec=pt.getIntensityReceived();
            minIntensity = (irec < minIntensity) ? irec : minIntensity;
            maxIntensity = (irec > maxIntensity) ? irec : maxIntensity;
            intensityList.add(irec);
            int id2=pointsSc.InsertNextPoint(pt.getSourcePosition().toArray());
            vtkVertex v2=new vtkVertex();
            v2.GetPointIds().SetId(0, id2);
            vertSc.InsertNextCell(v2);
            times.InsertNextValue(pt.getTime());
            cnt++;
        }

        // Set base color
        Color baseColor = new Color(0, 0, 255);


        // Color each point based on base color scaled by intensity
        Color plotColor;
        float[] baseHSL = ColorUtil.getHSLColorComponents(baseColor);
        for(double intensity : intensityList)
        {
            plotColor = ColorUtil.scaleLightness(baseHSL, intensity, minIntensity, maxIntensity, 0.5f);
            colors.InsertNextTuple4(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), plotColor.getAlpha());
        }

        polydata.GetCellData().GetScalars().Modified();
        polydata.Modified();

        originalPoints = new vtkPoints();
        originalPoints.DeepCopy(points);

        originalPointsSc = new vtkPoints();
        originalPointsSc.DeepCopy(pointsSc);

        geometryFilter = new vtkGeometryFilter();
        geometryFilter.SetInputData(polydata);
        geometryFilter.PointClippingOn();
        geometryFilter.CellClippingOff();
        geometryFilter.ExtentClippingOff();
        geometryFilter.MergingOff();
        geometryFilter.SetPointMinimum(0);
        geometryFilter.SetPointMaximum(cnt);

        geometryFilterSc = new vtkGeometryFilter();
        geometryFilterSc.SetInputData(polydataSc);
        geometryFilterSc.PointClippingOn();
        geometryFilterSc.CellClippingOff();
        geometryFilterSc.ExtentClippingOff();
        geometryFilterSc.MergingOff();
        geometryFilterSc.SetPointMinimum(0);
        geometryFilterSc.SetPointMaximum(cnt);

        vtkPolyDataMapper pointsMapper = new vtkPolyDataMapper();
        pointsMapper.SetScalarModeToUseCellData();
        pointsMapper.SetInputConnection(geometryFilter.GetOutputPort());

        vtkActor actor = new SaavtkLODActor();
        actor.SetMapper(pointsMapper);
        vtkPolyDataMapper lodMapper = ((SaavtkLODActor)actor).setQuadricDecimatedLODMapper(geometryFilter.GetOutputPort());

        actor.GetProperty().SetPointSize(2.0);

        vtkPolyDataMapper pointsMapperSc = new vtkPolyDataMapper();
        pointsMapperSc.SetInputConnection(geometryFilterSc.GetOutputPort());

        actorSpacecraft = new SaavtkLODActor();
        actorSpacecraft.SetMapper(pointsMapperSc);
        ((SaavtkLODActor)actorSpacecraft).setQuadricDecimatedLODMapper(geometryFilterSc.GetOutputPort());
        actorSpacecraft.GetProperty().SetColor(0.0, 1.0, 0.0);
        // for Itokawa optimized lidar data, show in different color.
        if (path.contains("_v2"))
            actorSpacecraft.GetProperty().SetColor(1.0, 0.0, 1.0);

        actorSpacecraft.GetProperty().SetPointSize(2.0);

        actors.add(actor);
        actors.add(actorSpacecraft);
    }

    public LaserLidarDataPerUnit(String path,
            BodyViewConfig polyhedralModelConfig) throws IOException
    {
        super(path, polyhedralModelConfig);
        offsetMultiplier=1e6;
    }

}

