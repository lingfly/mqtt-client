package com.lingfly.mqttclient.websocket;

import com.google.gson.Gson;
import com.lingfly.mqttclient.dao.FriendsMapper;
import com.lingfly.mqttclient.entity.Friends;
import com.lingfly.mqttclient.entity.FriendsExample;
import com.lingfly.mqttclient.entity.Message;
import org.eclipse.paho.client.mqttv3.MqttClient;
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
public class Close {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static Gson gson = new Gson();

    @Autowired
    private FriendsMapper friendsMapper;
    public void noticeFriends(String username, Map<String, Session> nameToSession,
                               Map<String,List<String>> friendOnline){
        //查询已添加的好友
//        FriendsExample friendsExample = new FriendsExample();
//        FriendsExample.Criteria criteria = friendsExample.createCriteria();
//        criteria.andNameEqualTo(username);
//        List<Friends> friendsList = friendsMapper.selectByExample(friendsExample);
//        List<String> friends = toList(friendsList);

        List<String> flist = friendOnline.get(username);
        for (String f : flist){
            List<String> fflist = friendOnline.get(f);
            fflist.remove(username);
            Message message = new Message();
            message.setFriends(fflist);
            try {
                nameToSession.get(f).getBasicRemote().sendText(gson.toJson(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
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
