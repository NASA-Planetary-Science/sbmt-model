package edu.jhuapl.sbmt.model.dtm;

import edu.jhuapl.saavtk2.event.EventSource;
import edu.jhuapl.sbmt.model.dem.DEMKey;

public class CreateDEMEvent extends DEMEvent<Void>
{

    public CreateDEMEvent(EventSource source, DEMKey key)
    {
        super(source, key, null);
    }

}
