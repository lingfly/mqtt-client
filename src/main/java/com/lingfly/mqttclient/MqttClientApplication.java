package com.lingfly.mqttclient;

import com.lingfly.mqttclient.websocket.MyWebSocket;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(value = "com.lingfly.mqttclient.dao")
public class MqttClientApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(MqttClientApplication.class);
        ConfigurableApplicationContext configurableApplicationContext = springApplication.run(args);
        //解决WebSocket不能注入的问题
        MyWebSocket.setApplicationContext(configurableApplicationContext);
    }

}
