package com.c1games.terminal.simulation.pathfinding;

import com.c1games.terminal.algo.Coords;

public enum Direction {
  SPAWNED, HORIZONTAL,VERTICAL, STUCK;

  public static Direction fromCoordinates(Coords from, Coords to) {
    if (from.x == to.x && from.y != to.y)
      return VERTICAL;
    else if (from.x != to.x && from.y == to.y)
      return HORIZONTAL;
    else if (from.equals(to))
      return STUCK;
    else
      throw new IllegalArgumentException("not adjacent: " + from + " and " + to);
  }
}
