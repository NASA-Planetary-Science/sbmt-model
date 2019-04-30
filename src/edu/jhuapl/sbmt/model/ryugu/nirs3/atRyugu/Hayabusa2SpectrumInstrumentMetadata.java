package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.util.ArrayList;
import java.util.List;

import edu.jhuapl.sbmt.model.bennu.InstrumentMetadata;
import edu.jhuapl.sbmt.model.bennu.SearchSpec;

public class Hayabusa2SpectrumInstrumentMetadata<S extends SearchSpec> implements InstrumentMetadata<S>
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
}