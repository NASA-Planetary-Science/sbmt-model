package edu.jhuapl.sbmt.model.bennu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import edu.jhuapl.saavtk.metadata.Key;
import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.saavtk.metadata.SettableMetadata;
import edu.jhuapl.saavtk.metadata.Version;
import edu.jhuapl.sbmt.model.image.ImageSource;

public class OREXSearchSpec extends Hashtable<String, String> implements SearchSpec
{
    String dataName;
    String dataRootLocation;
    String dataPath;
    String dataListFilename;
    String source;
    String xAxisUnits;
    String yAxisUnits;
    String dataDescription;

    public OREXSearchSpec()
    {

    }

    public OREXSearchSpec(String name, String location, String dataPath, String filename, ImageSource source, String xAxisUnits, String yAxisUnits, String dataDescription)
    {
        put("dataName", dataName = name);
        put("dataRootLocation", dataRootLocation = location);
        put("dataPath", this.dataPath = dataPath);
        put("dataListFilename", this.dataListFilename = filename);
        put("source", this.source = source.toString());
        put("xAxisUnits", this.xAxisUnits = xAxisUnits);
        put("yAxisUnits", this.yAxisUnits = yAxisUnits);
        put("dataDescription", this.dataDescription = dataDescription);
    }

    public void fromFile(String csvLine)
    {
        String[] parts = csvLine.split(",");
        put("dataName", dataName = parts[0]);
        put("dataRootLocation", dataRootLocation = parts[1]);
        put("dataPath", this.dataPath = parts[2]);
        put("dataListFilename", this.dataListFilename = parts[3]);
        put("source", this.source = parts[4]);
        put("xAxisUnits", this.xAxisUnits = parts[5]);
        put("yAxisUnits", this.yAxisUnits = parts[6]);
        put("dataDescription", this.dataDescription = parts[7]);
    }

    public void toFile(BufferedWriter writer) throws IOException
    {
        writer.write(getDataName() + "," + getDataRootLocation() + "," + getDataPath() + "," + getDataListFilename() + "," + getSource() + "," + getxAxisUnits() + "," + getyAxisUnits() + "," + getDataDescription());
        writer.newLine();
    }

    public OREXSearchSpec(Hashtable<String, String> copy)
    {
        putAll(copy);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getDataName()
     */
    @Override
    public String getDataName()
    {
        return get("dataName");
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getDataRootLocation()
     */
    @Override
    public String getDataRootLocation()
    {
        return get("dataRootLocation");
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getDataPath()
     */
    @Override
    public String getDataPath()
    {
        return get("dataPath");
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getDataListFilename()
     */
    @Override
    public String getDataListFilename()
    {
        return get("dataListFilename");
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getSource()
     */
    @Override
    public ImageSource getSource()
    {
        return ImageSource.valueFor(get("source"));
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getxAxisUnits()
     */
    @Override
    public String getxAxisUnits()
    {
        return get("xAxisUnits");
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getyAxisUnits()
     */
    @Override
    public String getyAxisUnits()
    {
        return get("yAxisUnits");
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.SearchSpec#getDataDescription()
     */
    @Override
    public String getDataDescription()
    {
        return get("dataDescription");
    }

    Key<String> dataNameKey = Key.of("dataName");
    Key<String> dataRootLocationKey = Key.of("dataRootLocation");
    Key<String> dataPathKey = Key.of("dataPath");
    Key<String> dataListFilenameKey = Key.of("dataListFilename");
    Key<String> sourceKey = Key.of("source");
    Key<String> xAxisUnitsKey = Key.of("xAxisUnits");
    Key<String> yAxisUnitsKey = Key.of("yAxisUnits");
    Key<String> dataDescriptionKey = Key.of("dataDescription");

    @Override
    public Metadata store()
    {
        System.out.println("OREXSearchSpec: store: storing");
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
        write(dataNameKey, getDataName(), configMetadata);
        write(dataRootLocationKey, getDataRootLocation(), configMetadata);
        write(dataPathKey, getDataPath(), configMetadata);
        write(dataListFilenameKey, getDataListFilename(), configMetadata);
        write(sourceKey, get("source"), configMetadata);
        write(xAxisUnitsKey, getxAxisUnits(), configMetadata);
        write(yAxisUnitsKey, getyAxisUnits(), configMetadata);
        write(dataDescriptionKey, getDataDescription(), configMetadata);



        return configMetadata;
    }

    @Override
    public void retrieve(Metadata sourceMetadata)
    {
        dataName = read(dataNameKey, sourceMetadata);
        dataRootLocation = read(dataRootLocationKey, sourceMetadata);
        dataPath = read(dataPathKey, sourceMetadata);
        dataListFilename = read(dataListFilenameKey, sourceMetadata);
        source = read(sourceKey, sourceMetadata);
        xAxisUnits = read(xAxisUnitsKey, sourceMetadata);
        yAxisUnits = read(yAxisUnitsKey, sourceMetadata);
        dataDescription = read(dataDescriptionKey, sourceMetadata);

    }

    private <T> T read(Key<T> key, Metadata configMetadata)
    {
        if (configMetadata.hasKey(key) == false) return null;
        T value = configMetadata.get(key);
        if (value != null)
            return value;
        return null;
    }

    private <T> void write(Key<T> key, T value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value);
        }
    }
}