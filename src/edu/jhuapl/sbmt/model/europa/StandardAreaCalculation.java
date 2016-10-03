package edu.jhuapl.sbmt.model.europa;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ListDataListener;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.sbmt.client.SmallBodyModel;

public class StandardAreaCalculation implements AreaCalculation
{
    private int[] boundaryColor = {255, 0, 255, 255};
    private double offset = 3.0;

    private List<SurfacePatch> surfacePatches = new ArrayList<SurfacePatch>();


    private List<String> patchNames = new ArrayList<String>();
    private HashMap<String, Integer> nameToPatchIndex = new HashMap<String, Integer>();
    private HashMap<String, SurfacePatch> nameToPatch = new HashMap<String, SurfacePatch>();
    private HashMap<Integer, SurfacePatch> indexToPatch = new HashMap<Integer, SurfacePatch>();
    private HashMap<SurfacePatch, Integer> patchToIndex = new HashMap<SurfacePatch, Integer>();
    private HashMap<Integer, SurfacePatch> cellIdToPatch = new HashMap<Integer, SurfacePatch>();
    private HashMap<vtkProp, SurfacePatch> propToPatch = new HashMap<vtkProp, SurfacePatch>();
    private HashMap<PolyDataHeatMap, SurfacePatch> heatMapToPatch = new HashMap<PolyDataHeatMap, SurfacePatch>();

    private int npatches;
    private SurfacePatch patches[];
    private vtkPolyData patchPolylines[];
    private vtkActor patchBorderActors[];
    private vtkPolyData patchMeshes[];
    private PolyDataHeatMap patchHeatMapActors[];
    private List<vtkProp> areaCalculationActors = new ArrayList<vtkProp>();

    public PolyDataHeatMap getPatchHeatMapActor(int index)
    {
        return patchHeatMapActors[index];
    }

    private String currentPatchName;
    private SurfacePatch currentPatch;
    private Set<String> visiblePatches;

    private ScalarRange scalarRange = new ScalarRange();

    public ScalarRange getScalarRange() { return scalarRange; }
    public void setScalarRange(ScalarRange scalarRange)
    {
        this.scalarRange = scalarRange;
    }



    private String directoryName;
    private List<String> areaCalculationFiles;

    public Double getSurfacePatchValue(vtkProp prop, int cellId, double[] pickPosition)
    {
        Double result = null;
        SurfacePatch pickedPatch = getPatch(prop);
        String patchName = pickedPatch.getName();
        Integer patchIndex = nameToPatchIndex.get(patchName);
        if (patchIndex != null && cellId < patchMeshes[patchIndex].GetNumberOfCells())
        {
            vtkPolyData patchMesh = patchMeshes[patchIndex];

            System.out.println("CellId: " + cellId + " / " + patchMesh.GetNumberOfCells());
            vtkIdList idList = new vtkIdList();
            patchMesh.GetCellPoints(cellId, idList);

            int numberOfCells = idList.GetNumberOfIds();
            if (numberOfCells != 3)
            {
                System.err.println("Error: Cells must have exactly 3 vertices!");
                return null;
            }

            // should use PolyDataUtil.interpolateWithinCell() to get a more accurate value. -turnerj1
            Integer pointId = idList.GetId(0);

            result = pickedPatch.getScalarValue(prop, pointId, pickPosition);
            String typeName = pickedPatch.getCurrentDataType();
            System.out.println("Surface Patch: " + pickedPatch.getName() + " has " + typeName + " value of: " + result);
        }

        return result;
    }


    public void setOffset(double offset)
    {
        this.offset = offset;
        System.out.println("New Offset: " + offset);
        // recreate the polygon data with the new offset
        this.markPatchesOutOfDate();
        this.initializePatches(npatches);
    }

    public List<String> getPatchNames()
    {
        return patchNames;
    }

    public SurfacePatch getCurrentPatch()
    {
        return currentPatch;
    }

    public String getCurrentPatchName()
    {
        return currentPatchName;
    }

    public void setCurrentPatch(SurfacePatch currentPatch)
    {
        this.currentPatch = currentPatch;
        currentIndex = this.getPatchIndex(currentPatch);
        currentPatchName = currentPatch != null ? Integer.toString(currentPatch.getId()) : null;
//        currentFlybyStateHistory = nameToFlybyStateHistory.get(currentTrajectoryName);
//        this.setTimeFraction(0.0);
    }

    public void setCurrentPatchName(String currentPatchName)
    {
        SurfacePatch patch = nameToPatch.get(currentPatchName);
        setCurrentPatch(patch);
    }

    public void setCurrentPatchIndex(Integer index)
    {
        if (index != null)
            setCurrentPatch(getPatchByIndex(index));
    }


    public SurfacePatch getPatchByCellId(int cellId)
    {
        return this.cellIdToPatch.get(cellId);
    }

    public SurfacePatch getPatch(vtkProp prop)
    {
        return this.propToPatch.get(prop);
    }

    public SurfacePatch getPatch(StructuredGridHeatMap heatMap)
    {
        return this.heatMapToPatch.get(heatMap);
    }

    public SurfacePatch getPatchByIndex(int index)
    {
        return this.indexToPatch.get(index);
    }

    public Integer getPatchIndex(SurfacePatch patch)
    {
        return this.patchToIndex.get(patch);
    }

    public SurfacePatch getPatchByName(String name)
    {
//        return getTrajectoryByCellId(nameToTrajectoryId.get(name));
        return nameToPatch.get(name);
    }


    public void markPatchesOutOfDate()
    {
        patchBorderActors = null;
    }

    public void setShowPatches(Set<String> patchNames)
    {
        this.visiblePatches = patchNames;
        updateActorVisibility();
    }

    public void redraw()
    {
        markPatchesOutOfDate();
        updateActorVisibility();
    }

    private void updateActorVisibility()
    {
        initializePatches();

        areaCalculationActors.clear();

        for (int ipatch=0; ipatch<npatches; ipatch++)
        {
            if (visiblePatches != null && visiblePatches.contains(Integer.toString(patches[ipatch].getId())))
                areaCalculationActors.add(patchBorderActors[ipatch]);
        }

        for (int ipatch=0; ipatch<npatches; ipatch++)
        {
            if (visiblePatches != null && visiblePatches.contains(Integer.toString(patches[ipatch].getId())))
                areaCalculationActors.add(patchHeatMapActors[ipatch].getHeatMapActor());
        }
    }

    public List<vtkProp> getVisibleActors()
    {
        return areaCalculationActors;
    }

    public void initialize()
    {
        npatches = 0;
        npatches = 0;
        patchNames.clear();
        nameToPatch.clear();
        nameToPatchIndex.clear();
        indexToPatch.clear();
        patchToIndex.clear();
        cellIdToPatch.clear();
        propToPatch.clear();
        heatMapToPatch.clear();

        patchNames.clear();

        markPatchesOutOfDate();
        initializePatches(getSize());

        Integer firstIndex = 0;
        this.setCurrentPatchIndex(firstIndex);


    }

    public void initializePatches()
    {
        initializePatches(this.npatches);
    }

    public void initializePatches(int npatches)
    {
        this.npatches = npatches;

        if (patchBorderActors == null)
        {
            patches = new SurfacePatch[npatches];
            // iterate over the patches
            for (int ipatch=0; ipatch<npatches; ipatch++)
            {

                String name = Integer.toString(ipatch);
                patchNames.add(name);
                patches[ipatch] = getValue(ipatch);

                nameToPatch.put(name, patches[ipatch]);
                nameToPatchIndex.put(name, ipatch);
                indexToPatch.put(ipatch, patches[ipatch]);
                patchToIndex.put(patches[ipatch], ipatch);
            }

            createSurfacePatchPolygon();

            patchBorderActors = new vtkActor[npatches];
            patchHeatMapActors = new PolyDataHeatMap[npatches];

//            visiblePatches.clear();
            scalarRange.clearMinMax();

            for (int ipatch=0; ipatch<npatches; ipatch++)
            {
                SurfacePatch patch = patches[ipatch];

                // patch border
                vtkPolyDataMapper patchBorderMapper = new vtkPolyDataMapper();
                patchBorderMapper.SetInputData(patchPolylines[ipatch]);
                vtkActor patchBorderActor = new vtkActor();
                patchBorderActors[ipatch] = patchBorderActor;
                patchBorderActors[ipatch].SetMapper(patchBorderMapper);
                // for now, only pick on the surface patch, not the border
//                propToPatch.put(patchBorderActor, patch);

                // calculate pass min/max values
                Map<String, List<Double>> surfaceColumns = patch.getSurfaceDataColumns();
                scalarRange.minMaxSurfaceColumns(surfaceColumns);

//                double[] normalizedScalarArray = patch.getNormalizedSurfaceData(currentDataField);
//                PolyDataHeatMap patchHeatMap = createHeatMap(patchMeshes[ipatch], normalizedScalarArray);

            }

            for (int ipatch=0; ipatch<npatches; ipatch++)
            {
                SurfacePatch patch = patches[ipatch];

                // patch heat map
                String currentDataField = (String)patch.getSelectedItem();

                double[] scalarArray = patch.getSurfaceData(currentDataField);
                double[] range = patch.getSurfaceDataRange(currentDataField);
                System.out.println("Range " + ipatch + ": " + range[0] + ", " + range[1]);
                PolyDataHeatMap patchHeatMap = createHeatMap(patchMeshes[ipatch], scalarArray);
                patchHeatMap.setRange(range);

                patchHeatMapActors[ipatch] = patchHeatMap;
                propToPatch.put(patchHeatMap.getHeatMapActor(), patch);
                heatMapToPatch.put(patchHeatMap, patch);
            }

            System.out.println("Created " + this.patches.length + "Patches");
        }
    }

    private void createSurfacePatchPolygon()
    {
        patchPolylines = new vtkPolyData[npatches];
        patchBorderActors = new vtkActor[npatches];
        patchMeshes = new vtkPolyData[npatches];
        patchHeatMapActors = new PolyDataHeatMap[npatches];

        System.out.println("Calculating patch polys with offset: " + offset);
        int cellId = 0;
        vtkIdList idList = new vtkIdList();

        for (int ipatch=0; ipatch<npatches; ipatch++)
        {
            vtkPoints points = new vtkPoints();
            vtkCellArray lines = new vtkCellArray();
            vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
            colors.SetNumberOfComponents(4);

            SurfacePatch patch =  patches[ipatch];

            int size = patch.getBoundarySize();
            double[][] boundary = patch.getBoundaryVertex();
            idList.SetNumberOfIds(size+1);

            for (int j=0;j<size;++j)
            {
                Double x = boundary[j][0];
                Double y = boundary[j][1];
                Double z = boundary[j][2];

                // calculate offset above surface of planet so boundaries are not obscured
                double radius = Math.sqrt(x*x + y*y + z*z);
                double xhat = x / radius;
                double yhat = y / radius;
                double zhat = z / radius;
                double xoffset = offset * xhat;
                double yoffset = offset * yhat;
                double zoffset = offset * zhat;

                points.InsertNextPoint(x + xoffset, y + yoffset, z + zoffset);
                idList.SetId(j, j);
            }

            idList.SetId(size, 0);

            lines.InsertNextCell(idList);
            colors.InsertNextTuple4(boundaryColor[0], boundaryColor[1], boundaryColor[2], boundaryColor[3]);

            cellIdToPatch.put(cellId, patch);
            ++cellId;

            vtkPolyData patchPolyline = new vtkPolyData();
            patchPolyline.SetPoints(points);
            patchPolyline.SetLines(lines);
            patchPolyline.GetCellData().SetScalars(colors);
            patchPolylines[ipatch] = patchPolyline;

            vtkPolyData patchMesh = this.createSurfacePatchMesh(ipatch, patch);
            patchMeshes[ipatch] = patchMesh;
        }
    }

    private vtkPolyData createSurfacePatchMesh(int ipatch, SurfacePatch patch)
    {
        // patch vertices
        double[][] patchVertex = patch.getPatchVertex();
        int[][] patchIndex = patch.getPatchIndex();
        int nvertices = patch.getPatchSize();

        // expand to full array

        // determine max i, j values
        int maxi = 0;
        int maxj = 0;
        for(int v=0; v<nvertices; v++)
        {
            int i = patchIndex[v][0];
            int j = patchIndex[v][1];
            if (i > maxi) maxi = i;
            if (j > maxj) maxj = j;
        }

        int isize = maxi+1;
        int jsize = maxj+1;

        // create a grid of vertex ids, where -1 represents a missing vertex

        // initialize
        int[][] vidgrid = new int[isize][jsize];
        for (int i=0; i<isize; i++)
            for (int j=0; j<jsize; j++)
                vidgrid[i][j] = -1;

        // populate the vertex id grid
        for(int vid=0; vid<nvertices; vid++)
        {
            int i = patchIndex[vid][0];
            int j = patchIndex[vid][1];
            vidgrid[i][j] = vid;
        }


        // copy into vtkPolyData
        vtkPolyData result = new vtkPolyData();

        // fill the vtkPoints, with offset
        vtkPoints surfacePatchPoints = new vtkPoints();
        for(int vid=0; vid<nvertices; vid++)
        {
            double[] v = patchVertex[vid];

            // calculate offset above surface of planet so boundaries are not obscured
            double radius = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
            double vhat[] = { v[0]/radius, v[1]/radius, v[2]/radius };
            double voffset[] = { v[0] + offset*vhat[0], v[1] + offset*vhat[1], v[2] + offset*vhat[2] };

            surfacePatchPoints.InsertNextPoint(voffset[0], voffset[1], voffset[2]);
        }

        // fill in the vtkCellArray
        vtkCellArray surfacePatchPolys = new vtkCellArray();

        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(3);

        for (int i=1; i<isize; ++i)
            for (int j=1; j<jsize; ++j)
            {
                // Get the indices of the 4 corners of the rectangle to the upper left
                int i0 = vidgrid[i-1][j-1];
                int i1 = vidgrid[i][j-1];
                int i2 = vidgrid[i-1][j];
                int i3 = vidgrid[i][j];

                // Add upper left triangle
                if (i0>=0 && i1>=0 && i2>=0)
                {
                    idList.SetId(0, i0);
                    idList.SetId(1, i2);
                    idList.SetId(2, i1);
                    surfacePatchPolys.InsertNextCell(idList);
                }
                // Add bottom right triangle
                if (i2>=0 && i1>=0 && i3>=0)
                {
                    idList.SetId(0, i2);
                    idList.SetId(1, i3);
                    idList.SetId(2, i1);
                    surfacePatchPolys.InsertNextCell(idList);
                }
            }

        result.SetPoints(surfacePatchPoints);
        result.SetPolys(surfacePatchPolys);

        return result;
    }

    public PolyDataHeatMap createHeatMap(vtkPolyData source, double[] scalars)
    {
        PolyDataHeatMap result = new PolyDataHeatMap(source);

        double hueRange[] = { 0.7, 0.0 };
        double satRange[] = { 0.9, 0.9 };
        double valueRange[] = { 1.0, 1.0 };
        result.makeHSVRangeCLUT(hueRange, satRange, valueRange).setupMapperAndActor();


//      result.makeDefaultCLUT().setupMapperAndActor();

        result.setOpacity(1.0);
        result.setInterpolationToGouraud();

        result.mapScalarsToVertices(scalars);
//        result.mapScalarsToPolys(scalars, true);

        return result;
    }


    public StandardAreaCalculation(String directoryName, List<String> areaCalculationFiles, String name, Integer id, SimulationRun simulationRun, SmallBodyModel smallBodyModel)
    {
        this.directoryName = directoryName;
        this.areaCalculationFiles = areaCalculationFiles;
        this.name = name;
        this.id = id;
    }

    private String currentTrajectoryName;
    public String getCurrentTrajectoryName()
    {
        return currentTrajectoryName;
    }

    public void setCurrentTrajectory(String trajectoryName)
    {
        this.currentTrajectoryName = trajectoryName;
        surfacePatches.clear();
    }

    public void load()
    {
        String trajectoryName = getCurrentTrajectoryName();
        System.out.println("Initializing Area Calculation: " + name + " for " + trajectoryName);

        File areaFile = null;
        File boundaryFile = null;
        File patchFile = null;

        if (directoryName != null)
        {
            String areaCalculationDirName = directoryName + File.separator + name;
            File areaCalculationDir = new File(areaCalculationDirName);
            if (areaCalculationDir.isDirectory())
            {
                File[] files = areaCalculationDir.listFiles();
                for (File file : files)
                {
                    String fileName = file.getName();
                    if (fileName.startsWith(trajectoryName))
                    {
                        System.out.println("Checking SurfacePatch File: " + file.getName());
                        if (fileName.endsWith("_area.txt"))
                            areaFile = file;
                        else if (fileName.endsWith("_bound.csv"))
                            boundaryFile = file;
                        else if (fileName.endsWith("_patch.csv"))
                            patchFile = file;
                    }
                }
            }
        }

        if (areaCalculationFiles != null)
        {
            int nfiles = areaCalculationFiles.size();
            File[] files = new File[nfiles];
            for (int i=0; i<nfiles; i++)
            {
                String fileName = areaCalculationFiles.get(i);
                String filePath = directoryName + File.separator + fileName;
                File file = new File(filePath);
                if (fileName.startsWith(trajectoryName))
                {
                    System.out.println("Checking SurfacePatch File: " + file.getName());
                    if (fileName.endsWith("_area.txt"))
                        areaFile = file;
                    else if (fileName.endsWith("_bound.csv"))
                        boundaryFile = file;
                    else if (fileName.endsWith("_patch.csv"))
                        patchFile = file;
                }
            }
        }

        if (areaFile != null && boundaryFile != null && patchFile != null)
        {
            System.out.println("Parsing SurfacePatch Files: " + areaFile.getName() + ", " + boundaryFile.getName() + ", " + patchFile.getName());
            surfacePatches = CsvSurfacePatch.parseCsvFiles(areaFile, boundaryFile, patchFile, scalarRange);
            npatches = surfacePatches.size();
        }
        else
        {
            surfacePatches = new ArrayList<SurfacePatch>();
            npatches = 0;
        }

    }

    private String name;
    public String getName()
    {
        return name;
    }

    private Integer id;
    public Integer getId()
    {
        return id;
    }

    private Integer currentIndex;
    public Integer getCurrentIndex()
    {
        return currentIndex != null ? currentIndex : 0;
    }

    public void setCurrentIndex(Integer currentIndex)
    {
        this.currentIndex = currentIndex;
    }

    public Integer getLength()
    {
        return surfacePatches.size();
    }

    public void put(Integer index, SurfacePatch surfacePatch)
    {
        surfacePatches.add(index, surfacePatch);
    }

    public SurfacePatch getValue(Integer index)
    {
        return surfacePatches.size() > 0 ? surfacePatches.get(index) : null;
    }

    public SurfacePatch getCurrentValue()
    {
        return getValue(currentIndex);
    }

    public Set<String> getAllSurfacePatchNames()
    {
        Set<String> result = new HashSet<String>();
        for (int i=0; i<getLength(); i++)
            result.add(Integer.toString(getValue(i).getId()));
        return result;
    }

    public String toString()
    {
        return getName();
    }

    @Override
    public void addListDataListener(ListDataListener arg0)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getElementAt(int index)
    {
        return getValue(index);
    }

    @Override
    public int getSize()
    {
        return getLength();
    }

    @Override
    public void removeListDataListener(ListDataListener arg0)
    {
        // TODO Auto-generated method stub

    }


    private String currentDataField = null;

    @Override
    public String getCurrentDataField()
    {
        return currentDataField;
    }


    @Override
    public void setCurrentDataField(String fieldName)
    {
        this.currentDataField = fieldName;
        for (SurfacePatch surfacePatch : surfacePatches)
            surfacePatch.setSelectedItem(fieldName);
    }




}
