package com.socket.code;

import com.socket.packet.TransPacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TransPacketCode {
    /**
     * 编码
     * @return
     */
    public static byte[] encode(TransPacket transPacket){
        // 创建ByteBuf对象
        ByteBuffer byteBuf = ByteBuffer.allocate(64 + transPacket.getLen());
        byteBuf.order(ByteOrder.LITTLE_ENDIAN); //设置为小段模式
        // 按照通信协议填充ByteBUf
        byteBuf.putInt(transPacket.getMagic());// 魔数
        byteBuf.put(transPacket.getSerial_num());	// 写入serial
        byteBuf.putShort(transPacket.getSession_id());	// 写入sessionid
        byteBuf.putShort(transPacket.getToken()); //token
        byteBuf.putShort(transPacket.getSeq());//写入消息序号
        byteBuf.putShort(transPacket.getMessageType()); //消息类型
        byteBuf.putInt(transPacket.getLen()); //消息长度
        byteBuf.put(transPacket.getResv());//内容长度
//        if(transPacket.getLen() != 0)
//        byteBuf.put(transPacket.getBody());
        byte[] head = new byte[64];
        byteBuf.flip(); //切换读模式
        byteBuf.get(head);
        byteBuf.clear();
        byteBuf = null;
        if(transPacket.getLen() != 0){
            byte[] req = new byte[64 + transPacket.getLen()];
            System.arraycopy(head,0,req,0,64);
            System.arraycopy(transPacket.getBody(),0,req,64,transPacket.getLen());
            return req;
        }else{
            return head;
        }
    }

    /**
     * 解码
     * @return
     */
    public static TransPacket decode(byte[] bytes){
        ByteBuffer byteBuf = ByteBuffer.allocate(64);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.put(bytes);
        byteBuf.flip();
        byte[] serial_num = new byte[24];
        byte[] len = new byte[24];
        TransPacket packet = new TransPacket();
        packet.setMagic(byteBuf.getInt());  // 读取magic
        byteBuf.get(serial_num,0,24);
        packet.setSerial_num(serial_num); // 读取serila
        packet.setSession_id(byteBuf.getShort()); // 读取session
        packet.setToken(byteBuf.getShort()); //token
        packet.setSeq(byteBuf.getShort());	// 读取seq
        packet.setMessageType(byteBuf.getShort());	// 读取当前的消息类型
        packet.setLen(byteBuf.getInt());
        byteBuf.get(len);
        packet.setResv(len);
        return packet;
    }
}
