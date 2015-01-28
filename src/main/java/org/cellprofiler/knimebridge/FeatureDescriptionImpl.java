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
 * Implementation of a feature description.
 *
 */
public class FeatureDescriptionImpl implements IFeatureDescription {
	final String objectName;
	final String name;
	final Class<?> type;
	
	/**
	 * Initialize a feature description using the name of 
	 * the segmentation that produces it (or "Images" for
	 * image-wide features, the CellProfiler feature name
	 * and the data type.
	 * 
	 * @param objectName
	 * @param name
	 * @param type
	 */
	public FeatureDescriptionImpl(String objectName, String name, Class<?> type) {
		this.objectName = objectName;
		this.name = name;
		this.type = type;
	}
	@Override
	public String getObjectName() {
		return objectName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getType() {
		return type;
	}
}
