package edu.jhuapl.sbmt.model.dtm;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.jhuapl.saavtk.util.MapUtil;
import edu.jhuapl.saavtk2.event.Event;
import edu.jhuapl.saavtk2.event.EventListener;
import edu.jhuapl.saavtk2.table.BasicTable.TableRowAddedEvent;
import edu.jhuapl.saavtk2.table.BasicTable.TableRowRemovedEvent;
import edu.jhuapl.sbmt.model.custom.CustomShapeModel;
import edu.jhuapl.sbmt.model.dem.DEM;
import edu.jhuapl.sbmt.model.dem.DEMKey;

public class DEMConfigFile implements EventListener
{

    boolean loaded;
    DEMTable table;
    Path path;

    public DEMConfigFile(Path path, DEMTable table)
    {
        this.path = path;
        this.table = table;
        table.addListener(this);
    }

    @Override
    public void handle(Event event)
    {
        table.removeListener(this);
        if (event instanceof TableRowAddedEvent || event instanceof TableRowRemovedEvent)
        {
            save();
        }
        table.addListener(this);
    }

    protected void save() // saves dem keys in table to config file, overwriting whatever is already there
    {
        MapUtil configMap = new MapUtil(path.toString());

        String demNames = "";
        String demFilenames = "";

        for (int i = 0; i < table.getNumberOfRows(); ++i)
        {
            demFilenames += table.getKey(i).fileName;
            demNames += table.getKey(i).displayName;

            if (i < table.getNumberOfRows() - 1)
            {
                demNames += CustomShapeModel.LIST_SEPARATOR;
                demFilenames += CustomShapeModel.LIST_SEPARATOR;
            }
        }

        Map<String, String> newMap = new LinkedHashMap<String, String>();

        newMap.put(DEM.DEM_NAMES, demNames);
        newMap.put(DEM.DEM_FILENAMES, demFilenames);

        configMap.put(newMap);
    }

    public void load() // loads dem keys into table from config file
    {
        if (loaded)
            return;

        table.removeListener(this);

        MapUtil configMap = new MapUtil(path.toString());

        if (configMap.containsKey(DEM.DEM_NAMES))
        {
            boolean needToUpgradeConfigFile = false;
            String[] demNames = configMap.getAsArray(DEM.DEM_NAMES);
            String[] demFilenames = configMap.getAsArray(DEM.DEM_FILENAMES);
            if (demFilenames == null)
            {
                // for backwards compatibility
                demFilenames = configMap.getAsArray(DEM.DEM_MAP_PATHS);
                demNames = new String[demFilenames.length];
                for (int i = 0; i < demFilenames.length; ++i)
                {
                    demNames[i] = new File(demFilenames[i]).getName();
                    demFilenames[i] = "dem" + i + ".FIT";
                }

                // Mark that we need to upgrade config file to latest version
                // which we'll do at end of function.
                needToUpgradeConfigFile = true;
            }

            int numDems = demNames != null ? demNames.length : 0;
            for (int i = 0; i < numDems; ++i)
            {
                DEMKey key = new DEMKey(demFilenames[i], demNames[i]);
                table.appendRow(key);
            }

            if (needToUpgradeConfigFile)
                save();
        }

        table.addListener(this);

        loaded = true;
    }

}
