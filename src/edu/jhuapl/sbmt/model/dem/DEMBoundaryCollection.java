package edu.jhuapl.sbmt.model.dem;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nom.tam.fits.FitsException;

import vtk.vtkActor;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.dem.DEM.DEMKey;

public class DEMBoundaryCollection extends AbstractModel implements PropertyChangeListener
{
    public class DEMBoundary extends AbstractModel implements PropertyChangeListener
    {
        private vtkActor actor;
        private vtkPolyData boundary;
        private vtkPolyDataMapper boundaryMapper;
        private DEM dem;

        public DEMBoundary(DEM dem)
        {
            this.dem = dem;

            boundary = new vtkPolyData();
            boundaryMapper = new vtkPolyDataMapper();
            actor = new vtkActor();

            initialize();
        }

        private void initialize()
        {
            boundary.DeepCopy(dem.getBoundary());
            int numPoints = boundary.GetNumberOfPoints();
            vtkPoints points = boundary.GetPoints();
            for (int i=0; i<numPoints; ++i)
            {
                double[] pt = points.GetPoint(i);
                double[] closestPoint = smallBodyModel.findClosestPoint(pt);
                points.SetPoint(i, closestPoint);
            }

            PolyDataUtil.shiftPolyLineInNormalDirectionOfPolyData(
                    boundary,
                    smallBodyModel.getSmallBodyPolyData(),
                    smallBodyModel.getCellNormals(),
                    smallBodyModel.getCellLocator(),
                    0.003);

            boundaryMapper.SetInputData(boundary);

            actor.SetMapper(boundaryMapper);
            actor.GetProperty().SetColor(0.0, 0.392, 0.0);
            actor.GetProperty().SetLineWidth(1.0);
        }

        public void propertyChange(PropertyChangeEvent evt)
        {
            if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
            {
                initialize();
                this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
            }
        }

        @Override
        public List<vtkProp> getProps()
        {
            List<vtkProp> props = new ArrayList<vtkProp>();
            props.add(actor);
            return props;
        }

        public DEM getDEM()
        {
            return dem;
        }

        public DEMKey getKey()
        {
            return dem.getKey();
        }

        public Color getColor()
        {
            double[] c = actor.GetProperty().GetColor();
            return new Color((float)c[0], (float)c[1], (float)c[2]);
        }

        public void setColor(Color color)
        {
            float[] c = color.getRGBColorComponents(null);
            actor.GetProperty().SetColor(c[0], c[1], c[2]);
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }

        public Number getLineWidth()
        {
            return actor.GetProperty().GetLineWidth();
        }

        public void setLineWidth(Double value)
        {
            actor.GetProperty().SetLineWidth(value);
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
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

    private HashMap<DEMBoundary, List<vtkProp>> boundaryToActorsMap = new HashMap<DEMBoundary, List<vtkProp>>();
    private HashMap<vtkProp, DEMBoundary> actorToBoundaryMap = new HashMap<vtkProp, DEMBoundary>();
    private SmallBodyModel smallBodyModel;
    private ModelManager modelManager;
    // Create a buffer of initial boundary colors to use. We cycle through these colors when creating new boundaries
    private Color[] initialColors = {Color.RED, Color.PINK.darker(), Color.ORANGE.darker(),
            Color.GREEN.darker(), Color.MAGENTA, Color.CYAN.darker(), Color.BLUE,
            Color.GRAY, Color.DARK_GRAY, Color.BLACK};
    private int initialColorIndex = 0;

    public DEMBoundaryCollection(SmallBodyModel smallBodyModel, ModelManager modelManager)
    {
        this.smallBodyModel = smallBodyModel;
        this.modelManager = modelManager;
    }

    protected DEMBoundary createBoundary(
            DEMKey key,
            SmallBodyModel smallBodyModel) throws IOException, FitsException
    {
        DEMBoundary boundary;

        // If the DEM already exists in the DEM collection, use that instead of creating a new one
        DEMCollection demCollection = (DEMCollection)modelManager.getModel(ModelNames.DEM);
        DEM dem = demCollection.getDEM(key);
        if(dem != null)
        {
            boundary = new DEMBoundary(dem);
        }
        else
        {
            boundary = new DEMBoundary((DEM)SbmtModelFactory.createDEM(key, smallBodyModel));
        }

        boundary.setBoundaryColor(initialColors[initialColorIndex++]);
        if (initialColorIndex >= initialColors.length)
            initialColorIndex = 0;
        return boundary;
    }

    private boolean containsKey(DEMKey key)
    {
        for (DEMBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.getKey().equals(key))
                return true;
        }

        return false;
    }

    private boolean containsDEM(DEM dem)
    {
        for (DEMBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.dem.equals(dem))
                return true;
        }

        return false;
    }

    private DEMBoundary getBoundaryFromDEM(DEM dem)
    {
        for (DEMBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.dem.equals(dem))
                return boundary;
        }

        return null;
    }

    private DEMBoundary getBoundaryFromKey(DEMKey key)
    {
        for (DEMBoundary boundary : boundaryToActorsMap.keySet())
        {
            if (boundary.getKey().equals(key))
                return boundary;
        }

        return null;
    }

    public void addBoundary(DEM dem)
    {
        if (containsDEM(dem))
            return;

        DEMBoundary boundary = new DEMBoundary(dem);

        smallBodyModel.addPropertyChangeListener(boundary);
        boundary.addPropertyChangeListener(this);

        boundaryToActorsMap.put(boundary, new ArrayList<vtkProp>());

        List<vtkProp> boundaryPieces = boundary.getProps();

        boundaryToActorsMap.get(boundary).addAll(boundaryPieces);

        for (vtkProp act : boundaryPieces)
            actorToBoundaryMap.put(act, boundary);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void addBoundary(DEMKey key) throws IOException, FitsException
    {
        if (containsKey(key))
            return;

        DEMBoundary boundary = createBoundary(key, smallBodyModel);

        smallBodyModel.addPropertyChangeListener(boundary);
        boundary.addPropertyChangeListener(this);

        boundaryToActorsMap.put(boundary, new ArrayList<vtkProp>());

        List<vtkProp> boundaryPieces = boundary.getProps();

        boundaryToActorsMap.get(boundary).addAll(boundaryPieces);

        for (vtkProp act : boundaryPieces)
            actorToBoundaryMap.put(act, boundary);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeBoundary(DEM dem)
    {
        DEMBoundary boundary = getBoundaryFromDEM(dem);

        boundary.removePropertyChangeListener(this);
        smallBodyModel.removePropertyChangeListener(boundary);

        List<vtkProp> actors = boundaryToActorsMap.get(boundary);

        for (vtkProp act : actors)
            actorToBoundaryMap.remove(act);

        boundaryToActorsMap.remove(boundary);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeBoundary(DEMKey key)
    {
        DEMBoundary boundary = getBoundaryFromKey(key);

        if(boundary != null)
        {
            boundary.removePropertyChangeListener(this);
            smallBodyModel.removePropertyChangeListener(boundary);

            List<vtkProp> actors = boundaryToActorsMap.get(boundary);

            if(actors != null)
            {
                for (vtkProp act : actors)
                    actorToBoundaryMap.remove(act);
            }

            boundaryToActorsMap.remove(boundary);

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public void removeAllBoundaries()
    {
        actorToBoundaryMap.clear();
        boundaryToActorsMap.clear();

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public List<vtkProp> getProps()
    {
        return new ArrayList<vtkProp>(actorToBoundaryMap.keySet());
    }

    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        return "Boundary of maplet";
    }

    public DEMBoundary getBoundary(vtkProp actor)
    {
        return actorToBoundaryMap.get(actor);
    }

    public DEMBoundary getBoundary(DEMKey key)
    {
        return getBoundaryFromKey(key);
    }

    public DEMBoundary getBoundary(DEM dem)
    {
        return getBoundaryFromDEM(dem);
    }

    public boolean containsBoundary(DEMKey key)
    {
        return containsKey(key);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }
}
