package com.socket.server;

import com.socket.code.TransPacketCode;
import com.socket.packet.TransPacket;
import com.socket.utils.Command;
import com.socket.utils.UtilsTool;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class IServerSocket {

    private Map<String, List<MyRuns>> map;
    public static void main(String[] args) {
        IServerSocket iServerSocket = new IServerSocket();
        iServerSocket.startAction(8000);
    }

    public void startAction(int port){
        ServerSocket serverSocket=null;
        try {
            serverSocket=new ServerSocket(port);  //端口号
            System.out.println("服务端服务启动监听：");
            //通过死循环开启长连接，开启线程去处理消息
            while(true){
                Socket socket=serverSocket.accept();
                new Thread(new MyRuns(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket!=null) {
                    serverSocket.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    class MyRuns implements Runnable{
        Socket socket;
        InputStream reader;
        OutputStream writer;
        int num = 0;
        public MyRuns(Socket socket) {
            super();
            this.socket = socket;
        }

        public byte[] recBody(int len){
            num++;
            System.out.println(num + " -> length - > " + len);
            byte[] bytes = new byte[len];
            int length = len;
            int off = 0;
            while(length > 0){
                try {
                    int readLength = reader.read(bytes,off,length);
                    if(readLength < length){
                        off = off + readLength;
                    }
                    length = length - readLength;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return bytes;
        }

        public void run() {
            try {
                reader = socket.getInputStream();//读取客户端消息
                writer = socket.getOutputStream();//向客户端写消息
                int len = 64;
                byte[] headbytes = new byte[len];
                int off = 0;
                while(true){
                    int readLength = reader.read(headbytes,off,len);
                    if(readLength < len){
                        len = len - readLength;
                        off = readLength;
                    } else{ //得到完整的包头
                        TransPacket packet = TransPacketCode.decode(headbytes);//解头部信息
                        System.out.println(packet);
                        if(packet.getMagic() != UtilsTool.getTunnelMagic()){
                            System.out.println(packet);
                            continue;
                        }
                        if(packet.getMessageType() == Command.TUNNEL_TYPE_REG){
                            //注册
                            short session = packet.getSession_id();
                            if (map == null){
                                map = new HashMap<String, List<MyRuns>>();
                            }
                            if(map.containsKey(String.valueOf(session))){
                                List<MyRuns> list = map.get(String.valueOf(session));
                                list.add(this);
                            }else{
                                List<MyRuns> list = new ArrayList<MyRuns>();
                                list.add(this);
                                map.put(String.valueOf(session),list);
                            }
                        } else if(packet.getMessageType() == Command.TUNNEL_TYPE_DATA){
                            //接收内容body
                            packet.setBody(recBody(packet.getLen()));
                            //转发信息
                            Set<String> keySet = map.keySet();
                            for (String key : keySet){
                                if(key.equals(String.valueOf(packet.getSession_id()))){
                                    List<MyRuns> list = map.get(key);
                                    for (int i = 0; i < list.size(); i++) {
                                        if(list.get(i) != this){
                                            byte[] b = TransPacketCode.encode(packet);
                                            list.get(i).writer.write(b);//转发
                                        }
                                    }
                                }
                            }
                        }
                        len = 64;
                        headbytes = new byte[len];
                        off = 0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader!=null) {
                        reader.close();
                    }
                    if (writer!=null) {
                        writer.close();
                    }
                    if (socket!=null) {
                        socket.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

    }
}
