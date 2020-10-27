package com.socket.server;

import com.socket.code.TransPacketCode;
import com.socket.packet.TransPacket;
import com.socket.utils.Command;
import com.socket.utils.UtilsTool;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IServerSocket {
    private static Logger logger = Logger.getLogger(IServerSocket.class);
    private static Map<String, List<MyRuns>> map = new ConcurrentHashMap<String,List<MyRuns>>();
    private static List<MyRuns> list = Collections.synchronizedList(new ArrayList<MyRuns>()); //线程安全的list
    public static void main(String[] args) {
//        final int port = Integer.parseInt(args[0]);
        final int port = 8000;
        new Thread(new Runnable() {
            public void run() {
                IServerSocket iServerSocket = new IServerSocket();
                iServerSocket.startAction(port);
            }
        }).start();
        logger.info("Scheduled opening duty to check is socket timeout");
        ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(2);
        MyTask myTask = new MyTask();
        scheduled.scheduleAtFixedRate(myTask, 4000, 10000, TimeUnit.MILLISECONDS);//4s表示首次执行任务的延迟时间，10s表示每次执行任务的间隔时间，TimeUnit.MILLISECONDS执行的时间间隔数值单位

    }

    public void startAction(int port){
        ServerSocket serverSocket=null;
        try {
            serverSocket=new ServerSocket(port);  //端口号

            logger.info("server open and listen:");
            //通过死循环开启长连接，开启线程去处理消息
            while(true){
                Socket socket=serverSocket.accept();
                MyRuns myRuns = new MyRuns(socket);
                if(list == null){
                    list = Collections.synchronizedList(new ArrayList<MyRuns>());
                }
                list.add(myRuns);
                new Thread(myRuns).start();
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

    static class MyTask implements Runnable{

        public void run() {
            Iterator<MyRuns> it = list.iterator();
            while(it.hasNext()){
                MyRuns myRuns = it.next();
                Date date = new Date();
                if((date.getTime() - myRuns.getDate().getTime()) / 1000 > 35){
                    //删除map中的myruls
                    Collection values = map.values();    //获取Map集合的value集合
                    for (Object object : values) {
                        List<MyRuns> myRunsList = (List) object;
                        Iterator<MyRuns> ita = myRunsList.iterator();
                        while(ita.hasNext()){
                            MyRuns myRunsb = ita.next();
                            if(myRunsb == myRuns){
                                myRuns.close();
                                ita.remove();
                            }
                        }

                    }
                    //删除list中的myruls
                    it.remove();
                    logger.info("remove timeout socket");
                }
            }
        }
    }

    class MyRuns implements Runnable{
        Socket socket;
        InputStream reader;
        OutputStream writer;
        Date date;
        public MyRuns(Socket socket) {
            super();
            this.socket = socket;
        }

        public byte[] recBody(int len){
            logger.info("socket : " +socket.getPort() +",recv length - > " + len);
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
                        off = off + readLength;
                    } else{ //得到完整的包头
                        TransPacket packet = TransPacketCode.decode(headbytes);//解头部信息
                        if(packet.getMagic() != UtilsTool.getTunnelMagic()){
                            System.out.println(packet);
                            len = 64;
                            headbytes = new byte[len];
                            off = 0;
                            continue;
                        }
                        date = new Date();
                        if(packet.getMessageType() == Command.TUNNEL_TYPE_REG){
                            //注册
                            logger.info("register success ,session_id"+packet.getSession_id());
                            logger.info("online socket is " + list.size());
                            short session = packet.getSession_id();
                            if (map == null){
                                map = new ConcurrentHashMap<String,List<MyRuns>>();
                            }
                            if(map.containsKey(String.valueOf(session))){
                                List<MyRuns> list = map.get(String.valueOf(session));
                                int ok = 0;
                                for (int i = 0; i < list.size(); i++) {
                                    if(list.get(i) == this){ //判断是否是重复注册
                                        ok = 1;
                                    }
                                }
                                if(ok == 0){
                                    list.add(this);
                                }
                            }else{
                                List<MyRuns> list = Collections.synchronizedList(new ArrayList<MyRuns>());
                                list.add(this);
                                map.put(String.valueOf(session),list);
                            }
                            writer.write(headbytes);
                        } else if(packet.getMessageType() == Command.TUNNEL_TYPE_DATA){
                            //接收内容body
                            packet.setBody(recBody(packet.getLen()));
                            //转发信息
                            List<MyRuns> list = map.get(String.valueOf(packet.getSession_id()));
                            if(list != null){
                                byte[] b = TransPacketCode.encode(packet);
                                for (int i = 0; i < list.size(); i++) {
                                    if(list.get(i) != this){
                                        if(list.get(i).writer == null){
                                            logger.error("socket is close, write error");
                                        }else{
                                            logger.info("send to sessionId -> " + packet.getSession_id());
                                            list.get(i).writer.write(b);//转发
                                        }
                                    }
                                }
                            }else{
                                logger.warn("not find this session :" + packet.getSession_id());
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
                    //删除map中的socket
                    Set<String> keySet = map.keySet();
                    for (String key : keySet){
                        List<MyRuns> list = map.get(key);
                        Iterator<MyRuns> it = list.iterator();
                        while(it.hasNext()){
                            MyRuns x = it.next();
                            if(x == this){
                                it.remove();
                            }
                        }
                    }
                    //删除list
                    Iterator<MyRuns> it = list.iterator();
                    while(it.hasNext()){
                        MyRuns x = it.next();
                        if(x == this){
                            it.remove();
                        }
                    }
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
        public void close(){
            try {
                //删除map中的socket
                Set<String> keySet = map.keySet();
                for (String key : keySet){
                    List<MyRuns> list = map.get(key);
                    Iterator<MyRuns> it = list.iterator();
                    while(it.hasNext()){
                        MyRuns x = it.next();
                        if(x == this){
                            it.remove();
                        }
                    }
                }
                Iterator<MyRuns> it = list.iterator();
                while(it.hasNext()){
                    MyRuns x = it.next();
                    if(x == this){
                        it.remove();
                    }
                }
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

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
