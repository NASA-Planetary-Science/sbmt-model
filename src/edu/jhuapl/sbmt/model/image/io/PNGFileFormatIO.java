package edu.jhuapl.sbmt.model.image.io;

import vtk.vtkImageData;
import vtk.vtkPNGReader;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class PNGFileFormatIO
{
    PerspectiveImage image;

    public PNGFileFormatIO(PerspectiveImage image)
    {
        this.image = image;
    }

    public void loadPngFile(String name)
    {
//        String name = getPngFileFullPath();

        String imageFile = null;
        if (image.getKey().source == ImageSource.IMAGE_MAP)
            imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
        else
            imageFile = image.getKey().name;

        if (image.getRawImage() == null)
            image.setRawImage(new vtkImageData());

        vtkPNGReader reader = new vtkPNGReader();
        reader.SetFileName(imageFile);
        reader.Update();
        image.getRawImage().DeepCopy(reader.GetOutput());
    }

    public int loadNumSlices(String filename)
    {
        return 0;
    }

}
