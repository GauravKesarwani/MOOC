package poke.server.management.managers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
//import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.management.ManagementHandler;
import poke.server.management.ManagementInitializer;

public class VotingManager {

	static HashMap<String,String> map = new HashMap<String,String>();
	protected static AtomicReference<VotingManager> instance = new AtomicReference<VotingManager>();
	protected static Logger logger = LoggerFactory.getLogger("management");
	
	public static VotingManager getInstance() {
		instance.compareAndSet(null, new VotingManager());
		return instance.get();
	}

	public static void findLeaders() {
		// TODO Auto-generated method stub
		map.put("one","192.168.0.71");
		map.put("two","192.168.0.81");
		map.put("three","192.168.0.91");
		map.put("four","192.168.0.101");
		map.put("five","192.168.0.111"); 		
	}

	private ChannelFuture channel;
	private ManagementHandler handler;
	private int N;
	
	protected Channel connect(String host,int port) {
		// Start the connection attempt.
		if (channel == null) {
			try {
				handler = new ManagementHandler();
				ManagementInitializer mi = new ManagementInitializer(false);

				Bootstrap b = new Bootstrap();
				// @TODO newFixedThreadPool(2);
				b.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).handler(mi);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// Make the connection attempt.
				channel = b.connect(host, port).syncUninterruptibly();
				channel.awaitUninterruptibly(5000l);
		//		channel.channel().closeFuture().addListener(new ManagementClosedListener(this));

				if (N == Integer.MAX_VALUE)
					N = 1;
				else
					N++;

			} catch (Exception ex) {
				logger.debug("failed to initialize the heartbeat connection");
				// logger.error("failed to initialize the heartbeat connection",
				// ex);
			}
		}

		if (channel != null && channel.isDone() && channel.isSuccess())
			return channel.channel();
		else
			throw new RuntimeException("Not able to establish connection to server");
	}
}
