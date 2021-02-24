package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;

public abstract class SimUnit {
  public boolean isEnemy;
  public String id;

  public double startHealth;
  public double range; //max euclidean distance
  public double walkerDamage;
  public double structureDamage;

  public Coords location;
  public double health;

  public SimUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, Coords location, double health) {
    this.isEnemy = isEnemy;
    this.id = id;
    this.startHealth = startHealth;
    this.range = range;
    this.walkerDamage = walkerDamage;
    this.structureDamage = structureDamage;
    this.location = location;
    this.health = health;
  }

  /**
   * tests if another unit is interactable with the calling unit
   * should first check if the other is enemy/friend or structure/mobile
   * @param other the oter unit
   */
  public boolean testUnitInteractable(SimUnit other) {
    if (this.walkerDamage != 0 || this.structureDamage != 0) {
      if (!other.isEnemy) {
        return false;
      }
    } else {
      if (other.isEnemy) {
        return false;
      }
    }
    return this.inRange(other);
  }

  public boolean inRange(SimUnit other) {
    int dx = (other.location.x - this.location.x);
    int dy = (other.location.y - this.location.y);
    return dx*dx + dy*dy <= this.range;
  }
}
