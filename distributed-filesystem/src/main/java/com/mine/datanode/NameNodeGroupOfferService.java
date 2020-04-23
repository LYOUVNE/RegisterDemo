package com.mine.datanode;

import java.util.concurrent.CountDownLatch;

/**
 * 负责跟一组NameNode进行通信的OfferServie组件
 */
public class NameNodeGroupOfferService {
    /**
     * 负责跟NameNode主节点通信的ServiceActor组件
     */
    private NameNodeServiceActor activeServiceActor;

    /**
     * 负责跟NameNode备节点通信的ServiceActor组件
     */
    private NameNodeServiceActor standbyServiceActor;

    /**
     * 构造函数
     */
    public NameNodeGroupOfferService() {
        this.activeServiceActor = new NameNodeServiceActor();
        this.standbyServiceActor = new NameNodeServiceActor();
    }

    /**
     * 启动OfferService组件
     */
    public void start() {
        register();
    }

    /**
     * 向主备两个NameNode节点进行注册
     */
    private void register() {
        try {
            CountDownLatch latch = new CountDownLatch(2);
            activeServiceActor.register(latch);
            standbyServiceActor.register(latch);
            latch.await();
            System.out.println("主备NameNode全部注册完毕......");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}