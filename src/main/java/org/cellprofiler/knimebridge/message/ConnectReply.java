/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * A reply to the connect request
 * 
 * @author Lee Kamentsky
 *
 */
public class ConnectReply extends AbstractReply {
	static final private String msgName = "connect-reply-1";
	protected ConnectReply() {
	}
	/**
	 * Receive a connect reply on the socket
	 * 
	 * @param socket
	 * @return
	 * @throws ProtocolException
	 */
	static ConnectReply recvConnectReply(Socket socket) throws ProtocolException {
		final ConnectReply reply = new ConnectReply();
		reply.recvNoException(socket);
		return reply;
	}
	@Override
	protected String getMsgName() {
		return msgName;
	}
	@Override
	protected void parse(ZMsg msg) {
		
	}
}
