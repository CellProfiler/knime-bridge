/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

/**
 * Malformed pipeline exception
 * 
 * @author Lee Kamentsky
 *
 */
public class PipelineException extends Exception {

	/**
	 * @param msg error message describing why pipeline could not be loaded or is misconfigured.
	 */
	public PipelineException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3496130560380740826L;

}
