package com.rake;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import weka.core.tokenizers.NGramTokenizer;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        String[] candidates = sentence.split("[^a-zA-Z0-9]");
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
                // Additional fix to exclude stop words from candidate keywords
                if (StringUtils.isNotBlank(tempPhrase) && _isDevoidOfStopWords(tempPhrase) && _isValidWord(tempPhrase))
                {
                    phraseList.add(tempPhrase);
                }
            }
        }
        return phraseList;
    }

    private boolean _isDevoidOfStopWords(String tempPhrase)
    {
        if (STOP_WORDS.contains(tempPhrase))
        {
            return false;
        }
        String [] words = tempPhrase.split(" ");
        for (int i = 0; i < words.length; i++)
        {
            if (STOP_WORDS.contains(words[i]))
            {
                return false;
            }
        }
        return true;
    }

    private boolean _isValidWord(String tempPhrase)
    {
        return tempPhrase.matches("^[A-Za-z0-9 ]+$");
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
        double totalScore = 0.0;
        for (String phrase : phraseList)
        {
            List<String> wordList = separateWords(phrase, 0);
            double candidateScore = 0.0;
            for (String word : wordList)
            {
                candidateScore += wordScoresMap.get(word);
            }
            candidateKeywordScores.add(new CandidateKeywordScore(phrase, candidateScore));
            totalScore += candidateScore;
        }
        _normalizeCandidateKeywordScores(candidateKeywordScores, totalScore);
        Collections.sort(candidateKeywordScores, Collections.reverseOrder(new CandidateKeywordScoreSorter()));
        return candidateKeywordScores;
    }

    private void _normalizeCandidateKeywordScores(List<CandidateKeywordScore> candidateKeywordScores, double totalScore)
    {
        for(CandidateKeywordScore candidateKeywordScore : candidateKeywordScores)
        {
            double currentScore = candidateKeywordScore.getScore();
            double normalizedScore = currentScore / totalScore;
            candidateKeywordScore.setScore(normalizedScore);
        }
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

    public List<CandidateKeywordScore> getKeywords(List<String> reviews)
    {
        Map<String, CandidateKeywordScore> scoreMap = Maps.newHashMap();
        for (String review : reviews)
        {
            List<String> sentenceList = splitSentences(review);
            List<String> phraseList = generateCandidateKeywords(sentenceList);
            Map<String, Double> wordScoresMap = getWordScores(phraseList);
            List<CandidateKeywordScore> candidateKeywordScores = getCandidateKeywordScores(phraseList, wordScoresMap);
            for (CandidateKeywordScore score : candidateKeywordScores)
            {
                String word = score.getWord();
                if (scoreMap.containsKey(word))
                {
                    CandidateKeywordScore existingScore = scoreMap.get(word);
                    existingScore.addToScore(score.getScore());
                    scoreMap.put(word, existingScore);
                }
                else
                {
                    scoreMap.put(word, score);
                }
            }
        }
        List<CandidateKeywordScore> finalScores = new ArrayList<CandidateKeywordScore>(scoreMap.values());
        Collections.sort(finalScores, Collections.reverseOrder(new CandidateKeywordScoreSorter()));
        return finalScores;
    }

    public static void main(String[] args)
    {
        Rake rake = Rake.getInstance();
        List<String> reviews = rake.getTestReviews();
        List<CandidateKeywordScore> finalScores = rake.getKeywords(reviews);
        rake._printList(finalScores);
    }

    private void _printList(List<CandidateKeywordScore> elems)
    {
        for (CandidateKeywordScore elem : elems)
        {
            System.out.println(elem);
        }
    }

    private void testTokenzier()
    {
        String text="Oren's is a popular lunch place, noisy and bustling. It seems to attract an eclectic clientele--we heard at least six languages spoken within 10 feet of our table.  The Sampler gives a generous taste of 6 preselected dips to go with the fresh pita; we also ordered several side dishes to share. Israelis praise the authenticity of the food. My very selective husband enjoyed the hummus, saying it is far better than most American versions. We will return to try the more substantial dinner offerings.";
        NGramTokenizer nGramTokenizer = new NGramTokenizer();
        nGramTokenizer.setNGramMinSize(1);
        nGramTokenizer.setNGramMaxSize(2);
        nGramTokenizer.setDelimiters("[^a-zA-Z0-9']");
        nGramTokenizer.tokenize(text);
        while (nGramTokenizer.hasMoreElements())
        {
            String candidate = (String) nGramTokenizer.nextElement();
            candidate = candidate.toLowerCase().trim();
            String[] words = candidate.split(" ");
            boolean hasStopWord = false;
            for (int i = 0; i < words.length ; i++)
            {
                if (STOP_WORDS.contains(words[i]))
                {
                    hasStopWord = true;
                    break;
                }

            }
            if (!hasStopWord)
            {
                System.out.println((String)nGramTokenizer.nextElement());
            }
        }
    }

    private static class CandidateKeywordScore
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

        public void setScore(double score)
        {
            m_score = score;
        }

        public void addToScore(double score)
        {
            m_score += score;
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

    private static class CandidateKeywordScoreSorter implements Comparator<CandidateKeywordScore>
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

    public List<String> getTestReviews()
    {
        CSVParser parser = null;
        File reviewFileHandle = new File("/home/rjanardhana/tmp/testReviews.txt");
        List<String> reviews = Lists.newArrayList();
        try
        {
            parser = CSVParser.parse(reviewFileHandle, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withDelimiter('|').withHeader());
            for (CSVRecord record : parser)
            {
                String reviewText = _trim(record.get("reviewtext"));
                reviews.add(reviewText);
            }
            parser.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return reviews;
    }

    private String _trim(String str)
    {
        return str != null ? str.trim().replaceAll(" +", " ") : str;
    }
}
