package com.github.longkerdandy.mithqtt.api.internal;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.Serializable;

/**
 * Contains a topic name and granted Qos Level.
 * This is part of the {@link Subscribe}
 */
public class TopicSubscription implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8798351166806964707L;
	private String topic;
    private MqttQoS grantedQos;

    protected TopicSubscription() {
    }

    public TopicSubscription(String topic, MqttQoS grantedQos) {
        this.topic = topic;
        this.grantedQos = grantedQos;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public MqttQoS getGrantedQos() {
        return grantedQos;
    }

    public void setGrantedQos(MqttQoS grantedQos) {
        this.grantedQos = grantedQos;
    }
}
