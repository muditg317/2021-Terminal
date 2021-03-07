package com.c1games.terminal.myalgo.strategy;

import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.myalgo.MyAlgo;

public class EarlyGame {
  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  public static void execute(MyAlgo algoState, GameState move) {
    MainStrategy.execute(algoState, move);

    //WAS: Utility.deployRandomScramblers(move);
  }
}
