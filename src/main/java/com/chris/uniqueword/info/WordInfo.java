package com.chris.uniqueword.info;

import java.util.Objects;

/**
 * 记录word信息
 * @author chris xu 2018/08/09.
 */
public class WordInfo {
    /**
     * word 第一次出现的行号
     */
    private int lineNo;

    /**
     * word 在第一次出现行中的索引
     */
    private byte index;

    /**
     * word 出现的次数
     */
    private int count;

    public WordInfo(int lineNo, byte index, int count) {
        this.lineNo = lineNo;
        this.index = index;
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WordInfo wordInfo = (WordInfo) o;
        return lineNo == wordInfo.lineNo &&
                index == wordInfo.index &&
                count == wordInfo.count;
    }

    public int getAndIncreaceCount() {
        return count ++;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNo, index, count);
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public byte getIndex() {
        return index;
    }

    public void setIndex(byte index) {
        this.index = index;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void addCount(int count) {
        this.count = this.count + count;
    }
}
