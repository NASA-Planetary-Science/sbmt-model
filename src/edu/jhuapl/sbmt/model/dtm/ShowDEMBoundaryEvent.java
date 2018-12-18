package edu.jhuapl.sbmt.model.dtm;

import edu.jhuapl.saavtk2.event.EventSource;
import edu.jhuapl.sbmt.model.dem.DEMKey;

public class ShowDEMBoundaryEvent extends DEMEvent<Void>
{

    public ShowDEMBoundaryEvent(EventSource source, DEMKey key)
    {
        super(source, key, null);
    }

}
