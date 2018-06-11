package edu.jhuapl.sbmt.model.bennu.otes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.jhuapl.saavtk.metadata.Key;
import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.saavtk.metadata.MetadataManager;
import edu.jhuapl.saavtk.metadata.Serializers;
import edu.jhuapl.saavtk.metadata.SettableMetadata;
import edu.jhuapl.saavtk.metadata.TrackedMetadataManager;
import edu.jhuapl.saavtk.metadata.Version;
import edu.jhuapl.sbmt.model.phobos.HierarchicalSearchSpecification;

public class OTESSearchSpecification extends HierarchicalSearchSpecification
{
    private final TrackedMetadataManager stateManager;
    private ArrayList<String> dataProducts = new ArrayList<String>();
    private ArrayList<String> dataLocations = new ArrayList<String>();

    public OTESSearchSpecification() throws IOException
    {
        // Call parent constructor with name of the root node that we want
        super("OTES");
        this.stateManager = TrackedMetadataManager.of("OTES Hierarchy");
        initializeStateManager();
        readHierarchy();
    }

    public void readHierarchy() throws IOException
    {
        Serializers.deserialize(new File("/Users/steelrj1/Desktop/OTES.json"), "OTES", stateManager);   //TODO: update to read from SBVConfig
        for (int i=0; i<dataProducts.size(); i++)
        {
            String product = dataProducts.get(i);
            System.out.println("OTESSearchSpecification: readHierarchy: data product " + product);
            addHierarchicalSearchPath(new String[] {product}, i,-1);
        }
    }

    public void initializeStateManager()
    {
        if (!stateManager.isRegistered()) {
            stateManager.register(new MetadataManager() {
                final Key<ArrayList<String>> dataProductsKey = Key.of("dataProducts");
                final Key<ArrayList<String>> dataLocationsKey = Key.of("dataLocations");

                @Override
                public Metadata store()
                {
                    SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
                    result.put(dataProductsKey, dataProducts);
                    result.put(dataLocationsKey, dataLocations);
                    return result;
                }

                @Override
                public void retrieve(Metadata state)
                {
                    dataProducts = state.get(dataProductsKey);
                    dataLocations = state.get(dataLocationsKey);
                }
            });
        }
    }

    public static void main(String[] args) throws IOException
    {
        new OTESSearchSpecification().readHierarchy();
    }
}
