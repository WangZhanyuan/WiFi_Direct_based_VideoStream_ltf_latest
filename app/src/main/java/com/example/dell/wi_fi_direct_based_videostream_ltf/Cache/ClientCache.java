package com.example.dell.wi_fi_direct_based_videostream_ltf.Cache;

import com.example.dell.wi_fi_direct_based_videostream_ltf.Multicast.MulticastServer;
import com.example.dell.wi_fi_direct_based_videostream_ltf.UDP.EchoServer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCache {
    private ConcurrentHashMap<Integer, byte[]> cCache = new ConcurrentHashMap<>();
    private int cCacheSize;
    private EchoServer echoServer;
    private int maxStamp;
    private int minStamp;

    //构造方法
    public ClientCache(int cCacheSize) {
        this.cCacheSize = cCacheSize;
        this.maxStamp = getMaxStamp();
        Thread thread = new Thread();
        thread.run();
    }

    //设置echoserver
    public void setechoServer(EchoServer echoServer, MulticastServer multicastServer){
        this.echoServer=echoServer;
//        this.multicastServer=multicastServer;
    }

    //将拆分结果放入缓冲区
    public void put (Data data) {
        cCache.put(data.getTimeStamp(), data.getData());
    }

    //获取缓冲区末的数据,并删除
    public byte[] get () {
        byte[] res = cCache.get(minStamp);
        cCache.remove(minStamp++);
        return res;
    }

    //获取最新的时间戳
    public int getMaxStamp() {
        int maxStamp = 0;
        for (int tempStamp : cCache.keySet()) {
            maxStamp = tempStamp > maxStamp ? tempStamp : maxStamp;
        }
        return maxStamp;
    }

    //检查UDP包是否连续,收到不连续的包则认为丢包，请求重传
    public boolean checkPacket(int timeStamp) {
        return timeStamp == ++maxStamp ? true : false;
    }

    //解码UDP数据包，拆分时间戳和编码结果
    public Data decodeUDPMessage (byte[] message) {
        Data res = new Data();
        byte[] timeStamp = null;
        byte[] data = null;
        System.arraycopy(message,0,timeStamp,0,4);
        System.arraycopy(message,4,data,0,data.length-4);
        res.setTimeStamp(bytes2Int(timeStamp));
        res.setData(data);
        return res;
    }

    //byte[]转int
    public static int bytes2Int(byte[] bytes){
        int num=bytes[3] & 0xFF;
        num |=((bytes[2] <<8)& 0xFF00);
        num |=((bytes[1] <<16)& 0xFF0000);
        num |=((bytes[0] <<24)& 0xFF0000);
        return num;
    }

    //写个线程来不断获取EchoServer的数据
    private class Thread extends java.lang.Thread{
        @Override
        public void run(){
            byte[] message = echoServer.pollFramedata();
            if (message != null){
                Data res = decodeUDPMessage(message);
                if (checkPacket(res.getTimeStamp())){
                    //没有丢包
                    put(res);
                }else{
                    //触发丢包事件
                    maxStamp = getMaxStamp();
                }
            }
        }
    }


    //内部类，用来记录拆分出来的结果
    private class Data {
        private int timeStamp;
        private byte[] data;

        public void setTimeStamp(int timeStamp) {
            this.timeStamp = timeStamp;
        }

        public int getTimeStamp() {
            return timeStamp;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }
    }
}
