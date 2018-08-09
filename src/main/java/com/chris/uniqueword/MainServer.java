package com.chris.uniqueword;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.chris.uniqueword.info.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * 负责将100GB文件读取，并分发给100个处理节点
 * 并负责将数据汇总起来。
 *
 * @author chris xu 2018/08/08.
 */
public class MainServer extends AbstractActor {
    private HashMap<String, WordInfo> map = new HashMap<>();
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = receiveBuilder();
        return builder
                .match(StartCommand.class, s -> {
                    start();
                })
                .match(ACK.class, ack -> {
                    sendData(getSender());
                })
                .match(StatisticsData.class, data -> {
                    sum(data);
                    if (workers.size() == 0) {
                        System.out.println("汇总完成");
                        removeRepeat();
                        System.out.println(getFirstUniqueWord());
                        getContext().getSystem().terminate();
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
            if (map.get(key).getCount() != 1) {
                iterator.remove();
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
                int lineNo = wordInfo.getLineNo();
                //如果行号小 则直接替换
                if (lineNo < minLineNo) {
                    minLineNo = lineNo;
                    minIndex = wordInfo.getIndex();
                    result = key;
                    // 如果行相同，比较行内索引,索引不可能相等
                } else if (lineNo == minLineNo) {
                    int index = wordInfo.getIndex();
                    if (index < minIndex) {
                        minIndex = index;
                        result = key;
                    }
                }
            }
        }
        return result;
    }

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
        System.out.println("开始给 " + worker.toString() + " 发送数据");
        String[] words = wordsBuilder.getWords();
        if (words != null) {
            //WordsContainer 之前为了减少对象的创建，将该对象设置为了成员变量
            // 在汇总时，发现有相同的行号，相同的索引号，肯定是公用对象引起的。
            WordsContainer data = new WordsContainer();
            data.setWords(words);
            data.setLineNo(lineNo++);
            worker.tell(data, getSelf());
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
        HashMap<String, WordInfo> workerMap = data.getData();
        if (workerMap == null || workerMap.size() == 0) {
            workers.remove(getSender().toString());
            return;
        }

        Set<String> keySet = workerMap.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            WordInfo workerWordInfo = workerMap.get(key);
            WordInfo mainWordInfo = map.get(key);
            // 如果汇总的map中没有这个key
            if (mainWordInfo == null) {
                map.put(key, workerWordInfo);
            } else {
                // 将之前的存储的次数和当前worker返回的次数数据相加
                // 没有必要比较行内索引
                int workerLineNo = workerWordInfo.getLineNo();
                int mainLineNo = mainWordInfo.getLineNo();
                // 不存在行号相等的数据
                if (workerLineNo > mainLineNo) {
                    mainWordInfo.addCount(workerWordInfo.getCount());
                } else {
                    workerWordInfo.addCount(mainWordInfo.getCount());
                    map.put(key, workerWordInfo);
                }
            }
        }

        workers.remove(getSender().toString());
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
