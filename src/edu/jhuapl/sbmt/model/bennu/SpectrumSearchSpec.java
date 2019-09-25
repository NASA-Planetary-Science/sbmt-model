package edu.jhuapl.sbmt.model.bennu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import edu.jhuapl.sbmt.model.image.ImageSource;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.SettableMetadata;

public class SpectrumSearchSpec extends Hashtable<String, String> implements SearchSpec
{
    String dataName;
    String dataRootLocation;
    String dataPath;
    String dataListFilename;
    String source;
    String xAxisUnits;
    String yAxisUnits;
    String dataDescription;

    public SpectrumSearchSpec()
    {

    }

    public SpectrumSearchSpec(String name, String location, String dataPath, String filename, ImageSource source, String xAxisUnits, String yAxisUnits, String dataDescription)
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

    public SpectrumSearchSpec(Hashtable<String, String> copy)
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

    final static Key<String> dataNameKey = Key.of("dataName");
    final static Key<String> dataRootLocationKey = Key.of("dataRootLocation");
    final static Key<String> dataPathKey = Key.of("dataPath");
    final static Key<String> dataListFilenameKey = Key.of("dataListFilenameKey");
    final static Key<String> sourceKey = Key.of("source");
    final static Key<String> xAxisUnitsKey = Key.of("xAxisUnits");
    final static Key<String> yAxisUnitsKey = Key.of("yAxisUnits");
    final static Key<String> dataDescriptionKey = Key.of("dataDescription");

	@Override
	public Metadata store()
	{
		SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
		configMetadata.put(dataNameKey, getDataName());
		configMetadata.put(dataRootLocationKey, getDataRootLocation());
		configMetadata.put(dataPathKey, getDataPath());
		configMetadata.put(dataListFilenameKey, getDataListFilename());
		configMetadata.put(sourceKey, getSource().toString());
		configMetadata.put(xAxisUnitsKey, getxAxisUnits());
		configMetadata.put(yAxisUnitsKey, getyAxisUnits());
		configMetadata.put(dataDescriptionKey, getDataDescription());
        return configMetadata;
	}

	@Override
	public void retrieve(Metadata sourceMetadata)
	{
		put("dataName", sourceMetadata.get(dataNameKey));
		put("dataRootLocation", sourceMetadata.get(dataRootLocationKey));
		put("dataPath", sourceMetadata.get(dataPathKey));
		put("dataListFilename", sourceMetadata.get(dataListFilenameKey));
		put("source", sourceMetadata.get(sourceKey));
		put("xAxisUnits", sourceMetadata.get(xAxisUnitsKey));
		put("yAxisUnits", sourceMetadata.get(yAxisUnitsKey));
		put("dataDescription", sourceMetadata.get(dataDescriptionKey));

	}
}