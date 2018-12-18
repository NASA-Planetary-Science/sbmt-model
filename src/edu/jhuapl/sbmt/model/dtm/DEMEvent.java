package edu.jhuapl.sbmt.model.dtm;

import edu.jhuapl.saavtk2.event.BasicEvent;
import edu.jhuapl.saavtk2.event.EventSource;
import edu.jhuapl.sbmt.model.dem.DEMKey;

public class DEMEvent<T> extends BasicEvent<T>{


    final DEMKey key;

	public DEMEvent(EventSource source, DEMKey key, T value) {
		super(source, value);
		this.key=key;
	}

	public DEMKey getKey()
    {
        return key;
    }

}
