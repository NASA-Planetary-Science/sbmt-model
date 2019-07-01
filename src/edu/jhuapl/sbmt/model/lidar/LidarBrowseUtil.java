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
import java.util.Scanner;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileCache.UnauthorizedAccessException;
import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.util.TimeUtil;

/**
 * Utility class that provides various methods for working with lidar browse
 * data.
 * <P>
 * The methods in this class originated from the class
 * edu.jhuapl.sbmt.model.lidar.LidarDataPerUnit and the class
 * edu.jhuapl.smbt.model.lidar.LidarBrowseDataCollection (prior to 2019May24).
 * The naming of those classes are not intuitive. Those classes also violated
 * the separation of principles. That class has since been refactored - part of
 * which is here.
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
			is = LidarBrowseUtil.class.getResourceAsStream(aBodyViewConfig.lidarBrowseFileListResourcePath);
		}
		else if (FileCache.isFileGettable(aBodyViewConfig.lidarBrowseFileListResourcePath))
		{
			try
			{
				is = new FileInputStream(FileCache.getFileFromServer(aBodyViewConfig.lidarBrowseFileListResourcePath));
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
			String aLine;
			while ((aLine = aBR.readLine()) != null)
			{
				String path = aLine;
				double timeBeg = Double.NaN;
				double timeEnd = Double.NaN;
				int indexFirstSpace = aLine.indexOf(' ');
				if (indexFirstSpace != -1)
				{
					path = aLine.substring(0, indexFirstSpace);

					String timeRangeStr = aLine.substring(indexFirstSpace + 1);
					timeRangeStr = timeRangeStr.replace(" - ", " ");
					String strArr[] = timeRangeStr.split("[ ]+");
					try
					{
						if (strArr.length == 2)
						{
							timeBeg = TimeUtil.str2etA(strArr[0].trim());
							timeEnd = TimeUtil.str2etA(strArr[1].trim());
						}
					}
					catch (Exception aExp)
					{
						try
						{
							if (strArr.length == 2)
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
				}
				String name = new File(path).getName();
				if (name.toLowerCase().endsWith(".gz"))
					name = name.substring(0, name.length() - 3);

				LidarFileSpec lidarSpec = new LidarFileSpec(path, name, timeBeg, timeEnd);
				retL.add(lidarSpec);
			}
		}

		return retL;
	}

	/**
	 * Loads the list of LidarFileSpecs from the specified input file.
	 */
	public static List<LidarFileSpec> loadLidarFileSpecList(String aBrowseFileStr) throws IOException
	{
		List<LidarFileSpec> retL = new ArrayList<>();

		File listFile = FileCache.getFileFromServer(aBrowseFileStr);
		try (Scanner scanner = new Scanner(new FileInputStream(listFile)))
		{
			while (scanner.hasNext())
			{
				String filename = scanner.next();
				double begTime = Double.valueOf(scanner.next());
				double endTime = Double.valueOf(scanner.next());
				String path = filename;
				String name = Paths.get(filename).getFileName().toString();

				LidarFileSpec tmpSpec = new LidarFileSpec(path, name, begTime, endTime);
				retL.add(tmpSpec);
			}
		}

		return retL;
	}

}
