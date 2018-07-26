package services.moleculer.transporter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;

import io.datatree.Promise;
import io.datatree.Tree;

public class RedisSentinelTransporter extends Transporter {

	final RedisClient client;
	StatefulRedisPubSubConnection<byte[], byte[]> pubConnection;
	StatefulRedisPubSubConnection<byte[], byte[]> subConnection;
	RedisPubSubAsyncCommands<byte[], byte[]> pubApi;
	RedisPubSubAsyncCommands<byte[], byte[]> subApi;
	
	public RedisSentinelTransporter(RedisURI uri) {
		if (uri.getSentinels().isEmpty()) throw new IllegalArgumentException();
		this.client = RedisClient.create(uri);
	}
	
	@Override
	public void connect() {
		
		try {
			if (pubConnection == null) {
			
				subConnection = client.connectPubSub(new ByteArrayCodec());
				subApi = subConnection.async();
				subConnection.addListener(new RedisPubSubListener<byte[], byte[]>() {
					
					@Override
					public void unsubscribed(byte[] channel, long count) {
						
					}
					
					@Override
					public void subscribed(byte[] channel, long count) {
						
					}
					
					@Override
					public void punsubscribed(byte[] pattern, long count) {
						
					}
					
					@Override
					public void psubscribed(byte[] pattern, long count) {
						
					}
					
					@Override
					public void message(byte[] pattern, byte[] channel, byte[] message) {
					}
					
					@Override
					public void message(byte[] channel, byte[] message) {					
						String chan = new String(channel, StandardCharsets.UTF_8);
//						try {
//							System.out.println("rec "  + chan + " " + serializer.read(message));
//						}
//						catch (Exception e) {}
						
						received(chan, message);						
					}
				});
				
				pubConnection = client.connectPubSub(new ByteArrayCodec());
				pubApi = pubConnection.async();
			}
			connected();
		}
		catch (Exception e) {
			logger.warn("Unable to connect to a redis sentinel", e);
			reconnect();
		}
		
		
	}
	
	protected void disconnect() {
		if (pubConnection != null) {
			try {
				pubConnection.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occurred while closing Redis client!", cause);
			} finally {
				pubConnection = null;
				pubApi = null;
			}
		}
		if (subConnection != null) {
			try {
				subConnection.close();
			} catch (Throwable cause) {
				logger.warn("Unexpected error occurred while closing Redis client!", cause);
			} finally {
				subConnection = null;
			}
		}
	}

	protected void reconnect() {
		disconnect();
		logger.info("Trying to reconnect...");
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}
	
	@Override
	public void publish(String channel, Tree message) {
		if (pubApi != null) {
			byte[] messageBytes = null;
			try {
				messageBytes = serializer.write(message);
			}
			catch (Exception cause) {
				logger.warn("Unable to serilaze message", cause);
				return;
			}
//			System.out.println("pub " + channel + " " + message);
			pubApi.publish(channel.getBytes(StandardCharsets.UTF_8), messageBytes);
		}
	}
	
	@Override
	public Promise subscribe(String channel) {
		
//		System.out.println("sub " + channel);
		RedisFuture<Void> subscribe = subApi.subscribe(channel.getBytes(StandardCharsets.UTF_8));
		try {
			subscribe.await(1, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		return Promise.resolve();
	}

	@Override
	public void stopped() {
		disconnect();
	}
}
