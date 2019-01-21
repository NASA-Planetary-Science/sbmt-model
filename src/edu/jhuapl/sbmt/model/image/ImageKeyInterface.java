package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.saavtk.metadata.Metadata;

public interface ImageKeyInterface
{

	String toString();

	String getName();

	String getImageFilename();

	ImageType getImageType();

	public Metadata store();

	public ImageSource getSource();

}