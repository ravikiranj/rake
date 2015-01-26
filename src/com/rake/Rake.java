package com.rake;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Rake
 *
 * @author rjanardhana
 * @since Jan 2015
 */
public class Rake
{
    private Rake()
    {
    }

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
    private static String STOP_WORD_REGEX;

    static
    {
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
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
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
        String[] candidates = sentence.split("[^a-zA-Z0-9_+-/]");
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

    public List<String> generateCandidateKeywords(List<String> sentences)
    {
        List<String> phraseList = Lists.newArrayList();
        for (String sentence : sentences)
        {
            sentence = sentence.trim();
            String tempStopWordRegexSplit = sentence.replaceAll(STOP_WORD_REGEX, "|");
            String[] phrases = tempStopWordRegexSplit.split("\\|");
            for (int i = 0; i < phrases.length; i++)
            {
                String tempPhrase = phrases[i].trim().toLowerCase();
                if (StringUtils.isNotBlank(tempPhrase))
                {
                    phraseList.add(tempPhrase);
                }
            }
        }
        return phraseList;
    }

    public Map<String, Double> getWordScores(List<String> phraseList)
    {
        Map<String, Double> wordScoresMap = Maps.newHashMap();
        Map<String, Integer> wordFrequencyMap = Maps.newHashMap();
        Map<String, Integer> wordDegreeMap = Maps.newHashMap();
        for (String phrase : phraseList)
        {
            List<String> wordList = separateWords(phrase, 0);
            int wordListLen = wordList.size();
            int wordListDegree = wordListLen - 1;
            for (String word : wordList)
            {
                // Freq
                if (wordFrequencyMap.containsKey(word))
                {
                    wordFrequencyMap.put(word, wordFrequencyMap.get(word) + 1);
                }
                else
                {
                    wordFrequencyMap.put(word, 1);
                }

                // Degree
                if (wordDegreeMap.containsKey(word))
                {
                    wordDegreeMap.put(word, wordDegreeMap.get(word) + wordListDegree);
                }
                else
                {
                    wordDegreeMap.put(word, wordListDegree);
                }
            }
        }

        for (Map.Entry<String, Integer> mapEntry : wordFrequencyMap.entrySet())
        {
            String word = mapEntry.getKey();
            int wordFreq = mapEntry.getValue();
            int wordDegree = wordDegreeMap.get(word);
            wordDegreeMap.put(word, wordDegree + wordFreq);
        }

        // Calculate Word scores = deg(w)/freq(w)
        for (Map.Entry<String, Integer> mapEntry : wordFrequencyMap.entrySet())
        {
            String word = mapEntry.getKey();
            int wordFreq = mapEntry.getValue();
            int wordDegree = wordDegreeMap.get(word);
            wordScoresMap.put(word, (double) wordDegree / wordFreq);
        }
        return wordScoresMap;
    }

    public List<CandidateKeywordScore> getCandidateKeywordScores(List<String> phraseList, Map<String, Double> wordScoresMap)
    {
        List<CandidateKeywordScore> candidateKeywordScores = Lists.newArrayList();
        for (String phrase : phraseList)
        {
            List<String> wordList = separateWords(phrase, 0);
            double candidateScore = 0.0;
            for (String word : wordList)
            {
                candidateScore += wordScoresMap.get(word);
            }
            candidateKeywordScores.add(new CandidateKeywordScore(phrase, candidateScore));
        }
        Collections.sort(candidateKeywordScores, Collections.reverseOrder(new CandidateKeywordScoreSorter()));
        return candidateKeywordScores;
    }

    public boolean isNumber(String str)
    {
        try
        {
            if (str.contains("."))
            {
                Integer.parseInt(str);
            } else
            {
                Double.parseDouble(str);
            }
        } catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }

    public <T> void printArray(T[] arr)
    {
        for (int i = 0; i < arr.length; i++)
        {
            System.out.println(i + " = " + arr[i]);
        }

    }

    public <T> void printList(List<T> elemList)
    {
        int i = 0;
        for (T elem : elemList)
        {
            System.out.println(i++ + " = " + elem);
        }
    }

    public static void main(String[] args)
    {
        Rake rake = Rake.getInstance();
        String text = "Rangoon Ruby is my new Palo Alto favorite restaurant. I have eaten there three times during last two weeks and have been satisfied each time. See how they toss the tea leaf salad in the table, enjoy tasty coconut rice and sesame chicken, for example. Every time I was served by Ashley, who is top notch professional. She has the eye for details and the warmest smile. The only small minus is that it gets quite loud when they are full. However, for 2 persons they found a table very fast. Reservation is recommended, restaurant seems to be full both on Sunday afternoons and week evenings. If you are looking for a quiet meal, they also do home deliveries...";
        List<String> sentenceList = rake.splitSentences(text);
        List<String> phraseList = rake.generateCandidateKeywords(sentenceList);
        Map<String, Double> wordScoresMap = rake.getWordScores(phraseList);
        List<CandidateKeywordScore> candidateKeywordScores = rake.getCandidateKeywordScores(phraseList, wordScoresMap);
        System.out.println(candidateKeywordScores);
    }

    private class CandidateKeywordScore
    {
        String m_word;
        double m_score;

        public CandidateKeywordScore(String word, double score)
        {
            m_word = word;
            m_score = score;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CandidateKeywordScore that = (CandidateKeywordScore) o;

            return Objects.equal(this.m_word, that.m_word) &&
                    Objects.equal(this.m_score, that.m_score);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(m_word, m_score);
        }

        public String getWord()
        {
            return m_word;
        }

        public double getScore()
        {
            return m_score;
        }

        @Override
        public String toString()
        {
            return "CandidateKeywordScore{" +
                    "m_word='" + m_word + '\'' +
                    ", m_score=" + m_score +
                    '}';
        }
    }

    private class CandidateKeywordScoreSorter implements Comparator<CandidateKeywordScore>
    {

        @Override
        public int compare(CandidateKeywordScore o1, CandidateKeywordScore o2)
        {
            if (o1 == null && o2 == null)
            {
                return 0;
            }
            if (o1 == null)
            {
                return -1;
            }
            if (o2 == null)
            {
                return 1;
            }
            double scoreDiff = o1.getScore() - o2.getScore();
            if (scoreDiff > 0.0)
            {
                return 1;
            }
            else if (scoreDiff < 0.0)
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
    }
}
