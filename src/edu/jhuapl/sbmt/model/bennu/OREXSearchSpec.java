package edu.jhuapl.sbmt.model.bennu;

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