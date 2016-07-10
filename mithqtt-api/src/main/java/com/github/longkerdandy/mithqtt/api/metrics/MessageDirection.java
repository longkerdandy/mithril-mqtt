package com.github.longkerdandy.mithqtt.api.metrics;

/**
 * Message Direction
 */
public enum MessageDirection {
    IN,
    OUT;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
