package edu.jhuapl.sbmt.model.spectrum;

public enum SpectrumColoringStyle
{
    RGB("RGB"),
    EMISSION_ANGLE("Emission Angle");

    private String name;

    public String toString()
    {
        return name;
    }

    private SpectrumColoringStyle(String name)
    {
        this.name = name;
    }

    public static SpectrumColoringStyle getStyleForName(String name)
    {
        for (SpectrumColoringStyle style : values())
        {
            if (style.toString().equals(name))
                return style;
        }

        return null;
    }

}
