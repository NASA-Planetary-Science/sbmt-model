package edu.jhuapl.sbmt.model.eros;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.joda.time.DateTime;

import edu.jhuapl.saavtk.metadata.FixedMetadata;
import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.saavtk.metadata.SettableMetadata;
import edu.jhuapl.saavtk.metadata.Version;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.sbmt.query.SearchMetadata;
import edu.jhuapl.sbmt.query.SearchResultsMetadata;
import edu.jhuapl.sbmt.query.database.DatabaseQueryBase;
import edu.jhuapl.sbmt.query.database.DatabaseSearchMetadata;
import edu.jhuapl.sbmt.query.database.SpectraDatabaseSearchMetadata;


/**
 * This class provides functions for querying the database.
 */
//This must be final because it is a singleton with a clone method.
public final class NisQuery extends DatabaseQueryBase
{
    private static NisQuery ref = null;

    public static String getNisPath(List<String> result)
    {
        int id = Integer.parseInt(result.get(0).split("/")[3]);
        int year = Integer.parseInt(result.get(1));
        int dayOfYear = Integer.parseInt(result.get(2));

        return getNisPath(id, year, dayOfYear);
    }

    public static String getNisPath(int name, int year, int dayOfYear)
    {
        String str = "/NIS/";
        str += year + "/";

        if (dayOfYear < 10)
            str += "00";
        else if (dayOfYear < 100)
            str += "0";

        str += dayOfYear + "/";

        str += "N0" + name + ".NIS";

        return str;
    }

    public static NisQuery getInstance()
    {
        if (ref == null)
            ref = new NisQuery();
        return ref;
    }

    @Override
    public NisQuery clone()
    {
        return getInstance();
    }

    private NisQuery()
    {
        super(null);
    }

    @Override
    public String getDataPath()
    {
        return "/NIS/2000";
    }

    @Override
    public SearchResultsMetadata runQuery(SearchMetadata queryMetadata)
    {
        FixedMetadata metadata = queryMetadata.getMetadata();
        double fromIncidence = metadata.get(DatabaseSearchMetadata.FROM_INCIDENCE);
        double toIncidence = metadata.get(DatabaseSearchMetadata.TO_INCIDENCE);
        double fromEmission = metadata.get(DatabaseSearchMetadata.FROM_EMISSION);
        double toEmission = metadata.get(DatabaseSearchMetadata.TO_EMISSION);
        double fromPhase = metadata.get(DatabaseSearchMetadata.FROM_PHASE);
        double toPhase = metadata.get(DatabaseSearchMetadata.TO_PHASE);
        double startDistance = metadata.get(DatabaseSearchMetadata.FROM_DISTANCE);
        double stopDistance = metadata.get(DatabaseSearchMetadata.TO_DISTANCE);
        DateTime startDate = new DateTime(metadata.get(DatabaseSearchMetadata.START_DATE));
        DateTime stopDate = new DateTime(metadata.get(DatabaseSearchMetadata.STOP_DATE));
        List<Integer> polygonTypes = metadata.get(DatabaseSearchMetadata.POLYGON_TYPES);
        TreeSet<Integer> cubeList = metadata.get(SpectraDatabaseSearchMetadata.CUBE_LIST);

        List<List<String>> results = null;

        double minIncidence = Math.min(fromIncidence, toIncidence);
        double maxIncidence = Math.max(fromIncidence, toIncidence);
        double minEmission = Math.min(fromEmission, toEmission);
        double maxEmission = Math.max(fromEmission, toEmission);
        double minPhase = Math.min(fromPhase, toPhase);
        double maxPhase = Math.max(fromPhase, toPhase);

        try
        {
            double minScDistance = Math.min(startDistance, stopDistance);
            double maxScDistance = Math.max(startDistance, stopDistance);

            HashMap<String, String> args = new HashMap<>();
            args.put("startDate", String.valueOf(startDate.getMillis()));
            args.put("stopDate", String.valueOf(stopDate.getMillis()));
            args.put("minScDistance", String.valueOf(minScDistance));
            args.put("maxScDistance", String.valueOf(maxScDistance));
            args.put("minIncidence", String.valueOf(minIncidence));
            args.put("maxIncidence", String.valueOf(maxIncidence));
            args.put("minEmission", String.valueOf(minEmission));
            args.put("maxEmission", String.valueOf(maxEmission));
            args.put("minPhase", String.valueOf(minPhase));
            args.put("maxPhase", String.valueOf(maxPhase));
            for (int i=0; i<4; ++i)
            {
                if (polygonTypes.contains(i))
                    args.put("polygonType"+i, "1");
                else
                    args.put("polygonType"+i, "0");
            }
            if (cubeList != null && cubeList.size() > 0)
            {
                String cubesStr = "";
                int size = cubeList.size();
                int count = 0;
                for (Integer i : cubeList)
                {
                    cubesStr += "" + i;
                    if (count < size-1)
                        cubesStr += ",";
                    ++count;
                }
                args.put("cubes", cubesStr);
            }

            results = doQuery("searchnis.php", constructUrlArguments(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            results = getResultsFromFileListOnServer(getDataPath() + "/nisTimes.txt", getDataPath(), getGalleryPath(), "");

        }

        return SearchResultsMetadata.of("", results);
    }

//    /**
//     * Run a query which searches for msi images between the specified dates.
//     * Returns a list of URL's of the fit files that match.
//     *
//     * @param startDate
//     * @param endDate
//     */
//    @Override
//    public List<List<String>> runQuery(
//            @SuppressWarnings("unused") String type,
//            DateTime startDate,
//            DateTime stopDate,
//            @SuppressWarnings("unused") boolean sumOfProductsSearch,
//            @SuppressWarnings("unused") List<Integer> camerasSelected,
//            @SuppressWarnings("unused") List<Integer> filtersSelected,
//            double startDistance,
//            double stopDistance,
//            @SuppressWarnings("unused") double startResolution,
//            @SuppressWarnings("unused") double stopResolution,
//            @SuppressWarnings("unused") String searchString,
//            List<Integer> polygonTypes,
//            double fromIncidence,
//            double toIncidence,
//            double fromEmission,
//            double toEmission,
//            double fromPhase,
//            double toPhase,
//            TreeSet<Integer> cubeList,
//            @SuppressWarnings("unused") ImageSource msiSource,
//            @SuppressWarnings("unused") int limbType)
//    {
//        List<List<String>> results = null;
//
//        double minIncidence = Math.min(fromIncidence, toIncidence);
//        double maxIncidence = Math.max(fromIncidence, toIncidence);
//        double minEmission = Math.min(fromEmission, toEmission);
//        double maxEmission = Math.max(fromEmission, toEmission);
//        double minPhase = Math.min(fromPhase, toPhase);
//        double maxPhase = Math.max(fromPhase, toPhase);
//
//        try
//        {
//            double minScDistance = Math.min(startDistance, stopDistance);
//            double maxScDistance = Math.max(startDistance, stopDistance);
//
//            HashMap<String, String> args = new HashMap<>();
//            args.put("startDate", String.valueOf(startDate.getMillis()));
//            args.put("stopDate", String.valueOf(stopDate.getMillis()));
//            args.put("minScDistance", String.valueOf(minScDistance));
//            args.put("maxScDistance", String.valueOf(maxScDistance));
//            args.put("minIncidence", String.valueOf(minIncidence));
//            args.put("maxIncidence", String.valueOf(maxIncidence));
//            args.put("minEmission", String.valueOf(minEmission));
//            args.put("maxEmission", String.valueOf(maxEmission));
//            args.put("minPhase", String.valueOf(minPhase));
//            args.put("maxPhase", String.valueOf(maxPhase));
//            for (int i=0; i<4; ++i)
//            {
//                if (polygonTypes.contains(i))
//                    args.put("polygonType"+i, "1");
//                else
//                    args.put("polygonType"+i, "0");
//            }
//            if (cubeList != null && cubeList.size() > 0)
//            {
//                String cubesStr = "";
//                int size = cubeList.size();
//                int count = 0;
//                for (Integer i : cubeList)
//                {
//                    cubesStr += "" + i;
//                    if (count < size-1)
//                        cubesStr += ",";
//                    ++count;
//                }
//                args.put("cubes", cubesStr);
//            }
//
//            results = doQuery("searchnis.php", constructUrlArguments(args));
//
///*            for (List<String> res : results)
//            {
//                String path = this.getNisPath(res);
//
//                matchedImages.add(path);
//            }*/
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//        //return matchedImages;
//        return results;
//
//        //System.err.println("Error: Not implemented. Do not call.");
//        //return null;
//    }

/*    public List<String> runSpectralQuery(
            DateTime startDate,
            DateTime stopDate,
            List<Integer> filters,
            boolean iofdbl,
            boolean cifdbl,
            double startDistance,
            double stopDistance,
            double startResolution,
            double stopResolution,
            String searchString,
            List<Integer> polygonTypes,
            double fromIncidence,
            double toIncidence,
            double fromEmission,
            double toEmission,
            double fromPhase,
            double toPhase,
            TreeSet<Integer> cubeList,
            ImageSource msiSource,
            int limbType)
    {
    }*/

    @Override
    protected List<List<String>> getCachedResults(
            String pathToImageFolder
            )
    {
        JOptionPane.showMessageDialog(null,
                "SBMT had a problem while performing the search. Ignoring search parameters and listing all cached spectra.",
                "Warning",
                JOptionPane.WARNING_MESSAGE);
    	// Create a map of actual files, with key the segment of the
    	// file name that will match the output of getNisPath.
        final List<File> fileList = getCachedFiles(pathToImageFolder);
        final Map<String, File> filesFound = new TreeMap<>();
        for (File file: fileList)
        {
        	// Format for NIS path is /NIS/YYYY/..., basically everything after the
        	// cache directory prefix.
            String path = file.getPath().substring(Configuration.getCacheDir().length());
            filesFound.put(path, file);
        }

        final List<List<String>> result = new ArrayList<>();
        SortedMap<String, List<String>> inventory = getDataInventory();
        // Match the current inventory against the cached file map.
        for (Entry<String, List<String>> each: inventory.entrySet())
        {
            List<String> res = each.getValue();
            if (!res.get(0).contains("NIS")) continue;
            String path = getNisPath(res);
            if (filesFound.containsKey(path))
                result.add(res);
        }
        return result;
    }

    protected void changeImagePathToFullPath(List<String> result)
    {
        String fullPath = result.get(1) + "/" + result.get(2) + "/" + result.get(0);
        if (!fullPath.contains("/"))
        {
            result.set(0, getDataPath() + "/" + fullPath);
        }
    }

    @Override
    public Metadata store()
    {
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
        return configMetadata;
    }

    @Override
    public void retrieve(Metadata source)
    {

    }
}
