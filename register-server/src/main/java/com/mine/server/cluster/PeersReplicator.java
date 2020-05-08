package com.mine.server.cluster;


import com.mine.server.web.AbstractRequest;
import com.mine.server.web.CancelRequest;
import com.mine.server.web.HeartbeatRequest;
import com.mine.server.web.RegisterRequest;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 集群同步batch
 */
public class PeersReplicator {

    /**
     * 集群同步生成batch的间隔时间：500ms
     */
    private static final long PEERS_REPLICATE_BATCH_INTERVAL = 500;

    private static PeersReplicator instance = new PeersReplicator();

    private PeersReplicator() {
        // 启动接收请求和打包batch的线程
        AcceptorBatchThread acceptorBatchThread = new AcceptorBatchThread();
        acceptorBatchThread.setDaemon(true);
        acceptorBatchThread.start();

        // 启动同步发送batch的线程
        PeersReplicateThread peersReplicateThread = new PeersReplicateThread();
        peersReplicateThread.setDaemon(true);
        peersReplicateThread.start();
    }

    public static PeersReplicator getInstance() {
        return instance;
    }

    /**
     * 第一层队列：接收请求的高并发写入，无界队列
     */
    private ConcurrentLinkedQueue<AbstractRequest> acceptorQueue = new ConcurrentLinkedQueue<>();

    /**
     * 第二层队列：有界队列，用于batch生成
     */
    private LinkedBlockingQueue<AbstractRequest> batchQueue = new LinkedBlockingQueue<>(1000000);

    /**
     * 第三层队列：有界队列，用于batch的同步发送
     */
    private LinkedBlockingQueue<PeersReplicateBatch> replicateQueue = new LinkedBlockingQueue<>(10000);

    /**
     * 同步服务注册请求
     */
    public void replicateRegister(RegisterRequest request) {
        request.setType(AbstractRequest.REGISTER_REQUEST);
        acceptorQueue.add(request);
    }

    /**
     * 同步服务下线请求
     */
    public void replicateCancel(CancelRequest request) {
        request.setType(AbstractRequest.CANCEL_REQUEST);
        acceptorQueue.add(request);
    }

    /**
     * 同步发送心跳请求
     */
    public void replicateHeartbeat(HeartbeatRequest request) {
        request.setType(AbstractRequest.HEARTBEAT_REQUEST);
        acceptorQueue.add(request);
    }

    /**
     * 负责接收数据以及打包为batch的后台线程
     */
    class AcceptorBatchThread extends Thread {
        long latestBatchGeneration = System.currentTimeMillis();

        @Override
        public void run() {
            try {
                // 从第一层队列获取请求，并且放入第二层队列中
                AbstractRequest request = acceptorQueue.poll();
                if (request != null) {
                    batchQueue.put(request);
                }

                // 采用一定的策略来进行打包，每隔500ms生成一个batch
                long now = System.currentTimeMillis();
                while (true) {
                    if (now - latestBatchGeneration >= PEERS_REPLICATE_BATCH_INTERVAL) {
                        PeersReplicateBatch batch = createBatch();
                        replicateQueue.offer(batch);
                    }
                    this.latestBatchGeneration = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private PeersReplicateBatch createBatch() {
            PeersReplicateBatch batch = new PeersReplicateBatch();

            Iterator<AbstractRequest> iterator = batchQueue.iterator();
            while (iterator.hasNext()) {
                batch.add(iterator.next());
            }

            batchQueue.clear();

            return batch;
        }
    }

    /**
     * 集群同步线程
     */
    class PeersReplicateThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    PeersReplicateBatch batch = replicateQueue.take();
                    if(batch != null) {
                        // 遍历所有的其他的register-server地址
                        // 给每个地址的register-server都发送一个http请求同步batch
                        System.out.println("给所有其他的register-server发送请求，同步batch过去......");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
