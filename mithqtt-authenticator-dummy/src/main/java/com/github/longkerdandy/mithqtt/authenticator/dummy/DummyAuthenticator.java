package com.github.longkerdandy.mithqtt.authenticator.dummy;

import com.github.longkerdandy.mithqtt.api.auth.AuthorizeResult;
import com.github.longkerdandy.mithqtt.api.auth.Authenticator;
import io.j1st.power.storage.mongo.MongoStorage;
import io.j1st.power.storage.mongo.entity.ProductStatus;
import io.netty.handler.codec.mqtt.MqttGrantedQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.apache.commons.configuration.AbstractConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy Authenticator
 * This authenticator basically authorize everything, it should only been used for test purpose
 */
@SuppressWarnings("unused")
public class DummyAuthenticator implements Authenticator {

    private boolean allowDollar;    // allow $ in topic
    private String deniedTopic;     // topic will be rejected


//    protected MongoStorage mongoStorage;

    @Override
    public void init(AbstractConfiguration config) {
        this.allowDollar = config.getBoolean("allowDollar", true);
        this.deniedTopic = config.getString("deniedTopic", null);
//        mongoStorage = new MongoStorage();
//        mongoStorage.init(config);

    }

    @Override
    public void destroy() {
    }

    @Override
    public AuthorizeResult authConnect(String clientId, String userName, String password) {
        //验证clentId是否有效
//        if(!mongoStorage.isAgentExists(clientId)) {
//            return AuthorizeResult.FORBIDDEN;
//        }
//        //验证用户名密码是否合法
//        if(!mongoStorage.isAgentAuth(userName, password)) {
//            return AuthorizeResult.FORBIDDEN;
//        }
//        //验证product状态是否正常
//        Integer status = this.mongoStorage.getProductStatusByAgentId(clientId);
//        if(status == null || !status.equals(ProductStatus.SERVICE.value())){
//            return AuthorizeResult.FORBIDDEN;
//        }
        return AuthorizeResult.OK;
    }

    @Override
    public AuthorizeResult authPublish(String clientId, String userName, String topicName, int qos, boolean retain) {
        if (!this.allowDollar && topicName.startsWith("$")) return AuthorizeResult.FORBIDDEN;
        if (topicName.equals(this.deniedTopic)) return AuthorizeResult.FORBIDDEN;
        return AuthorizeResult.OK;
    }

    @Override
    public List<MqttGrantedQoS> authSubscribe(String clientId, String userName, List<MqttTopicSubscription> requestSubscriptions) {
        List<MqttGrantedQoS> r = new ArrayList<>();
        requestSubscriptions.forEach(subscription -> {
            if (!this.allowDollar && subscription.topic().startsWith("$")) r.add(MqttGrantedQoS.FAILURE);
            if (subscription.topic().equals(this.deniedTopic)) r.add(MqttGrantedQoS.FAILURE);
            r.add(MqttGrantedQoS.valueOf(subscription.requestedQos().value()));
        });
        return r;
    }

    @Override
    public String oauth(String credentials) {
        return credentials;
    }
}
