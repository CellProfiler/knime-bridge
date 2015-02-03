/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonWriter;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.numeric.real.DoubleType;

import org.cellprofiler.knimebridge.MockClientServerPair.RunWithBridge;
import org.cellprofiler.knimebridge.MockClientServerPair.RunWithSockets;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

@SuppressWarnings("deprecation")
public class TestKnimeBridge {
	@Test
	public void testConnectAndDisconnect() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		mock.stop();
	}

	@Test
	public void testLoadPipeline() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		final String pipeline = "Not a pipeline";
		final String [] channels = { "Foo", "Bar" };
		Future<Object> client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.loadPipeline(pipeline);
				} catch (ZMQException e) {
					
					e.printStackTrace();
					Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
					Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
					Assert.fail();
				};
				
			}
		});
		Future<Object> server = handlePipelineReq(mock, pipeline, channels);
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
		mock.stop();
	}

	/**
	 * @param mock
	 * @param pipeline
	 * @param channels
	 */
	private Future<Object> handlePipelineReq(MockClientServerPair mock,
			final String pipeline, final String[] channels) {
		return mock.runOnServer(new RunWithSockets() {

			@Override
			public void run(Socket socket) {
				ZMsg msg = ZMsg.recvMsg(socket);
				ZFrame client = msg.unwrap();
				String sessionID = msg.popString();
				assertNotNull(sessionID);
				String recievedPipeline = msg.popString();
				assertEquals(pipeline, recievedPipeline);
				ZMsg msgOut = new ZMsg();
				msgOut.add(sessionID);
				StringWriter sw = new StringWriter();
				JsonWriter writer = Json.createWriter(sw);
				writer.writeArray(Json.createArrayBuilder()
						.add(Json.createArrayBuilder().add(channels[0]).add(channels[1]).build())
						.add(Json.createArrayBuilder()
								.add("java.lang.Integer")
								.add("java.lang.Float")
								.add("java.lang.Double")
								.add("java.lang.String").build())
						.add(Json.createObjectBuilder()
								.add(KBConstants.IMAGE, Json.createArrayBuilder()
										.add(Json.createArrayBuilder().add("ImageNumber").add(0).build())
										.add(Json.createArrayBuilder().add("FileName_DNA").add(3).build())
										.build())
								.add("Nucleus", Json.createArrayBuilder()
										.add(Json.createArrayBuilder().add("ObjectNumber").add(0).build())
										.add(Json.createArrayBuilder().add("Location_CenterX").add(2).build())
										.build())
								.build())
						.build());
				writer.close();
				msgOut.add(sw.toString());
				msgOut.wrap(client);
				msgOut.send(socket);
			}});
	}

	@Test
	public void testGetInputChannels() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		final String pipeline = "Not a pipeline";
		final String [] channels = { "Foo", "Bar" };
		Future<Object> client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.loadPipeline(pipeline);
					Set<String> channelsOut = new HashSet<String>(bridge.getInputChannels());
					for (String channel:channels) {
						assertTrue(channelsOut.contains(channel));
					}
				} catch (ZMQException e) {
					
					e.printStackTrace();
					Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
					Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
					Assert.fail();
				};
				
			}
		});
		Future<Object> server = handlePipelineReq(mock, pipeline, channels);
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
		mock.stop();
	}

	@Test
	public void testGetObjectNames() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		final String pipeline = "Not a pipeline";
		final String [] channels = { "Foo", "Bar" };
		Future<Object> client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.loadPipeline(pipeline);
					List<String> objects = bridge.getObjectNames();
					assertEquals(objects.size(), 2);
					assertEquals(objects.get(0), "Nucleus");
					assertEquals(objects.get(1), "Image");
				} catch (ZMQException e) {
					
					e.printStackTrace();
					Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
					Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
					Assert.fail();
				};
				
			}
		});
		Future<Object> server = handlePipelineReq(mock, pipeline, channels);
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
		mock.stop();
	}

	@Test
	public void testGetFeatures() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		final String pipeline = "Not a pipeline";
		final String [] channels = { "Foo", "Bar" };
		Future<Object> client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.loadPipeline(pipeline);
					List<IFeatureDescription> features = bridge.getFeatures(null);
					assertEquals(features.size(), 2);
					for (IFeatureDescription feature:features) {
						assertEquals(feature.getObjectName(), KBConstants.IMAGE);
						if (feature.getName().equals("ImageNumber")) {
							assertEquals(feature.getType(), Integer.class);
						} else if (feature.getName().equals("FileName_DNA")) {
							assertEquals(feature.getType(), String.class);
						} else {
						Assert.fail(String.format("Unexpected feature, %s", feature.getName()));
						}
					}
					features = bridge.getFeatures("Nucleus");
					assertEquals(features.size(), 2);
					for (IFeatureDescription feature:features) {
						assertEquals(feature.getObjectName(), "Nucleus");
						if (feature.getName().equals("ObjectNumber")) {
							assertEquals(feature.getType(), Integer.class);
						} else if (feature.getName().equals("Location_CenterX")) {
							assertEquals(feature.getType(), Double.class);
						} else {
						Assert.fail(String.format("Unexpected feature, %s", feature.getName()));
						}
					}
				} catch (ZMQException e) {
					
					e.printStackTrace();
				Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
				Assert.fail();
				};
				
			}
		});
		Future<Object> server = handlePipelineReq(mock, pipeline, channels);
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
		Assert.fail();
		}
		mock.stop();

	}
	private ImgPlus<DoubleType> makeImgPlus(long [] dims, String name, AxisType [] axes) {
		// use a random number generator
		final Random rnd = new Random( 1241234 );
		
		Img<DoubleType>img = new PlanarImgFactory<DoubleType>().create(dims, new DoubleType());
		

		// create reference array
		final double[] reference = new double[ ( int ) img.size() ];

		// iterate over image and reference array and fill with data
		final Cursor< DoubleType > cursor = img.cursor();
		int i = 0;

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final double value = rnd.nextDouble();
			reference[ i++ ] = value;
			cursor.get().set( value );
		}

		return new ImgPlus<DoubleType>(img, name, axes);
	}

	@Test
	public void testRun() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		final String pipeline = "Not a pipeline";
		final String [] channels = { "Foo", "Bar" };
		Future<Object> client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.loadPipeline(pipeline);
				} catch (ZMQException e) {
					
					e.printStackTrace();
				Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
				Assert.fail();
				};
				
			}
		});
		Future<Object> server = handlePipelineReq(mock, pipeline, channels);
		AxisType [] axes = new AxisType[] { Axes.X, Axes.Y };
		ImgPlus<DoubleType> foo = makeImgPlus(new long[] {20, 37}, "Foo", axes);
		ImgPlus<DoubleType> bar = makeImgPlus(new long[] {31, 18}, "Bar", axes);
		final Map<String, ImgPlus<?>> map = new Hashtable<String, ImgPlus<?>>();
		map.put("Foo", foo);
		map.put("Bar", bar);
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
		Assert.fail();
		}
		
		final String stringMeasurement = "I'm hacking in Konstanz on Jan 25, 2015 at 5:54 in the evening";
		final byte [] stringBuffer = stringMeasurement.getBytes();
		final double [] doubleValues = { 
				0.02818247,  0.93481654,  0.73093317,  0.15134177,  0.309717  ,
		        0.92397538,  0.48923451,  0.07391248,  0.39880301,  0.21000923 };
		final byte [] doubleBuffer = {
				32, -121,  -44,   62,  -35,  -37, -100,   63,  -55,   59,   67,
		         94,    4,  -22,  -19,   63,   65, -123,   71,  -10,  -51,   99,
		        -25,   63,  -24,  -47,  -31,  -52,   42,   95,  -61,   63,  -28,
		        117,  -77,   62,  103,  -46,  -45,   63,  -92,  -24,    7,  -49,
		         52, -111,  -19,   63,  -18,   -3,   61,   69,  -98,   79,  -33,
		         63,  -16,   27,  -85, -107,  -19,  -21,  -78,   63,   88,  -15,
		         10,   10,   -3, -123,  -39,   63, -112,  -39,  100,   26, -107,
		        -31,  -54,   63
		};
		final float [] floatValues = {
				0.3150188F ,  0.10941596F,  0.50460899F,  0.19905493F,  0.85943407F
		};
		final byte [] floatBuffer = {
				37,  74, -95,  62, 121,  21, -32,  61,  14,  46,   1,  63,  14,
			    -43,  75,  62, -33,   3,  92,  63				
		};
		final int [] intValues = { 426783998,   132743707, -2014287369,  1332426665,   -32671763,
			       -1878354646 };
		final byte [] intBuffer = {
				-2,   52,  112,   25,   27, -126,  -23,    7,   -9,  105,  -16,
				-121,  -87,   55,  107,   79,  -19,  119,   13,   -2,   42, -107,
				10, -112 };
		
		client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.run(map);
					double [] doubles = bridge.getDoubleMeasurements(
							new FeatureDescriptionImpl("Nuclei", "X", Double.class));
					assertEquals(3, doubles.length);
					assertEquals(doubleValues[3], doubles[0], .0001);
					assertEquals(doubleValues[4], doubles[1], .0001);
					assertEquals(doubleValues[5], doubles[2], .0001);
					float [] floats = bridge.getFloatMeasurements(
							new FeatureDescriptionImpl("Cytoplasm", "Y", Float.class));
					assertEquals(floatValues[3], floats[0], .0001);
					assertEquals(floatValues[4], floats[1], .0001);
					int [] ints = bridge.getIntMeasurements(
							new FeatureDescriptionImpl(KBConstants.IMAGE, "ImageNumber", Integer.class));
					assertEquals(intValues[5], ints[0]);
					ints = bridge.getIntMeasurements(
							new FeatureDescriptionImpl("Nuclei", "ObjectNumber", Integer.class));
					assertEquals(intValues[0], ints[0]);
					assertEquals(intValues[1], ints[1]);
					assertEquals(intValues[2], ints[2]);
					String s = bridge.getStringMeasurement(
							new FeatureDescriptionImpl(KBConstants.IMAGE, "HackathonComment", String.class));
					assertEquals(stringMeasurement, s);
					
				} catch (ZMQException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (CellProfilerException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
				Assert.fail();
				}
			}
		});
		server = mock.runOnServer(new RunWithSockets() {
			
			@Override
			public void run(Socket socket) {
				ZMsg msg = ZMsg.recvMsg(socket);
				ZFrame client = msg.unwrap();
				String sessionID = msg.popString();
				assertNotNull(sessionID);
				String recievedPipeline = msg.popString();
				assertEquals(pipeline, recievedPipeline);
				String imgMetadata = msg.popString();
				byte [] data0 = msg.pop().getData();
				byte [] data1 = msg.pop().getData();
				JsonArray images = Json.createReader(new StringReader(imgMetadata)).readArray();
				assertEquals(2, images.size());
				JsonArray image = images.getJsonArray(0);
				if (image.getString(0) == "Foo") {
					assertEquals("Bar", images.getJsonArray(1).getString(0));
					assertEquals(data1.length, 31*18*Double.SIZE / Byte.SIZE);
					assertEquals(data0.length, 20*37*Double.SIZE / Byte.SIZE);
				} else {
					assertEquals(image.getString(0), "Bar");
					image = images.getJsonArray(1);
					assertEquals("Foo", image.getString(0));
					assertEquals(data0.length, 31*18*Double.SIZE / Byte.SIZE);
					assertEquals(data1.length, 20*37*Double.SIZE / Byte.SIZE);
				}
				JsonArray axes = image.getJsonArray(1);
				assertEquals(axes.size(), 2);
				JsonArray axis = axes.getJsonArray(0);
				JsonArray axisOther = axes.getJsonArray(1);
				String axisName = axis.getString(0);
				assertEquals(axisName, Axes.X.getLabel());
				assertEquals(axis.getInt(1), 20);
				assertEquals(axis.getInt(2), 37);
				assertEquals(Axes.Y.getLabel(), axisOther.getString(0));
				assertEquals(37, axisOther.getInt(1));
				assertEquals(1, axisOther.getInt(2));
				ZMsg msgOut = new ZMsg();
				msgOut.add("run-reply-1");
				msgOut.add(createMetadata().toString());
				byte [] buf = new byte [10*Double.SIZE / Byte.SIZE + 5*Float.SIZE / Byte.SIZE+ 6 * Integer.SIZE / Byte.SIZE +
				                         stringBuffer.length];
				System.arraycopy(doubleBuffer, 0, buf, 0, doubleBuffer.length);
				int off = doubleBuffer.length;
				System.arraycopy(floatBuffer, 0, buf, off, floatBuffer.length);
				off += floatBuffer.length;
				System.arraycopy(intBuffer, 0, buf, off, intBuffer.length);
				off += intBuffer.length;
				System.arraycopy(stringBuffer, 0, buf, off, stringBuffer.length);
				msgOut.add(buf);
				msgOut.wrap(client);
				msgOut.send(socket);
			}
			
			private JsonArray createMetadata() {
				return Json.createArrayBuilder()
					// Double features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
							.add("Nuclei")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Area").add(3).build())
								.add(Json.createArrayBuilder().add("X").add(3).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add("Cytoplasm")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Area").add(2).build())
								.add(Json.createArrayBuilder().add("X").add(2).build())
								.build())
							.build())
						.build())
					
					// Float features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
							.add("Nuclei")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Y").add(3).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add("Cytoplasm")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Y").add(2).build())
							    .build())
							.build())
						.build())
					// Int features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
							.add("Nuclei")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("ObjectNumber").add(3).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add("Cytoplasm")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("ObjectNumber").add(2).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add(KBConstants.IMAGE)
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("ImageNumber").add(1).build())
								.build())
							.build())
						.build())
					// String features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
						    .add(KBConstants.IMAGE)
						    .add(Json.createArrayBuilder()
						    	.add(Json.createArrayBuilder()
						    		.add("HackathonComment")
						    		.add(stringMeasurement.getBytes().length)
						    		.build())
						    	.build())
							.build())
						.build())
					.build();
			}
		});
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
		Assert.fail();
		}
		mock.stop();
	}

	@Test
	public void testRunGroup() {
		MockClientServerPair mock = new MockClientServerPair();
		assertNull(mock.error);
		final String pipeline = "Not a pipeline";
		final String [] channels = { "Foo", "Bar" };
		Future<Object> client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.loadPipeline(pipeline);
				} catch (ZMQException e) {
					
					e.printStackTrace();
				Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
				Assert.fail();
				};
				
			}
		});
		Future<Object> server = handlePipelineReq(mock, pipeline, channels);
		AxisType [] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		ImgPlus<DoubleType> foo = makeImgPlus(new long[] {20, 37, 10}, "Foo", axes);
		ImgPlus<DoubleType> bar = makeImgPlus(new long[] {31, 18, 14}, "Bar", axes);
		final Map<String, ImgPlus<?>> map = new Hashtable<String, ImgPlus<?>>();
		map.put("Foo", foo);
		map.put("Bar", bar);
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
		Assert.fail();
		}
		
		final String stringMeasurement = "I'm hacking in Konstanz on Jan 25, 2015 at 5:54 in the evening";
		final byte [] stringBuffer = stringMeasurement.getBytes();
		final double [] doubleValues = { 
				0.02818247,  0.93481654,  0.73093317,  0.15134177,  0.309717  ,
		        0.92397538,  0.48923451,  0.07391248,  0.39880301,  0.21000923 };
		final byte [] doubleBuffer = {
				32, -121,  -44,   62,  -35,  -37, -100,   63,  -55,   59,   67,
		         94,    4,  -22,  -19,   63,   65, -123,   71,  -10,  -51,   99,
		        -25,   63,  -24,  -47,  -31,  -52,   42,   95,  -61,   63,  -28,
		        117,  -77,   62,  103,  -46,  -45,   63,  -92,  -24,    7,  -49,
		         52, -111,  -19,   63,  -18,   -3,   61,   69,  -98,   79,  -33,
		         63,  -16,   27,  -85, -107,  -19,  -21,  -78,   63,   88,  -15,
		         10,   10,   -3, -123,  -39,   63, -112,  -39,  100,   26, -107,
		        -31,  -54,   63
		};
		final float [] floatValues = {
				0.3150188F ,  0.10941596F,  0.50460899F,  0.19905493F,  0.85943407F
		};
		final byte [] floatBuffer = {
				37,  74, -95,  62, 121,  21, -32,  61,  14,  46,   1,  63,  14,
			    -43,  75,  62, -33,   3,  92,  63				
		};
		final int [] intValues = { 426783998,   132743707, -2014287369,  1332426665,   -32671763,
			       -1878354646 };
		final byte [] intBuffer = {
				-2,   52,  112,   25,   27, -126,  -23,    7,   -9,  105,  -16,
				-121,  -87,   55,  107,   79,  -19,  119,   13,   -2,   42, -107,
				10, -112 };
		
		client = mock.runOnClient(new RunWithBridge() {
			
			@Override
			public void run(IKnimeBridge bridge) {
				try {
					bridge.run(map);
					double [] doubles = bridge.getDoubleMeasurements(
							new FeatureDescriptionImpl("Nuclei", "X", Double.class));
					assertEquals(3, doubles.length);
					assertEquals(doubleValues[3], doubles[0], .0001);
					assertEquals(doubleValues[4], doubles[1], .0001);
					assertEquals(doubleValues[5], doubles[2], .0001);
					float [] floats = bridge.getFloatMeasurements(
							new FeatureDescriptionImpl("Cytoplasm", "Y", Float.class));
					assertEquals(floatValues[3], floats[0], .0001);
					assertEquals(floatValues[4], floats[1], .0001);
					int [] ints = bridge.getIntMeasurements(
							new FeatureDescriptionImpl(KBConstants.IMAGE, "ImageNumber", Integer.class));
					assertEquals(intValues[5], ints[0]);
					ints = bridge.getIntMeasurements(
							new FeatureDescriptionImpl("Nuclei", "ObjectNumber", Integer.class));
					assertEquals(intValues[0], ints[0]);
					assertEquals(intValues[1], ints[1]);
					assertEquals(intValues[2], ints[2]);
					String s = bridge.getStringMeasurement(
							new FeatureDescriptionImpl(KBConstants.IMAGE, "HackathonComment", String.class));
					assertEquals(stringMeasurement, s);
					
				} catch (ZMQException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (CellProfilerException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (PipelineException e) {
					e.printStackTrace();
				Assert.fail();
				} catch (ProtocolException e) {
					e.printStackTrace();
				Assert.fail();
				}
			}
		});
		server = mock.runOnServer(new RunWithSockets() {
			
			@Override
			public void run(Socket socket) {
				ZMsg msg = ZMsg.recvMsg(socket);
				ZFrame client = msg.unwrap();
				String sessionID = msg.popString();
				assertNotNull(sessionID);
				String recievedPipeline = msg.popString();
				assertEquals(pipeline, recievedPipeline);
				String imgMetadata = msg.popString();
				byte [] data0 = msg.pop().getData();
				byte [] data1 = msg.pop().getData();
				JsonArray images = Json.createReader(new StringReader(imgMetadata)).readArray();
				assertEquals(2, images.size());
				JsonArray image = images.getJsonArray(0);
				if (image.getString(0) == "Foo") {
					assertEquals("Bar", images.getJsonArray(1).getString(0));
					assertEquals(data1.length, 31*18*14*Double.SIZE / Byte.SIZE);
					assertEquals(data0.length, 20*37*10*Double.SIZE / Byte.SIZE);
				} else {
					assertEquals(image.getString(0), "Bar");
					image = images.getJsonArray(1);
					assertEquals("Foo", image.getString(0));
					assertEquals(data0.length, 31*18*14*Double.SIZE / Byte.SIZE);
					assertEquals(data1.length, 20*37*10*Double.SIZE / Byte.SIZE);
				}
				JsonArray axes = image.getJsonArray(1);
				assertEquals(axes.size(), 3);
				assertEquals(axes.getJsonArray(0).getString(0), Axes.X.getLabel());
				assertEquals(axes.getJsonArray(1).getString(0), Axes.Y.getLabel());
				assertEquals(axes.getJsonArray(2).getString(0), Axes.Z.getLabel());
				assertEquals(axes.getJsonArray(0).getInt(1), 20);
				assertEquals(axes.getJsonArray(0).getInt(2), 37*10);
				assertEquals(37, axes.getJsonArray(1).getInt(1));
				assertEquals(10, axes.getJsonArray(1).getInt(2));
				assertEquals(10, axes.getJsonArray(2).getInt(1));
				assertEquals(1, axes.getJsonArray(2).getInt(2));
				
				ZMsg msgOut = new ZMsg();
				msgOut.add("run-reply-1");
				msgOut.add(createMetadata().toString());
				byte [] buf = new byte [10*Double.SIZE / Byte.SIZE + 5*Float.SIZE / Byte.SIZE+ 6 * Integer.SIZE / Byte.SIZE +
				                         stringBuffer.length];
				System.arraycopy(doubleBuffer, 0, buf, 0, doubleBuffer.length);
				int off = doubleBuffer.length;
				System.arraycopy(floatBuffer, 0, buf, off, floatBuffer.length);
				off += floatBuffer.length;
				System.arraycopy(intBuffer, 0, buf, off, intBuffer.length);
				off += intBuffer.length;
				System.arraycopy(stringBuffer, 0, buf, off, stringBuffer.length);
				msgOut.add(buf);
				msgOut.wrap(client);
				msgOut.send(socket);
			}
			
			private JsonArray createMetadata() {
				return Json.createArrayBuilder()
					// Double features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
							.add("Nuclei")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Area").add(3).build())
								.add(Json.createArrayBuilder().add("X").add(3).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add("Cytoplasm")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Area").add(2).build())
								.add(Json.createArrayBuilder().add("X").add(2).build())
								.build())
							.build())
						.build())
					
					// Float features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
							.add("Nuclei")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Y").add(3).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add("Cytoplasm")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("Y").add(2).build())
							    .build())
							.build())
						.build())
					// Int features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
							.add("Nuclei")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("ObjectNumber").add(3).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add("Cytoplasm")
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("ObjectNumber").add(2).build())
								.build())
							.build())
						.add(Json.createArrayBuilder()
							.add(KBConstants.IMAGE)
							.add(Json.createArrayBuilder()
								.add(Json.createArrayBuilder().add("ImageNumber").add(1).build())
								.build())
							.build())
						.build())
					// String features
					.add(Json.createArrayBuilder()
						.add(Json.createArrayBuilder()
						    .add(KBConstants.IMAGE)
						    .add(Json.createArrayBuilder()
						    	.add(Json.createArrayBuilder()
						    		.add("HackathonComment")
						    		.add(stringMeasurement.getBytes().length)
						    		.build())
						    	.build())
							.build())
						.build())
					.build();
			}
		});
		try {
			server.get();
			client.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
		Assert.fail();
		}
		mock.stop();
	}

}
