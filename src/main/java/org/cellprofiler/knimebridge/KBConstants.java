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

}
