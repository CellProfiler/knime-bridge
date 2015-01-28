/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * @author Lee Kamentsky
 *
 * The base for replies received from the server.
 * The basic protocol for a reply is a "wrapper"
 * composed of a session ID frame and a blank frame
 * followed by the message type.
 */
public abstract class AbstractReply {
	protected abstract String getMsgName();
	protected abstract void parse(ZMsg msg) throws ProtocolException, PipelineException, CellProfilerException;
	private static final String CPEXCEPTION_MSG_NAME = "cellprofiler-exception-1";
	private static final String PIPELINE_EXCEPTION_MSG_NAME = "pipeline-exception-1";
	private String sessionID;
	
	/**
	 * @return the session ID of the reply.
	 */
	public String getSessionID() {
		return sessionID;
	}
	/**
	 * Receive a reply
	 * 
	 * @param socket get the reply from this socket
	 * 
	 * @throws CellProfilerException if CellProfiler encountered an error while running a pipeline
	 * @throws PipelineException if the pipeline could not be parsed
	 * @throws ProtocolException if the client could not parse the server's response
	 */
	public void recv(Socket socket) throws CellProfilerException, PipelineException, ProtocolException {
		ZMsg msg = ZMsg.recvMsg(socket);
		sessionID = msg.unwrap().toString();
		final String msgName = msg.popString();
		if (msgName == null) {
			throw new ProtocolException("Missing message name");
		}
		if (! msgName.equals(getMsgName())) {
			if (msgName.equals(CPEXCEPTION_MSG_NAME)) {
				throw new CellProfilerException(msg.popString());
			} else if (msgName.equals(PIPELINE_EXCEPTION_MSG_NAME)) {
				throw new PipelineException(msg.popString());
			}
		}
		parse(msg);
	}
	/**
	 * Receive a message, treating CellProfiler exceptions as protocol exceptions
	 * for messages that do not expect CP exceptions
	 * 
	 * @param socket get the reply from this socket
	 * @throws PipelineException if the pipeline could not be parsed
	 * @throws ProtocolException if the client could not understand the server
	 */
	public void recvNoCPException(Socket socket) throws PipelineException, ProtocolException {
		try {
			recv(socket);
		} catch (CellProfilerException e) {
			throw new ProtocolException("Unexpected cellprofiler exception");
		}
	}
	
	/**
	 * Receive a message, expecting no server-side exceptions
	 * 
	 * @param socket get the reply from this socket
	 * @throws ProtocolException if the client could not understand the server
	 */
	public void recvNoException(Socket socket) throws ProtocolException {
		try {
			recvNoCPException(socket);
		} catch (PipelineException e) {
			throw new ProtocolException("Unexpected pipeline exception");
		}
	}

}
