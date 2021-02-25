package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.map.GameState;

public class Simulator {

  SimBoard board;

  public Simulator(GameState move) {
    this.board = new SimBoard(move);
  }
}
