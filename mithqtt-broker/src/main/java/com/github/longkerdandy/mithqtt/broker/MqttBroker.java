package com.github.longkerdandy.mithqtt.broker;

import com.github.longkerdandy.mithqtt.broker.comm.BrokerListenerFactoryImpl;
import com.github.longkerdandy.mithqtt.broker.handler.BytesMetricsHandler;
import com.github.longkerdandy.mithqtt.broker.handler.MessageMetricsHandler;
import com.github.longkerdandy.mithqtt.broker.handler.SyncStorageHandler;
import com.github.longkerdandy.mithqtt.api.storage.sync.SyncStorage;
import com.github.longkerdandy.mithqtt.api.auth.Authenticator;
import com.github.longkerdandy.mithqtt.api.comm.BrokerCommunicator;
import com.github.longkerdandy.mithqtt.api.comm.BrokerListenerFactory;
import com.github.longkerdandy.mithqtt.api.metrics.MetricsService;
import com.github.longkerdandy.mithqtt.broker.session.SessionRegistry;
import com.github.longkerdandy.mithqtt.broker.util.Validator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * MQTT Bridge
 */
public class MqttBroker {

    private static final Logger logger = LoggerFactory.getLogger(MqttBroker.class);

    public static void main(String[] args) throws Exception {

        logger.debug("Starting MQTT broker ...");

        // load config
        logger.debug("Loading MQTT broker config files ...");
        PropertiesConfiguration brokerConfig;
        PropertiesConfiguration storeConfig;
        PropertiesConfiguration communicatorConfig;
        PropertiesConfiguration authenticatorConfig;
        PropertiesConfiguration metricsConfig;
        if (args.length >= 5) {
            brokerConfig = new PropertiesConfiguration(args[0]);
            storeConfig = new PropertiesConfiguration(args[1]);
            communicatorConfig = new PropertiesConfiguration(args[2]);
            authenticatorConfig = new PropertiesConfiguration(args[3]);
            metricsConfig = new PropertiesConfiguration(args[4]);
        } else {
            brokerConfig = new PropertiesConfiguration("config/broker.properties");
            storeConfig = new PropertiesConfiguration("config/store.properties");
            communicatorConfig = new PropertiesConfiguration("config/communicator.properties");
            authenticatorConfig = new PropertiesConfiguration("config/authenticator.properties");
            metricsConfig = new PropertiesConfiguration("config/metrics.properties");
        }

        final String brokerId = brokerConfig.getString("broker.id");

        // validator
        logger.debug("Initializing validator ...");
        Validator validator = new Validator(brokerConfig);

        // session registry
        logger.debug("Initializing session registry ...");
        SessionRegistry registry = new SessionRegistry();

        // storage
        logger.debug("Initializing sync storage ...");
        SyncStorage store = (SyncStorage) Class.forName(storeConfig.getString("storage.sync.class")).newInstance();
        store.init(storeConfig);

        logger.debug("Clearing broker connection state in storage, this may take some time ...");
        clearBrokerConnectionState(brokerId, store);

        // communicator
        logger.debug("Initializing communicator ...");
        BrokerCommunicator communicator = (BrokerCommunicator) Class.forName(communicatorConfig.getString("communicator.class")).newInstance();
        BrokerListenerFactory listenerFactory = new BrokerListenerFactoryImpl(registry);
        communicator.init(communicatorConfig, brokerId, listenerFactory);

        // authenticator
        logger.debug("Initializing authenticator...");
        Authenticator authenticator = (Authenticator) Class.forName(authenticatorConfig.getString("authenticator.class")).newInstance();
        authenticator.init(authenticatorConfig);

        // metrics
        logger.debug("Initializing metrics ...");
        final boolean metricsEnabled = metricsConfig.getBoolean("metrics.enabled");
        MetricsService metrics = metricsEnabled ? (MetricsService) Class.forName(metricsConfig.getString("metrics.class")).newInstance() : null;
        if (metricsEnabled) metrics.init(metricsConfig);

        // broker
        final int keepAlive = brokerConfig.getInt("mqtt.keepalive.default");
        final int keepAliveMax = brokerConfig.getInt("mqtt.keepalive.max");
        final boolean ssl = brokerConfig.getBoolean("mqtt.ssl.enabled");
        final SslContext sslContext = ssl ? SslContextBuilder.forServer(new File(brokerConfig.getString("mqtt.ssl.certPath")), new File(brokerConfig.getString("mqtt.ssl.keyPath")), brokerConfig.getString("mqtt.ssl.keyPassword")).build() : null;
        final String host = brokerConfig.getString("mqtt.host");
        final int port = ssl ? brokerConfig.getInt("mqtt.ssl.port") : brokerConfig.getInt("mqtt.port");

        // tcp server
        logger.debug("Initializing tcp server ...");
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.getDefaultFactory());
        EventLoopGroup bossGroup = brokerConfig.getBoolean("netty.useEpoll") ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        EventLoopGroup workerGroup = brokerConfig.getBoolean("netty.useEpoll") ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        EventLoopGroup handlerGroup = brokerConfig.getBoolean("netty.useEpoll") ? new EpollEventLoopGroup() : new NioEventLoopGroup();

        // shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.debug("MQTT broker is shutting down ...");

                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
                if (metricsEnabled) metrics.destroy();
                communicator.destroy();
                authenticator.destroy();

                logger.debug("Clearing broker connection state in storage, this may take some time ...");
                clearBrokerConnectionState(brokerId, store);

                store.destroy();

                logger.info("MQTT broker has been shut down.");
            }
        });

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(brokerConfig.getBoolean("netty.useEpoll") ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        // ssl
                        if (ssl) {
                            p.addLast("ssl", sslContext.newHandler(ch.alloc()));
                        }
                        // idle
                        p.addFirst("idleHandler", new IdleStateHandler(0, 0, keepAlive));
                        // metrics
                        if (metricsEnabled) {
                            p.addLast("bytesMetrics", new BytesMetricsHandler(metrics, brokerId));
                        }
                        // mqtt encoder & decoder
                        p.addLast("encoder", MqttEncoder.INSTANCE);
                        p.addLast("decoder", new MqttDecoder());
                        // metrics
                        if (metricsEnabled) {
                            p.addLast("msgMetrics", new MessageMetricsHandler(metrics, brokerId));
                        }
                        // logic handler
                        p.addLast(handlerGroup, "logicHandler", new SyncStorageHandler(authenticator, communicator, store, registry, validator, brokerId, keepAlive, keepAliveMax));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, brokerConfig.getInt("netty.soBacklog"))
                .childOption(ChannelOption.SO_KEEPALIVE, brokerConfig.getBoolean("netty.soKeepAlive"));

        // Bind and start to accept incoming connections.
        ChannelFuture f = b.bind(host, port).sync();

        logger.info("MQTT broker is up and running.");

        // Wait until the server socket is closed.
        // Do this to gracefully shut down the server.
        f.channel().closeFuture().sync();
    }

    /**
     * Loop and mark every clients currently connect to the broker as disconnect
     *
     * @param brokerId Broker Id
     * @param store    Sync Storage
     */
    protected static void clearBrokerConnectionState(String brokerId, SyncStorage store) {
        List<String> r = store.getConnectedClients(brokerId, "0", 100);
        if (r != null) {
            r.forEach(client -> store.removeConnectedNode(client, brokerId));
        }
        final int iterPos = 0;
        while (r != null && r.size() > 0 && !r.get(iterPos).equals("0")) {
            r = store.getConnectedClients(brokerId, r.get(iterPos), 100);
            if (r != null) {
                r.forEach(client -> store.removeConnectedNode(client, brokerId));
            }
        }
    }
}
