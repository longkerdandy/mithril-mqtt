package com.github.longkerdandy.mithqtt.api.internal;

import java.io.Serializable;

/**
 * Represent MQTT Message's VariableHeader which only contains Packet Id
 */
public class PacketId implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5933061800555550532L;
	private int packetId;

    protected PacketId() {
    }

    public PacketId(int packetId) {
        this.packetId = packetId;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }
}
