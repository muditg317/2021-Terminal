package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.*;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.myalgo.utility.Utility;
import com.c1games.terminal.simulation.pathfinding.PathFinder;
import com.c1games.terminal.simulation.units.*;

import java.util.*;

public class SimBoard {
  private final static double ROOT2 = 1.41421356237;
  private final static int yPosXWidth = 7;
  private final static int maxYPosX = 4;
  private final static int yNegXWidth = 7;
  private final static int maxYNegX = 4;
  private final static int numBuckets = (maxYNegX+1) * (maxYPosX+1);

  private Config config;
//  private FrameData data;
  private int minMovementsToSelfDestruct;

  private final List<List<SimUnit>> unitBuckets;
  private final List<SimUnit>[][] unitListArrays;
  private final List<SimUnit> allUnits;
  private final List<StructureUnit> structures;
  private final List<Wall> walls;
  private final List<SupportTower> supportTowers;
  private final List<Turret> turrets;
  private final List<MobileUnit> mobileUnits;
  private final List<Scout> scouts;
  private final List<Interceptor> interceptors;
  private final List<Demolisher> demolishers;
//  private final Map<Class<? extends SimUnit>, List<List<? extends SimUnit>>> unitSetListMap;
  private float p1Health;
  private float p2Health;
  private Set<Coords> traversedPoints;

  public SimBoard() {
    unitBuckets = new ArrayList<>(numBuckets);
    for (int i = 0; i < numBuckets; i++) {
      unitBuckets.add(new ArrayList<>());
    }
    @SuppressWarnings("unchecked") // the arraylist type will be correct
    List<SimUnit>[][] unitListArrays = new ArrayList[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];
    for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        unitListArrays[x][y] = new ArrayList<>();
      }
    }
    this.unitListArrays = unitListArrays;
    this.allUnits = new ArrayList<>();
    this.structures = new ArrayList<>();
    this.walls = new ArrayList<>();
    this.supportTowers = new ArrayList<>();
    this.turrets = new ArrayList<>();
    this.mobileUnits = new ArrayList<>();
    this.scouts = new ArrayList<>();
    this.interceptors = new ArrayList<>();
    this.demolishers = new ArrayList<>();
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
    config = move.config;
//    data = move.data;
    p1Health = move.data.p1Stats.integrity;
    p2Health = move.data.p2Stats.integrity;
    minMovementsToSelfDestruct = 5; // move.config.mechanics.stepsRequiredSelfDestruct; <- mechanics is null for some reason
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

  private void removeUnit(SimUnit unit, int allUnitsIndex) {
    unitBuckets.get(getHashIndex(unit)).remove(unit);
    unitListArrays[unit.getX()][unit.getY()].remove(unit);
    allUnits.remove(allUnitsIndex);
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
  private boolean updateUnitBucket(SimUnit unit, Coords prevLocation) {
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
  private boolean updateUnitLocation(SimUnit unit, Coords prevLocation) {
    if (unit.getLocation().equals(prevLocation)) {
      return false;
    }
    updateUnitBucket(unit, prevLocation);
    unitListArrays[prevLocation.x][prevLocation.y].remove(unit);
    unitListArrays[unit.getX()][unit.getY()].add(unit);
    return true;
  }

  private void updateInteractableUnits(SimUnit unit) {
    if (unit.getRange() == 0) {
      unit.setInteractable(List.of());
    }
    List<SimUnit> interactable = unit.getInteractable();
    if (interactable != null && (unit instanceof StructureUnit || (unit instanceof MobileUnit && ((MobileUnit) unit).getFramesSinceMoved() < ((MobileUnit) unit).getSpeed()))) {
      for (int i = interactable.size() - 1; i >= 0; i--) {
        SimUnit simUnit = interactable.get(i);
        if (simUnit instanceof MobileUnit && ((MobileUnit) simUnit).getFramesSinceMoved() == ((MobileUnit) simUnit).getSpeed() && !unit.testUnitInteractable(simUnit)) {
          interactable.remove(i);
        }
      }
    }

    // determine which buckets to check in
    // in the worst case reduces by only half
    boolean[] goodBuckets = new boolean[numBuckets];
    int unitYP = Math.min((unit.getX() + unit.getY() - 13) / yPosXWidth, maxYPosX);
    int unitYN = Math.min((unit.getX() - unit.getY() + 14) / yNegXWidth, maxYNegX);
    int tiltedRange = (int) (2 * Math.floor(unit.getRange() / ROOT2));
    int minYP = Math.max(0, unitYP-tiltedRange);
    int maxYP = Math.min(maxYPosX, unitYP+tiltedRange);
    int minYN = Math.max(0, unitYN-tiltedRange);
    int maxYN = Math.min(maxYNegX, unitYN+tiltedRange);
    for (int yPosX = minYP; yPosX <= maxYP; yPosX++) {
      for (int yNegX = minYN; yNegX <= maxYN; yNegX++) {
        goodBuckets[yNegX + yPosX * (maxYNegX + 1)] = true;
      }
    }


    // Check each bucket
    if (interactable == null) {
      interactable = new ArrayList<>((int) (unit.getRange()*10));
    }
    for (int i = 0; i < numBuckets; i++) {
      if (goodBuckets[i]) {
        List<SimUnit> bucket = unitBuckets.get(i);
//      Iterator<SimUnit> unitIterator = bucket.iterator();
        int bucketSize = bucket.size();
        for (int j = 0; j < bucketSize; j++) {
          SimUnit potentialTarget = bucket.get(j);
          if (unit instanceof MobileUnit && ((MobileUnit) unit).getFramesSinceMoved() == ((MobileUnit) unit).getSpeed() && unit.testUnitInteractable(potentialTarget)) {
            interactable.add(potentialTarget);
          }
        }
      }
    }
    unit.setInteractable(interactable);
//    return bucketIndices.stream().flatMap(hashIndex -> unitBuckets.get(hashIndex).stream()).filter(unit::testUnitInteractable).collect(Collectors.toUnmodifiableList());
  }


  public boolean hasWall(Coords coords) {
    for (SimUnit unit : this.unitListArrays[coords.x][coords.y]) {
      if (unit instanceof StructureUnit) {
        return true;
      }
    }
    return false;
  }

  public StructureUnit getStructureAt(Coords coords) {
    for (SimUnit unit : this.unitListArrays[coords.x][coords.y]) {
      if (unit instanceof StructureUnit) {
        return (StructureUnit) unit;
      }
    }
    return null;
  }

  /**
   * performs a complete simulation of the board state until all mobile units are dead
   */
  public void simulate() {
    allUnits.sort(Comparator.comparingInt(SimUnit::getID));
    traversedPoints = new HashSet<>();
    while (mobileUnits.size() > 0) {
      if (Simulator.DEBUG) debugPrint();

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
    for (int i = 0, mobileUnitsSize = mobileUnits.size(); i < mobileUnitsSize; i++) {
      MobileUnit mobileUnit = mobileUnits.get(i);
      Coords prevLocation;
      if ((prevLocation = mobileUnit.attemptMove()) != null) {
        traversedPoints.add(prevLocation);
        if (!updateUnitLocation(mobileUnit, prevLocation)) { // executes if the mobile unit moved to the same place
          if (mobileUnit.onTargetEdge()) {
            if (mobileUnit.isEnemy()) p1Health--;
            else p2Health--;
            mobileUnit.takeDamage(mobileUnit.getHealth());
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
    int minX = Math.max(mobileUnit.getX() - 1, 0);
    int maxX = Math.min(mobileUnit.getX() + 1, MapBounds.BOARD_SIZE - 1);
    int minY = Math.max(mobileUnit.getY() - 1, 0);
    int maxY = Math.min(mobileUnit.getY() + 1, MapBounds.BOARD_SIZE - 1);
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        List<SimUnit> simUnits = unitListArrays[x][y];
        for (int i = 0, simUnitsSize = simUnits.size(); i < simUnitsSize; i++) {
          SimUnit unit = simUnits.get(i);
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
    for (int i = 0, allUnitsSize = allUnits.size(); i < allUnitsSize; i++) {
      SimUnit unit = allUnits.get(i);
      updateInteractableUnits(unit);
    }
  }

  /**
   * perform shielding for all support towers
   */
  private void simulateShielding() {
    for (int i = 0, supportTowersSize = supportTowers.size(); i < supportTowersSize; i++) {
      SupportTower supportTower = supportTowers.get(i);
      for (SimUnit unit : supportTower.getInteractable()) {
        supportTower.shield((MobileUnit) unit);
      }
    }
  }

  /**
   * simulates the attack process of every unit on the board
   */
  private void simulateAttacks() {
    for (int i = 0, allUnitsSize = allUnits.size(); i < allUnitsSize; i++) {
      SimUnit unit = allUnits.get(i);
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
    boolean anyDead = false;
    for (int i = allUnits.size() - 1; i >= 0; i--) {
      SimUnit unit = allUnits.get(i);
      if (unit.getHealth() <= 0) {
        removeUnit(unit, i);
        anyDead = true;
      }
    }
    if (anyDead) {
      PathFinder.forceUpdate(this);
    }
  }

  public double enemySPOnBoard() {
    double enemySP = 0;
    for (int x = 0; x < unitListArrays.length; x++) {
      List<SimUnit>[] row = unitListArrays[x];
      for (int y = 0; y < row.length; y++) {
        List<SimUnit> units = row[y];
        if (units.isEmpty() || units.get(0) instanceof MobileUnit) {
          continue;
        }
        // there is a structure here
        StructureUnit unit = (StructureUnit) units.get(0);
        if (unit.isEnemy()) {
          Config.UnitInformation info = unit.getConfig();
          enemySP += Utility.healthToSP(info, unit.getHealth());
        }
      }
    }
    return enemySP;
  }

  public float getP1Health() {
    return p1Health;
  }

  public float getP2Health() {
    return p2Health;
  }

  public Set<Coords> getTraversedPoints() {
    return traversedPoints;
  }

  public List<SimUnit> getAllUnits() {
    return allUnits;
  }

  public List<StructureUnit> getStructures() {
    return structures;
  }

  public List<Wall> getWalls() {
    return walls;
  }

  public List<SupportTower> getSupportTowers() {
    return supportTowers;
  }

  public List<Turret> getTurrets() {
    return turrets;
  }

  public List<MobileUnit> getMobileUnits() {
    return mobileUnits;
  }

  public List<Scout> getScouts() {
    return scouts;
  }

  public List<Interceptor> getInterceptors() {
    return interceptors;
  }

  public List<Demolisher> getDemolishers() {
    return demolishers;
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
