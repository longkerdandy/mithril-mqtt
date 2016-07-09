package com.github.longkerdandy.mithqtt.api.comm;

/**
 * Broker Listener Factory
 */
public interface BrokerListenerFactory {

    /**
     * Create a new BrokerListener
     *
     * @return BrokerListener
     */
    BrokerListener newListener();
}
