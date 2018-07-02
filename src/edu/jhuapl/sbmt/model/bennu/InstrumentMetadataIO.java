package edu.jhuapl.sbmt.model.bennu;

public interface InstrumentMetadataIO
{

    OREXSpectrumInstrumentMetadata<OREXSearchSpec> getInstrumentMetadata(
            String instrumentName);

    void readHierarchyForInstrument(String instrumentName);

}
