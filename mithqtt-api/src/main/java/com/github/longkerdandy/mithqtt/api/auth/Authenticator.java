package com.github.longkerdandy.mithqtt.api.auth;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.apache.commons.configuration.AbstractConfiguration;

import java.util.List;

/**
 * Authenticator
 */
public interface Authenticator {

    /**
     * Init the authenticator
     *
     * @param config Authenticator Configuration
     */
    void init(AbstractConfiguration config);

    /**
     * Destroy the authenticator
     */
    void destroy();

    /**
     * Authorize client CONNECT
     *
     * @param clientId Client Id
     * @param userName User Name
     * @param password Password
     * @return Authorize Result
     */
    AuthorizeResult authConnect(String clientId, String userName, String password);

    /**
     * Authorize client PUBLISH
     *
     * @param clientId  Client Id
     * @param userName  User Name
     * @param topicName Topic Name
     * @param qos       QoS
     * @param retain    Retain
     * @return Authorize Result
     */
    AuthorizeResult authPublish(String clientId, String userName, String topicName, int qos, boolean retain);

    /**
     * Authorize client SUBSCRIBE
     *
     * @param clientId             Client Id
     * @param userName             User Name
     * @param requestSubscriptions List of request Topic Subscription
     * @return List of granted QoS
     */
    List<MqttQoS> authSubscribe(String clientId, String userName, List<MqttTopicSubscription> requestSubscriptions);

}
