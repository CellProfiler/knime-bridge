package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.PipelineException;
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
