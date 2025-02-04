package com.perry.core.consts;

/**
 * Created by dds on 2020/4/19.
 * ddssingsong@163.com
 */
public class Urls {

    //    private final static String IP = "192.168.2.111";
//    public final static String IP = "42.192.40.58:5000";
//    服务器测试地址
//    青岛测试地址
//    public final static String IP = "192.168.31.143:808";

    public final static String IP = "183.66.138.222:808";


    private final static String HOST = "http://" + IP + "/";

    // 信令地址http版本
    public final static String HTTP = "http://" + IP + "/data";

    public static String URL_HOST = HTTP;

    // 信令地址
    public final static String WS = "ws://" + IP + "/ws";

    // 获取用户列表
    public static String getUserList() {
        return HOST + "userList";
    }

    // 获取房间列表
    public static String getRoomList() {
        return HOST + "roomList";
    }
}
