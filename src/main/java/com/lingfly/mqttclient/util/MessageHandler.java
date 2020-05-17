package com.lingfly.mqttclient.util;

import com.google.gson.Gson;
import com.lingfly.mqttclient.entity.Message;
import com.lingfly.mqttclient.websocket.MyWebSocket;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Component
public class MessageHandler {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Map<String, Session> nameToSession = MyWebSocket.nameToSession;

    private static Gson gson = new Gson();


    public void msgForward(String topic, MqttMessage message){
        Session toSession = nameToSession.get(topic);
        if (toSession == null){//不在线
            //TODO:不考虑并发的话，能收到消息则客户端一定在线
            log.info("Mqtt Client 接收信息错误");
        }
        else {
            //将消息向前端转发
            byte[] payload = message.getPayload();
            String json = new String(payload);
            Message msg = gson.fromJson(json,Message.class);
            try {
                toSession.getBasicRemote().sendText(gson.toJson(msg));
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("来自{}的消息：{}，转发到{}",msg.getFromUser(),msg.getSendMsg(),msg.getToUser());
        }
    }
}
