package com.kaixin.copy_echo.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词过滤器
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Component
public class SensitiveFilter {

    //创建一个日志打印对象
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //将敏感词换成 ***
    private static final String REPLACEMENT = "***";

    //前缀树的根节点
    private TrieNode rootNode = new TrieNode();

    /*
     *   初始化前缀树
     *    @PostConstruct该注解被用来修饰一个非静态的void（）方法
     *    被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行，并且只会被服务器执行一次。
     *    PostConstruct在构造函数之后执行，init（）方法之前执行。
     *   详细知识介绍: https://blog.csdn.net/qq360694660/article/details/82877222
     * */

    @PostConstruct
    public void init() {
        /*
         *  try括号中能够写多行语句，会自动关闭括号中的资源j
         * */
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyWord = null;
            while ((keyWord = reader.readLine()) != null) {
                addKeyword(keyWord);
            }

        } catch (IOException exception) {
            logger.error("加载敏感词文件失败" + exception.getMessage());
        }
    }

    /*
     *   将一个敏感词加入前缀树中
     * */
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;

        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            //首先判断是否存在相同子节点
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                subNode = new TrieNode(); //初始化子节点
                tempNode.addSubNode(c, subNode);
            }
            tempNode = subNode;
            //设置结束标志(叶子节点),表示这个字符是该敏感词的最后一个字符
            if (i == keyword.length() - 1) tempNode.setKeywordEnd(true);
        }
    }

    //判断某个字符是否是符号
    private boolean isSymbol(Character c) {
        //两个十六进制是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2e80 || c > 0x9FFF);
    }

    /*
     *  过滤敏感词
     *  @param text 待过滤的文本
     *  @return 过滤后的文本(即用 *** 替代敏感词)
     *
     * */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) return null;

        StringBuilder sb = new StringBuilder();
        TrieNode tempNode = rootNode;

        int sta = 0;

        while (sta < text.length()) {
            char c = text.charAt(sta);
            //如果是符号,并且不被夹在敏感词之间,就可以直接加入结果字符串中
            if (isSymbol(c) && tempNode == rootNode) {
                sb.append(c);
                sta++;
            } else {
                int t = sta;
                while ((tempNode = tempNode.getSubNode(c)) != null) {
                    t++;
                    if (tempNode.isKeywordEnd) {
                        sb.append(REPLACEMENT);
                        break;
                    }
                    c = text.charAt(t);
                    while (isSymbol(c) && t + 1 < text.length()) {
                        t++;
                        c = text.charAt(t);
                    }
                }
                if (tempNode == null) {
                    sb.append(text.charAt(sta));
                    sta++;
                } else if (tempNode.isKeywordEnd) sta = t;
                tempNode = rootNode;
            }
        }
        return sb.toString();
    }


    /*
     *   定义前缀树
     * */
    private class TrieNode {
        //关键词结束标识(叶子结点)
        private boolean isKeywordEnd = false;
        //子结点(key: 子结点字符 value: 子结点类型)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }
}
