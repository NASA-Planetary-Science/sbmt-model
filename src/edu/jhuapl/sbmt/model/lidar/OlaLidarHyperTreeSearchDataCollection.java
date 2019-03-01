package edu.jhuapl.sbmt.model.lidar;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import edu.jhuapl.saavtk.gui.ProgressBarSwingWorker;
import edu.jhuapl.saavtk.model.PointInRegionChecker;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileCache.NonexistentRemoteFile;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.lidar.LidarPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeSkeleton;
import edu.jhuapl.sbmt.lidar.hyperoctree.ola.OlaFSHyperPoint;
import edu.jhuapl.sbmt.lidar.hyperoctree.ola.OlaFSHyperTreeSkeleton;
import edu.jhuapl.sbmt.util.TimeUtil;

public class OlaLidarHyperTreeSearchDataCollection extends LidarSearchDataCollection    // currently implemented only for OLA lidar points, but could be revised to handle any points satisfying the LidarPoint interface.
{
	public enum TrackFileType
	{
		TEXT,
		BINARY,
		OLA_LEVEL_2
	};

	private Map<String, FSHyperTreeSkeleton> skeletons = new HashMap<String, FSHyperTreeSkeleton>();
	private FSHyperTreeSkeleton currentSkeleton;
	private JComponent parentForProgressMonitor;
	private boolean loading=false;
	//    Map<Integer, List<OlaFSHyperPoint>> filesWithPoints = new HashMap<Integer, List<OlaFSHyperPoint>>();
	Map<Integer, HashSet<OlaFSHyperPoint>> filesWithPoints = new HashMap<Integer, HashSet<OlaFSHyperPoint>>();

	@Override
	public boolean isLoading()
	{
		return loading;
	}

	public OlaLidarHyperTreeSearchDataCollection(SmallBodyModel smallBodyModel)
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
			//            System.out.println("Adding datasource: " + datasourceName + " - " + datasourcePath);
			Path basePath = Paths.get(datasourcePath);
			FSHyperTreeSkeleton skeleton = skeletons.get(datasourceName);
			if (skeleton == null)
			{
				skeleton = new OlaFSHyperTreeSkeleton(basePath);
				skeletons.put(datasourceName, skeleton);
			}
		}
	}

	public void setCurrentDatasourceSkeleton(String datasourceName)
	{
		if (datasourceName != null && datasourceName.length() > 0)
		{
			//            System.out.println("Setting current datasource: " + datasourceName);
			FSHyperTreeSkeleton skeleton = skeletons.get(datasourceName);
			if (skeleton != null)
				currentSkeleton = skeleton;
		}
	}

	private Set<FSHyperTreeSkeleton> readIn = new HashSet<FSHyperTreeSkeleton>();

	public void readSkeleton() throws NonexistentRemoteFile
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
		// Here it corresponds to the list of leaves of a hypertree that intersect the bounding box of the user selection area.

		setTimeSeparationBetweenTracks(timeSeparationBetweenTracks);
		setMinTrackLength(minTrackLength);


		ProgressBarSwingWorker dataLoader=new ProgressBarSwingWorker(parentForProgressMonitor,"Loading Lidar datapoints ("+cubeList.size()+" individual chunks)")
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				Stopwatch sw=new Stopwatch();
				sw.start();
				loading=true;

				originalPoints.clear();
				filesWithPoints.clear();

				int cnt=0;
				for (Integer cidx : cubeList)
				{
					Path leafPath=currentSkeleton.getNodeById(cidx).getPath();

					//                    System.out.println("OlaLidarHyperTreeSearchDataCollection: setLidarData: Loading data partition "+(cnt+1)+"/"+cubeList.size()+" (id="+cidx+") \""+leafPath+"\"");
					Path dataFilePath=leafPath.resolve("data");
					File dataFile=FileCache.getFileFromServer(dataFilePath.toString());
					if (!dataFile.exists())
						dataFile=FileCache.getFileFromServer(FileCache.FILE_PREFIX+dataFilePath.toString());
					List<LidarPoint> pts=readDataFile(dataFile,pointInRegionChecker,new double[]{startDate,stopDate});
					for (int i=0; i<pts.size(); i++)
					{

						OlaFSHyperPoint currPt = (OlaFSHyperPoint)pts.get(i);

						int fileNum = currPt.getFileNum();
						originalPointsSourceFiles.put(currPt, fileNum);

						// if list already exists, just add point
						if(filesWithPoints.containsKey(fileNum)) {
							//                            List<OlaFSHyperPoint> currList = filesWithPoints.get(fileNum);
							HashSet<OlaFSHyperPoint> currList = filesWithPoints.get(fileNum);
							if (!currList.contains(currPt)) {
								currList.add(currPt);
							}
							filesWithPoints.put(fileNum, currList);
						} else { // otherwise, create list and add point
							//                            List<OlaFSHyperPoint> currList = new ArrayList<OlaFSHyperPoint>();
							HashSet<OlaFSHyperPoint> currList = new HashSet<OlaFSHyperPoint>();
							currList.add(currPt);
							filesWithPoints.put(fileNum, currList);
						}
					}
					//
					cnt++;
					double progressPercentage=((double)cnt/(double)cubeList.size()*100);
					setProgress((int)progressPercentage);
					if (isCancelled())
						break;
				}

				// now build originalpoints from all of the unique lists
				for (Integer key : filesWithPoints.keySet()) {
					originalPoints.addAll(filesWithPoints.get(key));
				}

				cancel(true);
				loading=false;

				sw.reset();
				sw.start();

				return null;
			}

		};
		dataLoader.executeDialog();
		initTranslationArray(originalPoints.size());

		radialOffset = 0.0;

		computeTracks();

		removeTracksThatAreTooSmall();

		// sometimes the last track ends up with bad times because the user cancelled the search, so remove any that are bad in this respect
		List<Track> tracksToRemove=Lists.newArrayList();
		for (Track t : tracks)
			if (t.timeRange[0].length()==0 || t.timeRange[1].length()==0)
				tracksToRemove.add(t);
		for (Track t : tracksToRemove)
			tracks.remove(t);

		assignInitialColorToTrack();

		updateTrackPolydata();

		selectPoint(-1);

		//        pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);

	}

	static vtkPoints points=new vtkPoints();
	static vtkCellArray cells=new vtkCellArray();

	public static List<LidarPoint> readDataFile(File dataInputFile, PointInRegionChecker pointInRegionChecker, double[] timeLimits) {
		List<LidarPoint> pts=Lists.newArrayList();
		try {
			DataInputStream stream=new DataInputStream(new FileInputStream(dataInputFile));
			while (true) {
				OlaFSHyperPoint pt=new OlaFSHyperPoint(stream);                                         // TODO: this is OLA-specific
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

		tracks.clear();

		int size = originalPoints.size();
		if (size == 0)
			return;

		ProgressBarSwingWorker trackComputer=new ProgressBarSwingWorker(parentForProgressMonitor,"Computing tracks from results")
		{

			@Override
			protected Void doInBackground() throws Exception
			{


				double prevTime;
				Set<Integer> keys = filesWithPoints.keySet();
				double count = 0;
				double total = keys.size();

				ArrayList<LidarPoint> allPointsSortedbyFileThenTime = Lists.newArrayList();


				for (Integer fileNum : keys) {
					// create a new track for this file
					Track track = new Track();
					track.registerSourceFileIndex(fileNum, localFileMap);

					// get all current points and convert to a list, then sort by time
					HashSet<OlaFSHyperPoint> currPointsSet = filesWithPoints.get(fileNum);
					List<OlaFSHyperPoint> pntsFromCurrFile = new ArrayList<OlaFSHyperPoint>(currPointsSet);
					Collections.sort(pntsFromCurrFile);

					allPointsSortedbyFileThenTime.addAll(pntsFromCurrFile);

					// get start and stop time of tracks in this file
					OlaFSHyperPoint start = pntsFromCurrFile.get(0);
					OlaFSHyperPoint stop = pntsFromCurrFile.get(pntsFromCurrFile.size() - 1);

					// original start and stop points are first and last.
					// this will change if points are separated by more than
					// time separation between tracks
					int istart = allPointsSortedbyFileThenTime.indexOf(start);
					int istop = allPointsSortedbyFileThenTime.indexOf(stop);


					prevTime = start.getTime();
					for (OlaFSHyperPoint point : pntsFromCurrFile) {
						double currentTime = point.getTime();
						double diff = currentTime - prevTime;
						if (diff >= getTimeSeparationBetweenTracks()) {
							/*
							 *  save current track before starting next one
							 */
							int iLastPoint = pntsFromCurrFile.indexOf(point) - 1;
							istop = allPointsSortedbyFileThenTime.indexOf(pntsFromCurrFile.get(iLastPoint)); // get index of last point
							stop = (OlaFSHyperPoint) allPointsSortedbyFileThenTime.get(istop); // get last point

							// create the current track
							track.timeRange = new String[]
									{TimeUtil.et2str(start.getTime()),
											TimeUtil.et2str(stop.getTime())};
							track.startId = istart;
							track.stopId = istop;
							// and add to list of all tracks
							tracks.add(track);

							/*
							 * start a new track beginning with current point
							 */

							track = new Track();
							track.registerSourceFileIndex(fileNum, localFileMap);
							istart = allPointsSortedbyFileThenTime.indexOf(point);
							start = point;
						}
						else {
							/*
							 * We want to keep going since the time between these 2 points was
							 * not greater than timeSeparationBetweenTracks.
							 * But first lets check if it was the last point, so we can save
							 * this track if it was.
							 */
							int indexOfPoint = pntsFromCurrFile.indexOf(point);
							if (indexOfPoint == pntsFromCurrFile.size()-1) {
								// this is the last point in the file, so finalize this last track
								istop = allPointsSortedbyFileThenTime.indexOf(pntsFromCurrFile.get(indexOfPoint)); // get index of this point
								stop = (OlaFSHyperPoint) allPointsSortedbyFileThenTime.get(istop); // get this point

								// create the current track
								track.timeRange = new String[]
										{TimeUtil.et2str(start.getTime()),
												TimeUtil.et2str(stop.getTime())};
								track.startId = istart;
								track.stopId = istop;
								// and add to list of all tracks
								tracks.add(track);
							}
						}
						prevTime = currentTime;
					}

					count++;
					double progressPercentage=((double)count/(double)total*100);
					setProgress((int)progressPercentage);
					if (isCancelled())
						break;
				}

				originalPoints = allPointsSortedbyFileThenTime;

				cancel(true);
				return null;


			}



		};

		trackComputer.executeDialog();

		// sort tracks by their starting time
		Collections.sort(tracks, new Comparator<Track>() {
			public int compare(Track track1, Track track2) {
				double track1Start = TimeUtil.str2et(track1.timeRange[0]);
				double track2Start = TimeUtil.str2et(track2.timeRange[0]);
				return track1Start > track2Start ? 1 : track1Start < track2Start ? -1 : 0;
			}
		});


	}




	public FSHyperTreeSkeleton getCurrentSkeleton()
	{
		return currentSkeleton;
	}

}
