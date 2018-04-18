package edu.jhuapl.sbmt.model.image;

public enum ImageSource
{
    SPICE("SPICE Derived", "pds"),
    GASKELL("SPC Derived", "gaskell"),
    GASKELL_UPDATED("SPC Derived", "gaskell_updated"),
    LABEL("Label Derived", "label"),
    CORRECTED("Corrected", "corrected"),
    CORRECTED_SPICE("Corrected SPICE Derived", "corrected_pds"),
    IMAGE_MAP("ImageMap", "image_map"),
    LOCAL_CYLINDRICAL("LocalCylindrical", "local_cylindrical"),
    LOCAL_PERSPECTIVE("LocalPerspective", "local_perspective"),
    FALSE_COLOR("FalseColor", "false_color"),
    GENERATED_CYLINDRICAL("GeneratedCylindrical", "generated_cylindrical");

    private String string; //String used in the GUI Pointing drop-down menu
    private String databaseTableName; //String used in the database table name

    private ImageSource(String nameString, String databaseTableName)
    {
        this.string = nameString;
        this.databaseTableName = databaseTableName;
    }

    public String toString()
    {
        return string;
    }

    public String getDatabaseTableName()
    {
        return databaseTableName;
    }

    public static String printSources(int tabLen)
    {
        String out = new String();
        String tab = new String();
        int i = 0;
        while (i < tabLen)
        {
            tab = tab + " ";
            i++;
        }
        ImageSource[] sources = ImageSource.values();
        String imagesource = new String();
        for (ImageSource s : sources)
        {
            out = out + tab + s.name() + "\n";
        }
        return out;
    }

}