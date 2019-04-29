//package edu.jhuapl.sbmt.model.bennu.ovirs;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import crucible.crust.metadata.api.Key;
//import crucible.crust.metadata.api.Metadata;
//import crucible.crust.metadata.api.MetadataManager;
//import crucible.crust.metadata.api.Serializers;
//import crucible.crust.metadata.impl.SettableMetadata;
//import crucible.crust.metadata.api.TrackedMetadataManager;
//import crucible.crust.metadata.api.Version;
//import edu.jhuapl.sbmt.model.bennu.otes.OTESSearchSpecification;
//import edu.jhuapl.sbmt.model.bennu.otes.SpectraHierarchicalSearchSpecification;
//import edu.jhuapl.sbmt.model.image.ImageSource;
//
//public class OVIRSSearchSpecification extends SpectraHierarchicalSearchSpecification
//{
//    private final TrackedMetadataManager stateManager;
//
//    public OVIRSSearchSpecification() throws IOException
//    {
//        // Call parent constructor with name of the root node that we want
//        super("OVIRS");
//        this.stateManager = TrackedMetadataManager.of("OVIRS Hierarchy");
//        initializeStateManager();
//        readHierarchy();
////        specs.add(new OTESSearchSpec("L2 Spot Emissivity", "/earth/osirisrex/otes/spectra/l2", "spectra", ImageSource.CORRECTED_SPICE));
////        specs.add(new OTESSearchSpec("L3 Spot Emissivity", "/earth/osirisrex/otes/spectra/l3", "spectra", ImageSource.CORRECTED_SPICE));
////        Serializers.serialize("OTES", stateManager, new File("/Users/steelrj1/Desktop/test.json"));
//    }
//
//    public void readHierarchy() throws IOException
//    {
//        Serializers.deserialize(new File("/Users/steelrj1/Desktop/OVIRS.json"), "OVIRS", stateManager);   //TODO: update to read from SBVConfig
////        for (int i=0; i<specs.size(); i++)
////        {
////            ArrayList<String> product = specs.get(i);
////            System.out.println("OVRSSearchSpecification: readHierarchy: data product " + product);
////            addHierarchicalSearchPath(new String[] {product.get(0)}, i,-1);
////        }
//    }
//
//    public void initializeStateManager()
//    {
//        if (!stateManager.isRegistered()) {
//            stateManager.register(new MetadataManager() {
//                final Key<ArrayList<ArrayList<String>>> searchSpecKey = Key.of("specs");
//
//                @Override
//                public Metadata store()
//                {
//                    SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
//                    result.put(searchSpecKey, specs);
//
//                    return result;
//                }
//
//                @Override
//                public void retrieve(Metadata state)
//                {
//                    specs = state.get(searchSpecKey);
//                    System.out.println(
//                            "OVIRSSearchSpecification.initializeStateManager().new MetadataManager() {...}: retrieve: specs size " + specs.size());
////                    OTESSearchSpec otesSearchSpec = specs.get(0);
////                    System.out.println(
////                            "OTESSearchSpecification.initializeStateManager().new MetadataManager() {...}: retrieve: specs 0 " + otesSearchSpec);
//                    for (ArrayList<String> spec : specs)
//                    {
//                        System.out.println(
//                                "OVIRSSearchSpecification.initializeStateManager().new MetadataManager() {...}: retrieve: " + spec);
//                    }
////                    dataProducts = state.get(dataProductsKey);
////                    dataLocations = state.get(dataLocationsKey);
//                }
//            });
//        }
//    }
//
//    public static void main(String[] args) throws IOException
//    {
//        new OTESSearchSpecification();
//    }
//
//    class OVIRSSearchSpec extends ArrayList<String>
//    {
//
//        public OVIRSSearchSpec(String name, String location, String filename, ImageSource source)
//        {
//            add(name);
//            add(location);
//            add(filename);
//            add(source.toString());
//        }
//
//        public String getName()
//        {
//            return get(0);
//        }
//
//        public String getLocation()
//        {
//            return get(1);
//        }
//
//        public String getFilename()
//        {
//            return get(2);
//        }
//
//        public ImageSource getImageSource()
//        {
//            return ImageSource.valueFor(get(3));
//        }
//    }
//}
