package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.map.GameState;

public class EarlyGame {
  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  static void execute(MyAlgo algoState, GameState move) {
    MainStrategy.execute(algoState, move);

    //WAS: Utility.deployRandomScramblers(move);
  }
}
