package com.chris.uniqueword;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.chris.uniqueword.info.StartCommand;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.chris.uniqueword.CuratorClientFactory.STATICS;
import static com.chris.uniqueword.CuratorClientFactory.getID;

/**
 * 这个类不能使用countdownlatch 因为其模拟的是分布式，所以要使用
 * zookeeper 下建立两个节点
 * @author chris xu 2018/08/09.
 */
public class Main {

    private CountDownLatch cdl = new CountDownLatch(1);
    NodeCacheListener listener = new NodeCacheListener() {
        @Override
        public void nodeChanged() {
            cdl.countDown();
        }
    };

    @Test
    public void main() throws Exception {
        CuratorFramework client = CuratorClientFactory.getInstance();
        if (client.checkExists().forPath(STATICS) != null) {
            client.delete().guaranteed().deletingChildrenIfNeeded().forPath(STATICS);
        }

        // 创建节点
        client.create().creatingParentsIfNeeded().forPath(STATICS);
        NodeCache cache = new NodeCache(client,STATICS,false);
        cache.start(true);
        cache.getListenable().addListener(listener);
        ActorSystem system = ActorSystem.create("firstUniqueWord");
        Map<String, ActorRef> workers = new HashMap<>();
        int workerCount = 50;
        try {
            // 创建主节点
            ActorRef mainServer = system.actorOf(MainServer.props(workers), "mainServer");
            // 创建worker节点
            for (int i = 0; i < workerCount; i++) {
                ActorRef worker = system.actorOf(Worker.props(mainServer), "worker" + (i + 1));
                workers.put(getID(worker.toString()), worker);
            }
            // 开始
            mainServer.tell(new StartCommand(), ActorRef.noSender());
            cdl.await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }
}
