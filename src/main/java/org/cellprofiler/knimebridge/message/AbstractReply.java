package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

public abstract class AbstractReply {
	protected abstract String getMsgName();
	protected abstract void parse(ZMsg msg) throws ProtocolException, PipelineException, CellProfilerException;
	private static final String CPEXCEPTION_MSG_NAME = "cellprofiler-exception-1";
	private static final String PIPELINE_EXCEPTION_MSG_NAME = "pipeline-exception-1";
	private String sessionID;
	
	public String getSessionID() {
		return sessionID;
	}
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
	public void recvNoCPException(Socket socket) throws PipelineException, ProtocolException {
		try {
			recv(socket);
		} catch (CellProfilerException e) {
			throw new ProtocolException("Unexpected cellprofiler exception");
		}
	}
	
	public void recvNoException(Socket socket) throws ProtocolException {
		try {
			recvNoCPException(socket);
		} catch (PipelineException e) {
			throw new ProtocolException("Unexpected pipeline exception");
		}
	}

}
