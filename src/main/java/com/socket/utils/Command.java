package com.socket.utils;

public interface Command {
    byte TUNNEL_TYPE_REG = 0;              //客户端注册到服务器
    byte TUNNEL_TYPE_REMOTE_CONNECT = 1;   //服务端返回当前连接客户端ID
    byte TUNNEL_TYPE_REMOTE_DISCONNECT = 2;//服务端返回当前断开客户端的ID
    byte TUNNEL_TYPE_CLOSE_DATA = ((byte)0x11); //消息关闭
    byte TUNNEL_TYPE_DATA = ((byte)0x10);           //消息 传输
}
