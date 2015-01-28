/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * @author Lee Kamentsky
 *
 * Request information (channels and features) that
 * will be produced by this pipeline.
 */
public class PipelineInfoReq extends ZMsg {
	private static final String msgName = "pipeline-info-req-1";
	
	protected PipelineInfoReq(String sessionID, String pipeline) {
		add(msgName);
		add(pipeline);
		wrap(new ZFrame(sessionID));
	}
	
	/**
	 * Send a pipeline info request and receive the server's reply
	 * 
	 * @param socket send the request on this socket
	 * @param sessionID the session ID from connect
	 * @param pipeline the pipeline text for the query
	 * @return a completed PipelineInfoReply that has information
	 *         about the channels and measurements produced.
	 * @throws PipelineException if the pipeline could not be parsed
	 * @throws ProtocolException if there was an error parsing the server's response
	 */
	public static PipelineInfoReply send(Socket socket, String sessionID, String pipeline) 
			throws PipelineException, ProtocolException {
		new PipelineInfoReq(sessionID, pipeline).send(socket);
		return PipelineInfoReply.recvReply(socket);
	}

}
