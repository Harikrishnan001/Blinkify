package com.example.blinkify;

import java.util.*;

class WordCompletion {

    private Map<String, Integer> wordFrequencies;

    public WordCompletion(Map<String,Integer> wordFrequencies) {
        this.wordFrequencies=wordFrequencies;
        // Compute word frequencies in the corpus
        /*for (String word : corpus) {
            if (wordFrequencies.containsKey(word)) {
                wordFrequencies.put(word, wordFrequencies.get(word) + 1);
            } else {
                wordFrequencies.put(word, 1);
            }
        }*/
    }

    public List<String> completeWord(String prefix, int numCompletions) {
        List<String> completions = new ArrayList<>();

        // Find all words that start with the given prefix
        for (String word : wordFrequencies.keySet()) {
            if (word.startsWith(prefix)) {
                completions.add(word);
            }
        }

        // Sort the completions by frequency (descending order)
        completions.sort((w1, w2) -> wordFrequencies.get(w2) - wordFrequencies.get(w1));

        // Return the top 'numCompletions' completions
        return completions.subList(0, Math.min(numCompletions, completions.size()));
    }
}
