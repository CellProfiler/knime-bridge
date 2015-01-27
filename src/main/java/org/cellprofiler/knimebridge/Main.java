package org.cellprofiler.knimebridge;

import io.scif.SCIFIO;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.services.DatasetIOService;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.imagej.Dataset;
import net.imagej.ImgPlus;

import org.zeromq.ZMQException;

/**
 * A simple UI for driving the knime bridge
 * 
 * @author Lee Kamentsky
 *
 */
public class Main {
	static public final SCIFIO scifio = new SCIFIO();

	/**
	 * @author Lee Kamentsky
	 *
	 */
	@SuppressWarnings("serial")
	static public class RunAction extends AbstractAction {
		private final JFrame frame;
		private final IKnimeBridge bridge;
		public RunAction(String name, JFrame frame, IKnimeBridge bridge) {
			super(name);
			this.frame = frame;
			this.bridge = bridge;
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent event) {
			Map<String, ImgPlus<?>> imageMap = new Hashtable<String, ImgPlus<?>>();
			for (String channel:bridge.getInputChannels()) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle(String.format("Open %s image", channel));
			    FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "Images (*.jpg, *.png, *.tif)", "jpg", "png", "tif");
			    chooser.setFileFilter(filter);
			    int returnVal = chooser.showOpenDialog(frame);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	ImgOpener opener = new ImgOpener();
			    	File file = chooser.getSelectedFile();
			    	try {
						imageMap.put(channel, opener.openImgs(file.getPath()).get(0));
					} catch (ImgIOException e) {
						JOptionPane.showMessageDialog(
								frame, e.getMessage(), 
								String.format("Can't open \"%s\".", file.getName()),
								JOptionPane.ERROR_MESSAGE);
						return;
					}
			    }
			}
			Exception e1 = null;
			try {
				bridge.run(imageMap);
				final JDialog dlg = new JDialog(frame);
				JScrollPane scroller = new JScrollPane();

				final JTree tree = new JTree();
				dlg.add(scroller, BorderLayout.CENTER);
				scroller.setViewportView(tree);
				tree.setModel(new TreeModel() {
					DefaultMutableTreeNode root;
					
					
					@Override
					public void valueForPathChanged(TreePath path, Object newValue) {
					}
					
					@Override
					public void removeTreeModelListener(TreeModelListener l) {
						
					}
					
					@Override
					public boolean isLeaf(Object node) {
						final DefaultMutableTreeNode treeNode = cast(node);
						return treeNode.getLevel() > 2;
					}
					
					DefaultMutableTreeNode cast(Object node) {
						assert node instanceof DefaultMutableTreeNode;
						return (DefaultMutableTreeNode) node;
					}
					
					@Override
					public Object getRoot() {
						if (root == null) {
							root = new DefaultMutableTreeNode("Measurements", true);
							root.add(new DefaultMutableTreeNode(KBConstants.IMAGE, true));
							for (String object_name:bridge.getObjectNames()) {
								root.add(new DefaultMutableTreeNode(object_name, true));
							}
						}
						return root;
					}
					
					@Override
					public int getIndexOfChild(Object parent, Object child) {
						DefaultMutableTreeNode tParent = cast(parent);
						DefaultMutableTreeNode tChild = cast(child);
						return tParent.getIndex(tChild);
					}
					
					@Override
					public int getChildCount(Object parent) {
						DefaultMutableTreeNode tParent = cast(parent);
						if (tParent.getChildCount() == 0) {
							switch (tParent.getLevel()) {
							case 1: 
								for (IFeatureDescription feature:bridge.getFeatures(tParent.getUserObject().toString())) {
									tParent.add(new DefaultMutableTreeNode(new FeatureHolder(feature), true));
								}
								break;
								
							case 2:
								Object oFeature = tParent.getUserObject();
								if (oFeature instanceof FeatureHolder) {
									for (String value: ((FeatureHolder)oFeature).getValues(bridge)) {
										tParent.add(new DefaultMutableTreeNode(value, true));
									}
								}
								break;
							default:
								break;
							} 
						}
						return tParent.getChildCount();
					}
					
					@Override
					public Object getChild(Object parent, int index) {
						
						return cast(parent).getChildAt(index);
					}
					
					@Override
					public void addTreeModelListener(TreeModelListener l) {
						// TODO Auto-generated method stub
						
					}
				});
				JButton button = new JButton(new AbstractAction("OK"){

					@Override
					public void actionPerformed(ActionEvent arg0) {
						dlg.setVisible(false);
						dlg.dispose();
					}});
				dlg.add(button, BorderLayout.PAGE_END);
				dlg.pack();
				dlg.setVisible(true);
				return;
			} catch (ZMQException e) {
				e.printStackTrace();
				e1 = e;
			} catch (CellProfilerException e) {
				e.printStackTrace();
				e1 = e;
			} catch (PipelineException e) {
				e.printStackTrace();
				e1 = e;
			} catch (ProtocolException e) {
				e.printStackTrace();
				e1 = e;
			}
			JOptionPane.showMessageDialog(
					frame, e1.getMessage(), 
					"Failed during run",
					JOptionPane.ERROR_MESSAGE);
		}

	}
	static class FeatureHolder {
		final IFeatureDescription feature;
		FeatureHolder(IFeatureDescription feature) {
			this.feature = feature;
		}
		@Override
		public String toString() {
			return this.feature.getName();
		}
		public List<String> getValues(IKnimeBridge bridge) {
			Class <?> featureType = feature.getType();
			List<String> result = new ArrayList<String>();
			if (Double.class.isAssignableFrom(featureType)) { 
				for (double value:bridge.getDoubleMeasurements(feature)) {
					result.add(Double.toString(value));
				}
			} else if (Float.class.isAssignableFrom(featureType)) { 
				for (float value:bridge.getFloatMeasurements(feature)) {
					result.add(Float.toString(value));
				}
			} else if (Integer.class.isAssignableFrom(featureType)) {
				for (int value:bridge.getIntMeasurements(feature)) {
					result.add(Integer.toString(value));
				}
			} else if (String.class.isAssignableFrom(featureType)) {
				result.add(bridge.getStringMeasurement(feature));
			}
			return result;
		}
	}

	@SuppressWarnings("serial")
	private static final class LoadPipelineAction extends AbstractAction {
		private final JFrame frame;
		private final IKnimeBridge bridge;

		private LoadPipelineAction(String name, JFrame frame,
				IKnimeBridge bridge) {
			super(name);
			this.frame = frame;
			this.bridge = bridge;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser chooser = new JFileChooser();
		    FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "CellProfiler pipelines (*.cppipe)", "cppipe");
		    chooser.setFileFilter(filter);
		    int returnVal = chooser.showOpenDialog(frame);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		    	try {
		    		final File file = chooser.getSelectedFile();
			    	FileReader rdr = new FileReader(file);
			    	StringBuilder sb = new StringBuilder();
			    	char [] buffer = new char [1000];
			    	while (true) {
			    		final int nRead = rdr.read(buffer);
			    		if (nRead < 0) break;
			    		sb.append(buffer, 0, nRead);
			    	}
			    	bridge.loadPipeline(sb.toString());
		    	} catch (ProtocolException e) {
			    	e.printStackTrace();
			    	return;
			    } catch (FileNotFoundException e) {
					e.printStackTrace();
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				} catch (ZMQException e) {
					e.printStackTrace();
					return;
				} catch (PipelineException e) {
					e.printStackTrace();
					return;
				}
		    	final JDialog dlg = new JDialog(frame, "Pipeline stats");
		    	Container panel = dlg.getContentPane();
		    	BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		    	List<String> channels = bridge.getInputChannels();
		    	Object [] mrStupid = new Object[channels.size()];
		    	int idx = 0;
		    	for (String marshmallow: channels) {
		    		// Put marshmallows in mr stupid
		    		mrStupid[idx++] = marshmallow;
		    	}
		    	JList channelList = new JList(mrStupid);
		    	panel.add(channelList, BorderLayout.NORTH);
		    	List<String> object_names = new ArrayList<String>(bridge.getObjectNames());
		    	Collections.sort(object_names);
		    	List<Object []> features = new ArrayList<Object[]>();
		    	String imageTitle = "Per-image";
	    		for (IFeatureDescription fd: bridge.getFeatures(KBConstants.IMAGE)) {
	    			features.add(new Object [] { imageTitle, fd.getName(), fd.getType().getName() });
	    			imageTitle = "";
	    		}
		    	for (String object_name:object_names) {
		    		String on = object_name;
		    		for (IFeatureDescription fd: bridge.getFeatures(object_name)) {
		    			features.add(new Object [] { on, fd.getName(), fd.getType().getName() });
		    			on = "";
		    		}
		    	}
		    	JScrollPane tableScroller = new JScrollPane();
		    	tableScroller.setMinimumSize(new Dimension(480, 640));
		    	panel.add(tableScroller, BorderLayout.CENTER);
		    	JTable table = new JTable(
		    			features.toArray(new Object [0][]),
		    			new Object [] {"Segmentation", "Feature", "Data type"});
		    	tableScroller.getViewport().add(table, BorderLayout.CENTER);
		    	JButton ok = new JButton(new AbstractAction("OK") {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						dlg.setVisible(false);
						dlg.dispose();
						
					}});
		    	panel.add(ok, BorderLayout.PAGE_END);
		    	dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		    	dlg.pack();
		    	dlg.setVisible(true);
		    }
		}
	}

	/**
	 * @param args - first argument is TCP address of server
	 */
	public static void main(String[] args) {
		final IKnimeBridge bridge = new KnimeBridgeFactory().newKnimeBridge();
		try {
			bridge.connect(new URI(args[0]));
		} catch (ZMQException e) {
			e.printStackTrace();
			return;
		} catch (ProtocolException e) {
			e.printStackTrace();
			return;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}		
		final JFrame frame = new JFrame();
		Container panel = frame.getContentPane();
		BoxLayout layout = new javax.swing.BoxLayout(panel, BoxLayout.X_AXIS);
		JButton load_pipeline_button = new JButton(new LoadPipelineAction("Load pipeline", frame, bridge));
		panel.add(load_pipeline_button, BorderLayout.LINE_START);
		JButton run_pipeline_button = new JButton(new RunAction("Run", frame, bridge));
		panel.add(run_pipeline_button,  BorderLayout.LINE_END);
		frame.pack();
		frame.setVisible(true);
	}

}
