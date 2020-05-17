package com.lingfly.mqttclient.controller;

import com.lingfly.mqttclient.dao.FriendsMapper;
import com.lingfly.mqttclient.entity.Friends;
import com.lingfly.mqttclient.entity.FriendsExample;
import com.lingfly.mqttclient.service.LoginService;
import com.lingfly.mqttclient.service.constant.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private LoginService loginService;
    @RequestMapping("/home")
    public String home(){

        return "login";
    }

    @RequestMapping("/login")
    public ModelAndView login(@RequestParam(value = "username",required = true)String username,
                              @RequestParam(value = "password",required = true)String password){
        System.out.println(username+"  "+password+"用户登录");
        int retCode = loginService.login(username, password);
        if (retCode != Password.CORRECT_PASSWORD){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("login");
            modelAndView.addObject("username",username);
            modelAndView.addObject("return_code",retCode);
            return modelAndView;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("chat");
        modelAndView.addObject("username",username);


        return modelAndView;
    }


    //测试
    @Autowired
    private FriendsMapper friendsMapper;
    @RequestMapping("/test")
    public @ResponseBody
    String test(){
        FriendsExample friendsExample = new FriendsExample();
        FriendsExample.Criteria criteria = friendsExample.createCriteria();
        criteria.andNameEqualTo("P21614004");
        List<Friends> friends = friendsMapper.selectByExample(friendsExample);
        for (Friends f: friends){
            System.out.println(f.getName()+": "+f.getFriend());
        }
        return "123";
    }


}
