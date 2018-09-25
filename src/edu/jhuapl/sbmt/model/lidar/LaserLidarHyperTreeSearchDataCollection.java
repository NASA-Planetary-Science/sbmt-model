package edu.jhuapl.sbmt.model.lidar;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import vtk.vtkCellArray;
import vtk.vtkPoints;

import edu.jhuapl.saavtk.model.PointInRegionChecker;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;
import edu.jhuapl.sbmt.lidar.hyperoctree.laser.Hayabusa2LaserLidarPoint;

public class LaserLidarHyperTreeSearchDataCollection extends LidarSearchDataCollection
{
    public enum TrackFileType
    {
        TEXT
    };

    private Map<String, FSHyperTreeSkeleton> skeletons = new HashMap<String, FSHyperTreeSkeleton>();
    private FSHyperTreeSkeleton currentSkeleton;
    private JComponent parentForProgressMonitor;
    private boolean loading=false;

    @Override
    public boolean isLoading()
    {
        return loading;
    }

    public LaserLidarHyperTreeSearchDataCollection(SmallBodyModel smallBodyModel)
    {
        super(smallBodyModel);
    }

    public void clearDatasourceSkeletons()
    {
        skeletons.clear();
    }

    /**
     * Creates a skeleton for the specified datasource name, assumes the data source path for the name
     * is already added to the lidarDatasourceMap
     * @param datasourceName
     */
    public void addDatasourceSkeleton(String datasourceName, String datasourcePath)
    {
        if (datasourceName != null && datasourceName.length() > 0)
        {
            System.out.println("Adding datasource: " + datasourceName + " - " + datasourcePath);
            Path basePath = Paths.get(datasourcePath);
            FSHyperTreeSkeleton skeleton = skeletons.get(datasourceName);
            if (skeleton == null)
            {
                skeleton = new FSHyperTreeSkeleton(basePath);
                skeletons.put(datasourceName, skeleton);
            }
        }
    }

    public void setCurrentDatasourceSkeleton(String datasourceName)
    {
        if (datasourceName != null && datasourceName.length() > 0)
        {
            System.out.println("Setting current datasource: " + datasourceName);
            FSHyperTreeSkeleton skeleton = skeletons.get(datasourceName);
            if (skeleton != null)
                currentSkeleton = skeleton;
        }
    }

    private Set<FSHyperTreeSkeleton> readIn = new HashSet<FSHyperTreeSkeleton>();

    public void readSkeleton()
    {
        if (!readIn.contains(currentSkeleton))
        {
            currentSkeleton.read();
            readIn.add(currentSkeleton);
        }
    }

    public TreeSet<Integer> getLeavesIntersectingBoundingBox(BoundingBox bbox, double[] tlims)
    {
        double[] bounds=new double[]{bbox.xmin,bbox.xmax,bbox.ymin,bbox.ymax,bbox.zmin,bbox.zmax,tlims[0],tlims[1]};
        return currentSkeleton.getLeavesIntersectingBoundingBox(bounds);
    }

    public TreeSet<Integer> getLeavesIntersectingBoundingBox(BoundingBox bbox, double[] tlims, double[] scrangeLims)
    {
        double[] bounds=new double[]{bbox.xmin,bbox.xmax,bbox.ymin,bbox.ymax,bbox.zmin,bbox.zmax,tlims[0],tlims[1],scrangeLims[0],scrangeLims[1]};
        return currentSkeleton.getLeavesIntersectingBoundingBox(bounds);
    }

    public void setParentForProgressMonitor(JComponent component)
    {
        this.parentForProgressMonitor=component;
    }

    @Override
    public void setLidarData(String dataSource, final double startDate,
            final double stopDate, final TreeSet<Integer> cubeList,
            final PointInRegionChecker pointInRegionChecker,
            double timeSeparationBetweenTracks, int minTrackLength)
                    throws IOException, ParseException
    {
        // In the old LidarSearchDataCollection class the cubeList came from a predetermined set of cubes all of equal size.
        // Here it corresponds to the list of leaves of an octree that intersect the bounding box of the user selection area.

     //   ProgressBarSwingWorker dataLoader=new ProgressBarSwingWorker(parentForProgressMonitor,"Loading LASER datapoints ("+cubeList.size()+" individual chunks)")
     //   {
     //       @Override
     //       protected Void doInBackground() throws Exception
     //      {
                Stopwatch sw=new Stopwatch();
                sw.start();
                loading=true;

//                originalPoints.clear();
                int cnt=0;
                for (Integer cidx : cubeList)
                {
                    Path leafPath=currentSkeleton.getNodeById(cidx).getPath();
                    System.out.println("Loading data partition "+(cnt+1)+"/"+cubeList.size()+" (id="+cidx+") \""+leafPath+"\"");
                    Path dataFilePath=leafPath.resolve("data");
                    File dataFile=FileCache.getFileFromServer(dataFilePath.toString());
                    if (!dataFile.exists())
                        dataFile=FileCache.getFileFromServer(FileCache.FILE_PREFIX+dataFilePath.toString());
                    List<LidarPoint> pts=readDataFile(dataFile,pointInRegionChecker,new double[]{startDate,stopDate});
                    for (int i=0; i<pts.size(); i++)
                    {
                        originalPoints.add(pts.get(i));
                        originalPointsSourceFiles.put(pts.get(i),((Hayabusa2LaserLidarPoint)pts.get(i)).getFileNum());
                    }
                    //
                    cnt++;
                    double progressPercentage=((double)cnt/(double)cubeList.size()*100);
      //             setProgress((int)progressPercentage);
      //              if (isCancelled())
      //                  break;
               }
      //          cancel(true);
                loading=false;

                System.out.println("Data Reading Time="+sw.elapsedMillis()+" ms");
                sw.reset();
                sw.start();

      //          return null;
      //     }


//            return null;
  //     }
    //};
    //dataLoader.executeDialog();

       initTranslationArray(originalPoints.size());

//       while (isLoading())
//       {
//           try
//           {
//               Thread.sleep(100);  // check every fraction of a second whether the data loading is complete
//           }
//           catch (InterruptedException e)
//           {
//               // TODO Auto-generated catch block
//               e.printStackTrace();
//           }

                    // Sort points in time order
    //                Collections.sort(originalPoints);

    //                System.out.println("Sorting Time="+sw.elapsedMillis()+" ms");
    //                sw.reset();
    //                sw.start();

                    radialOffset = 0.0;
//                    translation[0] = translation[1] = translation[2] = 0.0;

                    computeTracks();

                    sw=new Stopwatch();
                    System.out.println("Compute Track Time="+sw.elapsedMillis()+" ms");
                    sw.reset();
                    sw.start();

                    removeTracksThatAreTooSmall();

                    // sometimes the last track ends up with bad times because the user cancelled the search, so remove any that are bad in this respect
                    List<Track> tracksToRemove=Lists.newArrayList();
                    for (Track t : tracks)
                        if (t.timeRange[0].length()==0 || t.timeRange[1].length()==0)
                            tracksToRemove.add(t);
                    for (Track t : tracksToRemove)
                        tracks.remove(t);

                    System.out.println("Remove Small Tracks Time="+sw.elapsedMillis()+" ms");
                    sw.reset();
                    sw.start();

                    assignInitialColorToTrack();

                    System.out.println("Assign Initial Colors Time="+sw.elapsedMillis()+" ms");
                    sw.reset();
                    sw.start();


                    updateTrackPolydata();

                    System.out.println("UpdatePolyData Time="+sw.elapsedMillis()+" ms");


            selectPoint(-1);

            pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
       }
//    }

    static vtkPoints points=new vtkPoints();
    static vtkCellArray cells=new vtkCellArray();

    public static List<LidarPoint> readDataFile(File dataInputFile, PointInRegionChecker pointInRegionChecker, double[] timeLimits) {
        List<LidarPoint> pts=Lists.newArrayList();
        try {
            DataInputStream stream=new DataInputStream(new FileInputStream(dataInputFile));
            while (true) {
                Hayabusa2LaserLidarPoint pt=new Hayabusa2LaserLidarPoint(stream);
                if (pt.getTime()<timeLimits[0] || pt.getTime()>timeLimits[1])   // throw away points outside time limits
                    continue;
                if (pointInRegionChecker!=null)
                {
                    if (pointInRegionChecker.checkPointIsInRegion(pt.getTargetPosition().toArray()))
                        pts.add(pt);    // if region checker exists then filter on space as well as time
                    continue;
                }
                //
/*                int id=points.InsertNextPoint(pt.getTargetPosition().toArray());
                vtkVertex vert=new vtkVertex();
                vert.GetPointIds().SetId(0, id);
                cells.InsertNextCell(vert);*/
                pts.add(pt);    // if the region checker does not exist and the point is within the time limits then add it
            }
        } catch (IOException e)
        {
            if (!e.getClass().equals(EOFException.class))
                e.printStackTrace();
        }
/*        //
        vtkPolyData polyData=new vtkPolyData();
        polyData.SetPoints(points);
        polyData.SetVerts(cells);
        //
        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
        writer.SetFileTypeToBinary();
        writer.SetInputData(polyData);
        writer.Write();*/
        return pts;
    }

    @Override
    protected void computeTracks()
    {
        localFileMap.clear();
        localFileMap.putAll(getCurrentSkeleton().getFileMap());
        super.computeTracks();
        //
        for (int i=0; i<tracks.size(); i++)
        {
            Track track=tracks.get(i);
            for (int j=track.startId; j<=track.stopId; j++)
                track.registerSourceFileIndex(((Hayabusa2LaserLidarPoint)originalPoints.get(j)).getFileNum(), getCurrentSkeleton().getFileMap());
        }
    }

    public FSHyperTreeSkeleton getCurrentSkeleton()
    {
        return currentSkeleton;
    }

}
