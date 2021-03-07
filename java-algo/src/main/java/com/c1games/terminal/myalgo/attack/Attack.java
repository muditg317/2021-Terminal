package com.c1games.terminal.myalgo.attack;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;

import java.util.Set;


public abstract class Attack {
  public Set<Coords> clearLocations;

  public abstract void learn(double thingToLearn);
  public abstract void execute(GameState move);

  public abstract double evaluation(GameState move);

  public abstract double getExpectedAttackValue();
}
