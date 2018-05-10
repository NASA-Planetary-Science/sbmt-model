package edu.jhuapl.sbmt.model.lidar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SmallBodyModel;

public class LidarBrowseDataCollection extends AbstractModel implements PropertyChangeListener
{
    /**
     * Contains information about a single lidar file.
     */
    public static class LidarDataFileSpec
    {
        public String path;
        public String name;
        public String comment;

        @Override
        public String toString()
        {
            return name + " (" + comment + ")";
        }
    }

    private BodyViewConfig polyhedralModelConfig;
    private List<vtkProp> lidarPerUnitActors = new ArrayList<vtkProp>();

    private HashMap<String, LidarDataPerUnit> fileToLidarPerUnitMap = new HashMap<String, LidarDataPerUnit>();
    private HashMap<vtkProp, String> actorToFileMap = new HashMap<vtkProp, String>();
    private double radialOffset = 0.0;
    private double startPercent = 0.0;
    private double stopPercent = 1.0;
    private boolean showSpacecraftPosition = true;

    public LidarBrowseDataCollection(SmallBodyModel smallBodyModel)
    {
        this.polyhedralModelConfig = smallBodyModel.getSmallBodyConfig();
    }

    protected LidarDataPerUnit createLidarDataPerUnitWhateverThatIs(String path, BodyViewConfig config) throws IOException
    {
        return new LidarDataPerUnit(
                path, polyhedralModelConfig);
    }

    public void addLidarData(String path) throws IOException
    {
        if (fileToLidarPerUnitMap.containsKey(path))
            return;

        LidarDataPerUnit lidarData = createLidarDataPerUnitWhateverThatIs(path, polyhedralModelConfig);
        lidarData.setShowSpacecraftPosition(showSpacecraftPosition);

        lidarData.addPropertyChangeListener(this);

        fileToLidarPerUnitMap.put(path, lidarData);

        for (vtkProp prop : lidarData.getProps())
        {
            actorToFileMap.put(prop, path);
            lidarPerUnitActors.add(prop);
        }

        this.setOffset(radialOffset);
        this.setPercentageShown(startPercent, stopPercent);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeLidarData(String path)
    {
        List<vtkProp> props = fileToLidarPerUnitMap.get(path).getProps();
        for (vtkProp prop : props)
        {
            lidarPerUnitActors.remove(prop);
            actorToFileMap.remove(prop);
        }

        fileToLidarPerUnitMap.remove(path);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void removeAllLidarData()
    {
        lidarPerUnitActors.clear();
        actorToFileMap.clear();
        fileToLidarPerUnitMap.clear();
        System.out.println("LidarBrowseDataCollection.removeAllLidarData()");
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public LidarDataPerUnit getLidarData(String path)
    {
        return fileToLidarPerUnitMap.get(path);
    }

    @Override
    public List<vtkProp> getProps()
    {
        return lidarPerUnitActors;
    }

    @Override
    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        LidarDataPerUnit data = fileToLidarPerUnitMap.get(actorToFileMap.get(prop));
        return data != null ? data.getClickStatusBarText(prop, cellId, pickPosition) : "";
    }

    public String getLidarName(vtkActor actor)
    {
        return actorToFileMap.get(actor);
    }

    public boolean containsLidarData(String file)
    {
        return fileToLidarPerUnitMap.containsKey(file);
    }

    public List<LidarDataFileSpec> getAllLidarPaths() throws FileNotFoundException
    {
        List<LidarDataFileSpec> lidarSpecs = new ArrayList<LidarDataFileSpec>();

        InputStream is;
        if (polyhedralModelConfig.lidarBrowseFileListResourcePath.startsWith("/edu"))
        {
            is = getClass().getResourceAsStream(polyhedralModelConfig.lidarBrowseFileListResourcePath);
        }
        else
        {
            is = new FileInputStream(FileCache.getFileFromServer(polyhedralModelConfig.lidarBrowseFileListResourcePath));
        }
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(isr);

        String line;
        try
        {
            while ((line = in.readLine()) != null)
            {
                LidarDataFileSpec lidarSpec = new LidarDataFileSpec();
                int indexFirstSpace = line.indexOf(' ');
                if (indexFirstSpace == -1)
                {
                    lidarSpec.path = line;
                    lidarSpec.comment = "";
                }
                else
                {
                    lidarSpec.path = line.substring(0,indexFirstSpace);
                    lidarSpec.comment = line.substring(indexFirstSpace+1);
                }
                lidarSpec.name = new File(lidarSpec.path).getName();
                if (lidarSpec.name.toLowerCase().endsWith(".gz"))
                    lidarSpec.name = lidarSpec.name.substring(0, lidarSpec.name.length()-3);
                lidarSpecs.add(lidarSpec);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return lidarSpecs;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    @Override
    public void setOffset(double offset)
    {
        radialOffset = offset;

        if (fileToLidarPerUnitMap.isEmpty())
            return;

        for (String key : fileToLidarPerUnitMap.keySet())
        {
            LidarDataPerUnit data = fileToLidarPerUnitMap.get(key);
            data.setOffset(offset);
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setPercentageShown(double startPercent, double stopPercent)
    {
        this.startPercent = startPercent;
        this.stopPercent = stopPercent;

        if (fileToLidarPerUnitMap.isEmpty())
            return;

        for (String key : fileToLidarPerUnitMap.keySet())
        {
            LidarDataPerUnit data = fileToLidarPerUnitMap.get(key);
            data.setPercentageShown(startPercent, stopPercent);
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void setShowSpacecraftPosition(boolean show)
    {
        showSpacecraftPosition = show;

        if (fileToLidarPerUnitMap.isEmpty())
            return;

        for (String key : fileToLidarPerUnitMap.keySet())
        {
            LidarDataPerUnit data = fileToLidarPerUnitMap.get(key);
            data.setShowSpacecraftPosition(show);
        }

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public double getOffsetScale()
    {
        return polyhedralModelConfig.lidarOffsetScale;
    }
}
