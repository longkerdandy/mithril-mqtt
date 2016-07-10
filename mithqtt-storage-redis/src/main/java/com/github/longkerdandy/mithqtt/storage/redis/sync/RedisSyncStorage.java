package com.github.longkerdandy.mithqtt.storage.redis.sync;

import com.github.longkerdandy.mithqtt.api.storage.sync.SyncStorage;
import com.github.longkerdandy.mithqtt.api.internal.InternalMessage;
import com.github.longkerdandy.mithqtt.api.internal.Publish;
import com.lambdaworks.redis.ValueScanCursor;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.configuration.AbstractConfiguration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Redis Synchronized Storage
 */
@SuppressWarnings("unused")
public interface RedisSyncStorage extends SyncStorage {
}
