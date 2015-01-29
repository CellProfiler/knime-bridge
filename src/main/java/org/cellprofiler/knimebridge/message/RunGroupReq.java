/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import java.util.Map;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZMQ.Socket;

import net.imagej.ImgPlus;

/**
 * @author Lee Kamentsky
 *
 */
@SuppressWarnings("deprecation")
public class RunGroupReq extends RunReq {
	private static final String msgName="run-group-request-1";
	protected RunGroupReq(String sessionID, String pipeline,
			Map<String, ImgPlus<?>> imageMap) {
		super(sessionID, pipeline, imageMap);
	}
	public String getMessageName() {
		return msgName;
	}
	/**
	 * Send a request to run a pipeline to the server,
	 * receiving a reply containing the computed
	 * measurements.
	 * 
	 * @param socket communicate over this socket
	 * @param sessionID the session ID from the connect request
	 * @param pipeline the pipeline to run
	 * @param imageMap a map of channel name to the image that
	 *                 should be used as input to CellProfiler
	 *                 for that channel.
	 * @return a RunReply containing the computed measurements
	 * @throws CellProfilerException if CellProfiler encountered a problem
	 *                 during the course of running the pipeline.
	 * @throws PipelineException if the pipeline could not be parsed
	 * @throws ProtocolException if there was a communication problem
	 *                 between the client and server.
	 */
	static public RunReply run(
			Socket socket, String sessionID, 
			String pipeline, Map<String, ImgPlus<?>> imageMap) throws CellProfilerException, PipelineException, ProtocolException {
		RunGroupReq req = new RunGroupReq(sessionID, pipeline, imageMap);
		req.send(socket);
		RunReply reply = new RunReply();
		reply.recv(socket);
		return reply;
	}
	
}
