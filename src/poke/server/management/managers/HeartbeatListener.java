/*
 * copyright 2013, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.management.managers;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.HeartMonitor;
import poke.monitor.MonitorListener;
import poke.server.NetworkNode;
import poke.server.Server;
import poke.server.management.managers.HeartbeatData.BeatStatus;

public class HeartbeatListener implements MonitorListener {
	protected static Logger logger = LoggerFactory.getLogger("management");

	private HeartbeatData data;

	public HeartbeatListener(HeartbeatData data) {
		this.data = data;
	}

	public HeartbeatData getData() {
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.monitor.MonitorListener#getListenerID()
	 */
	@Override
	public String getListenerID() {
		return data.getNodeId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.monitor.MonitorListener#onMessage(eye.Comm.Management)
	 */
	@Override
	public void onMessage(eye.Comm.Management msg) {
		if (logger.isDebugEnabled())
			logger.debug(msg.getBeat().getNodeId());

		if (msg.hasGraph()) {
			logger.info("Received graph responses");
		} else if (msg.hasBeat()
				&& msg.getBeat().getNodeId().equals(data.getNodeId())) {
			logger.info("Received HB response from "
					+ msg.getBeat().getNodeId());
			data.setLastBeat(System.currentTimeMillis());
			data.setStatus(BeatStatus.Active);
		} else if (msg.hasElection()) {
			logger.info("Received Election Message from "
					+ msg.getElection().getNodeId());
		} else
			logger.error("Received heartbeatMgr from on wrong channel or unknown host: "
					+ msg.getBeat().getNodeId());
	}

	@Override
	public void connectionClosed() {
		// note a closed management port is likely to indicate the primary port
		// has failed as well
		logger.info("connection close");
		logger.info(data.getNodeId());
		String lookupnode = null;
		HashMap<String, NetworkNode> hm = Server.getNetwork();
		if (data.getNodeId().equals("zero"))
			lookupnode = "one";
		if (data.getNodeId().equals("one"))
			lookupnode = "two";
		if (data.getNodeId().equals("two"))
			lookupnode = "three";
		if (data.getNodeId().equals("three"))
			lookupnode = "zero";

		NetworkNode n = hm.get(lookupnode);
		logger.info(n.getNodeId());

		/*
		 * 
		 * find myId depending on the id of the neighbour node
		 */

		String myId = null;
		if (data.getNodeId().equals("zero"))
			myId = "three";
		if (data.getNodeId().equals("one"))
			myId = "zero";
		if (data.getNodeId().equals("two"))
			myId = "one";
		if (data.getNodeId().equals("three"))
			myId = "two";

		HeartbeatConnector c = Server.getserverhbconn();

		ConcurrentLinkedQueue<HeartMonitor> monitors = c.getMonitors();
		monitors.clear();
		HeartbeatData newdata = new HeartbeatData(n.getNodeId(), n.getHost(),
				n.getPort(), n.getManagementPort());
		c.addConnectToThisNode(newdata);

		HeartbeatManager hbm = Server.gethbManager();
		ConcurrentHashMap<String, HeartbeatData> incominghbs = hbm
				.getIncomingHB();
		incominghbs.remove(data.getNodeId(), data);

		newdata.setStatus(BeatStatus.Active);
		ElectionManager mgr = Server.getElectionMgr();
		mgr.setMonitor(monitors);

		mgr.setHeartbeatConnector(c);
	}

	@Override
	public void connectionReady() {
		// do nothing at the moment
	}
}
