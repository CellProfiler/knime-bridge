/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

/**
 * Describes a feature - its name and type
 * 
 * @author Lee Kamentsky
 *
 */
public interface IFeatureDescription {
	/**
	 * @return the name of the segmentation or null if an image measurement
	 */
	public String getObjectName();
	/**
	 * @return the feature name
	 */
	public String getName();
	
	/**
	 * @return the class of the datatype to be returned
	 */
	public Class<?> getType();
}
