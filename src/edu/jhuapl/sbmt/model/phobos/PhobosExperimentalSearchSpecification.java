package edu.jhuapl.sbmt.model.phobos;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


public class PhobosExperimentalSearchSpecification extends HierarchicalSearchSpecification
{
    public enum Mission
    {
        PHOBOS_2("Phobos 2"),
        VIKING_ORBITER_1("Viking Orbiter 1"),
        VIKING_ORBITER_2("Viking Orbiter 2"),
        MEX("MEX"),
        MRO("MRO"),
        MGS("MGS");

        private final String name;

        private Mission(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public enum Instrument
    {
        VSK("VSK"),
        VIS_A("VIS-A"),
        VIS_B("VIS-B"),
        HRSC("HRSC"),
        HIRISE("HiRISE"),
        MOC("MOC");

        private final String name;

        private Instrument(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public enum Band
    {
        CHANNEL_1("Channel 1"),
        MINUS_BLUE("Minus Blue"),
        VIOLET("Violet"),
        CLEAR("Clear"),
        GREEN("Green"),
        RED("Red"),
        ALL("All");

        private final String name;

        private Band(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    // Constructor
    public PhobosExperimentalSearchSpecification()
    {
        // Root node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Phobos");
        treeModel = new DefaultTreeModel(rootNode);

        // Phobos 2
        {
            // Instruments
            DefaultMutableTreeNode vsk = new DefaultMutableTreeNode(Instrument.VSK);
            vsk.add(new DefaultMutableTreeNode(Band.CHANNEL_1));

            // Mission
            DefaultMutableTreeNode phobos2 = new DefaultMutableTreeNode(Mission.PHOBOS_2);
            phobos2.add(vsk);

            // Add to root
            rootNode.add(phobos2);
        }

        // Viking Orbiter 1
        {
            // Instruments
            DefaultMutableTreeNode vis_a = new DefaultMutableTreeNode(Instrument.VIS_A);
            vis_a.add(new DefaultMutableTreeNode(Band.MINUS_BLUE));
            vis_a.add(new DefaultMutableTreeNode(Band.VIOLET));
            vis_a.add(new DefaultMutableTreeNode(Band.CLEAR));
            vis_a.add(new DefaultMutableTreeNode(Band.GREEN));
            vis_a.add(new DefaultMutableTreeNode(Band.RED));

            // Mission
            DefaultMutableTreeNode viking1 = new DefaultMutableTreeNode(Mission.VIKING_ORBITER_1);
            viking1.add(vis_a);

            // Add to root
            rootNode.add(viking1);
        }

        // Viking Orbiter 2
        {
            // Instruments
            DefaultMutableTreeNode vis_b = new DefaultMutableTreeNode(Instrument.VIS_B);
            vis_b.add(new DefaultMutableTreeNode(Band.VIOLET));
            vis_b.add(new DefaultMutableTreeNode(Band.CLEAR));
            vis_b.add(new DefaultMutableTreeNode(Band.GREEN));
            vis_b.add(new DefaultMutableTreeNode(Band.RED));

            // Mission
            DefaultMutableTreeNode viking2 = new DefaultMutableTreeNode(Mission.VIKING_ORBITER_2);
            viking2.add(vis_b);

            // Add to root
            rootNode.add(viking2);
        }

        // MEX
        {
            // Instruments
            DefaultMutableTreeNode hrsc = new DefaultMutableTreeNode(Instrument.HRSC);
            hrsc.add(new DefaultMutableTreeNode(Band.ALL));

            // Mission
            DefaultMutableTreeNode mex = new DefaultMutableTreeNode(Mission.MEX);
            mex.add(hrsc);

            // Add to root
            rootNode.add(mex);
        }

        // MRO
        {
            // Instruments
            DefaultMutableTreeNode hirise = new DefaultMutableTreeNode(Instrument.HIRISE);
            hirise.add(new DefaultMutableTreeNode(Band.ALL));

            // Mission
            DefaultMutableTreeNode mro = new DefaultMutableTreeNode(Mission.MRO);
            mro.add(hirise);

            // Add to root
            rootNode.add(mro);
        }

        // MGS
        {
            // Instruments
            DefaultMutableTreeNode moc = new DefaultMutableTreeNode(Instrument.MOC);
            moc.add(new DefaultMutableTreeNode(Band.ALL));

            // Mission
            DefaultMutableTreeNode mgs = new DefaultMutableTreeNode(Mission.MGS);
            mgs.add(moc);

            // Add to root
            rootNode.add(mgs);
        }
    }

    // Method for processing tree selections
    @Override
    public void processTreeSelections(TreePath[] selectedPaths)
    {
        // Go through each selected path
        for(TreePath tp : selectedPaths)
        {
            // Extract mission, instrument, and band


            String mission = ((DefaultMutableTreeNode)tp.getPathComponent(0)).getUserObject().toString();
            String instrument = ((DefaultMutableTreeNode)tp.getPathComponent(1)).getUserObject().toString();
            //String band = ((DefaultMutableTreeNode)tp.getPathComponent(2)).getUserObject().toString();

            // Process it
            System.out.println("Mission: " + mission + ", Instrument: " + instrument);// + ", Band: " + band);
        }
    }
}
