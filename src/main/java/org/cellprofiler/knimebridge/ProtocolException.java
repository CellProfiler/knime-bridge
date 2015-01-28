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
 * This exception is thrown when the Knime bridge
 * client fails to parse a message from the
 * CellProfiler worker.
 */
public class ProtocolException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ProtocolException() {
		
	}
	
	public ProtocolException(String message) {
		super(message);
	}

}
