package edu.jhuapl.sbmt.model.eros;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataReader;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.FileCache;

public class NLRDataEverything extends AbstractModel
{
    private ArrayList<vtkProp> actors = new ArrayList<vtkProp>();

    public NLRDataEverything()
    {
        setVisible(false);
    }

    private void initialize()
    {
        System.out.println("initializing");
        File file = FileCache.getFileFromServer("/NLR/nlrdata.vtk.gz");

        if (file == null)
        {
            System.out.println(file + " could not be loaded");
            return;
        }

        vtkPolyDataReader nlrReader = new vtkPolyDataReader();
        nlrReader.SetFileName(file.getAbsolutePath());

        vtkPolyDataMapper pointsMapper = new vtkPolyDataMapper();
        pointsMapper.SetInputConnection(nlrReader.GetOutputPort());
        pointsMapper.Update();

        vtkActor actor = new vtkActor();
        actor.SetMapper(pointsMapper);
        actor.GetProperty().SetColor(0.0, 0.0, 1.0);
        actor.GetProperty().SetPointSize(1.0);

        actors.add(actor);
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        return "NLR data";
    }

    public List<vtkProp> getProps()
    {
        if (actors.isEmpty())
            initialize();

        return actors;
    }
}

