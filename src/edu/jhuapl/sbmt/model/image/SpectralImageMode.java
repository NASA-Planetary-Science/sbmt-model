package edu.jhuapl.sbmt.model.image;

public enum SpectralImageMode
{
    MONO {
        public String toString()
        {
            return "Monospectral";
        }
    },
    MULTI {
        public String toString()
        {
            return "Multispectral";
        }
    },
    HYPER {
        public String toString()
        {
            return "Hyperspectral";
        }
    },
}