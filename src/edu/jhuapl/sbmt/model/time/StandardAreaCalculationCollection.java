package edu.jhuapl.sbmt.model.time;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.swing.event.ListDataListener;

import edu.jhuapl.sbmt.client.SmallBodyModel;

public class StandardAreaCalculationCollection implements AreaCalculationCollection
{
    private NavigableMap<Integer, AreaCalculation> indexToAreaCalculation = new TreeMap<Integer, AreaCalculation>();
    private StateHistoryModel simulationRun;
    private SmallBodyModel smallBodyModel;

    public StandardAreaCalculationCollection(String runDirName, StateHistoryModel simulationRun, SmallBodyModel torsoBodyModel)
    {
        this.name = runDirName;
        this.simulationRun = simulationRun;
        this.smallBodyModel = torsoBodyModel;

        File runDir = new File(runDirName);
        File[] simulationRunFiles = runDir.listFiles();
        int index = 0;
        System.out.println("Loading Area Calculation List: " + runDirName);

        Map<String, List<String>> areaCalculationFiles = new HashMap<String, List<String>>();

        for (File file : simulationRunFiles)
        {
            String areaFileName = file.getName();
            if (file.isDirectory())
            {
                System.out.println("Area Calculation From Directory " + index + ": " + areaFileName);
                AreaCalculation areaCalculation = new StandardAreaCalculation(runDirName, null, areaFileName, index, simulationRun, torsoBodyModel);
                put(index, areaCalculation);
                index++;
            }
            else
            {
                String[] fileNameTokens = areaFileName.split("\\.");
                if (fileNameTokens.length == 3 && (areaFileName.endsWith(".csv") || areaFileName.endsWith(".txt")))
                {
                    String areaCalculationToken = fileNameTokens[1];
                    String areaCalculationName = areaCalculationToken.split("_")[0];
                    List<String> files = areaCalculationFiles.get(areaCalculationName);
                    if (files == null)
                    {
                        files = new ArrayList<String>();
                        areaCalculationFiles.put(areaCalculationName, files);
                    }
                    files.add(areaFileName);
                }
            }
        }

        for (String areaCalculationName : areaCalculationFiles.keySet())
        {
            List<String> files = areaCalculationFiles.get(areaCalculationName);
            System.out.println("Area Calculation From Files " + index + ": " + areaCalculationName);
            AreaCalculation areaCalculation = new StandardAreaCalculation(runDirName, files, areaCalculationName, index, simulationRun, torsoBodyModel);
            put(index, areaCalculation);
            index++;
        }
    }

    private String name;
    public String getName()
    {
        return name;
    }

    private Integer currentIndex;
    public Integer getCurrentIndex()
    {
        return currentIndex;
    }

    public void setCurrentIndex(Integer currentIndex)
    {
        this.currentIndex = currentIndex;
        if (currentTrajectoryName != null && currentIndex != null)
        {
            AreaCalculation areaCalculation = getValue(currentIndex);
            areaCalculation.load();
        }


    }

    private String currentTrajectoryName;
    public void setCurrentTrajectory(String trajectoryName)
    {
        this.currentTrajectoryName = trajectoryName;
        for (Integer areaCalculationIndex : indexToAreaCalculation.keySet())
        {
            AreaCalculation areaCalculation = getValue(areaCalculationIndex);
            areaCalculation.setCurrentTrajectory(trajectoryName);
            if (areaCalculationIndex == currentIndex)
            {
                areaCalculation.load();
                areaCalculation.markPatchesOutOfDate();
                areaCalculation.initializePatches();

//                areaCalculation.setShowPatches(areaCalculation.getAllSurfacePatchNames());
            }
        }
    }

    public Integer getLength()
    {
        return indexToAreaCalculation.size();
    }

    public void put(Integer index, AreaCalculation areaCalculation)
    {
        indexToAreaCalculation.put(index, areaCalculation);
    }

    public AreaCalculation getValue(Integer index)
    {
        return index == null ? null : indexToAreaCalculation.get(index);
    }

    public AreaCalculation getCurrentValue()
    {
        return currentIndex != null ? getValue(currentIndex) : null;
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


}
