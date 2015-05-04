package poke.server.voting;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.management.managers.ElectionManager;
import poke.server.resources.ResourceUtil;
import eye.Comm.InitVoting;
import eye.Comm.NameSpace;
import eye.Comm.NameSpaceOperation;
import eye.Comm.NameSpaceOperation.SpaceAction;
import eye.Comm.Payload;
import eye.Comm.PokeStatus;
import eye.Comm.Request;

//this class handles the voting request and provides the decision
public class IntraClusterVoting {
	protected static Logger logger = LoggerFactory.getLogger("server");

	public Request setNeighbor(ServerConf cfg, Request request) {
		Request.Builder rb = Request.newBuilder();
		String iam = cfg.getServer().getProperty("node.id");

		//if leader get back the voting request 
		if (iam.equals(ElectionManager.getLeader()) && request.getBody().getSpaceOp().getData().getNsId()==0) {
			Payload.Builder pb = Payload.newBuilder();
			InitVoting.Builder initbuilder = InitVoting.newBuilder();
			int noNodes=(int)request.getBody().getSpaceOp().getData().getLastModified();
			int noVotes=(int)request.getBody().getSpaceOp().getData().getCreated();
			//MAJORITY WINS ELECTION
			if(noVotes>(noNodes/2)) {
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
					PokeStatus.SUCCESS, "Ready to host competition"));
			
			initbuilder.setPortIp(cfg.getServer().getProperty("port"));
			initbuilder.setHostIp(cfg.getServer().getProperty("ip"));
			} else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.FAILURE, "Do not want to host competition"));
			}
			pb.setInitVoting(initbuilder.build());
			rb.setBody(pb.build());
			Request reply = rb.build();
			System.out.println("Sending message to client back about voting"+ reply);
			return reply;
		}
		
		// get vote of the current node
		int vote = getVote();

		// finding the node to forward data
		NodeDesc nd = cfg.getNearest().getNearestNodes().values().iterator()
				.next();
		logger.info(" Nearest node participating in voting will be "
				+ nd.getNodeId());
		String node = nd.getNodeId();
		rb.setHeader(ResourceUtil.buildHeaderforVoting(request.getHeader(),
				node, "server"));

		Payload.Builder pb = Payload.newBuilder();
		NameSpaceOperation.Builder nsb = NameSpaceOperation.newBuilder();
		NameSpace.Builder nb = NameSpace.newBuilder();
		
		InitVoting.Builder initbuilder = InitVoting.newBuilder();
		initbuilder.setHostIp("");
		pb.setInitVoting(initbuilder.build());
		nsb.setAction(SpaceAction.UPDATESPACE);

		// namespace's created field is used for keeping track of number of votes
		// namespace's LastModified field is used for keeping track of number of nodes hopped
		if (!iam.equals(ElectionManager.getLeader())) {
			nb.setCreated(request.getBody().getSpaceOp().getData().getCreated()
					+ vote);			
			nb.setLastModified(request.getBody().getSpaceOp().getData()
					.getLastModified() + 1);
			logger.info("Vote given by node is : " + vote);
		}
		nb.setNsId(0);
		pb.setSpaceOp(nsb.setData(nb.build()));
		rb.setBody(pb.build());

		Request reply = rb.build();
		return reply;
	}

	public int getVote() {
		Random r = new Random();
		int randomNumber = r.nextInt(2);
		return randomNumber;
	}
}
