/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.imagej;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import org.cellprofiler.knimebridge.IKnimeBridge;
import org.cellprofiler.knimebridge.KnimeBridgeFactory;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.scijava.module.Module;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;
import org.scijava.service.AbstractService;

/**
 * @author Lee Kamentsky
 * 
 * The CellProfiler service has lots of fun stuff
 * that will talk to CellProfiler over the
 * Knime bridge.
 *
 */
@Plugin(type=Service.class)
public class DefaultCellProfilerService extends AbstractService implements CellProfilerService {
	@Override
	public IKnimeBridge connectToBridge(URI uri) throws ProtocolException {
		IKnimeBridge bridge = new KnimeBridgeFactory().newKnimeBridge();
		bridge.connect(uri);
		return bridge;
	}

	@Override
	public Module createPipelineModule(IKnimeBridge bridge, File pipeline) throws IOException, PipelineException, ProtocolException {
    	FileReader rdr = new FileReader(pipeline);
    	try {
	    	StringBuilder sb = new StringBuilder();
	    	char [] buffer = new char [1000];
	    	while (true) {
	    		final int nRead = rdr.read(buffer);
	    		if (nRead < 0) break;
	    		sb.append(buffer, 0, nRead);
	    	}
	    	return createPipelineModule(bridge, sb.toString());
    	} finally {
    		rdr.close();
    	}
	}

	@Override
	public Module createPipelineModule(IKnimeBridge bridge, String pipeline)
			throws PipelineException, ProtocolException {
		return PipelineModule.newInstance(bridge, pipeline);
	}
	
}
