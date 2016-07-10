package com.github.longkerdandy.mithqtt.api.internal;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.Serializable;
import java.util.List;

/**
 * Represent MQTT SUBACK Message's VariableHeader and Payload
 */
public class SubAck implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -6163088813268061614L;
	private int packetId;
    private List<MqttQoS> grantedQoSLevels;

    protected SubAck() {
    }

    public SubAck(int packetId, List<MqttQoS> grantedQoSLevels) {
        this.packetId = packetId;
        this.grantedQoSLevels = grantedQoSLevels;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public List<MqttQoS> getGrantedQoSLevels() {
        return grantedQoSLevels;
    }

    public void setGrantedQoSLevels(List<MqttQoS> grantedQoSLevels) {
        this.grantedQoSLevels = grantedQoSLevels;
    }
}
