package com.resonanceplanner.roster;

import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.common.error.FieldValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Which pairs of level and ascension rank exist in the game (D34, D104), with no database. Rank r allows the levels
 * from the cap of the previous rank (1 for rank 0) to the cap of its own: with caps 20 and 40, rank 0 is levels 1-20,
 * rank 1 is 20-40 and rank 2 is only 40. The goal cannot be lower than the present. The caps come from
 * game-rules.json.
 */
@Component
public class RosterRules {

    private static final String LOWER_GOAL = "L'obiettivo non può essere più basso della situazione attuale.";

    private final List<Integer> ascensionLevels;
    private final int maxLevel;

    public RosterRules(GameRules gameRules) {
        this.ascensionLevels = gameRules.ascensionLevels();
        this.maxLevel = gameRules.maxLevel();
    }

    /**
     * Every wrong field with its message, in the order of the form: empty if the progress is possible. A side with a
     * rank that does not exist is not judged by level, and a goal is not compared with a present that is wrong.
     */
    public Map<String, String> problems(Progress progress) {
        Map<String, String> problems = new LinkedHashMap<>();
        boolean currentOk = checkSide(problems, "current", progress.currentLevel(), progress.currentRank());
        boolean targetOk = checkSide(problems, "target", progress.targetLevel(), progress.targetRank());
        if (currentOk && targetOk) {
            checkGoalIsNotLower(problems, progress);
        }
        return problems;
    }

    /** Refuses a progress that does not exist, naming every wrong field in one answer. */
    public void requireValid(Progress progress) {
        Map<String, String> problems = problems(progress);
        if (!problems.isEmpty()) {
            throw new FieldValidationException(problems);
        }
    }

    // Returns true when the side is possible; otherwise it records the problem under the field that is wrong
    private boolean checkSide(Map<String, String> problems, String side, int level, int rank) {
        int highestRank = ascensionLevels.size();
        if (rank < 0 || rank > highestRank) {
            problems.put(side + "Rank", "Il grado va da 0 a " + highestRank + ".");
            return false;
        }
        int lowest = lowestLevel(rank);
        int highest = highestLevel(rank);
        if (level < lowest || level > highest) {
            problems.put(side + "Level", describeLevels(rank, lowest, highest));
            return false;
        }
        return true;
    }

    private void checkGoalIsNotLower(Map<String, String> problems, Progress progress) {
        if (progress.targetRank() < progress.currentRank()) {
            problems.put("targetRank", LOWER_GOAL);
        } else if (progress.targetRank() == progress.currentRank() && progress.targetLevel() < progress.currentLevel()) {
            problems.put("targetLevel", LOWER_GOAL);
        }
    }

    // The levels before the first ascension start from 1; after an ascension, from the cap that was just reached
    private int lowestLevel(int rank) {
        return rank == 0 ? 1 : ascensionLevels.get(rank - 1);
    }

    // The last rank goes up to the maximum level of the game
    private int highestLevel(int rank) {
        return rank < ascensionLevels.size() ? ascensionLevels.get(rank) : maxLevel;
    }

    private static String describeLevels(int rank, int lowest, int highest) {
        if (lowest == highest) {
            return "Con il grado " + rank + " il livello è " + lowest + ".";
        }
        return "Con il grado " + rank + " il livello va da " + lowest + " a " + highest + ".";
    }
}
