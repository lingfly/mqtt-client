package com.lingfly.mapper;


import com.lingfly.mqttclient.MqttClientApplication;
import com.lingfly.mqttclient.dao.FriendsMapper;
import com.lingfly.mqttclient.entity.Friends;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = MqttClientApplication.class)
@RunWith(SpringRunner.class)
public class MapperTest {
    @Autowired
    private FriendsMapper friendsMapper;
    @Test
    public void test(){
        Friends friends=new Friends();
        friends.setName("a");
        friends.setName("b");
        friendsMapper.insert(friends);
    }
}
