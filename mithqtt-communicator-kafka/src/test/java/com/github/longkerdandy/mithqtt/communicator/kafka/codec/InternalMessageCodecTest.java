package com.github.longkerdandy.mithqtt.communicator.kafka.codec;

import com.github.longkerdandy.mithqtt.api.internal.InternalMessage;
import com.github.longkerdandy.mithqtt.api.internal.SubAck;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * InternalMessageDecoder Test
 */
public class InternalMessageCodecTest {

    @Test
    @SuppressWarnings("unchecked")
    public void subAckTest() {
        List<MqttQoS> grantedQosLevels = new ArrayList<>();
        grantedQosLevels.add(MqttQoS.EXACTLY_ONCE);
        grantedQosLevels.add(MqttQoS.FAILURE);
        grantedQosLevels.add(MqttQoS.AT_LEAST_ONCE);
        SubAck subAck = new SubAck(10000, grantedQosLevels);
        InternalMessage<SubAck> msg = new InternalMessage<>(MqttMessageType.SUBACK, false, MqttQoS.AT_LEAST_ONCE, false,
                MqttVersion.MQTT_3_1_1, "Client_A", "User_A", "Broker_A", subAck);

        InternalMessageDeserializer decoder = new InternalMessageDeserializer();
        InternalMessageSerializer encoder = new InternalMessageSerializer();

        // encode
        byte[] bytes = encoder.serialize("topic", msg);
        assert bytes != null && bytes.length > 0;

        // decode
        msg = decoder.deserialize("topic", bytes);
        assert msg.getMessageType() == MqttMessageType.SUBACK;
        assert !msg.isDup();
        assert msg.getQos() == MqttQoS.AT_LEAST_ONCE;
        assert !msg.isRetain();
        assert msg.getVersion() == MqttVersion.MQTT_3_1_1;
        assert msg.getClientId().equals("Client_A");
        assert msg.getUserName().equals("User_A");
        assert msg.getBrokerId().equals("Broker_A");
        assert msg.getPayload().getPacketId() == 10000;
        assert msg.getPayload().getGrantedQoSLevels().size() == 3;
        assert msg.getPayload().getGrantedQoSLevels().get(0) == MqttQoS.EXACTLY_ONCE;
        assert msg.getPayload().getGrantedQoSLevels().get(1) == MqttQoS.FAILURE;
        assert msg.getPayload().getGrantedQoSLevels().get(2) == MqttQoS.AT_LEAST_ONCE;

        encoder.close();
        decoder.close();
    }
}
