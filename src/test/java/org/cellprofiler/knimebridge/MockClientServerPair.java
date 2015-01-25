package org.cellprofiler.knimebridge;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;

import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

public class MockClientServerPair {
	public interface RunWithSockets {
		public void run(Socket socket);
	}
	public interface RunWithBridge {
		public void run(IKnimeBridge bridge);
	}
	private final boolean [] die = { false };
	private final IKnimeBridge bridge = new KnimeBridgeFactory().newKnimeBridge();
	private final Socket serverSocket;
	private final SynchronousQueue<Runnable> serverQueue = new SynchronousQueue<Runnable>();
	private final SynchronousQueue<Runnable> clientQueue = new SynchronousQueue<Runnable>();
	private final Thread serverThread;
	private final Thread clientThread;
	private final String [] addr = { null };
	public String error = null;
	public String sessionID;
	public MockClientServerPair() {
		final Context context = KnimeBridgeImpl.theContext();
		final SynchronousQueue<Integer> portQueue = new SynchronousQueue<Integer>();
		final SynchronousQueue<Socket> socketQueue = new SynchronousQueue<Socket>();
		serverThread = new Thread(
				new Runnable() {

					@Override
					public void run() {
						Socket socket = context.socket(ZMQ.REP);
						int port = socket.bindToRandomPort("tcp://127.0.0.1");
						try {
							portQueue.put(port);
							socketQueue.put(socket);
						} catch (InterruptedException e) {
							error = e.getMessage();
							return;
						}
						while (! die[0]) {
							try {
								serverQueue.take().run();
							} catch (InterruptedException e) {
							}
						}
						socket.close();
					}
					
				});
		clientThread = new Thread(
				new Runnable() {

					@Override
					public void run() {
						while (! die[0]) {
							try {
								clientQueue.take().run();
							} catch (InterruptedException e) {
								
							}
						}
						bridge.disconnect();
					}
				});
		serverThread.setName("ServerThread");
		clientThread.setName("Client thread");
		serverThread.start();
		int port=0;
		Socket temp = null;
		try {
			port = portQueue.take().intValue();
			temp = socketQueue.take();
		} catch (InterruptedException e) {
			error = e.getMessage();
		}
		serverSocket = temp;
		if (temp == null) return;
		addr[0] = String.format("tcp://127.0.0.1:%d", port);
		clientThread.start();
		try {
			Future<Object> connectReply = runOnServer(new RunWithSockets() {
	
				@Override
				public void run(Socket socket) {
					ZMsg msg = ZMsg.recvMsg(socket);
					ZFrame client = msg.unwrap();
					assert msg.popString().equals("connect-req-1");
					msg.destroy();
					ZMsg msgOut = new ZMsg();
					msgOut.add(client);
					msgOut.add("connect-reply-1");
					msgOut.send(socket);
				}});
			Future<Object> connectReq = runOnClient(new RunWithBridge() {
				
				@Override
				public void run(IKnimeBridge bridge) {
					try {
						bridge.connect(new URI(addr[0]));
					} catch (ZMQException e) {
						error = String.format("ZMQ exception on connect: %s", e.getMessage());
						e.printStackTrace();
					} catch (ProtocolException e) {
						error = String.format("ProtocolException exception on connect: %s", e.getMessage());
						e.printStackTrace();
					} catch (URISyntaxException e) {
						error = String.format("URI syntax exception on connect: %s", e.getMessage());

						e.printStackTrace();
					}
				}
			});
			connectReply.get();
			connectReq.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			error = e.getMessage();
		} catch (ExecutionException e) {
			e.printStackTrace();
			error = e.getMessage();
		}
 	}
	Future<Object> runOnServer(final RunWithSockets rws) {
		FutureTask<Object> future = new FutureTask<Object>(new Runnable() {
			public void run() {
				rws.run(serverSocket);
			}
		}, new Object());
		try {
			serverQueue.put(future);
		} catch (InterruptedException e) {
			throw new AssertionError("Unexpected interrupt");
		}
		return future;
	}
	Future<Object> runOnClient(final RunWithBridge rwb) {
		FutureTask<Object> future = new FutureTask<Object>(new Runnable() {
			public void run() {
				rwb.run(bridge);
			}
		}, new Object());
		try {
			clientQueue.put(future);
		} catch (InterruptedException e) {
			throw new AssertionError("Unexpected interrupt");
		}
		return future;		
	}
	void stop() {
		die[0] = true;
		try {
			serverThread.interrupt();
			clientThread.interrupt();
			serverThread.join();
			clientThread.join();
		} catch (InterruptedException e) {
			
		}
	}
}
