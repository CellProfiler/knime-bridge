/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import java.util.UUID;

import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * A connect request asks the server to establish
 * a session.
 * 
 * @author Lee Kamentsky
 *
 */
public class ConnectReq extends ZMsg {
	private static final String msgName = "connect-request-1";
	private final String sessionID = UUID.randomUUID().toString();
	protected ConnectReq() {
		add(msgName);
		wrap(new ZFrame(sessionID));
	}
	/**
	 * Connect to CellProfiler
	 * 
	 * @param socket the socket to use to send and receive messages
	 * 
	 * @return the session ID that should be used to communicate
	 * @throws ProtocolException if the server's response could not be parsed
	 */
	static public String connect(Socket socket) throws ProtocolException {
		ConnectReq req = new ConnectReq();
		if (! req.send(socket)) {
			throw new ProtocolException("Failed to send connect request");
		}
		ConnectReply.recvConnectReply(socket);
		return req.sessionID;
	}
}
