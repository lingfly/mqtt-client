package com.lingfly.mqttclient.util;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.logging.Logger;

@Component
public class MqttMessageListener implements IMqttMessageListener {
    static final Class<?> cclass = MqttMessageListener.class;
    static final String className = cclass.getName();
    static final Logger log = Logger.getLogger(className);
    final ArrayList<MqttMessage> messages;
    @Autowired
    private MessageHandler messageHandler;

    public MqttMessageListener() {
        messages = new ArrayList<MqttMessage>();
    }

    public MqttMessage getNextMessage() {
        synchronized (messages) {
            if (messages.size() == 0) {
                try {
                    messages.wait(1000);
                }
                catch (InterruptedException e) {
                    // empty
                }
            }

            if (messages.size() == 0) {
                return null;
            }
            return messages.remove(0);
        }
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {

        log.info("message arrived: '"+"topic: "+ topic + new String(message.getPayload()) + "' "+this.hashCode()+
                " " + (message.isDuplicate() ? "duplicate" : ""));

        if (!message.isDuplicate()) {
            synchronized (messages) {
                messages.add(message);
                messages.notifyAll();
            }
            messageHandler.msgForward(topic,message);

        }

    }
}