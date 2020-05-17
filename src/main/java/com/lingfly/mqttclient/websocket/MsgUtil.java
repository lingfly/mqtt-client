package com.lingfly.mqttclient.websocket;

import com.google.gson.Gson;
import com.lingfly.mqttclient.dao.FriendsMapper;
import com.lingfly.mqttclient.dao.MessageMapper;
import com.lingfly.mqttclient.entity.Content;
import com.lingfly.mqttclient.entity.Friends;
import com.lingfly.mqttclient.entity.FriendsExample;
import com.lingfly.mqttclient.entity.Message;
import com.lingfly.mqttclient.util.MqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.websocket.Session;
import java.io.IOException;
import java.util.*;

@Component
public class MsgUtil {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FriendsMapper friendsMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MqttMessageListener listener;

    private static Gson gson = new Gson();

    //添加好友
    public void addFriend(String src,String dest, Map<String,Session> nameToSession,
                          Set<String> online, Map<String,List<String>> friendOnline,
                          Map<String,MqttClient> nameToClient) {
        //1.查询指定好友
        FriendsExample friendsExample = new FriendsExample();
        FriendsExample.Criteria criteria = friendsExample.createCriteria();
        criteria.andNameEqualTo(src);
        criteria.andFriendEqualTo(dest);
        List<Friends> friendsList = friendsMapper.selectByExample(friendsExample);


        if (friendsList.size() == 1) {//好友已添加
            Message response = new Message();
            response.setFriend("finished");
            try {
                nameToSession.get(src).getBasicRemote().sendText(gson.toJson(response));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        //2.如果未添加
        //写入数据库
        friendsMapper.insert(new Friends(src, dest));
        friendsMapper.insert(new Friends(dest, src));


        Message message = new Message();
        message.setFromUser(src);
        message.setToUser(dest);
        message.setDate(new Date());
        message.setSendMsg(src + "和你成为朋友了");

        Message response = new Message();
        response.setFriend("success");

        //查询已添加的好友
        friendsExample = new FriendsExample();
        criteria = friendsExample.createCriteria();
        criteria.andNameEqualTo(src);
        friendsList = friendsMapper.selectByExample(friendsExample);

        List<String> allFriend = toList(friendsList);
        response.setAllFriend(allFriend);

        //更新状态
        if (!online.contains(dest)) {//对方不在线
            try {
                //在线好友不变，总好友+1
                nameToSession.get(src).getBasicRemote().sendText(gson.toJson(response));

                //待发送的消息
                message.setIsSend(0);
                messageMapper.insertSelective(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        //双方在线
        MqttClient srcClient = nameToClient.get(src);

        try {
            //给对方发送添加好友消息
            message.setIsSend(1);
            messageMapper.insertSelective(message);
            srcClient.publish(dest, new MqttMessage(gson.toJson(message).getBytes()));

            List<String> srcFriend = friendOnline.get(src);
            if (srcFriend == null) {
                srcFriend = new ArrayList<>();
                srcFriend.add(dest);
                friendOnline.put(src, srcFriend);
            } else if (!srcFriend.contains(dest)) {
                srcFriend.add(dest);
            }
            //对方在线，在线好友+1，总好友+1
            Session srcSession = nameToSession.get(src);
            response.setFriends(srcFriend);
            srcSession.getBasicRemote().sendText(gson.toJson(response));


            //对被添加方的更新
            Message destResponse = new Message();
            friendsExample = new FriendsExample();
            criteria = friendsExample.createCriteria();
            criteria.andNameEqualTo(dest);
            friendsList = friendsMapper.selectByExample(friendsExample);

            allFriend = toList(friendsList);
            destResponse.setAllFriend(allFriend);

            List<String> destFriend = friendOnline.get(dest);
            if (destFriend == null) {
                destFriend = new ArrayList<>();
                destFriend.add(src);
                friendOnline.put(dest, destFriend);
            } else if (!destFriend.contains(src)) {
                destFriend.add(src);
            }
            destResponse.setFriends(destFriend);
            Session destSession = nameToSession.get(dest);
            destSession.getBasicRemote().sendText(gson.toJson(destResponse));
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //发送消息
    public void sendMsg(Content content,Map<String,Session> nameToSession,
                        Set<String> online, Map<String,List<String>> friendOnline,
                        Map<String,MqttClient> nameToClient){
        String from = content.getFromUser();
        String to = content.getToUser();
        Message message = new Message();
        message.setFromUser(from);
        message.setToUser(to);
        message.setSendMsg(content.getMsg());
        message.setDate(new Date());

        Message response = new Message();
        response.setSendMsg(content.getMsg());
        response.setDate(new Date());
        response.setFromUser(from);

        if (!nameToSession.containsKey(to)){//不在线
            message.setIsSend(0);
            messageMapper.insertSelective(message);
        }
        else {//在线
            MqttClient client = nameToClient.get(from);
            message.setIsSend(1);
            messageMapper.insertSelective(message);
            try {
                client.publish(to,new MqttMessage(gson.toJson(message).getBytes()));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        try {//向前端响应
            nameToSession.get(from).getBasicRemote().sendText(gson.toJson(response));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void deleteFriend(String src,String dest, Map<String,Session> nameToSession,
                             Set<String> online, Map<String,List<String>> friendOnline,
                             Map<String,MqttClient> nameToClient){
        //1.查询指定好友
        FriendsExample friendsExample = new FriendsExample();
        FriendsExample.Criteria criteria = friendsExample.createCriteria();
        criteria.andNameEqualTo(src);
        criteria.andFriendEqualTo(dest);
        List<Friends> friendsList = friendsMapper.selectByExample(friendsExample);

        Message response = new Message();


        if (friendsList.size() != 1){//没有这个好友
            response.setFriend("delfinished");
            try {
                nameToSession.get(src).getBasicRemote().sendText(gson.toJson(response));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        //删除好友
        friendsExample = new FriendsExample();
        FriendsExample.Criteria criteria1 = friendsExample.createCriteria();
        criteria1.andNameEqualTo(src);
        criteria1.andFriendEqualTo(dest);
        FriendsExample.Criteria criteria2 = friendsExample.createCriteria();
        criteria2.andNameEqualTo(dest);
        criteria2.andFriendEqualTo(src);
        friendsExample.or(criteria1);
        friendsExample.or(criteria2);
        friendsMapper.deleteByExample(friendsExample);

        if (nameToSession.get(dest)!=null){//对方在线
            //通知对方
            Session destSession = nameToSession.get(dest);
            FriendsExample destExample = new FriendsExample();
            FriendsExample.Criteria destCriteria = destExample.createCriteria();
            destCriteria.andNameEqualTo(dest);

            List<Friends> destFriends = friendsMapper.selectByExample(destExample);
            List<String> destAllFriend = toList(destFriends);
            List<String> destOnlineFriend = friendOnline.get(dest);
            destOnlineFriend.remove(src);
            friendOnline.put(dest,destOnlineFriend);
            Message message = new Message();
            message.setAllFriend(destAllFriend);
            message.setFriends(destOnlineFriend);
            try {
                destSession.getBasicRemote().sendText(gson.toJson(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //己方反馈
        Session srcSession = nameToSession.get(src);
        FriendsExample srcExample = new FriendsExample();
        FriendsExample.Criteria srcCriteria = srcExample.createCriteria();
        srcCriteria.andNameEqualTo(src);

        List<Friends> srcFriends = friendsMapper.selectByExample(srcExample);
        List<String> srcAllFriend = toList(srcFriends);
        List<String> srcOnlineFriend = friendOnline.get(src);
        srcOnlineFriend.remove(dest);
        friendOnline.put(src,srcOnlineFriend);
        response.setAllFriend(srcAllFriend);
        response.setFriends(srcOnlineFriend);
        response.setFriend("delsuccess");
        try {
            srcSession.getBasicRemote().sendText(gson.toJson(response));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> toList(List<Friends> friendsList){
        List<String> list = new ArrayList<>();
        for (Friends f : friendsList){
            list.add(f.getFriend());
        }
        return list;
    }
}
