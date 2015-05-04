/*
 * copyright 2012, gash
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
package poke.server.resources;

import java.beans.Beans;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.ClientPrintListener;
import poke.client.comm.CommConnection;
import poke.client.comm.CommListener;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.conf.ServerConf.ResourceConf;
import poke.server.management.managers.ElectionManager;
import poke.server.voting.IntraClusterVoting;
import eye.Comm.Request;

/**
 * Resource factory provides how the server manages resource creation. We hide
 * the creation of resources to be able to change how instances are managed
 * (created) as different strategies will affect memory and thread isolation. A
 * couple of options are:
 * <p>
 * <ol>
 * <li>instance-per-request - best isolation, worst object reuse and control
 * <li>pool w/ dynamic growth - best object reuse, better isolation (drawback,
 * instances can be dirty), poor resource control
 * <li>fixed pool - favor resource control over throughput (in this case failure
 * due to no space must be handled)
 * </ol>
 * 
 * @author gash
 * 
 */
public class ResourceFactory {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private static ServerConf cfg;
	private static AtomicReference<ResourceFactory> factory = new AtomicReference<ResourceFactory>();

	// request memeber vairable of this class is used to retrieve the request
	// changed and has to be sent forward
	private Request request;

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public static void initialize(ServerConf cfg) {
		try {
			ResourceFactory.cfg = cfg;
			factory.compareAndSet(null, new ResourceFactory());
		} catch (Exception e) {
			logger.error("failed to initialize ResourceFactory", e);
		}
	}

	public static ResourceFactory getInstance() {
		ResourceFactory rf = factory.get();
		if (rf == null)
			throw new RuntimeException("Server not intialized");

		return rf;
	}

	private ResourceFactory() {
	}

	/**
	 * Obtain a resource
	 * 
	 * @param route
	 * @return
	 */
	public Resource resourceInstance(Request req) {
		// when request arrived is related to Voting
		if (req.getHeader().getOriginator().equalsIgnoreCase("server")
				&& req.getBody().hasInitVoting()
				&& req.getBody().getInitVoting().getHostIp().equals("")) {
			IntraClusterVoting iv = new IntraClusterVoting();

			req = iv.setNeighbor(cfg, req);
			logger.info("Request sending for voting : " + req);
			request = req;
		}

		// Checking if the request contains the destination for routing the
		// request. If not, then build a new request and forward it.
		if (!req.getBody().hasInitVoting()
				&& (!req.getHeader().hasToNode()
						|| req.getHeader().getToNode() == "" || req.getHeader()
						.getToNode() == " ")) {
			Random r = new Random();
			int randomNumber = r.nextInt(4);

			String[] randomNode = { "zero", "one", "two", "three" };
			String node = randomNode[randomNumber];

			// forming a new request, header being changed, body remains the
			// same
			Request.Builder rb = Request.newBuilder();
			rb.setHeader(ResourceUtil.buildHeaderforServer(req.getHeader(),
					node));
			rb.setBody(req.getBody());
			req = rb.build();
		}

		// route the message
		if (req.getHeader().hasToNode()) {
			String iam = cfg.getServer().getProperty("node.id");
			/*
			 * if message originates from client and I am the leader then
			 * forward the message to the nearest node for processing else
			 * process the message normally
			 */
			if (!(req.getHeader().getOriginator().equalsIgnoreCase("server"))
					&& (iam.equals(ElectionManager.getLeader()))) {
				NodeDesc nd = cfg.getNearest().getNearestNodes().values()
						.iterator().next();
				logger.info(" Nearest node handling request " + nd.getNodeId());
				CommConnection conn = new CommConnection(nd.getHost(),
						nd.getPort());
				CommListener listener = new ClientPrintListener(
						"server listener");
				conn.addListener(listener);
				try {
					conn.sendMessage(req);
				} catch (Exception e) {
					logger.warn("Unable to send message to queue");
				}
			} else {
				if (iam.equalsIgnoreCase(req.getHeader().getToNode())) {
					logger.info("i am the one who will process request");
					// fall through and process normally if the message is
					// intended for me
				} else {
					// else forward the message to nearest node
					NodeDesc nd = cfg.getNearest().getNearestNodes().values()
							.iterator().next();
					logger.info(" Nearest node handling request "
							+ nd.getNodeId());
					CommConnection conn = new CommConnection(nd.getHost(),
							nd.getPort());
					CommListener listener = new ClientPrintListener(
							"server listener");
					conn.addListener(listener);
					try {
						conn.sendMessage(req);
					} catch (Exception e) {
						logger.warn("Unable to send message to queue");
					}
				}
			}
		}
		ResourceConf rc = cfg.findById(req.getHeader().getRoutingId()
				.getNumber());
		if (rc == null) {
			return null;
		}

		try {
			// strategy: instance-per-request
			Resource rsc = (Resource) Beans.instantiate(this.getClass()
					.getClassLoader(), rc.getClazz());
			return rsc;
		} catch (Exception e) {
			logger.error("unable to create resource " + rc.getClazz());
			return null;
		}
	}

}
