package com.chris.uniqueword.info;

/**
 * 主节点向服务节点发送的数据包，其内部包含word和lineNo
 * @author chris xu 2018/08/09.
 */
public class WordsContainer {
    /**
     * 100 个单词组成的数组
     */
    private String[] words;

    /**
     * 行号，记录这个words为整个文件的第几行，用来后面找第一个不重复的单词
     * 经过计算，行号为int 可以支持1600GB的数据大小，已够用
     */
    private int lineNo;

    public WordsContainer(String[] words, int lineNo) {
        this.words = words;
        this.lineNo = lineNo;
    }

    public WordsContainer() {
    }

    public String[] getWords() {
        return words;
    }

    public void setWords(String[] words) {
        this.words = words;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }
}
