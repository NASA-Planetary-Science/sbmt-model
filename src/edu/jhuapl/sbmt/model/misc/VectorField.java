package edu.jhuapl.sbmt.model.misc;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkArrowSource;
import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkGlyph3D;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.app.SmallBodyModel;

/**
 * Model for showing gravity vector field. Currently only used for testing,
 * not the released versions.
 */
public class VectorField extends AbstractModel implements PropertyChangeListener
{
    private PolyhedralModel smallBodyModel;
    private List<vtkProp> actors = new ArrayList<vtkProp>();
    private boolean generated = false;
    private vtkActor actor;
    private vtkPolyDataMapper mapper;

    public VectorField(SmallBodyModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
        smallBodyModel.addPropertyChangeListener(this);
    }

    @Override
    public List<vtkProp> getProps()
    {
        return actors;
    }

    private void update()
    {
        // There is no need to regenerate the data if generated is true
        if (generated)
            return;

        vtkPolyData smallBodyPolyData = smallBodyModel.getSmallBodyPolyData();

        vtkPolyData polydata = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray vert = new vtkCellArray();
        polydata.SetPoints( points );
        polydata.SetVerts( vert );
        vtkDataArray gravityVectors = smallBodyModel.getGravityVectorData();
        polydata.GetPointData().SetVectors(gravityVectors);

        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(1);

        double[] center = new double[3];
        int numberOfCells = smallBodyPolyData.GetNumberOfCells();
        for (int i=0; i<numberOfCells; ++i)
        {
            vtkTriangle cell = (vtkTriangle) smallBodyPolyData.GetCell(i);
            vtkPoints cellpoints = cell.GetPoints();
            double[] pt0 = cellpoints.GetPoint(0);
            double[] pt1 = cellpoints.GetPoint(1);
            double[] pt2 = cellpoints.GetPoint(2);
            cell.TriangleCenter(pt0, pt1, pt2, center);

            points.InsertNextPoint(center);
            idList.SetId(0, i);
            vert.InsertNextCell(idList);
        }

        vtkArrowSource arrowSource = new vtkArrowSource();
        arrowSource.SetTipResolution(1);
        arrowSource.SetTipRadius(0.05);
        arrowSource.SetShaftResolution(2);
        arrowSource.SetShaftRadius(0.005);

        vtkGlyph3D glyph3D = new vtkGlyph3D();
        glyph3D.SetSourceConnection(arrowSource.GetOutputPort());
        glyph3D.SetVectorModeToUseVector();
        glyph3D.SetInputData(polydata);
        glyph3D.SetScaleFactor(.01);
        glyph3D.Update();

        if (mapper == null)
            mapper = new vtkPolyDataMapper();
        mapper.SetInputConnection(glyph3D.GetOutputPort());
        mapper.Update();

        if (actor == null)
            actor = new vtkActor();
        actor.SetMapper(mapper);
        actor.GetProperty().SetColor(1.0, 0.0, 0.0);
        actor.PickableOff();
    }

    public void setShowVectorField(boolean show)
    {
        if (show == true && actors.size() == 0)
        {
            update();
            actors.add(actor);
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
        else if (show == false && actors.size() > 0)
        {
            actors.clear();
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            generated = false;
            if (actors.size() > 0)
                update();
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }
}
