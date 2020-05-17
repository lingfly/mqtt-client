package com.lingfly.mqttclient.service;

import com.lingfly.mqttclient.dao.UserInfoMapper;
import com.lingfly.mqttclient.entity.UserInfo;
import com.lingfly.mqttclient.entity.UserInfoExample;
import com.lingfly.mqttclient.service.constant.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    public int login(String username, String password){
        if (password.length()==0){
            return Password.NULL_PASSWORD;
        }
        UserInfoExample userInfoExample = new UserInfoExample();
        UserInfoExample.Criteria criteria = userInfoExample.createCriteria();
        criteria.andUsernameEqualTo(username);
        List<UserInfo>  users = userInfoMapper.selectByExample(userInfoExample);

        if (users.size()==0){
            UserInfo userInfo = new UserInfo();
            userInfo.setUsername(username);
            userInfo.setPassword(password);
            userInfoMapper.insertSelective(userInfo);
            return Password.CORRECT_PASSWORD;
        }

        UserInfo userInfo = users.get(0);
        if (userInfo.getPassword().equals(password)){
            return Password.CORRECT_PASSWORD;
        }
        return Password.ERROR_PASSWORD;

    }
}
