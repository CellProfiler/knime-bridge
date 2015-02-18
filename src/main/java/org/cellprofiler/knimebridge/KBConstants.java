/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

/**
 * @author Lee Kamentsky
 *
 * Contstants used throughout the Knime bridge
 */
public class KBConstants {
	/**
	 * The "object name" that should be used
	 * to retrieve per-image features.
	 */
	public final static String IMAGE = "Image"; 
	/*
	 * The index of the image table, which should be in every
	 * image table.
	 */
	public final static String IMAGE_NUMBER = "ImageNumber";
	/*
	 * The object-level measurement whose length is the number
	 * of objects in the segmentation and whose elements give the indexes
	 * of the objects in the segmentation.
	 */
	public final static String OBJECT_NUMBER = "ObjectNumber";
	
	/**
	 * When cleaning a pipeline, remove the ExportToDatabase module
	 */
	public final static int REMOVE_EXPORT_TO_DATABASE = 1;
	
	/**
	 * When cleaning a pipeline, remove the ExportToSpreadsheet module
	 */
	public final static int REMOVE_EXPORT_TO_SPREADSHEET = 2;
	/**
	 * When cleaning a pipeline, remove all measurement export modules
	 */
	public final static int REMOVE_EXPORT_MODULES = 
			REMOVE_EXPORT_TO_DATABASE + REMOVE_EXPORT_TO_SPREADSHEET;
	/**
	 * When cleaning a pipeline, remove the SaveImages module
	 */
	public final static int REMOVE_SAVE_IMAGES = 4;
	
	/**
	 * When cleaning a pipeline, remove all modules that are not appropriate
	 * for execution via the bridge.
	 */
	public final static int REMOVE_ALL = -1;
	
	/**
	 * The name of the ExportToDatabase CellProfiler module 
	 */
	public final static String EXPORT_TO_DATABASE = "ExportToDatabase";
	
	/**
	 * The name of the ExportToSpreadsheet CellProfiler module 
	 */
	public final static String EXPORT_TO_SPREADSHEET = "ExportToSpreadsheet";
	
	/**
	 * The name of the SaveImages module
	 */
	public final static String SAVE_IMAGES = "SaveImages";

}
