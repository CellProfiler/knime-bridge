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
	static public String connect(Socket socket) throws ProtocolException {
		ConnectReq req = new ConnectReq();
		if (! req.send(socket)) {
			throw new ProtocolException("Failed to send connect request");
		}
		ConnectReply.recvConnectReply(socket);
		return req.sessionID;
	}
}
