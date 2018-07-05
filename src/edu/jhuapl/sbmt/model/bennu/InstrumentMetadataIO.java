package edu.jhuapl.sbmt.model.bennu;

public interface InstrumentMetadataIO<S extends SearchSpec>
{

    InstrumentMetadata<S> getInstrumentMetadata(
            String instrumentName);

    void readHierarchyForInstrument(String instrumentName);

}
