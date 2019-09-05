package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.util.ArrayList;
import java.util.List;

import edu.jhuapl.sbmt.model.bennu.InstrumentMetadata;
import edu.jhuapl.sbmt.model.bennu.SearchSpec;
import edu.jhuapl.sbmt.model.bennu.SpectrumSearchSpec;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.SettableMetadata;

public class Hayabusa2SpectrumInstrumentMetadata<S extends SearchSpec> implements InstrumentMetadata<S>, MetadataManager
{
    String instrumentName;
    String queryType;
    List<S> searchMetadata = new ArrayList<S>();

    public Hayabusa2SpectrumInstrumentMetadata()
    {

    }

    public Hayabusa2SpectrumInstrumentMetadata(String instName)
    {
        this.instrumentName = instName;
    }

    public Hayabusa2SpectrumInstrumentMetadata(String instName, ArrayList<S> specs)
    {
        this.instrumentName = instName;
        this.searchMetadata = specs;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#setSpecs(java.util.ArrayList)
     */
    @Override
    public void setSpecs(ArrayList<S> specs)
    {
        this.searchMetadata = specs;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#getSpecs()
     */
    @Override
    public List<S> getSpecs()
    {
        return searchMetadata;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#addSearchSpecs(java.util.List)
     */
    @Override
    public void addSearchSpecs(List<S> specs)
    {
        this.searchMetadata.addAll(specs);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#addSearchSpec(S)
     */
    @Override
    public void addSearchSpec(S spec)
    {
        searchMetadata.add(spec);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#getInstrumentName()
     */
    @Override
    public String getInstrumentName()
    {
        return instrumentName;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#setInstrumentName(java.lang.String)
     */
    @Override
    public void setInstrumentName(String instrumentName)
    {
        this.instrumentName = instrumentName;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#getQueryType()
     */
    @Override
    public String getQueryType()
    {
        return queryType;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#setQueryType(java.lang.String)
     */
    @Override
    public void setQueryType(String queryType)
    {
        this.queryType = queryType;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadata#toString()
     */
    @Override
    public String toString()
    {
        return "Hayabusa2SpectrumInstrumentMetadata [instrumentName="
                + instrumentName + ", specs=" + searchMetadata + "]";
    }

    Key<Metadata[]> searchMetadataKey = Key.of("searchMetadata");

    private <T> void writeMetadataArray(Key<Metadata[]> key, MetadataManager[] values, SettableMetadata configMetadata)
    {
        if (values != null)
        {
            Metadata[] data = new Metadata[values.length];
            int i=0;
            for (MetadataManager val : values) data[i++] = val.store();
            configMetadata.put(key, data);
        }
    }

    private Metadata[] readMetadataArray(Key<Metadata[]> key, Metadata configMetadata)
    {
        Metadata[] values = configMetadata.get(key);
        if (values != null)
        {
            return values;
        }
        return null;
    }

    @Override
    public Metadata store()
    {
        SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
        SearchSpec[] specs = new SearchSpec[searchMetadata.size()];
        searchMetadata.toArray(specs);
        writeMetadataArray(searchMetadataKey, specs, configMetadata);
        return configMetadata;
    }

    @Override
    public void retrieve(Metadata source)
    {
        Metadata[] metadata = readMetadataArray(searchMetadataKey, source);
        for (Metadata meta : metadata)
        {
            SpectrumSearchSpec spec = new SpectrumSearchSpec();
            spec.retrieve(meta);
            addSearchSpec((S)spec);
        }
    }
}