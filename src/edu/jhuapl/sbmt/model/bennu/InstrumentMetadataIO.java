package edu.jhuapl.sbmt.model.bennu;

import java.io.FileNotFoundException;

public interface InstrumentMetadataIO<S extends SearchSpec>
{

    InstrumentMetadata<S> getInstrumentMetadata(
            String instrumentName);

    void readHierarchyForInstrument(String instrumentName);

    void loadMetadata() throws FileNotFoundException;

}
