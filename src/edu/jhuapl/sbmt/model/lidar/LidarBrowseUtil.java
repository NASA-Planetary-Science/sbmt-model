package edu.jhuapl.sbmt.model.lidar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileCache.UnauthorizedAccessException;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.util.TimeUtil;

import glum.gui.GuiUtil;

/**
 * Utility class that provides various methods for working with lidar catalog
 * data products.
 * <P>
 * A lidar catalog data product provides a listing of lidar (browse) files
 * available for a specific shape model.
 * <P>
 * Each entry in the lidar data catalog consists of the following fields:
 * <UL>
 * <LI>Product name
 * <LI>Start Time
 * <LI>Stop Time
 * <LI>Number of lidar data points
 * </UL>
 *
 * @author lopeznr1
 */
public class LidarBrowseUtil
{
	/**
	 * Loads the list of LidarFileSpecs from the specified BodyViewConfig.
	 */
	public static List<LidarFileSpec> loadLidarFileSpecListFor(BodyViewConfig aBodyViewConfig) throws IOException
	{
		List<LidarFileSpec> retL = new ArrayList<>();

		InputStream is = null;
		if (aBodyViewConfig.lidarBrowseFileListResourcePath.startsWith("/edu"))
		{
			String tmpName = aBodyViewConfig.lidarBrowseFileListResourcePath;
			is = LidarBrowseUtil.class.getResourceAsStream(tmpName);
		}
		else if (FileCache.isFileGettable(aBodyViewConfig.lidarBrowseFileListResourcePath))
		{
			try
			{
				File tmpFile = FileCache.getFileFromServer(aBodyViewConfig.lidarBrowseFileListResourcePath);
				is = new FileInputStream(tmpFile);
			}
			catch (UnauthorizedAccessException aExp)
			{
				aExp.printStackTrace();
				return retL;
			}
		}
		else
		{
			return retL;
		}

		try (BufferedReader aBR = new BufferedReader(new InputStreamReader(is)))
		{
			while (true)
			{
				// Bail at EOF
				String tmpLine = aBR.readLine();
				if (tmpLine == null)
					break;

				// Skip over empty lines / line comments
				String tmpStr = tmpLine.trim();
				if (tmpStr.isEmpty() == true || tmpStr.startsWith("#") == true)
					continue;

				String path = tmpLine;
				double timeBeg = Double.NaN;
				double timeEnd = Double.NaN;
				int numPoints = -1;

				int indexFirstSpace = tmpLine.indexOf(' ');
				if (indexFirstSpace != -1)
				{
					path = tmpLine.substring(0, indexFirstSpace);

					String timeRangeStr = tmpLine.substring(indexFirstSpace + 1);
					timeRangeStr = timeRangeStr.replace(" - ", " ");
					String strArr[] = timeRangeStr.split("[ ]+");
					try
					{
						if (strArr.length >= 2)
						{
							timeBeg = TimeUtil.str2etA(strArr[0].trim());
							timeEnd = TimeUtil.str2etA(strArr[1].trim());
						}
					}
					catch (Exception aExp)
					{
						try
						{
							if (strArr.length >= 2)
							{
								timeBeg = Double.parseDouble(strArr[0]);
								timeEnd = Double.parseDouble(strArr[1]);
							}
						}
						catch (Exception aExp2)
						{
							System.err.println("[LidarBrowseUtil] Bad ephemeris input. Raw string: " + timeRangeStr);
							aExp.printStackTrace();
						}
					}

					// Read number of points field
					if (strArr.length >= 3)
						numPoints = GuiUtil.readInt(strArr[2], -1);
				}
				String name = new File(path).getName();
				if (name.toLowerCase().endsWith(".gz"))
					name = FilenameUtils.getBaseName(name);

				LidarFileSpec tmpItem = new LidarFileSpec(path, name, numPoints, timeBeg, timeEnd);
				retL.add(tmpItem);
			}
		}

		return retL;
	}

	/**
	 * Loads the list of LidarFileSpecs from the specified catalog file.
	 *
	 * @param aRemotePathStr File path relative to the SBMT server
	 */
	public static List<LidarFileSpec> loadCatalog(String aRemotePathStr) throws IOException
	{
		List<LidarFileSpec> retL = new ArrayList<>();

		File tmpFile = FileCache.getFileFromServer(aRemotePathStr);
		try (BufferedReader aBR = new BufferedReader(new InputStreamReader(new FileInputStream(tmpFile))))
		{
			Pattern pattern = Pattern.compile("\\s+");
			while (true)
			{
				// Bail at EOF
				String tmpLine = aBR.readLine();
				if (tmpLine == null)
					break;

				// Skip over empty lines / line comments
				String tmpStr = tmpLine.trim();
				if (tmpStr.isEmpty() == true || tmpStr.startsWith("#") == true)
					continue;

				String[] strArr = pattern.split(tmpLine, -1);

				String path = strArr[0];
				String name = Paths.get(path).getFileName().toString();

				double timeBeg = Double.valueOf(strArr[1]);
				double timeEnd = Double.valueOf(strArr[2]);

				int numPoints = -1;
				if (strArr.length >= 4)
					numPoints = GuiUtil.readInt(strArr[3], -1);

				LidarFileSpec tmpItem = new LidarFileSpec(path, name, numPoints, timeBeg, timeEnd);
				retL.add(tmpItem);
			}
		}

		return retL;
	}

}
