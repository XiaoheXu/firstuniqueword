package com.chris.uniqueword;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * 单例的CuratorFramework
 * 建立两个节点，忙节点空闲节点，刚开始所有worker都挂在 idle
 * <p>
 * MainServer 开始从idle 获取所有的列表，然后开始分配任务，分配一个完成后，将其从idle上移除
 * 并在running下建立一个节点
 */
public class CuratorClientFactory {
    public static final String STATICS = "/statics";

    private static final ExponentialBackoffRetry retryPolicy =
            new ExponentialBackoffRetry(200, 10, 1000);

    private static volatile CuratorFramework client;

    public static final CuratorFramework getInstance() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .build();
        client.start();
        return client;
    }





public static String getID(String name){
        return name.substring(name.lastIndexOf("/")+1,name.indexOf("#"));
        }
        }
