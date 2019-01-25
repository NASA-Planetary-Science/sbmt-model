package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.saavtk.model.FileType;

public interface ImageKeyInterface
{

	String toString();

	String getName();

	String getImageFilename();

	ImageType getImageType();

	public Metadata store();

	public ImageSource getSource();

	int getSlice();

	ImagingInstrument getInstrument();

	FileType getFileType();

	String getBand();

	String getPointingFile();


}