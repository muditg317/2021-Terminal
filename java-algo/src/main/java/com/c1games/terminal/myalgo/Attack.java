package com.c1games.terminal.myalgo;
import com.c1games.terminal.algo.map.GameState;


public abstract class Attack {
  public abstract void learn(double thingToLearn);
  public abstract void execute(GameState move);
}
