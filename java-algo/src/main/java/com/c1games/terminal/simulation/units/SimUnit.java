package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.simulation.pathfinding.Edge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class SimUnit {
  private static int defaultID = 1000;

  private Config.UnitInformation config;

  private final boolean isEnemy;
  private final int id;

  private final double startHealth;
  private final double range; //max euclidean distance
  private final double walkerDamage;
  private final double structureDamage;

  private Coords location;
  private double health;

  private List<SimUnit> interactable;


  protected SimUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, Coords location, double health) {
    this.isEnemy = isEnemy;
    int thisID;
    try {thisID = Integer.parseInt(id);} catch (NumberFormatException e) {thisID = defaultID++;}
    this.id = thisID;
    this.startHealth = startHealth;
    this.range = range;
    this.walkerDamage = walkerDamage;
    this.structureDamage = structureDamage;
    this.location = location;
    this.health = health;
  }

  public static SimUnit fromAPIUnit(Unit unit, int x, int y) {
    SimUnit simUnit;
    switch (unit.type) {
      case Scout:
        simUnit = new Scout(unit, new Coords(x,y));
        break;
      case Interceptor:
        simUnit = new Interceptor(unit, new Coords(x,y));
        break;
      case Demolisher:
        simUnit = new Demolisher(unit, new Coords(x,y));
        break;
      case Wall:
        simUnit = new Wall(unit, new Coords(x,y));
        break;
      case Support:
        simUnit = new SupportTower(unit, new Coords(x,y));
        break;
      case Turret:
        simUnit = new Turret(unit, new Coords(x,y));
        break;
      default:
        throw new IllegalArgumentException("Unit not a standard type: " + unit.type);
    }
    simUnit.config = unit.unitInformation;
    return simUnit;
  }

  public abstract Function<SimUnit, Boolean> getInteractabilityFilter();

  /**
   * tests if another unit is interactable with the calling unit
   * should first check if the other is enemy/friend or structure/mobile
   * @param other the oter unit
   */
  public boolean testUnitInteractable(SimUnit other) {
    return this.getInteractabilityFilter().apply(other) && this.inRange(other);
  }

  public boolean inRange(SimUnit other) {
    int dx = (other.location.x - this.location.x);
    int dy = (other.location.y - this.location.y);
    return dx*dx + dy*dy <= this.range * this.range;
  }

  /**
   * returns the most optimal target from the interactable unit list
   * null if no options
   */
  public SimUnit chooseTarget() {
    SimUnit target = null;
    double distToTarget = -1;
    for (SimUnit potentialTarget : interactable) {
      if (potentialTarget != null && potentialTarget.health > 0) {
        if (target == null) {
          target = potentialTarget;
          distToTarget = location.distanceSquared(target.location);
          continue;
        }
        // rule 1: prioritize mobile units
        if (potentialTarget instanceof MobileUnit && target instanceof StructureUnit) {
          target = potentialTarget;
          distToTarget = location.distanceSquared(target.location);
          continue;
        } else if (potentialTarget instanceof StructureUnit && target instanceof MobileUnit) {
          continue;
        }

        // rule 2: prioritize closest enemy
        double distToPotential = location.distanceSquared(potentialTarget.location);
        if (distToPotential < distToTarget) {
          target = potentialTarget;
          distToTarget = distToPotential;
          continue;
        } else if (distToPotential > distToTarget) {
          continue;
        }

        // rule 3: prioritize lowest health
        if (potentialTarget.health < target.health) {
          target = potentialTarget;
          distToTarget = distToPotential;
          continue;
        } else if (potentialTarget.health > target.health) {
          continue;
        }

        // rule 4: prioritize deepest target
        int targetDepth = target.isEnemy ? (MapBounds.BOARD_SIZE - 1 - target.location.y) : target.location.y;
        int potentialDepth = target.isEnemy ? (MapBounds.BOARD_SIZE - 1 - target.location.y) : target.location.y;
        if (potentialDepth > targetDepth) {
          target = potentialTarget;
          distToTarget = distToPotential;
          continue;
        } else if (potentialDepth < targetDepth) {
          continue;
        }

        // rule 5: prioritize units closest to an edge
        int targetEdgeDist = Edge.distToEdge(target.location);
        int potentialEdgeDist = Edge.distToEdge(target.location);
        if (potentialEdgeDist < targetEdgeDist) {
          target = potentialTarget;
          distToTarget = distToPotential;
          continue;
        } else if (potentialEdgeDist > targetEdgeDist) {
          continue;
        }

        // fallback: most recently created
        if (potentialTarget.id > target.id) {
          target = potentialTarget;
          distToTarget = distToPotential;
        }
      }
    }
    return target;
  }

  public void addHealth(double amount) {
    health += amount;
  }

  public void takeDamage(double amount) {
    health -= amount;
    if (health < 0) health = 0;
  }

  public Config.UnitInformation getConfig() {
    return config;
  }

  public boolean isEnemy() {
    return isEnemy;
  }

  public int getID() {
    return id;
  }

  public double getStartHealth() {
    return startHealth;
  }

  public double getRange() {
    return range;
  }

  public double getWalkerDamage() {
    return walkerDamage;
  }

  public double getStructureDamage() {
    return structureDamage;
  }

  public Coords getLocation() {
    return location;
  }

  protected void setLocation(Coords newLocation) {
    if (this instanceof MobileUnit) {
      this.location = newLocation;
    }
  }

  public int getX() {
    return location.x;
  }

  public int getY() {
    return location.y;
  }

  public double getHealth() {
    return health;
  }

  public List<SimUnit> getInteractable() {
    return interactable;
  }

  public void setInteractable(List<SimUnit> interactable) {
    this.interactable = interactable;
  }
}
