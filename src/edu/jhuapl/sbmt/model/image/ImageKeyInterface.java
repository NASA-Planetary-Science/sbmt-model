package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.sbmt.core.image.IImagingInstrument;
import edu.jhuapl.sbmt.core.image.ImageSource;
import edu.jhuapl.sbmt.core.image.ImageType;

import crucible.crust.metadata.api.Metadata;

public interface ImageKeyInterface
{

	String toString();

	String getName();

	String getImageFilename();

	ImageType getImageType();

	public Metadata store();

	public ImageSource getSource();

	int getSlice();

	IImagingInstrument getInstrument();

	FileType getFileType();

	String getBand();

	String getPointingFile();

	String getOriginalName();

	String getFlip();

	public double getRotation();

}