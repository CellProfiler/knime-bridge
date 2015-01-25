package org.cellprofiler.knimebridge;

public class KnimeBridgeFactory {
	public IKnimeBridge newKnimeBridge() {
		return new KnimeBridgeImpl();
	}

}
