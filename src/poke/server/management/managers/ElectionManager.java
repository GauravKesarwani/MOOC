/*
 * copyright 2014, gash
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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.HeartMonitor;
import poke.server.management.ManagementHandler;
import poke.server.management.ManagementInitializer;
import poke.server.management.managers.HeartbeatData.BeatStatus;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.LeaderElection;
import eye.Comm.LeaderElection.VoteAction;
import eye.Comm.Management;

/**
 * The election manager is used to determine leadership within the network.
 * 
 * @author gash
 * 
 */
public class ElectionManager extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionManager> instance = new AtomicReference<ElectionManager>();
	private HeartbeatManager heartbeatMgr;
	private HeartbeatConnector heartbeatConn;
	private ManagementHandler handler;
	ConcurrentLinkedQueue<HeartMonitor> monitors;
	private static String leader = null;

	protected ChannelFuture channel; // do not use directly, call connect()!
	private String nodeId;
	private String myId;

	boolean forever = true;

	/** @brief the number of votes this server can cast */
	private int votes = 1;
	private String reqNodeId;
	private static int N = 0;

	public static ElectionManager getInstance(String id, int votes) {
		instance.compareAndSet(null, new ElectionManager(id, votes));
		return instance.get();
	}

	public static ElectionManager getInstance() {
		return instance.get();
	}

	public void setMonitor(ConcurrentLinkedQueue<HeartMonitor> monitors) {
		this.monitors = monitors;
	}

	/**
	 * initialize the manager for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected ElectionManager(String nodeId, int votes) {
		this.nodeId = nodeId;

		if (votes >= 0)
			this.votes = votes;
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect(String host, int port) {
		// Start the connection attempt.
		if (channel == null) {
			try {
				handler = new ManagementHandler();
				ManagementInitializer mi = new ManagementInitializer(false);

				Bootstrap b = new Bootstrap();
				b.group(new NioEventLoopGroup())
						.channel(NioSocketChannel.class).handler(mi);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// Make the connection attempt.
				channel = b.connect(host, port).syncUninterruptibly();
				channel.awaitUninterruptibly(5000l);

				if (N == Integer.MAX_VALUE)
					N = 1;
				else
					N++;

			} catch (Exception ex) {
				logger.debug("failed to initialize the heartbeat connection");
			}
		}

		if (channel != null && channel.isDone() && channel.isSuccess())
			return channel.channel();
		else
			throw new RuntimeException(
					"Not able to establish connection to server");
	}

	/*
	 * Generate Election Nominate Messages
	 */
	private Management generateEM(String nodeId, VoteAction v) {
		LeaderElection.Builder l = LeaderElection.newBuilder();
		l.setDesc("Leader election Attribute Set");
		l.setNodeId(nodeId);
		l.setBallotId("dummy ballot");
		l.setDesc("my vote");
		l.setVote(v);
		Management.Builder m = Management.newBuilder();
		m.setElection(l.build());
		return m.build();
	}

	public static String getLeader() {
		return leader;
	}

	@Override
	public void run() {
		logger.info("Starting the run method");

		while (true) {
			monitors = heartbeatConn.getMonitors();
			GeneratedMessage msg = null;
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for (HeartbeatData hd : heartbeatMgr.incomingHB.values()) {
				if (hd.getStatus() == BeatStatus.Active) {
					if (msg == null)
						msg = generateEM(this.nodeId, VoteAction.NOMINATE);
					logger.info("this.nodeId " + this.nodeId);

					try {
						logger.info("sending leader nominate message");

						for (HeartMonitor hm : monitors) {
							if (hm.getWhoami() == hd.getNodeId()) {
								String host = hm.getHost();
								int port = hm.getPort();
								Channel ch = connect(host, port);
								ch.writeAndFlush(msg);

							}

						}
					} catch (Exception e) {

					}
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public void processRequest(LeaderElection req) {
		if (req == null) {
			logger.info("Null request");
			return;
		}
		if (req.hasExpires()) {
			long ct = System.currentTimeMillis();
			if (ct > req.getExpires()) {
				logger.info("Election Over");
				return;
			}
		}

		logger.info("Election message received");

		if (req.getVote().getNumber() == VoteAction.ELECTION_VALUE) {
			logger.info("Election is declared");

		} else if (req.getVote().getNumber() == VoteAction.DECLAREVOID_VALUE) {
			// no one was elected, I am dropping into standby mode`
		} else if (req.getVote().getNumber() == VoteAction.DECLAREWINNER_VALUE) {
			// some node declared themself the leader
		} else if (req.getVote().getNumber() == VoteAction.ABSTAIN_VALUE) {
			// for some reason, I decline to vote
		} else if (req.getVote().getNumber() == VoteAction.NOMINATE_VALUE) {
			if (nodeId.equals("zero"))
				myId = "a";
			else if (nodeId.equals("one"))
				myId = "b";
			else if (nodeId.equals("two"))
				myId = "c";
			else if (nodeId.equals("three"))
				myId = "d";

			if (req.getNodeId().equals("zero"))
				reqNodeId = "a";
			else if (req.getNodeId().equals("one"))
				reqNodeId = "b";
			else if (req.getNodeId().equals("two"))
				reqNodeId = "c";
			else if (req.getNodeId().equals("three"))
				reqNodeId = "d";

			int comparedToMe = reqNodeId.compareTo(myId);
			if (comparedToMe < 0) {
				// Discard the id received from node and nominate myself
				logger.info("Discard request for leader for node :"
						+ req.getNodeId());
			} else if (comparedToMe > 0) {
				// forward the message to all the nearest nodes. In case of LCR
				// there will be only
				// one nearest node as LCR works for unidirectional ring
				logger.info("Forward Node Id for leader election : "
						+ req.getNodeId());
				for (HeartMonitor hm : monitors) {
					String host = hm.getHost();
					int port = hm.getPort();
					Channel ch = connect(host, port);
					GeneratedMessage msg = generateEM(req.getNodeId(),
							VoteAction.NOMINATE);
					ch.writeAndFlush(msg);
				}
			} else if (comparedToMe == 0) {
				logger.info("Node Id " + req.getNodeId() + " is leader !! ");
				leader = req.getNodeId();
			}

		}
	}

	public void setHeartbeatManager(HeartbeatManager hbmgr) {
		this.heartbeatMgr = hbmgr;
	}

	public void setHeartbeatConnector(HeartbeatConnector conn) {
		this.heartbeatConn = conn;

	}

}
