/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

/**
 * An exception reflected from CellProfiler, for instance
 * as caused by an internal error or misconfiguration.
 * 
 * @author Lee Kamentsky
 *
 */
public class CellProfilerException extends Exception {

	public CellProfilerException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1357767082431284712L;

}
