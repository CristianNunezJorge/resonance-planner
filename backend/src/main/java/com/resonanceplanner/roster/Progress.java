package com.resonanceplanner.roster;

/**
 * Level and ascension rank of a character, now and as a goal (D34). Together they say how far the character is and
 * how far the user wants to take it: the materials that are missing are calculated from the difference (T22).
 */
public record Progress(int currentLevel, int currentRank, int targetLevel, int targetRank) {}
