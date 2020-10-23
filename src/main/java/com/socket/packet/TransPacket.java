package com.socket.packet;

import com.socket.utils.UtilsTool;

import java.util.Arrays;

public class TransPacket {
    private int magic;      //TUNNEL_MAGIC 4
    private byte[] serial_num; //设备序列号 24
    private short session_id; //连接session 2
    private short token;      //token 2
    private short seq;           //消息序号 2
    private short messageType;  //消息类型 2
    private int len;          //消息长度 4
    private byte[] resv;       //保留 24

    private byte[] body;       //内容

    public TransPacket() {
        magic = UtilsTool.getTunnelMagic();
        seq = 0;
        token = 0;
        serial_num = new byte[24];
        resv = new byte[24];
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    public byte[] getSerial_num() {
        return serial_num;
    }

    public void setSerial_num(byte[] serial_num) {
        this.serial_num = serial_num;
    }

    public short getSession_id() {
        return session_id;
    }

    public void setSession_id(short session_id) {
        this.session_id = session_id;
    }

    public short getToken() {
        return token;
    }

    public void setToken(short token) {
        this.token = token;
    }

    public short getSeq() {
        return seq;
    }

    public void setSeq(short seq) {
        this.seq = seq;
    }

    public short getMessageType() {
        return messageType;
    }

    public void setMessageType(short messageType) {
        this.messageType = messageType;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getResv() {
        return resv;
    }

    public void setResv(byte[] resv) {
        this.resv = resv;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "TransPacket{" +
                "magic=" + magic +
                ", serial_num=" + Arrays.toString(serial_num) +
                ", session_id=" + session_id +
                ", token=" + token +
                ", seq=" + seq +
                ", messageType=" + messageType +
                ", len=" + len +
                ", resv=" + Arrays.toString(resv) +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
