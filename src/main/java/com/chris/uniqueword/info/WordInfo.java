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
    private int firstAppearLineNo;

    /**
     * word 在第一次出现行中的索引
     * byte如果每次传输的数字增加，这是要注意容量超量程问题
     * 修改为char 够用了
     *
     */
    private char firstAppearIndex;

    /**
     * word 出现的次数
     */
    private int count;

    public WordInfo(int firstAppearLineNo, char firstAppearIndex, int count) {
        this.firstAppearLineNo = firstAppearLineNo;
        this.firstAppearIndex = firstAppearIndex;
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
        return firstAppearLineNo == wordInfo.firstAppearLineNo &&
                firstAppearIndex == wordInfo.firstAppearIndex &&
                count == wordInfo.count;
    }

    public int getAndIncreaceCount() {
        return count ++;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstAppearLineNo, firstAppearIndex, count);
    }

    public int getFirstAppearLineNo() {
        return firstAppearLineNo;
    }

    public char getFirstAppearIndex() {
        return firstAppearIndex;
    }

    public int getCount() {
        return count;
    }

    public void addCount(int count) {
        this.count = this.count + count;
    }
}
