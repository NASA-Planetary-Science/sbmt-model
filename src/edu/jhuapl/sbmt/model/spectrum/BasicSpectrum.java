package edu.jhuapl.sbmt.model.spectrum;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkDoubleArray;
import vtk.vtkFeatureEdges;
import vtk.vtkIdList;
import vtk.vtkIdTypeArray;
import vtk.vtkLine;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.bennu.SearchSpec;


public abstract class BasicSpectrum extends Spectrum
{
    PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    protected SmallBodyModel smallBodyModel;
    protected SpectralInstrument instrument;

    protected String fullpath; // The actual path of the spectrum stored on the
                               // local disk (after downloading from the server)
    protected String serverpath; // The path of the spectrum as passed into the
                                 // constructor. This is not the
    // same as fullpath but instead corresponds to the name needed to download
    // the file from the server (excluding the hostname).

    protected boolean isSelected;
    protected double footprintHeight;

    protected vtkActor selectionActor = new vtkActor();
    protected vtkPolyData selectionPolyData = new vtkPolyData();
    protected vtkPolyData footprint;
    protected vtkPolyData shiftedFootprint;
    protected vtkActor footprintActor;
    protected vtkActor frustumActor;
    protected List<vtkProp> footprintActors = new ArrayList<vtkProp>();
    protected double[] frustumCenter;
    protected vtkActor outlineActor = new vtkActor();
    protected vtkPolyData outlinePolyData = new vtkPolyData();
    boolean isOutlineShowing;

    protected Vector3D toSunUnitVector;
    protected vtkActor toSunVectorActor = new vtkActor();
    protected vtkPolyData toSunVectorPolyData = new vtkPolyData();
    protected boolean isToSunVectorShowing;
    protected double toSunVectorLength;

    protected double[] spacecraftPosition = new double[3];
    protected double[] frustum1 = new double[3];
    protected double[] frustum2 = new double[3];
    protected double[] frustum3 = new double[3];
    protected double[] frustum4 = new double[3];

    protected int[] channelsToColorBy = { 0, 0, 0 };
    protected double[] channelsColoringMinValue = { 0.0, 0.0, 0.0 };
    protected double[] channelsColoringMaxValue = { 0.05, 0.05, 0.05 };

    protected DateTime dateTime;
    protected double duration;
    protected short polygon_type_flag;
    protected double range;
    protected List<LatLon> latLons = new ArrayList<LatLon>();
    protected double[] spectrum;
    protected double[] xData;
    protected double minIncidence;
    protected double maxIncidence;
    protected double minEmission;
    protected double maxEmission;
    protected double minPhase;
    protected double maxPhase;
    protected boolean showFrustum = false;

    protected String dataName;
    protected String xAxisUnits;
    protected String yAxisUnits;
    protected SearchSpec spec;

    protected SpectrumColoringStyle coloringStyle = SpectrumColoringStyle.RGB;


    public BasicSpectrum(String filename, SmallBodyModel smallBodyModel,
            SpectralInstrument instrument) throws IOException
    {
        File file = FileCache.getFileFromServer(filename);
        this.serverpath = filename; // path on server relative to data
                                    // repository root (e.g. relative to
                                    // /project/nearsdc/data/)
        this.instrument = instrument; //
        this.fullpath = file.getAbsolutePath();
        this.smallBodyModel = smallBodyModel;

        spectrum=new double[getNumberOfBands()];

        footprintHeight=smallBodyModel.getMinShiftAmount();


    }

    public abstract int getNumberOfBands();

    public vtkPolyData getSelectionPolyData()
    {
        return selectionPolyData;
    }

    public double[] getSpectrum()
    {
        return spectrum;
    }

    @Override
    public SpectralInstrument getInstrument()
    {
        return instrument;
    }

    @Override
    public double[] getBandCenters()
    {
        return instrument.getBandCenters();
    }

    public void generateFootprint()
    {
        if (!latLons.isEmpty())
        {
            vtkPolyData tmp = smallBodyModel.computeFrustumIntersection(
                    spacecraftPosition, frustum1, frustum2, frustum3, frustum4);

            vtkDoubleArray faceAreaFraction = new vtkDoubleArray();
            faceAreaFraction.SetName(faceAreaFractionArrayName);
            Frustum frustum = new Frustum(getFrustumOrigin(),
                    getFrustumCorner(0), getFrustumCorner(1),
                    getFrustumCorner(2), getFrustumCorner(3));
            for (int c = 0; c < tmp.GetNumberOfCells(); c++)
            {
                vtkIdTypeArray originalIds = (vtkIdTypeArray) tmp.GetCellData()
                        .GetArray(GenericPolyhedralModel.cellIdsArrayName);
                int originalId = originalIds.GetValue(c);
                vtkTriangle tri = (vtkTriangle) smallBodyModel
                        .getSmallBodyPolyData().GetCell(originalId); // tri on
                                                                     // original
                                                                     // body
                                                                     // model
                vtkTriangle ftri = (vtkTriangle) tmp.GetCell(c); // tri on
                                                                 // footprint
                faceAreaFraction.InsertNextValue(
                        ftri.ComputeArea() / tri.ComputeArea());
            }
            tmp.GetCellData().AddArray(faceAreaFraction);

            if (tmp != null)
            {
                // Need to clear out scalar data since if coloring data is being
                // shown,
                // then the color might mix-in with the image.
                tmp.GetCellData().SetScalars(null);
                tmp.GetPointData().SetScalars(null);

                footprint.DeepCopy(tmp);

                shiftedFootprint.DeepCopy(tmp);
                PolyDataUtil.shiftPolyDataInMeanNormalDirection(
                        shiftedFootprint, footprintHeight);

                createSelectionPolyData();
                createSelectionActor();
                createToSunVectorPolyData();
                createToSunVectorActor();
                createOutlinePolyData();
                createOutlineActor();
            }
        }
    }

    public List<vtkProp> getProps()
    {
        if (footprintActor == null && !latLons.isEmpty())
        {
            generateFootprint();

            vtkPolyDataMapper footprintMapper = new vtkPolyDataMapper();
            footprintMapper.SetInputData(shiftedFootprint);
            // footprintMapper.SetResolveCoincidentTopologyToPolygonOffset();
            // footprintMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(-.002,
            // -2.0);
            footprintMapper.Update();

            footprintActor = new vtkActor();
            footprintActor.SetMapper(footprintMapper);
            vtkProperty footprintProperty = footprintActor.GetProperty();
            double[] color = getChannelColor();
            footprintProperty.SetColor(color[0], color[1], color[2]);
            footprintProperty.SetLineWidth(2.0);
            footprintProperty.LightingOff();

            footprintActors.add(footprintActor);

            /*
             * // Compute the bounding edges of this surface vtkFeatureEdges
             * edgeExtracter = new vtkFeatureEdges();
             * edgeExtracter.SetInput(shiftedFootprint);
             * edgeExtracter.BoundaryEdgesOn(); edgeExtracter.FeatureEdgesOff();
             * edgeExtracter.NonManifoldEdgesOff();
             * edgeExtracter.ManifoldEdgesOff(); edgeExtracter.Update();
             *
             * vtkPolyDataMapper edgeMapper = new vtkPolyDataMapper();
             * edgeMapper.SetInputConnection(edgeExtracter.GetOutputPort());
             * edgeMapper.ScalarVisibilityOff();
             * //edgeMapper.SetResolveCoincidentTopologyToPolygonOffset();
             * //edgeMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(
             * -.004, -4.0); edgeMapper.Update();
             *
             * vtkActor edgeActor = new vtkActor();
             * edgeActor.SetMapper(edgeMapper);
             * edgeActor.GetProperty().SetColor(0.0, 0.39, 0.0);
             * edgeActor.GetProperty().SetLineWidth(2.0);
             * edgeActor.GetProperty().LightingOff();
             * footprintActors.add(edgeActor);
             */
        }

        if (frustumActor == null)
        {
            vtkPolyData frus = new vtkPolyData();

            vtkPoints points = new vtkPoints();
            vtkCellArray lines = new vtkCellArray();

            vtkIdList idList = new vtkIdList();
            idList.SetNumberOfIds(2);

            double dx = MathUtil.vnorm(spacecraftPosition)
                    + smallBodyModel.getBoundingBoxDiagonalLength();
            double[] origin = spacecraftPosition;
            double[] UL = { origin[0] + frustum1[0] * dx,
                    origin[1] + frustum1[1] * dx,
                    origin[2] + frustum1[2] * dx };
            double[] UR = { origin[0] + frustum2[0] * dx,
                    origin[1] + frustum2[1] * dx,
                    origin[2] + frustum2[2] * dx };
            double[] LL = { origin[0] + frustum3[0] * dx,
                    origin[1] + frustum3[1] * dx,
                    origin[2] + frustum3[2] * dx };
            double[] LR = { origin[0] + frustum4[0] * dx,
                    origin[1] + frustum4[1] * dx,
                    origin[2] + frustum4[2] * dx };

            points.InsertNextPoint(spacecraftPosition);
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

            frus.SetPoints(points);
            frus.SetLines(lines);

            vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
            frusMapper.SetInputData(frus);

            frustumActor = new vtkActor();
            frustumActor.SetMapper(frusMapper);
            vtkProperty frustumProperty = frustumActor.GetProperty();
            frustumProperty.SetColor(0.0, 1.0, 0.0);
            frustumProperty.SetLineWidth(2.0);
            frustumActor.VisibilityOff();

            footprintActors.add(frustumActor);
        }

        footprintActors.add(selectionActor);
        footprintActors.add(toSunVectorActor);
        footprintActors.add(outlineActor);

        return footprintActors;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            // System.out.println("updating spectral image");
            generateFootprint();
            setUnselected();

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    public void setSelected()
    {
        isSelected = true;
        selectionActor.VisibilityOn();
        selectionActor.Modified();
    }

    public void setUnselected()
    {
        isSelected = false;
        selectionActor.VisibilityOff();
        selectionActor.Modified();
    }

    public void shiftFootprintToHeight(double h)
    {
        vtkPolyData tmp = smallBodyModel.computeFrustumIntersection(
                spacecraftPosition, frustum1, frustum2, frustum3, frustum4);
        shiftedFootprint.DeepCopy(tmp);
        PolyDataUtil.shiftPolyDataInMeanNormalDirection(shiftedFootprint, h);
        createSelectionPolyData();
        createOutlinePolyData();
        //
        if (isSelected)
            selectionActor.VisibilityOn();
        //
        ((vtkPolyDataMapper) footprintActor.GetMapper())
                .SetInputData(shiftedFootprint);
        footprintActor.GetMapper().Update();
        ((vtkPolyDataMapper) selectionActor.GetMapper())
                .SetInputData(selectionPolyData);
        selectionActor.GetMapper().Update();
        ((vtkPolyDataMapper) outlineActor.GetMapper())
                .SetInputData(selectionPolyData);
        outlineActor.GetMapper().Update();

        //
        footprintHeight = h;
    }

    protected void createSelectionPolyData()
    {
        vtkFeatureEdges edgeFilter = new vtkFeatureEdges();
        edgeFilter.SetInputData(getShiftedFootprint());
        edgeFilter.BoundaryEdgesOn();
        edgeFilter.FeatureEdgesOff();
        edgeFilter.ManifoldEdgesOff();
        edgeFilter.NonManifoldEdgesOff();
        edgeFilter.Update();
        selectionPolyData.DeepCopy(edgeFilter.GetOutput());
    }

    protected void createSelectionActor()
    {
        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputData(selectionPolyData);
        mapper.Update();
        selectionActor.SetMapper(mapper);
        selectionActor.VisibilityOff();
        selectionActor.GetProperty().EdgeVisibilityOn();
        selectionActor.GetProperty().SetEdgeColor(0.5, 1, 0.5);
        selectionActor.GetProperty().SetLineWidth(5);
    }

    protected void createOutlinePolyData()
    {
        vtkFeatureEdges edgeFilter = new vtkFeatureEdges();
        edgeFilter.SetInputData(getShiftedFootprint());
        edgeFilter.BoundaryEdgesOn();
        edgeFilter.FeatureEdgesOff();
        edgeFilter.ManifoldEdgesOff();
        edgeFilter.NonManifoldEdgesOff();
        edgeFilter.Update();
        outlinePolyData.DeepCopy(edgeFilter.GetOutput());
    }

    protected void createOutlineActor()
    {
        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputData(outlinePolyData);
        mapper.Update();
        outlineActor.SetMapper(mapper);
        outlineActor.VisibilityOff();
        outlineActor.GetProperty().EdgeVisibilityOn();
        outlineActor.GetProperty().SetEdgeColor(0.4, 0.4, 1);
        outlineActor.GetProperty().SetLineWidth(2);
    }

    public double[] getToSunUnitVector()
    {
        return toSunUnitVector.toArray();
    }

    protected void createToSunVectorPolyData()
    {
        vtkPoints points = new vtkPoints();
        vtkCellArray cells = new vtkCellArray();
        Vector3D footprintCenter = new Vector3D(
                getUnshiftedFootprint().GetCenter());
        int id1 = points.InsertNextPoint(footprintCenter.toArray());
        int id2 = points.InsertNextPoint(footprintCenter
                .add(new Vector3D(getToSunUnitVector()).scalarMultiply(toSunVectorLength))
                .toArray());
        vtkLine line = new vtkLine();
        line.GetPointIds().SetId(0, id1);
        line.GetPointIds().SetId(1, id2);
        cells.InsertNextCell(line);
        toSunVectorPolyData.SetPoints(points);
        toSunVectorPolyData.SetLines(cells);
    }

    protected void createToSunVectorActor()
    {
        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputData(toSunVectorPolyData);
        mapper.Update();
        toSunVectorActor.SetMapper(mapper);
        toSunVectorActor.VisibilityOff();
        toSunVectorActor.GetProperty().SetColor(1, 1, 0.5);
    }

    /**
     * The shifted footprint is the original footprint shifted slightly in the
     * normal direction so that it will be rendered correctly and not obscured
     * by the asteroid.
     *
     * @return
     */
    public vtkPolyData getShiftedFootprint()
    {
        return shiftedFootprint;
    }

    /**
     * The original footprint whose cells exactly overlap the original asteroid.
     * If rendered as is, it would interfere with the asteroid.
     *
     * @return
     */
    public vtkPolyData getUnshiftedFootprint()
    {
        return footprint;
    }

    public void Delete()
    {
        footprint.Delete();
        shiftedFootprint.Delete();
    }

    public double getMinFootprintHeight()
    {
        return smallBodyModel.getMinShiftAmount();
    }

    public void setChannelColoring(int[] channels, double[] mins, double[] maxs)
    {
        for (int i = 0; i < 3; ++i)
        {
            channelsToColorBy[i] = channels[i];
            channelsColoringMinValue[i] = mins[i];
            channelsColoringMaxValue[i] = maxs[i];
        }
    }

    @Override
    public DateTime getDateTime()
    {
        return dateTime;
    }

    @Override
    public void setShowFrustum(boolean show)
    {
        showFrustum = show;

        if (showFrustum)
        {
            frustumActor.VisibilityOn();
        }
        else
        {
            frustumActor.VisibilityOff();
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);

    }

    @Override
    public void updateChannelColoring()
    {
        vtkProperty footprintProperty = footprintActor.GetProperty();
        double[] color = getChannelColor();
        footprintProperty.SetColor(color[0], color[1], color[2]);

    }

    @Override
    public double[] getChannelColor()
    {
        double[] color = new double[] { 1, 1, 1 };
        return color;
    }

    @Override
    public boolean isSelected()
    {
        return isSelected;
    }

    @Override
    public String getFullPath()
    {
        return fullpath;
    }

    @Override
    public double evaluateDerivedParameters(int channel)
    {
        switch (channel)
        {
        case 0:
             return spectrum[35] - spectrum[4];
        case 1:
             return spectrum[0] - spectrum[4];
        case 2:
             return spectrum[51] - spectrum[35];
        default:
            return 0.0;
        }
    }

    public boolean isFrustumShowing()
    {
        return showFrustum;
    }

    public boolean isToSunVectorShowing()
    {
        return isToSunVectorShowing;
    }

    public boolean isOutlineShowing()
    {
        return isOutlineShowing;
    }

    public double[] getSpacecraftPosition()
    {
        return spacecraftPosition;
    }

    public double[] getFrustumCenter()
    {
        return frustumCenter;
    }

    public double[] getFrustumCorner(int i)
    {
        switch (i)
        {
        case 0:
            return frustum1;
        case 1:
            return frustum2;
        case 2:
            return frustum3;
        case 3:
            return frustum4;
        }
        return null;
    }

    public double[] getFrustumOrigin()
    {
        return spacecraftPosition;
    }

    public void setShowToSunVector(boolean b)
    {
        isToSunVectorShowing = b;
        if (isToSunVectorShowing)
            toSunVectorActor.VisibilityOn();
        else
            toSunVectorActor.VisibilityOff();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setShowOutline(boolean b)
    {
        isOutlineShowing = b;
        if (isOutlineShowing)
            outlineActor.VisibilityOn();
        else
            outlineActor.VisibilityOff();
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    @Override
    public String getSpectrumPathOnServer()
    {
        return serverpath;
    }

    public String getDataName()
    {
        return dataName;
    }

    public void setDataName(String dataName)
    {
        this.dataName = dataName;
    }

    public String getxAxisUnits()
    {
        return xAxisUnits;
    }

    public void setxAxisUnits(String xAxisUnits)
    {
        this.xAxisUnits = xAxisUnits;
    }

    public String getyAxisUnits()
    {
        return yAxisUnits;
    }

    public void setyAxisUnits(String yAxisUnits)
    {
        this.yAxisUnits = yAxisUnits;
    }

    public double[] getxData()
    {
        return xData;
    }

    public SpectrumColoringStyle getColoringStyle()
    {
        return coloringStyle;
    }

    public void setColoringStyle(SpectrumColoringStyle coloringStyle)
    {
        this.coloringStyle = coloringStyle;
    }

    public void setMetadata(SearchSpec spec)
    {
        this.spec = spec;
    }

    public SearchSpec getMetadata()
    {
        return this.spec;
    }
}
