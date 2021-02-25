package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;

import java.util.List;

public class DeadUtility {

  /**
   * Calculates some rating of our defense..
   * Cuts the board in half and counts the total SP value of the structures on the left and right side and then
   * returns the value of the lowest side
   * @param move the game state
   * @return the defensive rating
   */
  private static float calculateDefenseRating(GameState move) {

    float leftRating = 0;
    float rightRating = 0;

    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (List<Unit> units : row) {
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player1) { //this is our structure
          float unitValue = 0;
          unitValue += unit.unitInformation.cost()[0] * unit.health / unit.unitInformation.startHealth.getAsDouble();
          if (x < 13) {
            leftRating += unitValue;
          } else {
            rightRating += unitValue;
          }
        }
      }
    }
    return 2 * Math.min(leftRating, rightRating);
  }

  /**
   * OLD ALGO
   * @param move
   * @return the estimated number of inters needed to defend a scout rush this turn given our defenses
   */
  static int neededScoutRushDefense(GameState move) {
    float mp = move.data.p1Stats.bits;
    float sp = move.data.p1Stats.cores;
    int turnNumber = move.data.turnInfo.turnNumber;
    int enemyMPCapacity = (int) move.data.p2Stats.bits * 5;
    double enemyMPPercentCapacity = move.data.p2Stats.bits / enemyMPCapacity;
    int scoutRushDefense = (int) ((2.0 * (move.data.p2Stats.bits) / 5) - (turnNumber / 2));
    if (enemyMPPercentCapacity < 0.5) {
      scoutRushDefense = 0;
    }

    scoutRushDefense = (int) Math.min(mp, scoutRushDefense);
    return scoutRushDefense;
  }
}
