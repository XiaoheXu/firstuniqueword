package com.chris.uniqueword;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.chris.uniqueword.info.*;
import org.apache.curator.framework.CuratorFramework;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.chris.uniqueword.CuratorClientFactory.STATICS;
import static com.chris.uniqueword.CuratorClientFactory.getID;

/**
 * 负责将100GB文件读取，并分发给100个处理节点
 * 并负责将数据汇总起来。
 *
 * @author chris xu 2018/08/08.
 */
public class MainServer extends AbstractActor {
    // zk 客户端
    CuratorFramework client = CuratorClientFactory.getInstance();
    // 先将统计结果放在map中，最后再去重
    private HashMap<String, WordInfo> map = new HashMap<>();
    // 每100 个单词算作一行，作为一个任务。
    // 如果一个单词平均6个单词，6*2=12个字节。 100GB/12字节 = 8738133 int够用
    private int lineNo = 0;
    private WordsBuilder wordsBuilder;

    private Map<String, ActorRef> workers;

    public static Props props(Map<String, ActorRef> workers) {
        return Props.create(MainServer.class, () -> new MainServer(workers));
    }

    public MainServer(Map<String, ActorRef> workers) {
        this.workers = workers;
        try {
            wordsBuilder = new WordsBuilder();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = receiveBuilder();
        return builder
                .match(StartCommand.class, s -> start())
                .match(ACK.class, ack -> sendData(getSender()))
                .match(StatisticsData.class, data -> {
                    sum(data);
                    workers.remove(getID(getSender().toString()));
                    if (workers.size() == 0) {
                        System.out.println("汇总完成");
                        removeRepeat();
                        System.out.println(getFirstUniqueWord());
                        getContext().getSystem().terminate();
                        client.delete().guaranteed().deletingChildrenIfNeeded().forPath(STATICS);
                    }

                }).build();
    }

    /**
     * 移除map中出现次数大于1的word
     */
    private void removeRepeat() {
        System.out.println("开始去重，删除出现次数大于1的数据");
        Set<String> keySet = map.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            int count = map.get(key).getCount();
            if (count != 1) {
                iterator.remove();
                if (count < 0) {
                    System.out.println(key + ": " + count);
                }
            }
        }
    }

    /**
     * 从汇总的结果中拿到第一个只出现一次的word
     *
     * @return
     */
    private String getFirstUniqueWord() {
        int minLineNo = Integer.MAX_VALUE;
        int minIndex = Integer.MAX_VALUE;
        String result = "not find the first unique word";
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            WordInfo wordInfo = map.get(key);
            if (wordInfo != null) {
                int lineNo = wordInfo.getFirstAppearLineNo();
                //如果行号小 则直接替换
                if (lineNo < minLineNo) {
                    minLineNo = lineNo;
                    minIndex = wordInfo.getFirstAppearIndex();
                    result = key;
                    // 如果行相同，比较行内索引,索引不可能相等
                } else if (lineNo == minLineNo) {
                    int index = wordInfo.getFirstAppearIndex();
                    if (index < minIndex) {
                        minIndex = index;
                        result = key;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 向所有的子节点发送数据
     */
    private void start() {
        System.out.println("开始分发任务");
        Iterator<String> iterator = workers.keySet().iterator();
        while (iterator.hasNext()) {
            sendData(workers.get(iterator.next()));
        }
    }

    /**
     * 往worker发送数据
     */
    private void sendData(ActorRef worker) {
        System.out.println("开始给 " + getID(worker.toString()) + " 发送数据");
        String[] words = wordsBuilder.getWords();
        if (words != null) {
            //WordsContainer 之前为了减少对象的创建，将该对象设置为了成员变量
            // 在汇总时，发现有相同的行号，相同的索引号，肯定是公用对象引起的。
            WordsContainer data = new WordsContainer();
            data.setWords(words);
            data.setLineNo(lineNo++);
            worker.tell(data, getSelf());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            worker.tell(new StopCommand(), getSelf());
        }
    }

    /**
     * 合并所有节点的统计信息
     *
     * @param data 客户端发送来的数据
     */
    private void sum(StatisticsData data) {
        System.out.println("汇总中...剩余worker" + workers.size() + "个待汇总。");
        HashMap<String, WordInfo> workerMap = data.getData();
        if (workerMap == null || workerMap.size() == 0) {
            workers.remove(getSender().toString());
            return;
        }

        Set<String> keySet = workerMap.keySet();
        for (String key : keySet) {
            WordInfo workerWordInfo = workerMap.get(key);
            WordInfo mainWordInfo = map.get(key);
            // 如果汇总的map中没有这个key
            if (mainWordInfo == null) {
                map.put(key, workerWordInfo);
            } else {
                // 将之前的存储的次数和当前worker返回的次数数据相加
                // 没有必要比较行内索引
                int workerLineNo = workerWordInfo.getFirstAppearLineNo();
                int mainLineNo = mainWordInfo.getFirstAppearLineNo();
                // 不存在行号相等的数据
                if (workerLineNo > mainLineNo) {
                    mainWordInfo.addCount(workerWordInfo.getCount());
                } else {
                    workerWordInfo.addCount(mainWordInfo.getCount());
                    map.put(key, workerWordInfo);
                }
            }
        }
    }

    /**
     * 关闭资源
     */
    @Override
    public void aroundPostStop() {
        super.aroundPostStop();
        try {
            wordsBuilder.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
