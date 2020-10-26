package com.socket.client;

import com.socket.code.TransPacketCode;
import com.socket.packet.TransPacket;
import com.socket.utils.Command;

import java.io.*;
import java.net.Socket;

public class IClientSocket {
    public static void main(String[] args) {
        IClientSocket client=new IClientSocket();
        client.startAction();
    }

    void readSocketInfo(InputStream reader){
        new Thread(new MyRuns(reader)).start();
    }

    class MyRuns implements Runnable{

        InputStream reader;
        int num = 0;
        public MyRuns(InputStream reader) {
            super();
            this.reader = reader;
        }

        public byte[] recBody(int len){
            num++;
            byte[] bytes = new byte[len];
            int length = len;
            int off = 0;
            System.out.println(num+"->"+len);
            while(length > 0){
                try {
                    int readLength = reader.read(bytes,off,length);
                    if(readLength == -1){
                        System.out.println("读取失败");
                    }
                    if(readLength < length){
                        off = off +  readLength;
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
                System.out.println("-------");
                int len = 64;
                byte[] headbytes = new byte[len];
                int off = 0;
                while (true){
                    //读取包头
                    int readLength = reader.read(headbytes,off,len);
                    if (readLength < len){
                        off = readLength;
                        len = len - readLength;
                    } else{
                        TransPacket packet = TransPacketCode.decode(headbytes);
                        //获取内容body
                        packet.setBody(recBody(packet.getLen()));
                        if(packet.getMessageType() == Command.TUNNEL_TYPE_DATA){
                            FileOutputStream out = null;
                            try {
                                out = new FileOutputStream("F:\\BaiduNetdiskDownload\\b.zip",true);
                                out.write(packet.getBody(),0,packet.getLen());
                                out.flush();
                                out.close();
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }finally{
                                try {
                                    if(out!=null) {
                                        out.close();
                                    }
                                }catch(IOException ex){

                                }
                            }
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void startAction(){
        Socket socket=null;
        BufferedReader reader=null;
        OutputStream writer=null;
        InputStream reader2=null;
        try {
            socket=new Socket("127.0.0.1", 8000);
            reader = new BufferedReader(new InputStreamReader(System.in));
            reader2=socket.getInputStream();
            writer = socket.getOutputStream();
            readSocketInfo(reader2);
            String lineString="";
            while(!(lineString=reader.readLine()).equals("exit")){
                if(lineString.indexOf("send") != -1){
                    TransPacket packet = new TransPacket();
                    packet.setMessageType(Command.TUNNEL_TYPE_REG);
                    packet.setSession_id((short) 1234);
                    byte[] bytes1 = packet.getSerial_num();
                    bytes1[0] = (byte)0x10;
                    bytes1[1] = (byte)0x20;
                    packet.setSerial_num(bytes1);
//                    String body = "发送信息";
//                    byte[] content1 = body.getBytes();
//                    packet.setLen(content1.length);
//                    packet.setBody(content1);
                    packet.setLen(0);
                    byte[] req = TransPacketCode.encode(packet); //编码
                    writer.write(req);
                }else if(lineString.indexOf("trans") != -1){
                    TransPacket packet = new TransPacket();
                    packet.setMessageType(Command.TUNNEL_TYPE_DATA);
                    packet.setSession_id((short) 1234);
                    byte[] bytes1 = packet.getSerial_num();
                    bytes1[0] = (byte)0x05;
                    bytes1[1] = (byte)0x0b;
                    packet.setSerial_num(bytes1);
                    String value = "你好";
                    byte[] vale = value.getBytes();
                    packet.setBody(vale);
                    packet.setLen(vale.length);
                    byte[] req = TransPacketCode.encode(packet);
                    writer.write(req);
//                    File file = new File("F:\\BaiduNetdiskDownload\\Rational Rose 破解版.zip");
//                    InputStream inputStream = new FileInputStream(file);
//                    int num = 0;
//                    Random random = new Random();
//                    do{
//                        num++;
//                        int r = random.nextInt(64) + 1;
//                        byte[] readBytes = new byte[1024 * r];
//                        int length = inputStream.read(readBytes);
//                        if(length == -1){
//                            System.out.println("读取完毕");
//                            break;
//                        }
//                        packet.setLen(length);
//                        packet.setBody(readBytes);
//                        byte[] req = TransPacketCode.encode(packet); //编码
//                        writer.write(req);
//                    }while (true);
//                    inputStream.close();
                } else{
                    writer.write(lineString.getBytes());
                    writer.flush();
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
