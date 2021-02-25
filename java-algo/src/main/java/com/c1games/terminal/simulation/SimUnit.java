package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.Unit;

import java.util.function.Function;

public abstract class SimUnit {
  private static int defaultID = 1000;

  public boolean isEnemy;
  public int id;

  public double startHealth;
  public double range; //max euclidean distance
  public double walkerDamage;
  public double structureDamage;

  public Coords location;
  public double health;

  public SimUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, Coords location, double health) {
    this.isEnemy = isEnemy;
    try {this.id = Integer.parseInt(id);} catch (Exception e) {this.id = defaultID++;}
    this.startHealth = startHealth;
    this.range = range;
    this.walkerDamage = walkerDamage;
    this.structureDamage = structureDamage;
    this.location = location;
    this.health = health;
  }

  public static SimUnit fromAPIUnit(Unit unit, int x, int y) {
    switch (unit.type) {
      case Scout:
        return new Scout(unit, new Coords(x,y));
      case Interceptor:
        return new Interceptor(unit, new Coords(x,y));
      case Demolisher:
        return new Demolisher(unit, new Coords(x,y));
      case Wall:
        return new Wall(unit, new Coords(x,y));
      case Support:
        return new SupportTower(unit, new Coords(x,y));
      case Turret:
        return new Turret(unit, new Coords(x,y));
      default:
        return null;
    }
  }

  public abstract Function<SimUnit, Boolean> getInteractabilityFilter();

  /**
   * tests if another unit is interactable with the calling unit
   * should first check if the other is enemy/friend or structure/mobile
   * @param other the oter unit
   */
  public boolean testUnitInteractable(SimUnit other) {
//    if (this.walkerDamage != 0 || this.structureDamage != 0) {
//      if (!other.isEnemy) {
//        return false;
//      }
//    } else {
//      if (other.isEnemy) {
//        return false;
//      }
//    }
    return this.getInteractabilityFilter().apply(other) && this.inRange(other);
  }

  public boolean inRange(SimUnit other) {
    int dx = (other.location.x - this.location.x);
    int dy = (other.location.y - this.location.y);
    return dx*dx + dy*dy <= this.range;
  }
}
