package poke.server.management.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import poke.server.management.managers.HeartbeatData.BeatStatus;

/*
 * This class will store all the heartbeatManagers. The incomingHB values is retrieved from HeartBeatManagers 
 * to check how many nodes are up and running. After all the nodes are running the election coordinator will 
 * declare the election. The respective election managers will then start sending the election nominate 
 * messages to nearest nodes.
 * 
 */

public class ElectionCoordinator extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionCoordinator> instance = new AtomicReference<ElectionCoordinator>();
	private static ConcurrentLinkedQueue<HeartbeatManager> hbManagers = new ConcurrentLinkedQueue<HeartbeatManager>();

	private String nodeId;
	private boolean electiondeclareFlag = false;

	public static ElectionCoordinator getInstance(String id) {
		instance.compareAndSet(null, new ElectionCoordinator(id));
		return instance.get();
	}

	public static ElectionCoordinator getInstance() {
		return instance.get();
	}

	protected ElectionCoordinator(String nodeId) {
		this.nodeId = nodeId;
	}

	public void addHBManager(HeartbeatManager heartbeatMgr) {
		// TODO Auto-generated method stub
		hbManagers.add(heartbeatMgr);
	}

	public boolean iselectionDeclared() {
		return electiondeclareFlag;
	}

	public void run() {
		logger.info("Inside the election coordinator run method");
		logger.info("Concurrent HB Managers size" + hbManagers.size());
		if (hbManagers.size() == 4) {
			for (HeartbeatManager hb : hbManagers) {
				for (HeartbeatData hd : hb.incomingHB.values()) {
					if (hd.getStatus() == BeatStatus.Active) {
						electiondeclareFlag = true;
						logger.info("Election declared flag set to true");
					}

				}
			}
		}// end if
	}

}
