package com.chris.uniqueword.info;

import java.util.HashMap;

/**
 * 每个worker返还给主节点的信息
 *
 * @author chris xu 2018/08/09.
 */
public class StatisticsData {
    /**
     * 保存某个worker的统计数据
     */
    private HashMap<String, WordInfo> data;

    public StatisticsData(HashMap<String, WordInfo> data) {
        this.data = data;
    }

    public StatisticsData() {
    }

    public HashMap<String, WordInfo> getData() {
        return data;
    }

    public void setData(HashMap<String, WordInfo> data) {
        // 使用复制的方法，而不是使用引用
        this.data = data;
    }
}
