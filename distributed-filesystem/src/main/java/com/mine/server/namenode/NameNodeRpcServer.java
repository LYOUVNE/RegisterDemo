package com.mine.server.namenode;

/**
 * NameNode的rpc服务的接口
 */
public class NameNodeRpcServer {
    /**
     * 负责管理元数据的核心组件
     */
    private FSNamesystem namesystem;

    public NameNodeRpcServer(FSNamesystem namesystem) {
        this.namesystem = namesystem;
    }

    /**
     * 创建目录
     * @param path 目录路径
     * @return 是否创建成功
     * @throws Exception
     */
    public boolean mkdir(String path) throws Exception{
        return this.namesystem.mkdir(path);
    }

    /**
     * 启动这个rpc server
     */
    public void run() {
        System.out.println("开始监听指定的rpc server的端口号，来接收请求");
    }
}
