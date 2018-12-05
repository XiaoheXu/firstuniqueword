package com.chris.uniqueword;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.chris.uniqueword.info.*;

import java.util.HashMap;

/**
 * 负责分析主节点发送来的word进行分析
 *
 * @author chris xu 2018/08/08.
 */
public class Worker extends AbstractActor {
    private HashMap<String, WordInfo> map = new HashMap<>();

    public static Props props(ActorRef mainServer) {
        return Props.create(Worker.class, () -> new Worker(mainServer));
    }

    private ActorRef mainServer;

    public Worker(ActorRef mainServer) {
        this.mainServer = mainServer;
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = receiveBuilder();
        return builder
                .match(WordsContainer.class, container -> statistics(container))
                .match(StopCommand.class, command -> sendStatistcs()).build();
    }

    /**
     * 将本节点的统计数据发送到主节点，发送之前先去掉count大于1的word
     */
    private void sendStatistcs() {
        StatisticsData statisticsData = new StatisticsData();
        statisticsData.setData(map);
        getSender().tell(statisticsData, getSelf());
    }

    /**
     * 统计word记录详细信息
     * 第一次接收到的数据，行号肯定靠前，所以不用更新行号，索引号，只需要更新出现次数
     * @param data 主节点发送来的数据
     */
    private void statistics(WordsContainer data) {
        System.out.println("节点：" + getSelf() + " 接收到了数据");
        String[] words = data.getWords();
        int lineNo = data.getLineNo();
        for (int i = 0; i < words.length; i++) {
            String key = words[i];
            if (key != null) {
                WordInfo wordInfo = map.get(key);
                if (wordInfo == null) {
                    wordInfo = new WordInfo(lineNo, (byte) i, 1);
                    map.put(key, wordInfo);
                } else {
                    wordInfo.getAndIncreaceCount();
                }
            }
        }

        // 告诉主节点，算完了
        getSender().tell(new ACK(), getSelf());
    }
}
