package com.lingfly.mqttclient.websocket;


import com.google.gson.Gson;
import com.lingfly.mqttclient.dao.FriendsMapper;
import com.lingfly.mqttclient.entity.Content;
import com.lingfly.mqttclient.entity.Friends;
import com.lingfly.mqttclient.entity.Message;
import com.lingfly.mqttclient.util.MqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;


@ServerEndpoint(value = "/chatSocket") //接受websocket请求路径
@Component  //注册到spring容器中
public class MyWebSocket {
    private static ApplicationContext applicationContext;
    public static void setApplicationContext(ApplicationContext applicationContext) {
        MyWebSocket.applicationContext = applicationContext;
    }


    //记录当前在线数目
    private static int count=0;

    //当前连接（每个websocket连入都会创建一个MyWebSocket实例
    private Session session;

    private Logger log = LoggerFactory.getLogger(this.getClass());


    private static Gson gson = new Gson();

    private String username;

    //在线用户
    private static Set<String> online = new HashSet<>();

    public static Map<String,Session> nameToSession=new HashMap<>();

    private static Map<String,MqttClient> nameToClient = new HashMap<>();

    private static Map<String,List<String>> friendOnline = new HashMap<>();

    private static final String broker = "tcp://mqtt.p2hp.com:1883";
//    private static final String broker = "tcp://localhost:1883";
    //保存clientId
    private static Set<String> clientId = new HashSet<>();
    
    //处理时间
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @OnOpen
    public void onOpen(Session session){
        //1.处理ws
        String queryString = session.getQueryString();
        String  name_arg = queryString.split("=")[1];
        try {
            username = URLDecoder.decode(name_arg,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println(queryString);
        this.session=session;
        nameToSession.put(username,session);
        online.add(username);



        //2.处理mqtt
        MqttClient mqttClient = createClient();
        MqttMessageListener listener = applicationContext.getBean(MqttMessageListener.class);
        try {
            mqttClient.subscribe(username,1,listener);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        nameToClient.put(username,mqttClient);

        Message message = new Message();


        //3.上线后的处理
        Open open = applicationContext.getBean(Open.class);
        open.noticeFriends(username,nameToSession,online,friendOnline,nameToClient);
        open.unsentHandle(username,nameToSession,nameToClient);

        broadcast(gson.toJson(message));
        log.info("新的连接加入：{}",session.getId());
    }


    //接受消息
    @OnMessage
    public void onMessage(String msg,Session session) throws IOException {
        MsgUtil msgUtil = applicationContext.getBean(MsgUtil.class);
        Content content = gson.fromJson(msg,Content.class);
        Message message = new Message();
        switch (content.getType()){

            case 1://群聊
                message.setFromUser(this.username);
                message.setSendMsg(content.getMsg());
                message.setDate(new Date());
                broadcast(gson.toJson(message));
                break;
            case 2://单聊
                msgUtil.sendMsg(content,nameToSession,online,friendOnline,nameToClient);

                break;
            case 3://添加好友
                String friend = content.getFriend();
                msgUtil.addFriend(username,friend,nameToSession,online,friendOnline,nameToClient);
                break;
            case 4://删除好友
                String delfriend = content.getFriend();
                msgUtil.deleteFriend(username,delfriend,nameToSession,online,friendOnline,nameToClient);
                break;
        }
        log.info("收到客户端{}消息：{}",session.getId(),msg);
    }

    //处理错误
    @OnError
    public void onError(Throwable error,Session session){
        log.info("发生错误{},{}",session.getId(),error.getMessage());
    }

    //处理连接关闭
    @OnClose
    public void onClose(){


        //mqtt客户端关闭
        MqttClient client = nameToClient.get(username);
        clientId.remove(client.getClientId());
        nameToClient.remove(username);


        Close close = applicationContext.getBean(Close.class);
        close.noticeFriends(username,nameToSession,friendOnline);


        //在线用户清除
        online.remove(this.username);
        nameToSession.remove(username);

        try {
            client.disconnect();
            client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }

        reduceCount();
        log.info("连接关闭:{}",username);
    }

    //群发消息

    //发送消息
    public void sendMessage(String message) throws IOException {
        log.info("发送消息:{}",message);
        this.session.getBasicRemote().sendText(message);
    }

    //广播消息
    public static void broadcast(String msg){
        //TODO:
    }




    //创建MQTT客户端
    public MqttClient createClient(){
        MqttClient mqttClient = null;
        try {
            mqttClient = new MqttClient(broker,createClientId());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            //MQTT3.1.1
            connOpts.setMqttVersion(4);
            mqttClient.connect(connOpts);
            System.out.println("Connecting to broker: "+broker);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return mqttClient;
    }
    
    public static String createClientId(){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        String res;
        while (true){
            //clientid为长度为1-23字节的字符串
            int length=random.nextInt(23);
            for(int i=0;i<=length;i++){
                int number=random.nextInt(62);
                sb.append(str.charAt(number));
            }
            res = sb.toString();
            if (clientId.contains(res)){
                continue;
            }
            clientId.add(res);
            break;
        }
        return res;
    }

    //获取在线连接数目
    public static int getCount(){
        return count;
    }

    //操作count，使用synchronized确保线程安全
    public static synchronized void addCount(){
        MyWebSocket.count++;
    }

    public static synchronized void reduceCount(){
        MyWebSocket.count--;
    }
}