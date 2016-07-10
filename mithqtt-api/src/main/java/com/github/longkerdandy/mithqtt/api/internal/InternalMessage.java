package com.github.longkerdandy.mithqtt.api.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageFactory;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import io.netty.handler.codec.mqtt.MqttVersion;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent MQTT Message passed in Communicator
 */
public class InternalMessage<T> implements Serializable {

	private static final long serialVersionUID = -4032909532911735019L;
	
	// fixed header
    private MqttMessageType messageType;
    private boolean dup;
    private MqttQoS qos;
    private boolean retain;

    // some info in CONNECT message, but useful for stateless transfer
    private MqttVersion version;
    private String clientId;
    private String userName;

    // broker id, only meaningful when the message is sent by broker
    private String brokerId;

    // variable header and payload
    private T payload;

    protected InternalMessage() {
    }

    public InternalMessage(MqttMessageType messageType, boolean dup, MqttQoS qos, boolean retain,
                           MqttVersion version, String clientId, String userName,
                           String brokerId) {
        this(messageType, dup, qos, retain, version, clientId, userName, brokerId, null);
    }

    public InternalMessage(MqttMessageType messageType, boolean dup, MqttQoS qos, boolean retain,
                           MqttVersion version, String clientId, String userName,
                           String brokerId, T payload) {
        this.messageType = messageType;
        this.dup = dup;
        this.qos = qos;
        this.retain = retain;
        this.version = version;
        this.clientId = clientId;
        this.userName = userName;
        this.brokerId = brokerId;
        this.payload = payload;
    }

    protected static <T> InternalMessage<T> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                            String brokerId,
                                                            MqttFixedHeader fixedHeader) {
        return new InternalMessage<T>(fixedHeader.messageType(), fixedHeader.isDup(), fixedHeader.qosLevel(), fixedHeader.isRetain(),
                version, clientId, userName, brokerId);
    }

    public static InternalMessage<Connect> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                           String brokerId,
                                                           MqttConnectMessage mqtt) {
        InternalMessage<Connect> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        msg.payload = mqtt.variableHeader().isWillFlag() ?
                new Connect(mqtt.variableHeader().isCleanSession(), mqtt.variableHeader().isWillRetain(), MqttQoS.valueOf(mqtt.variableHeader().willQos()), mqtt.payload().willTopic(), mqtt.payload().willMessage().getBytes()) :
                new Connect(mqtt.variableHeader().isCleanSession(), false, MqttQoS.AT_MOST_ONCE, null, null);
        return msg;
    }

    public static InternalMessage<ConnAck> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                           String brokerId,
                                                           MqttConnAckMessage mqtt) {
        InternalMessage<ConnAck> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        msg.payload = new ConnAck(mqtt.variableHeader().connectReturnCode(), mqtt.variableHeader().isSessionPresent());
        return msg;
    }

    public static InternalMessage<Subscribe> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                             String brokerId,
                                                             MqttSubscribeMessage mqtt, List<MqttQoS> returnCodes) {
        InternalMessage<Subscribe> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        // forge topic subscriptions
        if (mqtt.payload().topicSubscriptions().size() != returnCodes.size()) {
            throw new IllegalArgumentException("MQTT SUBSCRIBE message's subscriptions count not equal to granted QoS count");
        }
        List<TopicSubscription> topicSubscriptions = new ArrayList<>();
        for (int i = 0; i < mqtt.payload().topicSubscriptions().size(); i++) {
            TopicSubscription subscription = new TopicSubscription(mqtt.payload().topicSubscriptions().get(i).topicName(), returnCodes.get(i));
            topicSubscriptions.add(subscription);
        }
        msg.payload = new Subscribe(mqtt.variableHeader().messageId(), topicSubscriptions);
        return msg;
    }

    public static InternalMessage<SubAck> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                          String brokerId,
                                                          MqttSubAckMessage mqtt) {
        InternalMessage<SubAck> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        final List<MqttQoS> qosLevelsGranted = new ArrayList<MqttQoS>();
        for(int qos: mqtt.payload().grantedQoSLevels()) {
        	qosLevelsGranted.add(MqttQoS.valueOf(qos));
        }
        msg.payload = new SubAck(mqtt.variableHeader().messageId(), qosLevelsGranted);
        return msg;
    }

    public static InternalMessage<Unsubscribe> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                               String brokerId,
                                                               MqttUnsubscribeMessage mqtt) {
        InternalMessage<Unsubscribe> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        msg.payload = new Unsubscribe(mqtt.variableHeader().messageId(), mqtt.payload().topics());
        return msg;
    }

    public static InternalMessage<Publish> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                           String brokerId,
                                                           MqttPublishMessage mqtt) {
        InternalMessage<Publish> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        // forge bytes payload
        ByteBuf buf = mqtt.payload().duplicate();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        msg.payload = new Publish(mqtt.variableHeader().topicName(), mqtt.variableHeader().messageId(), bytes);
        return msg;
    }

    public static InternalMessage<Disconnect> fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                              String brokerId,
                                                              boolean cleanSession, boolean cleanExit) {
        return new InternalMessage<>(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false,
                version, clientId, userName, brokerId, new Disconnect(cleanSession, cleanExit));
    }

    @SuppressWarnings("rawtypes")
	public static InternalMessage fromMqttMessage(MqttVersion version, String clientId, String userName,
                                                  String brokerId,
                                                  MqttMessage mqtt) {
        if (mqtt.variableHeader() != null && mqtt.variableHeader() instanceof MqttMessageIdVariableHeader) {
            InternalMessage<PacketId> msg = fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
            msg.payload = new PacketId(((MqttMessageIdVariableHeader) mqtt.variableHeader()).messageId());
            return msg;
        } else {
            return fromMqttMessage(version, clientId, userName, brokerId, mqtt.fixedHeader());
        }
    }

    public MqttMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MqttMessageType messageType) {
        this.messageType = messageType;
    }

    public boolean isDup() {
        return dup;
    }

    public void setDup(boolean dup) {
        this.dup = dup;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public void setQos(MqttQoS qos) {
        this.qos = qos;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public MqttVersion getVersion() {
        return version;
    }

    public void setVersion(MqttVersion version) {
        this.version = version;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public MqttMessage toMqttMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(messageType, dup, qos, retain, 0);
        switch (messageType) {
            case CONNECT:
                Connect connect = (Connect) payload;
                boolean userNameFlag = StringUtils.isNotBlank(userName);
                boolean willFlag = connect.getWillMessage() != null && connect.getWillMessage().length > 0;
                return MqttMessageFactory.newMessage(fixedHeader,
                        new MqttConnectVariableHeader(version.protocolName(), version.protocolLevel(), userNameFlag, false, connect.isWillRetain(), connect.getWillQos().value(), willFlag, connect.isCleanSession(), 0),
                        new MqttConnectPayload(clientId, connect.getWillTopic(), new String(connect.getWillMessage()), userName, null));
            case CONNACK:
                ConnAck connAck = (ConnAck) payload;
                return MqttMessageFactory.newMessage(fixedHeader,
                        new MqttConnAckVariableHeader(connAck.getReturnCode(), connAck.isSessionPresent()),
                        null);
            case SUBSCRIBE:
                Subscribe subscribe = (Subscribe) payload;
                List<MqttTopicSubscription> subscriptions = new ArrayList<>();
                subscribe.getSubscriptions().forEach(s ->
                        subscriptions.add(new MqttTopicSubscription(s.getTopic(), MqttQoS.valueOf(s.getGrantedQos().value())))
                );
                return MqttMessageFactory.newMessage(fixedHeader,
                        MqttMessageIdVariableHeader.from(subscribe.getPacketId()),
                        new MqttSubscribePayload(subscriptions));
            case SUBACK:
                SubAck subAck = (SubAck) payload;
                List<Integer> grantedQosLevels = new ArrayList<Integer>();
                for(MqttQoS qos: subAck.getGrantedQoSLevels()) {
                	grantedQosLevels.add(qos.value());
                }
                return MqttMessageFactory.newMessage(fixedHeader,
                        MqttMessageIdVariableHeader.from(subAck.getPacketId()),
                        new MqttSubAckPayload(grantedQosLevels));
            case UNSUBSCRIBE:
                Unsubscribe unsubscribe = (Unsubscribe) payload;
                return MqttMessageFactory.newMessage(fixedHeader,
                        MqttMessageIdVariableHeader.from(unsubscribe.getPacketId()),
                        new MqttUnsubscribePayload(unsubscribe.getTopics()));
            case PUBLISH:
                Publish publish = (Publish) payload;
                return MqttMessageFactory.newMessage(fixedHeader,
                        (qos == MqttQoS.AT_MOST_ONCE) ?
                        		new MqttPublishVariableHeader(publish.getTopicName(), 0): 
                                new MqttPublishVariableHeader(publish.getTopicName(), publish.getPacketId()),
                        (publish.getPayload() != null && publish.getPayload().length > 0) ?
                                Unpooled.wrappedBuffer(publish.getPayload()) :
                                Unpooled.EMPTY_BUFFER);
            case UNSUBACK:
            case PUBACK:
            case PUBREC:
            case PUBREL:
            case PUBCOMP:
                PacketId packetId = (PacketId) payload;
                return MqttMessageFactory.newMessage(fixedHeader,
                        MqttMessageIdVariableHeader.from(packetId.getPacketId()),
                        null);
            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                return MqttMessageFactory.newMessage(fixedHeader,
                        null,
                        null);
            default:
                throw new IllegalStateException("unknown message type " + messageType);
        }
    }
}
