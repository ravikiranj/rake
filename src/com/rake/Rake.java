package com.rake;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rake
 *
 * @author rjanardhana
 * @since Jan 2015
 */
public class Rake
{
    private Rake() { }

    public static Rake getInstance()
    {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder
    {
        private static final Rake INSTANCE = new Rake();
    }


    private static final String HOME_DIR = System.getProperty("user.home");
    private static final String DATASETS_DIR = HOME_DIR + "/git/rake/data";
    private static final String STOP_WORDS_FILEPATH = DATASETS_DIR + "/stopwords.txt";

    private static List<String> STOP_WORDS;
    private static Joiner joiner = Joiner.on("|").skipNulls();
    private static String STOP_WORD_REGEX;

    static {
        STOP_WORDS = Lists.newArrayList();
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(STOP_WORDS_FILEPATH));
            List<String> stopWordRegexList = Lists.newArrayList();
            String line;
            while ((line = br.readLine()) != null)
            {
                String word = line.trim();
                STOP_WORDS.add(word);
                stopWordRegexList.add("\\b" + word + "\\b");
            }
            STOP_WORD_REGEX = Joiner.on("|").join(stopWordRegexList);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public List<String> splitSentences(String sentence)
    {
        String[] candidates = sentence.split("[.!?,;:]");
        return new ArrayList<String>(Arrays.asList(candidates));
    }

    public List<String> separateWords(String sentence, int minLength)
    {
        String[] candidates = sentence.split("[^a-zA-Z0-9_\\+\\-/]'");
        List<String> words = Lists.newArrayList();
        for (int i = 0; i < candidates.length; i++)
        {
            String candidate = candidates[i].trim().toLowerCase();
            if (StringUtils.isNotBlank(candidate) && candidate.length() > minLength && !isNumber(candidate))
            {
                words.add(candidate);
            }
        }
        return words;
    }

    public boolean isNumber(String str)
    {
        try
        {
            if (str.contains("."))
            {
                Integer.parseInt(str);
            }
            else
            {
                Double.parseDouble(str);
            }
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }

    public <T> void printArray(T[] arr)
    {
        for (int i = 0; i < arr.length; i++)
        {
            System.out.println(arr[i]);
        }

    }

    public <T> void printList(List<T> elemList)
    {
        for (T elem : elemList)
        {
            System.out.println(elem);
        }
    }

    public static void main(String[] args)
    {
        Rake rake = Rake.getInstance();
        for (String s : rake.splitSentences("asdasd;. as dasd! asdasd ;;,, ad,asd,asd"))
        {
            rake.printList(rake.separateWords(s, 3));
        }
    }

}
