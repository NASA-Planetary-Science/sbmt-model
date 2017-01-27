package edu.jhuapl.sbmt.model.phobos;

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

    // Order must match exactly what is defined in SmallBodyViewConfig's imageSearchFilterNames
    public enum CameraCheckbox
    {
        PHOBOS_2,
        VIKING_ORBITER_1_A,
        VIKING_ORBITER_1_B,
        VIKING_ORBITER_2_A,
        VIKING_ORBITER_2_B,
        MEX_HRSC,
        MRO_HIRISE,
        MGS_MOC
    };

    // Order must match exactly what is defined in SmallBodyViewConfig's imageSearchUserDefinedCheckBoxesNames
    public enum FilterCheckbox
    {
        VSK_CHANNEL_1,
        VSK_CHANNEL_2,
        VSK_CHANNEL_3,
        VIS_BLUE,
        VIS_MINUS_BLUE,
        VIS_VIOLET,
        VIS_CLEAR,
        VIS_GREEN,
        VIS_RED
    };

    /**
     *  This is where all the paths and corresponding mapping to camera/filter checkbox numbers are specified
     */
    public PhobosExperimentalSearchSpecification(String rootName)
    {
        // Call parent constructor
        super(rootName);

        // Phobos 2
        addHierarchicalSearchPath(new String[] {Mission.PHOBOS_2.toString(),Instrument.VSK.toString(),Band.CHANNEL_1.toString()},
                CameraCheckbox.PHOBOS_2.ordinal(),FilterCheckbox.VSK_CHANNEL_1.ordinal());

        // Viking Orbiter 1
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_1.toString(),Instrument.VIS_A.toString(),Band.MINUS_BLUE.toString()},
                CameraCheckbox.VIKING_ORBITER_1_A.ordinal(),FilterCheckbox.VIS_MINUS_BLUE.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_1.toString(),Instrument.VIS_A.toString(),Band.VIOLET.toString()},
                CameraCheckbox.VIKING_ORBITER_1_A.ordinal(),FilterCheckbox.VIS_VIOLET.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_1.toString(),Instrument.VIS_A.toString(),Band.CLEAR.toString()},
                CameraCheckbox.VIKING_ORBITER_1_A.ordinal(),FilterCheckbox.VIS_CLEAR.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_1.toString(),Instrument.VIS_A.toString(),Band.GREEN.toString()},
                CameraCheckbox.VIKING_ORBITER_1_A.ordinal(),FilterCheckbox.VIS_GREEN.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_1.toString(),Instrument.VIS_A.toString(),Band.RED.toString()},
                CameraCheckbox.VIKING_ORBITER_1_A.ordinal(),FilterCheckbox.VIS_RED.ordinal());

        // Viking Orbiter 2
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_2.toString(),Instrument.VIS_B.toString(),Band.VIOLET.toString()},
                CameraCheckbox.VIKING_ORBITER_2_B.ordinal(),FilterCheckbox.VIS_VIOLET.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_2.toString(),Instrument.VIS_B.toString(),Band.CLEAR.toString()},
                CameraCheckbox.VIKING_ORBITER_2_B.ordinal(),FilterCheckbox.VIS_CLEAR.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_2.toString(),Instrument.VIS_B.toString(),Band.GREEN.toString()},
                CameraCheckbox.VIKING_ORBITER_2_B.ordinal(),FilterCheckbox.VIS_GREEN.ordinal());
        addHierarchicalSearchPath(new String[] {Mission.VIKING_ORBITER_2.toString(),Instrument.VIS_B.toString(),Band.RED.toString()},
                CameraCheckbox.VIKING_ORBITER_2_B.ordinal(),FilterCheckbox.VIS_RED.ordinal());

        // MEX
        addHierarchicalSearchPath(new String[] {Mission.MEX.toString(),Instrument.HRSC.toString(),Band.ALL.toString()},
                CameraCheckbox.MEX_HRSC.ordinal(),-1);

        // MRO
        addHierarchicalSearchPath(new String[] {Mission.MRO.toString(),Instrument.HIRISE.toString(),Band.ALL.toString()},
                CameraCheckbox.MRO_HIRISE.ordinal(),-1);

        // MGS
        addHierarchicalSearchPath(new String[] {Mission.MGS.toString(),Instrument.MOC.toString(),Band.ALL.toString()},
                CameraCheckbox.MGS_MOC.ordinal(),-1);
    }
}
