package com.androidApp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.AssetManager;

/**
 * utilities to read files and raw resources.  We can't read assets, because we're a library.
 * @author Matthew
 * Copyright (c) 2013 Visible Automation LLC.  All Rights Reserved.
 */
public class FileUtils {
	
	// return the number of lines in the file
	public static int numLines(InputStream is) throws IOException {
		int lineCount = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		do {
			line = br.readLine();
			if (line != null) {
				lineCount++;
			}
		} while (line != null);
		return lineCount;
	}

	// given the number of lines in the file, read it into an array of strings.
	public static String[] readLines(InputStream is, int numLines) throws IOException {
		String[] lines = new String[numLines];
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		for (int i = 0; i < numLines; i++) {
			lines[i] = br.readLine();
		} 
		return lines;
	}
	
	/**
	 * function to read an asset file into an array of strings
	 * @param context context to access the asset
	 * @param asset assetname
	 * @return array of strings
	 * @throws IOException
	 */
	public static String[] readRawResource(Context context, int resourceId) throws IOException {
		InputStream is = context.getResources().openRawResource(resourceId);
		int nLines = FileUtils.numLines(is);
		is.close();
		is = context.getResources().openRawResource(resourceId);
		String[] lines = FileUtils.readLines(is, nLines);
		is.close();
		return lines;
	}
	
	/**
	 * because we have to include the recorder as a jar file, we can't use assets and resources
	 * @param cls class to get resource names
	 * @param resourceName resource name
	 * @return Array of Strings
	 * @throws IOException
	 */
	public static String[] readJarResource(Class cls, String resourceName) throws IOException {
		InputStream is = cls.getResourceAsStream(resourceName);
		int nLines = FileUtils.numLines(is);
		is.close();
		is = cls.getResourceAsStream(resourceName);
		String[] lines = FileUtils.readLines(is, nLines);
		is.close();
		return lines;
	}
	
	/**
	 * copy a file from inPath to outPath
	 * @param inPath
	 * @param outPath
	 * @throws IOException
	 */
	public static void copyFile(String inPath, String outPath) throws IOException {
		OutputStream os = new FileOutputStream(outPath);
		InputStream is = new FileInputStream(inPath);
		byte[] buffer = new byte[1024];
		do {
			int count = is.read(buffer);
			if (count > 0) {
				os.write(buffer, 0, count);
			}
			if (count < 1024) {
				break;
			}
		} while (true);
	}
	
	/**
	 * copy a list of files from one directory to the other
	 * @param srcDir source directoru
	 * @param dstDir destination directory
	 * @param fileList list of files
	 * @throws IOException
	 */
	public static void copyFileList(File srcDir, File dstDir, String[] fileList) throws IOException {
		for (int iFile = 0; iFile < fileList.length; iFile++) {
			String inPath = srcDir.getAbsolutePath() + "/" + fileList[iFile];
			String outPath = dstDir + File.separator + fileList[iFile];
			FileUtils.copyFile(inPath, outPath);
		}

	}
}
