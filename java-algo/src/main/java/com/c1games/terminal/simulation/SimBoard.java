package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.simulation.units.*;

import java.util.*;
import java.util.stream.Collectors;

public class SimBoard {
  private final static boolean DEBUG = true;
  private final static double ROOT2 = 1.41421356237;
  private final static int yPosXWidth = 7;
  private final static int maxYPosX = 4;
  private final static int yNegXWidth = 7;
  private final static int maxYNegX = 4;
  private final static int numBuckets = (maxYNegX+1) * (maxYPosX+1);

  private Config config;
  private int minMovementsToSelfDestruct;

  private final Map<Integer, HashSet<SimUnit>> unitBuckets;
  private final List<SimUnit>[][] unitListArrays;
  private final SortedSet<SimUnit> allUnits;
  private final SortedSet<StructureUnit> structures;
  private final SortedSet<Wall> walls;
  private final SortedSet<SupportTower> supportTowers;
  private final SortedSet<Turret> turrets;
  private final SortedSet<MobileUnit> mobileUnits;
  private final SortedSet<Scout> scouts;
  private final SortedSet<Interceptor> interceptors;
  private final SortedSet<Demolisher> demolishers;
//  private final Map<Class<? extends SimUnit>, List<SortedSet<? extends SimUnit>>> unitSetListMap;
  private float p1Health;
  private float p2Health;

  public SimBoard() {
    unitBuckets = new HashMap<>(numBuckets);
    for (int i = 0; i < numBuckets; i++) {
      unitBuckets.put(i, new HashSet<>());
    }
    @SuppressWarnings("unchecked") // the arraylist type will be correct
    List<SimUnit>[][] unitListArrays = new ArrayList[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];
    for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        unitListArrays[x][y] = new ArrayList<>();
      }
    }
    this.unitListArrays = unitListArrays;
    this.allUnits = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.structures = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.walls = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.supportTowers = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.turrets = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.mobileUnits = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.scouts = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.interceptors = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
    this.demolishers = new TreeSet<>(Comparator.comparingInt(SimUnit::getID));
//    this.unitSetListMap = Map.ofEntries(
//        new AbstractMap.SimpleImmutableEntry<>(Wall.class, List.of(structures, walls)),
//        new AbstractMap.SimpleImmutableEntry<>(SupportTower.class, List.of(structures, supportTowers)),
//        new AbstractMap.SimpleImmutableEntry<>(Turret.class, List.of(structures, turrets)),
//        new AbstractMap.SimpleImmutableEntry<>(Scout.class, List.of(mobileUnits, scouts)),
//        new AbstractMap.SimpleImmutableEntry<>(Interceptor.class, List.of(mobileUnits, interceptors)),
//        new AbstractMap.SimpleImmutableEntry<>(Demolisher.class, List.of(mobileUnits, demolishers))
//    );
  }

  public SimBoard(GameState move) {
    this(move, true);
  }

  public SimBoard(GameState move, boolean includeSpawnedUnits) {
    this();
    for (int i = 13; i < 41; i++) {
      for (int j = 0; j <= 14 - i%2; j++) {
        int x = j + (i-13)/2;
        int y = i - x;
        move.allUnits[x][y].stream().filter(unit -> unit.type != UnitType.Remove && unit.type != UnitType.Upgrade && (!unit.id.equals("spawned") || includeSpawnedUnits)).forEach(unit -> addUnit(SimUnit.fromAPIUnit(unit, x,y)));
      }
    }
    this.config = move.config;
    p1Health = move.data.p1Stats.integrity;
    p2Health = move.data.p2Stats.integrity;
    minMovementsToSelfDestruct = move.config.mechanics.stepsRequiredSelfDestruct;
  }

  // makes tilted spatial hashing for the map
  private int getHashIndex(Coords location) {
    int yPosX = Math.min((location.x + location.y - 13) / yPosXWidth, maxYPosX);
    int yNegX = Math.min((location.x - location.y + 14) / yNegXWidth, maxYNegX);
    return yNegX + yPosX * (maxYNegX + 1);
  }
  private int getHashIndex(SimUnit unit) {
    return getHashIndex(unit.getLocation());
  }

  void spawnUnit(Coords location, UnitType type) {
    addUnit(SimUnit.fromAPIUnit(new Unit(type, PlayerId.Player1, config), location.x, location.y));
  }

  private void addUnit(SimUnit unit) {
    unitBuckets.get(getHashIndex(unit)).add(unit);
    unitListArrays[unit.getX()][unit.getY()].add(unit);
    allUnits.add(unit);
    if (unit instanceof StructureUnit) {
      structures.add((StructureUnit) unit);
      if (unit instanceof Wall) {
        walls.add((Wall) unit);
      } else if (unit instanceof SupportTower) {
        supportTowers.add((SupportTower) unit);
      } else if (unit instanceof Turret) {
        turrets.add((Turret) unit);
      }
    } else if (unit instanceof MobileUnit) {
      mobileUnits.add((MobileUnit) unit);
      if (unit instanceof Scout) {
        scouts.add((Scout) unit);
      } else if (unit instanceof Interceptor) {
        interceptors.add((Interceptor) unit);
      } else if (unit instanceof Demolisher) {
        demolishers.add((Demolisher) unit);
      }
    }
//    for (Set<? extends SimUnit> unitSet : unitSetListMap.get(unit.getClass())) {
//      addToSet(unitSet, unit);
////      Class<? extends SimUnit> clazz = unit.getClass();
////      unitSet.add(clazz.cast(unit));
//    }
  }

  private void removeUnit(SimUnit unit) {
    unitBuckets.get(getHashIndex(unit)).remove(unit);
    unitListArrays[unit.getX()][unit.getY()].remove(unit);
    allUnits.remove(unit);
    if (unit instanceof StructureUnit) {
      structures.remove(unit);
      if (unit instanceof Wall) {
        walls.remove(unit);
      } else if (unit instanceof SupportTower) {
        supportTowers.remove(unit);
      } else if (unit instanceof Turret) {
        turrets.remove(unit);
      }
    } else if (unit instanceof MobileUnit) {
      mobileUnits.remove(unit);
      if (unit instanceof Scout) {
        scouts.remove(unit);
      } else if (unit instanceof Interceptor) {
        interceptors.remove(unit);
      } else if (unit instanceof Demolisher) {
        demolishers.remove(unit);
      }
    }
  }

  /**
   * moves a unit from one bucket to another
   * @return true if the unit bucket changed
   */
  public boolean updateUnitBucket(SimUnit unit, Coords prevLocation) {
    int oldIndex = getHashIndex(prevLocation);
    int unitIndex = getHashIndex(unit);
    if (oldIndex == unitIndex) {
      return false;
    }
    unitBuckets.get(oldIndex).remove(unit);
    unitBuckets.get(unitIndex).add(unit);
    return true;
  }

  /**
   * updates the necessary fields based on a unit's movement
   * @return true if the unit actually moved
   */
  public boolean updateUnitLocation(SimUnit unit, Coords prevLocation) {
    if (unit.getLocation().equals(prevLocation)) {
      return false;
    }
    updateUnitBucket(unit, prevLocation);
    return true;
  }

  public List<SimUnit> interactableUnits(SimUnit unit) {
    if (unit.getRange() == 0) {
      return List.of();
    }

    // determine which buckets to check in
    // in the worst case reduces by only half
    Set<Integer> bucketIndices = new HashSet<>(9);
    int unitYP = Math.min((unit.getX() + unit.getY() - 13) / yPosXWidth, maxYPosX);
    int unitYN = Math.min((unit.getX() - unit.getY() + 14) / yNegXWidth, maxYNegX);
    int tiltedRange = (int) (2 * Math.floor(unit.getRange() / ROOT2));
    int minYP = Math.max(0, unitYP-tiltedRange);
    int maxYP = Math.min(maxYPosX, unitYP+tiltedRange);
    int minYN = Math.max(0, unitYN-tiltedRange);
    int maxYN = Math.min(maxYNegX, unitYN+tiltedRange);
    for (int yPosX = minYP; yPosX <= maxYP; yPosX++) {
      for (int yNegX = minYN; yNegX <= maxYN; yNegX++) {
        bucketIndices.add(yNegX + yPosX * (maxYNegX + 1));
      }
    }

    // Check each bucket
    return bucketIndices.stream().flatMap(hashIndex -> unitBuckets.get(hashIndex).stream()).filter(unit::testUnitInteractable).collect(Collectors.toUnmodifiableList());
  }


  public boolean hasWall(Coords coords) {
    for (SimUnit unit : this.unitListArrays[coords.x][coords.y]) {
      if (unit instanceof StructureUnit && unit.getLocation().equals(coords)) {
        return true;
      }
    }
    return false;
  }

  /**
   * performs a complete simulation of the board state until all mobile units are dead
   */
  public void simulate() {
    while (mobileUnits.size() > 0) {
      if (DEBUG) debugPrint();

      simulateMovement();

      simulateTargeting();

      simulateShielding();

      simulateAttacks();

      simulateDeaths();
    }
  }

  /**
   * move all mobile units
   */
  private void simulateMovement() {
    for (MobileUnit mobileUnit : mobileUnits) {
      Coords prevLocation;
      if ((prevLocation = mobileUnit.attemptMove()) != null) {
        if (!updateUnitLocation(mobileUnit, prevLocation)) { // executes if the mobile unit moved to the same place
          if (mobileUnit.onTargetEdge()) {
            if (mobileUnit.isEnemy()) p1Health--;
            else p2Health--;
          } else if (mobileUnit.getTimesMoved() >= minMovementsToSelfDestruct) {
            simulateSelfDestruct(mobileUnit);
          }
        }
      }
    }
  }

  /**
   * simulate the damage done by the provided mobile unit self destructing
   */
  private void simulateSelfDestruct(MobileUnit mobileUnit) {
    mobileUnit.takeDamage(mobileUnit.getHealth());
    double selfDestructDamage = mobileUnit.getStartHealth();
    for (int x = mobileUnit.getX() - 1; x <= mobileUnit.getX() + 1; x++) {
      for (int y = mobileUnit.getY() - 1; y <= mobileUnit.getY() + 1; y++) {
        for (SimUnit unit : unitListArrays[x][y]) {
          if (unit instanceof StructureUnit && unit.isEnemy() != mobileUnit.isEnemy()) {
            unit.takeDamage(selfDestructDamage);
          }
        }
      }
    }
  }

  /**
   * updates the list of interactable units for every unit
   */
  private void simulateTargeting() {
    for (Set<SimUnit> unitSet : unitBuckets.values()) {
      for (SimUnit unit : unitSet) {
        unit.setInteractable(interactableUnits(unit));
      }
    }
  }

  /**
   * perform shielding for all support towers
   */
  private void simulateShielding() {
    for (SupportTower supportTower : supportTowers) {
      for (SimUnit unit : supportTower.getInteractable()) {
        ((MobileUnit) unit).applyShielding(supportTower.getShieldAmount());
      }
    }
  }

  /**
   * simulates the attack process of every unit on the board
   */
  private void simulateAttacks() {
    for (SimUnit unit : allUnits) {
      double walkerDamage = unit.getWalkerDamage();
      double structureDamage = unit.getStructureDamage();
      if (walkerDamage > 0 || structureDamage > 0) {
        SimUnit target = unit.chooseTarget();
        if (target != null) {
          target.takeDamage(target instanceof StructureUnit ? structureDamage : walkerDamage);
        }
      }
    }
  }

  /**
   * removes all units with 0 health
   */
  private void simulateDeaths() {
    for (Set<SimUnit> unitSet : unitBuckets.values()) {
      for (SimUnit unit : unitSet) {
        if (unit.getHealth() <= 0) {
          removeUnit(unit);
        }
      }
    }
  }

  public float getP1Health() {
    return p1Health;
  }

  public float getP2Health() {
    return p2Health;
  }

  private void debugPrint() {
    for (int y = MapBounds.BOARD_SIZE - 1; y >= 0; y--) {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("%2d ", y));
      for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
        sb.append(boardLocationToChar(x, y));
      }
      GameIO.debug().println(sb.toString());
    }
    StringBuilder sb = new StringBuilder();
    sb.append("   ");
    for(int i = 0; i < MapBounds.BOARD_SIZE; i++){
      sb.append(String.format("%2d ", i));
    }
    GameIO.debug().println(sb.toString());
  }

  private String boardLocationToChar(int x, int y) {
    if (!MapBounds.ARENA[x][y] || this.unitListArrays[x][y] == null) {
      return "   ";
    }
    List<SimUnit> units = unitListArrays[x][y];
    if (units.isEmpty()) {
      return " . ";
    }
    SimUnit unit = units.get(0);
    if (unit instanceof StructureUnit)
      return " " + StructureUnit.toChar(unit) + " ";
    return String.format("%2d ", units.size());
    }
}
