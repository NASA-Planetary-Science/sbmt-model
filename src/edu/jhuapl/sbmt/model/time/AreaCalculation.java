package edu.jhuapl.sbmt.model.time;

import java.util.List;
import java.util.Set;

import javax.swing.ListModel;

import vtk.vtkProp;


public interface AreaCalculation extends ListModel
{
    public String getName();

    public Integer getId();

    public Integer getCurrentIndex();

    public void setCurrentIndex(Integer index);

    public String getCurrentTrajectoryName();

    public void load();

    public void put(Integer index, SurfacePatch impact);

    public Integer getLength();

    public Double getSurfacePatchValue(vtkProp prop, int cellId, double[] pickPosition);

    public SurfacePatch getValue(Integer index);

    public SurfacePatch getCurrentValue();

    public Set<String> getAllSurfacePatchNames();

    public void setCurrentTrajectory(String trajectoryName);

    public void initialize();

    public void redraw();

    public List<String> getPatchNames();

    public void setOffset(double offset);

    public void initializePatches();

    public void setShowPatches(Set<String> patchNames);

    public List<vtkProp> getVisibleActors();

    public void markPatchesOutOfDate();

    public String getCurrentDataField();

    public void setCurrentDataField(String fieldName);


    public SurfacePatch getCurrentPatch();

    public String getCurrentPatchName();

    public void setCurrentPatch(SurfacePatch currentPatch);

    public void setCurrentPatchName(String currentPatchName);

    public void setCurrentPatchIndex(Integer index);

    public SurfacePatch getPatchByCellId(int cellId);

    public SurfacePatch getPatch(vtkProp prop);

    public SurfacePatch getPatch(StructuredGridHeatMap heatMap);

    public SurfacePatch getPatchByIndex(int index);

    public SurfacePatch getPatchByName(String name);

    public ScalarRange getScalarRange();

    public void setScalarRange(ScalarRange scalarRange);

    public PolyDataHeatMap getPatchHeatMapActor(int index);
}
