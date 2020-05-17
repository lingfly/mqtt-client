package com.lingfly.mqttclient;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.logging.Logger;

public class MqttPublishSample {
    static final Class<?> cclass = MqttPublishSample.class;
    static final String className = cclass.getName();
    static final Logger log = Logger.getLogger(className);
    String topic        = "MQTT Examples";
    String content      = "Message from MqttPublishSample";
    int qos             = 2;
    String broker       = "tcp://mqtt.p2hp.com:1883";
    String clientId     = "subscribe";
    @Test
    public void testSyncSubs1() throws Exception {

        MqttClient mqttClient = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        System.out.println("Connecting to broker: "+broker);
        mqttClient.connect(connOpts);
        System.out.println("Connected");

        MqttMessageListener listener = new MqttMessageListener();
        mqttClient.subscribe(topic, 2, listener);

        MqttMessage message = new MqttMessage(content.getBytes());
        message.setPayload(content.getBytes());
        mqttClient.publish(topic, message);

        log.info("Checking msg");
        MqttMessage msg = listener.getNextMessage();
        System.out.println(msg);

        mqttClient.disconnect();

        mqttClient.close();


    }
}
