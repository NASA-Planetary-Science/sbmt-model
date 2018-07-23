package edu.jhuapl.sbmt.model.bennu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

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
}