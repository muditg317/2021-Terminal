package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;

public abstract class MobileUnit extends SimUnit {
  public double speed; // num frames to move

  public Direction prevDirection;
  public StructureUnit[] previouslyInRange;
  public int framesSinceMoved;

  public MobileUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, double speed, Coords location, double health, Direction prevDirection) {
    super(isEnemy, id, startHealth, range, walkerDamage, structureDamage, location, health);
    this.speed = speed;
    this.prevDirection = prevDirection;
  }
}
