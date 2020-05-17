package com.lingfly.mqttclient.websocket;

import com.google.gson.Gson;
import com.lingfly.mqttclient.dao.FriendsMapper;
import com.lingfly.mqttclient.dao.MessageMapper;
import com.lingfly.mqttclient.entity.Friends;
import com.lingfly.mqttclient.entity.FriendsExample;
import com.lingfly.mqttclient.entity.Message;
import com.lingfly.mqttclient.entity.MessageExample;
import com.lingfly.mqttclient.util.MqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class Open {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FriendsMapper friendsMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MqttMessageListener listener;
    private static Gson gson = new Gson();

    public void noticeFriends(String username, Map<String,Session> nameToSession,
                              Set<String> online, Map<String,List<String>> friendOnline,
                              Map<String, MqttClient> nameToClient){


        //查询已添加的好友
        FriendsExample friendsExample = new FriendsExample();
        FriendsExample.Criteria criteria = friendsExample.createCriteria();
        criteria.andNameEqualTo(username);
        List<Friends> friends = friendsMapper.selectByExample(friendsExample);

        Message response = new Message();

        List<String> res = new ArrayList<>();
        for (Friends f : friends){
            if (online.contains(f.getFriend())){
                res.add(f.getFriend());
                List<String> flist=friendOnline.get(f.getFriend());;
                if (flist==null){
                    flist = new ArrayList<>();
                }
                flist.add(username);
                friendOnline.put(f.getFriend(),flist);
                Message notice = new Message();
                notice.setFriends(flist);

                friendsExample = new FriendsExample();
                criteria = friendsExample.createCriteria();
                criteria.andNameEqualTo(f.getFriend());
                List<Friends> allFriend = friendsMapper.selectByExample(friendsExample);
                notice.setAllFriend(toList(allFriend));
                try {
                    nameToSession.get(f.getFriend()).getBasicRemote().sendText(gson.toJson(notice));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        response.setAllFriend(toList(friends));
        response.setFriends(res);
        friendOnline.put(username,res);
        try {
            nameToSession.get(username).getBasicRemote().sendText(gson.toJson(response));
            log.info("通知好友上线");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unsentHandle(String username, Map<String,Session> nameToSession,
                             Map<String, MqttClient> nameToClient){
        //查询user未接收的消息
        MessageExample messageExample = new MessageExample();
        MessageExample.Criteria criteria = messageExample.createCriteria();
        criteria.andIsSendEqualTo(0);
        criteria.andToUserEqualTo(username);
        List<Message> messageList = messageMapper.selectByExample(messageExample);

        MessageExample updateExample = new MessageExample();
        MessageExample.Criteria updateCriteria = messageExample.createCriteria();
        MqttClient client = nameToClient.get(username);
        for (Message msg : messageList){
            msg.setIsSend(1);
            updateCriteria.andMsgIdEqualTo(msg.getMsgId());
            msg.setMsgId(null);
            messageMapper.updateByExampleSelective(msg,updateExample);
            try {
                client.publish(msg.getToUser(),new MqttMessage(gson.toJson(msg).getBytes()));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }


    }


    List<String> toList(List<Friends> friendsList){
        List<String> list = new ArrayList<>();
        for (Friends f : friendsList){
            list.add(f.getFriend());
        }
        return list;
    }
}
