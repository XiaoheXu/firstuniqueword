package com.chris.uniqueword;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 实现队列，内部使用NIO读取文件，将文件构建成一个单词队列
 * 内部记录着现在文件读到的位置，每次在发送数据是，只需要调用getWords
 * 方法就可以获取100 个单词。
 *
 * @author chris xu 2018/08/09.
 */
public class WordsBuilder {
    /**
     * 内部缓存数据的长度
     */
    private int bytesLen = 1024;

    /**
     * 每行多少个字符
     */
    private int wordsCount = 100;

    /**
     * 文件路径
     */
    private String path = "D:\\bigFile\\text.log";
    private RandomAccessFile file;
    private long fileLen;
    private FileChannel channel;

    private MappedByteBuffer buffer;

    public WordsBuilder() throws IOException {
        file = new RandomAccessFile(path, "rw");
        fileLen = file.length();
        channel = file.getChannel();
        buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLen);
    }

    /**
     * 文件读取的索引位置
     */
    private int index = 0;

    /**
     * 用来构建单词
     */
    private StringBuilder builder = new StringBuilder();

    public String[] getWords() {
        if (index >= fileLen) {
            return null;
        }

        String[] words = new String[wordsCount];
        int count = 0;
       while (index < fileLen && count < 100) {
           byte c = buffer.get();
           index = buffer.position();
           if (c == 13 || c == 10 || c == 32) {
               // 连续空数据的不保存到数组
               String str = builder.toString().trim();
               if (str.length() != 0) {
                   words[count++] = str;
               }
               builder.setLength(0);
           } else {
               builder.append((char) c);
           }
       }

        return words;
    }

    public void close() throws IOException {
        buffer.force();
        channel.close();
        file.close();
    }
}
