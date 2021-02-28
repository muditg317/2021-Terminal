package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

import java.util.*;
import java.util.stream.Collectors;

public class Boom {
  private static final Coords[] leftPath = {
      new Coords(3, 11),
      new Coords(2, 11),
      new Coords(2, 12),
      new Coords(1, 12),
      new Coords(1, 13),
      new Coords(1, 14),
      new Coords(0 ,14)
  };
  private static final Coords[] rightPath = {
      new Coords(24, 11),
      new Coords(25, 11),
      new Coords(25, 12),
      new Coords(26, 12),
      new Coords(26, 13),
      new Coords(26, 14),
      new Coords(27 ,14)
  };
  public static boolean awaitingBoom = false;
  public static int turnsUntilBoom = -1;
  public static Side side;
  private static AttackBreakdown attack;

  static double onlineAdjustment = 1;

  Side sideToBoom;
  int bombUnits;
  UnitType bombType;
  int followerUnits;
  int expectedDamage;

  public Boom(Side sideToBoom, int bombUnits, UnitType bombType, int followerUnits) {
    this.sideToBoom = sideToBoom;
    this.bombUnits = bombUnits;
    this.bombType = bombType;
    this.followerUnits = followerUnits;
  }

  Coords getBombStart() {
    return this.bombType == UnitType.Interceptor
        ? new Coords(this.sideToBoom == Side.RIGHT ? 23 : 27 - 23, 9)
        : new Coords(this.sideToBoom == Side.RIGHT ? 10 : 27 - 10, 3);
  }

  Coords getFollowerStart() {
    return this.bombType == UnitType.Interceptor
        ? new Coords(this.sideToBoom == Side.RIGHT ? 6 : 27 - 6, 7)
        : new Coords(this.sideToBoom == Side.RIGHT ? 9 : 27 - 9, 4);
  }

  static void evaluate(GameState move, int expectedMpSpentPerTurn) {
    if (Boom.awaitingBoom && Boom.turnsUntilBoom == 0) {
      awaitingBoom = false;
      turnsUntilBoom = -99;
    }
    if (Boom.awaitingBoom) {
      Boom.turnsUntilBoom--;
    }
    GameIO.debug().println("BOOM DECISION: ===========");
    float mp = move.data.p1Stats.bits;
    int MAX_EXTRAPOLATION_TURNS = Math.min(3, move.data.turnInfo.turnNumber / 5);
    if (Boom.turnsUntilBoom != 0) { //we WILL boom this turn. no need to check.
      boolean shouldStillBoom = false;
      int turns = 1;
      if (Boom.turnsUntilBoom == -99) {
        Boom.turnsUntilBoom = -1;
        turns = 0;
      }
      for (; turns <= MAX_EXTRAPOLATION_TURNS; turns++) {
        AttackBreakdown futureAttack = futureAttackThreshold(move, turns);
        Boom.side = futureAttack.side;
        float futureAttackThreshold = futureAttack.units.cost;
        float futureMP = extrapolateFutureMP(move, turns, expectedMpSpentPerTurn);
//        GameIO.debug().println("futureAttackThreshold: " + futureAttackThreshold);
//        GameIO.debug().println("futureMP: " + futureMP);
        if (futureMP >= futureAttackThreshold) {
          GameIO.debug().println("about to boom..." + mp + " / " + futureAttackThreshold + " reached -- expecting: " + futureMP +" in " + turns +" turns ||| started turn with: " + mp);
          Boom.awaitingBoom = true;
          Boom.turnsUntilBoom = turns;
          shouldStillBoom = true;
          break;
        }
      }

      if (!shouldStillBoom) {
        Boom.awaitingBoom = false;
        Boom.turnsUntilBoom = -1;
      }
    } else { //booming this turn
      AttackBreakdown attack = attackThreshold(move);
      Boom.attack = attack;
      Boom.side = attack.side;

    }
    //end boom decision

    Boom.debugPrint();
  }

  /**
   * Does the Boom. Does not set awaitingBoom and turnsUntilBoom.
   * @return True if executed, false if not
   */
  static boolean execute(GameState move) {
    Side boomSide = Boom.side;
    UnitCounts attackUnits = attack.units;
    int attackPoints = attackUnits.cost;
    int numInters = attackUnits.numInterceptors; //these are actuall scouts lul
    GameIO.debug().println("Going to boom right now");
    GameIO.debug().println("Cores:" + move.data.p1Stats.cores);
    if (!Boom.clearBoomPath(move, boomSide)) {
      GameIO.debug().println("BOOM PATH BLOCKED!!");
      return false;
    }
    if (!Boom.placeBoomLid(move, boomSide)) {
      GameIO.debug().println("BOOM LID NOT FINISHED!!");
      return false;
    }

//    SpawnUtility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals(Side.RIGHT) ? 23 : 4, 9)}, numInters);
//    SpawnUtility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals(Side.RIGHT) ? 6 : 21, 7)}, (int) move.data.p1Stats.bits);

    SpawnUtility.spawnScouts(move, new Coords(boomSide == Side.RIGHT ? 10 : 27 - 10, 3), Math.max(numInters, 3));
    SpawnUtility.spawnScouts(move, new Coords(boomSide == Side.RIGHT ? 9 : 27 - 9, 4), (int) move.data.p1Stats.bits);

    SpawnUtility.removeBuilding(move, new Coords(boomSide == Side.RIGHT ? 9 : 27 - 9, 5));
    SpawnUtility.removeBuilding(move, new Coords(4, 11));
//    Boom.awaitingBoom = false;
//    Boom.turnsUntilBoom = -99;
    return true;
  }

  /**
   * returns true if the lid is successfully placed
   * @param boomSide
   * @return
   */
  static boolean placeBoomLid(GameState move, Side boomSide) {
    List<Coords> toPlace = Arrays.stream(Locations.boomLid_right)
        .map(coords -> new Coords(boomSide == Side.RIGHT ? coords.x : (27 - coords.x), coords.y))
        .filter(coords -> move.getWallAt(coords) == null)
        .collect(Collectors.toList());
    // check if we have anough sp to fill the lid
    if (move.data.p1Stats.bits < toPlace.size() * move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1)) return false;

    // place the lid
    toPlace.forEach(coords -> SpawnUtility.placeWall(move, coords));

    // try placing shields
    double supportCost = move.config.unitInformation.get(UnitType.Support.ordinal()).cost1.orElse(4);
    int affordableSupports = (int) (move.data.p1Stats.cores / supportCost);
    GameIO.debug().printf("Booming with %d support towers\n", affordableSupports);
    List<Coords> neededSupports = Arrays.asList(Locations.safeSupportLocations);
    int maxY = neededSupports.stream().limit(affordableSupports).mapToInt(coords -> coords.y).max().orElse(0);
    GameIO.debug().printf("max Y: %d\n",maxY);
    List<Coords> defensiveWalls = new ArrayList<>();
    if (neededSupports.size() > 0 && maxY >= 3) { // place protective walls over the supports
      int wallY = maxY + 1;
      for (int x = 15-wallY; x <= (wallY+12); x++) {
        Coords wallPos = new Coords(x, wallY);
        if (move.getWallAt(wallPos) == null) {
          defensiveWalls.add(wallPos);
        }
      }
      GameIO.debug().printf("Need walls: %s\n",defensiveWalls);
      int neededWalls = defensiveWalls.size();
      double expectedWallCost = neededWalls * move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);
      while (wallY > 3 && expectedWallCost > (move.data.p1Stats.cores - neededSupports.size() * supportCost)) {
        GameIO.debug().printf("Not enough SP for wall! SP: %.2f, support cost: %.2f, wall cost: %.2f\n",move.data.p1Stats.cores, neededSupports.size()*supportCost, expectedWallCost);
        int finalMaxY = maxY;
        neededSupports = neededSupports.stream().filter(coords -> coords.y < finalMaxY).collect(Collectors.toList());
        maxY = neededSupports.stream().mapToInt(coords -> coords.y).max().orElse(0);
        GameIO.debug().printf("new Max Y: %d\n",maxY);
        wallY = maxY + 1;
        defensiveWalls = new ArrayList<>();
        for (int x = 15-wallY; x <= (wallY+12); x++) {
          Coords wallPos = new Coords(x, wallY);
          if (move.getWallAt(wallPos) == null) {
            defensiveWalls.add(wallPos);
          }
        }
        neededWalls = defensiveWalls.size();
        expectedWallCost = neededWalls * move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);
      }
    }
    GameIO.debug().println(defensiveWalls);
    GameIO.debug().println(neededSupports);

    //place walls to protect the supports
    Coords[] wallArray = defensiveWalls.toArray(new Coords[0]);
    SpawnUtility.placeWalls(move, wallArray);
    SpawnUtility.removeBuildings(move, wallArray);
    //now put down supports
    Coords[] supportArray = neededSupports.toArray(new Coords[0]);
    SpawnUtility.placeSupports(move, supportArray);
    SpawnUtility.removeBuildings(move, supportArray);

    return Arrays.stream(Locations.boomLid_right)
        .noneMatch(closeLocation -> move.getWallAt(new Coords(
            boomSide.equals(Side.RIGHT) ? closeLocation.x : (27 - closeLocation.x),
            closeLocation.y)) == null);
  }

  /**
   * clears an attack path for a boom attack
   * @param side the side which to hit
   * @return whether the path was already clear
   */
  static boolean clearBoomPath(GameState move, Side side) {
    boolean alreadyReady = true;
    for (int i = 0; i < Locations.boomPath_right.length; i++) {
      Coords openLocation = Locations.boomPath_right[i];
      if (openLocation.y > 13) continue;
      int x = side == Side.RIGHT ? openLocation.x : (27 - openLocation.x);
      Coords toOpen = new Coords(x, openLocation.y);
      boolean wasReady = alreadyReady;
      alreadyReady = !SpawnUtility.removeBuilding(move, toOpen) && alreadyReady;
      if (wasReady && !alreadyReady) {
        Unit wall = move.getWallAt(toOpen);
        GameIO.debug().printf("BOOM PATH NOT CLEARED: %s - %s\n", toOpen, wall == null ? "null" : wall.unitInformation.display.orElse("some tower"));
      }
    }
    return true; //alreadyReady
  }

  static void debugPrint() {
    GameIO.debug().println("awaitingBoom:\t" + Boom.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom:\t" + Boom.turnsUntilBoom);
  }

  /**
   * Returns the weaker side (Side.LEFT, Side.RIGHT) and the num of estimated bits needed to break the wall
   * @param move  the game state for which we're predicting
   * @return AttackBreakdown with where to attack and the related units needed
   */
  static AttackBreakdown attackThreshold(GameState move) {
    UnitCounts leftUnitCounts = enemyDefenseHeuristic(move, Side.LEFT);
    int leftCost = leftUnitCounts.cost;

    UnitCounts rightUnitCounts = enemyDefenseHeuristic(move, Side.RIGHT);
    int rightCost = rightUnitCounts.cost;

    int enemyHealth = (int) move.data.p2Stats.integrity;
    int minDamage = Math.min(10, enemyHealth);


    Side weakerSide = leftCost < rightCost ? Side.LEFT : Side.RIGHT;
    UnitCounts correctUnitCounts = leftCost < rightCost ? leftUnitCounts : rightUnitCounts;
    correctUnitCounts.numScouts = minDamage;
    correctUnitCounts.cost += minDamage;
    return new AttackBreakdown(weakerSide, correctUnitCounts);
  }

  /**
   * Returns the estimated bits needed to break the wall in X turns based on the weaker side.
   * @param move   the game state we are predicting for
   * @param turns  the number of turns we will look into the future
   * @return AttackBreakdown with where to attack and the related units needed
   */
  static AttackBreakdown futureAttackThreshold(GameState move, int turns) {
    // TODO: FIX ME
    // TODO: apply some scaling if they have more turns available
    AttackBreakdown curr = attackThreshold(move);
    curr.units.scale(1.2f);
    return curr;
  }

  /**
   * Guess of how strong their corner defense is on a certain side
   * @param move the game state to inspect
   * @param side which side we want to measure
   * @return UnitCounts with the number of scouts needed to only breach their wall
   */
  static UnitCounts enemyDefenseHeuristic(GameState move, Side side) {
    int enemyHealth = (int) move.data.p2Stats.integrity; //min amount we need to hit them by
    //walls only matter in a few locations -> we include turret healths since they are "walls"

    ExpectedDefense cornerSummary = enemyCornerSummary(move, side);
    int scoutDamage = (int) move.config.unitInformation.get(UnitType.Scout.ordinal()).attackDamageTower.orElse(2);
    int scoutBaseHealth = (int) move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);
    int expectedShielding = (int) (move.data.p1Stats.cores * 3.0 / 4.0);
    int scoutHealth = scoutBaseHealth + expectedShielding;

    int estScoutsKilled = (int) Math.ceil(cornerSummary.expectedScoutDamage / scoutHealth);
    int survivingScoutsNeeded = (int) cornerSummary.structureHealth / (scoutBaseHealth + 2 * scoutDamage);
    int boomScoutsNeeded = estScoutsKilled + survivingScoutsNeeded;
//    GameIO.debug().println(String.format("cornerSummary structureHealth: %s\n cornerSummary expectedDamage: %s\n",
//        cornerSummary.structureHealth, cornerSummary.expectedIntercepterDamage));
  //TODO: Fix boomScoutsNeeded. right now it is numInterceptors since i am too lazy :D
    return new UnitCounts(move, 0, boomScoutsNeeded, 0);
  }

  static Boom bestBoom(GameState move, double availableMP, double minDamage) {
    double interCost = move.config.unitInformation.get(UnitType.Interceptor.ordinal()).cost2.orElse(1);
    double scoutCost = move.config.unitInformation.get(UnitType.Interceptor.ordinal()).cost2.orElse(1);

    GameIO.debug().println("====EVALUATING BOOM====");

    Map<Boom, Double> damages = new HashMap<>();
    for (Side side : Side.values()) {
      GameState testState = Utility.duplicateState(move);

      placeBoomLid(testState, side);
      clearBoomPath(testState, side);
      
      for (int bombMP = 1; bombMP < availableMP; bombMP++) {
        int interBomb = (int) (bombMP / interCost);
        int scoutsFollowInter = (int) ((availableMP - (interBomb*interCost)) / scoutCost);
        Boom interBombBoom = new Boom(side, interBomb, UnitType.Interceptor, scoutsFollowInter);
        double interBoomDamage = interBombBoom.simulateDamage(testState);
        if (interBoomDamage > minDamage/2) {
          damages.put(interBombBoom, interBoomDamage);
        }

        int scoutBomb = (int) (bombMP / scoutCost);
        int scoutsFollowScout = (int) ((availableMP - (scoutBomb*scoutCost)) / scoutCost);
        Boom scoutBombBoom = new Boom(side, scoutBomb, UnitType.Scout, scoutsFollowScout);
        double scoutBoomDamage = scoutBombBoom.simulateDamage(testState);
        if (scoutBoomDamage > minDamage/2) {
          damages.put(scoutBombBoom, scoutBoomDamage);
        }
      }
    }
    
    Boom bestBoom = null;
    double bestDamage = minDamage / 2;
    for (Map.Entry<Boom, Double> entry : damages.entrySet()) {
      if (entry.getValue() > bestDamage) {
        bestBoom = entry.getKey();
        bestDamage = entry.getValue();
      }
    }
    if (bestBoom == null || bestDamage < minDamage) {
      GameIO.debug().println("Not doing enough damage!");
      damages.forEach((key, value) -> GameIO.debug().printf("%s, %s bomb. %d bombers, %d followers. damage:%.2f, need:%.2f\n",
          key.sideToBoom.toString(), key.bombType.toString(),
          key.bombUnits, key.followerUnits,
          value, minDamage));
      if (bestBoom != null) {
        GameIO.debug().printf("Current best hook: %s, %s bomb. %d bombers, %d followers. damage:%.2f out of %.2f\n",
            bestBoom.sideToBoom.toString(), bestBoom.bombType.toString(),
            bestBoom.bombUnits, bestBoom.followerUnits,
            bestDamage, minDamage);
      }
      return null;
    }

    GameIO.debug().printf("Current best hook: %s, %s bomb. %d bombers, %d followers. damage:%.2f out of %.2f\n",
        bestBoom.sideToBoom.toString(), bestBoom.bombType.toString(),
        bestBoom.bombUnits, bestBoom.followerUnits,
        bestDamage, minDamage);
    
    return bestBoom;
  }

  private double simulateDamage(GameState state) {
    GameState boomState = Utility.duplicateState(state);

    placeBoomLid(boomState, this.sideToBoom);
    clearBoomPath(boomState, this.sideToBoom);


    Coords bombStart = getBombStart();
    List<Coords> bombPath;
    int bombTargetEdge = (this.sideToBoom == Side.RIGHT && this.bombType == UnitType.Scout) ? MapBounds.EDGE_TOP_RIGHT : MapBounds.EDGE_BOTTOM_LEFT;
    try {
      bombPath = boomState.pathfind(bombStart, bombTargetEdge);
    } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid hook exit\n", x, y);
      return 0;
    }

    Coords followerStart = getFollowerStart();
    List<Coords> followerPath;
    int followerTargetEdge = this.sideToBoom == Side.RIGHT ? MapBounds.EDGE_TOP_RIGHT : MapBounds.EDGE_BOTTOM_LEFT;
    try {
      followerPath = boomState.pathfind(followerStart, followerTargetEdge);
    } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid hook exit\n", x, y);
      return 0;
    }

    Config.UnitInformation bombInfo = boomState.config.unitInformation.get(this.bombType.ordinal());
    Config.UnitInformation followerInfo = boomState.config.unitInformation.get(UnitType.Scout.ordinal());
    double bombDamage = bombInfo.attackDamageTower.orElse(this.bombType == UnitType.Interceptor ? 0 : 2);
    double followerDamage = followerInfo.attackDamageTower.orElse(2);

    double bombHealth = bombInfo.startHealth.orElse(5);
    List<Double> bombHealths = new ArrayList<>(this.bombUnits);
    List<Set<Unit>> bombShielders = new ArrayList<>(this.bombUnits);
    for (int d = 0; d < this.bombUnits; d++) {
      bombHealths.add(bombHealth);
      bombShielders.add(new HashSet<>(boomState.data.p1Units.support.size()));
    }

    double followerHealth = followerInfo.startHealth.orElse(5);
    List<Double> followerHealths = new ArrayList<>(this.followerUnits);
    List<Set<Unit>> followerShielders = new ArrayList<>(this.followerUnits);
    for (int d = 0; d < this.followerUnits; d++) {
      followerHealths.add(followerHealth);
      followerShielders.add(new HashSet<>(boomState.data.p1Units.support.size()));
    }

    float damageToBase = 0;
    float spTaken = 0;
    int p;
    for (p = 0; p < Math.min(bombPath.size(), followerPath.size()) && p < 100; p++) {
      Coords bombPathPoint = bombPath.get(p);
      Map<Unit, Coords> bombAttackerLocations = StrategyUtility.getTowerLocations(boomState, bombPathPoint, bombInfo.attackRange.orElse(this.bombType == UnitType.Interceptor ? 0 : 3.5));
      List<Unit> bombAttackers = new ArrayList<>(bombAttackerLocations.keySet());
      bombAttackers.sort((o1, o2) -> (int) Math.signum(bombPathPoint.distance(bombAttackerLocations.get(o2)) - bombPathPoint.distance(bombAttackerLocations.get(o1))));

      Coords followerPathPoint = followerPath.get(p);
      Map<Unit, Coords> followerAttackerLocations = StrategyUtility.getTowerLocations(boomState, followerPathPoint, followerInfo.attackRange.orElse(3.5));
      List<Unit> followerAttackers = new ArrayList<>(followerAttackerLocations.keySet());
      followerAttackers.sort((o1, o2) -> (int) Math.signum(followerPathPoint.distance(followerAttackerLocations.get(o2)) - followerPathPoint.distance(followerAttackerLocations.get(o1))));

      // apply shielding first!
      for (Unit attacker : bombAttackers) {
        if (attacker.owner == PlayerId.Player1) {
          if (attacker.type == UnitType.Support) {
            double shieldAmount = attacker.unitInformation.shieldPerUnit.orElse(attacker.upgraded ? 5 : 3) + (attacker.upgraded ? (attacker.unitInformation.shieldBonusPerY.orElse(0.3) * bombAttackerLocations.get(attacker).y) : 0);
            for (int d = 0; d < bombHealths.size(); d++) {
              if (!bombShielders.get(d).contains(attacker)) {
                bombHealths.set(d, bombHealths.get(d) + shieldAmount);
                bombShielders.get(d).add(attacker);
              }
            }
          }
        }
      }
      for (Unit attacker : followerAttackers) {
        if (attacker.owner == PlayerId.Player1) {
          if (attacker.type == UnitType.Support) {
            double shieldAmount = attacker.unitInformation.shieldPerUnit.orElse(attacker.upgraded ? 5 : 3) + (attacker.upgraded ? (attacker.unitInformation.shieldBonusPerY.orElse(0.3) * followerAttackerLocations.get(attacker).y) : 0);
            for (int d = 0; d < followerHealths.size(); d++) {
              if (!followerShielders.get(d).contains(attacker)) {
                followerHealths.set(d, followerHealths.get(d) + shieldAmount);
                followerShielders.get(d).add(attacker);
              }
            }
          }
        }
      }

      // evaluate attacks
      boolean needToRepath = false;
      int numBombsToAttack = bombHealths.size();
      int numFollowersToAttack = followerHealths.size();
      for (int frame = 0; frame < (1/bombInfo.speed.orElse(this.bombType == UnitType.Interceptor ? 0.25 : 1)); frame++) { //run each path point 4 times since inters move every 4 frames
        for (Unit attacker : bombAttackers) {
          if (attacker.owner == PlayerId.Player2) {
            if (bombAttackerLocations.get(attacker).distance(bombPathPoint) <= bombInfo.attackRange.orElse(this.bombType == UnitType.Interceptor ? 0 : 3.5)) {
              float initialTowerHealth = attacker.health;
              while (numBombsToAttack > 0 && attacker.health > 0) {
                attacker.health -= bombDamage;
                numBombsToAttack--;
                if (attacker.health <= 0) {
                  needToRepath = true;
                  break;
                }
              }
              float damageDone = initialTowerHealth - attacker.health;
              spTaken += (float) Utility.damageToSp(attacker, damageDone);
            }

            if (bombHealths.size() > 0) {
              if (bombAttackerLocations.get(attacker).distance(bombPathPoint) <= attacker.unitInformation.attackRange.orElse(attacker.upgraded ? 3.5 : 2.5)) {
                double initialBombHealth = bombHealths.get(bombHealths.size() - 1);
                double towerDamage = attacker.unitInformation.attackDamageWalker.orElse(0);
                bombHealths.set(bombHealths.size() - 1, Math.max(0, initialBombHealth - towerDamage));
                double afterHealth = bombHealths.get(bombHealths.size() - 1);
                double damageToBombers = initialBombHealth - afterHealth;
                if (afterHealth == 0) {
                  bombHealths.remove(bombHealths.size() - 1);
                }
              }
            } else {
              break;
            }
          }
        }
        if (frame < (1/followerInfo.speed.orElse(1))) { //simulate followers (scouts) only on turn 0
          for (Unit attacker : followerAttackers) {
            if (attacker.owner == PlayerId.Player2) {
              if (followerAttackerLocations.get(attacker).distance(followerPathPoint) <= followerInfo.attackRange.orElse(3.5)) {
                float initialTowerHealth = attacker.health;
                while (numFollowersToAttack > 0 && attacker.health > 0) {
                  attacker.health -= followerDamage;
                  numFollowersToAttack--;
                  if (attacker.health <= 0) {
                    needToRepath = true;
                    break;
                  }
                }
                float damageDone = initialTowerHealth - attacker.health;
                spTaken += (float) Utility.damageToSp(attacker, damageDone);
              }

              if (followerHealths.size() > 0) {
                if (followerAttackerLocations.get(attacker).distance(followerPathPoint) <= attacker.unitInformation.attackRange.orElse(attacker.upgraded ? 3.5 : 2.5)) {
                  double initialFollowerHealth = followerHealths.get(followerHealths.size() - 1);
                  double towerDamage = attacker.unitInformation.attackDamageWalker.orElse(0);
                  followerHealths.set(followerHealths.size() - 1, Math.max(0, initialFollowerHealth - towerDamage));
                  double afterHealth = followerHealths.get(followerHealths.size() - 1);
                  double damageToFollowers = initialFollowerHealth - afterHealth;
                  if (afterHealth == 0) {
                    followerHealths.remove(followerHealths.size() - 1);
                  }
                }
              } else {
                break;
              }
            }
          }
        }
      }
      if (needToRepath) {
//          GameIO.debug().printf("REPATHING!for:%s,at:%s\n",bombStart,pathPoint);
        List<Coords> newBombPath;
        try {
          newBombPath = boomState.pathfind(bombPathPoint, bombTargetEdge);
        } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid path point for boom\n", x, y);
          continue;
        }
        bombPath.subList(p, bombPath.size()).clear();
        bombPath.addAll(newBombPath);

        List<Coords> newFollowerPath;
        try {
          newFollowerPath = boomState.pathfind(followerPathPoint, followerTargetEdge);
        } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid path point for boom\n", x, y);
          continue;
        }
        followerPath.subList(p, followerPath.size()).clear();
        followerPath.addAll(newFollowerPath);
      }
    }
    Coords bombEndpoint = bombPath.get(bombPath.size()-1);
    if (!MapBounds.IS_ON_EDGE[bombTargetEdge][bombEndpoint.x][bombEndpoint.y]) {
      for (int x = Math.max(0, bombEndpoint.x-1); x <= Math.min(MapBounds.BOARD_SIZE-1, bombEndpoint.x+1); x++) {
        for (int y = Math.max(0, bombEndpoint.y-1); y <= Math.min(MapBounds.BOARD_SIZE-1, bombEndpoint.y+1); y++) {
          List<Unit> towers = boomState.allUnits[x][y];
          spTaken += (float) towers.stream().mapToDouble(towerUnit -> {
            if (towerUnit.owner == PlayerId.Player2) {
              float initialHealth = towerUnit.health;
              towerUnit.health -= bombHealth;
              towerUnit.health = Math.max(towerUnit.health, 0);
              float damageDone = initialHealth - towerUnit.health;
              return Utility.damageToSp(towerUnit, damageDone);
            }
            return 0;
          }).sum();
        }
      }
    } else {
      damageToBase += bombHealths.size();
    }
    for (; p < followerPath.size() && p < 100; p++) {
      Coords followerPathPoint = followerPath.get(p);
      Map<Unit, Coords> followerAttackerLocations = StrategyUtility.getTowerLocations(boomState, followerPathPoint, followerInfo.attackRange.orElse(3.5));
      List<Unit> followerAttackers = new ArrayList<>(followerAttackerLocations.keySet());
      followerAttackers.sort((o1, o2) -> (int) Math.signum(followerPathPoint.distance(followerAttackerLocations.get(o2)) - followerPathPoint.distance(followerAttackerLocations.get(o1))));

      // apply shielding first!
      for (Unit attacker : followerAttackers) {
        if (attacker.owner == PlayerId.Player1) {
          if (attacker.type == UnitType.Support) {
            double shieldAmount = attacker.unitInformation.shieldPerUnit.orElse(attacker.upgraded ? 5 : 3) + (attacker.upgraded ? (attacker.unitInformation.shieldBonusPerY.orElse(0.3) * followerAttackerLocations.get(attacker).y) : 0);
            for (int d = 0; d < followerHealths.size(); d++) {
              if (!followerShielders.get(d).contains(attacker)) {
                followerHealths.set(d, followerHealths.get(d) + shieldAmount);
                followerShielders.get(d).add(attacker);
              }
            }
          }
        }
      }

      // evaluate attacks
      boolean needToRepath = false;
      int numFollowersToAttack = followerHealths.size();
      for (int frame = 0; frame < (1/followerInfo.speed.orElse(1)); frame++) { //run each path point 4 times since inters move every 4 frames
        for (Unit attacker : followerAttackers) {
          if (attacker.owner == PlayerId.Player2) {
            if (followerAttackerLocations.get(attacker).distance(followerPathPoint) <= followerInfo.attackRange.orElse(3.5)) {
              float initialTowerHealth = attacker.health;
              while (numFollowersToAttack > 0 && attacker.health > 0) {
                attacker.health -= followerDamage;
                numFollowersToAttack--;
                if (attacker.health <= 0) {
                  needToRepath = true;
                  break;
                }
              }
              float damageDone = initialTowerHealth - attacker.health;
              spTaken += (float) Utility.damageToSp(attacker, damageDone);
            }

            if (followerHealths.size() > 0) {
              if (followerAttackerLocations.get(attacker).distance(followerPathPoint) <= attacker.unitInformation.attackRange.orElse(attacker.upgraded ? 3.5 : 2.5)) {
                double initialFollowerHealth = followerHealths.get(followerHealths.size() - 1);
                double towerDamage = attacker.unitInformation.attackDamageWalker.orElse(0);
                followerHealths.set(followerHealths.size() - 1, Math.max(0, initialFollowerHealth - towerDamage));
                double afterHealth = followerHealths.get(followerHealths.size() - 1);
                double damageToFollowers = initialFollowerHealth - afterHealth;
                if (afterHealth == 0) {
                  followerHealths.remove(followerHealths.size() - 1);
                }
              }
            } else {
              break;
            }
          }
        }
      }
      if (needToRepath) {
        List<Coords> newFollowerPath;
        try {
          newFollowerPath = boomState.pathfind(followerPathPoint, followerTargetEdge);
        } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid path point for boom\n", x, y);
          continue;
        }
        followerPath.subList(p, followerPath.size()).clear();
        followerPath.addAll(newFollowerPath);
      }
    }
    Coords followerEndpoint = followerPath.get(followerPath.size()-1);
    if (!MapBounds.IS_ON_EDGE[followerTargetEdge][followerEndpoint.x][followerEndpoint.y]) {
      for (int x = Math.max(0, followerEndpoint.x-1); x <= Math.min(MapBounds.BOARD_SIZE-1, followerEndpoint.x+1); x++) {
        for (int y = Math.max(0, followerEndpoint.y-1); y <= Math.min(MapBounds.BOARD_SIZE-1, followerEndpoint.y+1); y++) {
          List<Unit> towers = boomState.allUnits[x][y];
          spTaken += (float) towers.stream().mapToDouble(towerUnit -> {
            if (towerUnit.owner == PlayerId.Player2) {
              float initialHealth = towerUnit.health;
              towerUnit.health -= followerHealth;
              towerUnit.health = Math.max(towerUnit.health, 0);
              float damageDone = initialHealth - towerUnit.health;
              return Utility.damageToSp(towerUnit, damageDone);
            }
            return 0;
          }).sum();
        }
      }
    } else {
      damageToBase += followerHealths.size();
    }
    damageToBase *= onlineAdjustment;
    return damageToBase;
  }

  /**
   * Returns an array of the wall value and the turret value
   * Wall Value = return[0] = sum of the building healths in the corner with some discounts for edge walls
   * Turret Value = return[1] = total damage we will taking moving at 1 speed (scout)
   * @param side the side we are inspecting
   * @return Expected Defense for the path we will take
   */
  private static ExpectedDefense enemyCornerSummary(GameState move, Side side) {
    int effectiveWallHealth;
    //walls only matter in a few locations
    int x;
//    effectiveWallHealth += move.getWallAt(new Coords(x, 14)).health;
//    x = side.equals(Side.RIGHT) ? 26 : 1;
//    effectiveWallHealth += move.getWallAt(new Coords(x, 14)).health;
//    //these walls are discounted since they are worth less
//    effectiveWallHealth += 0.5 * move.getWallAt(new Coords(x, 15)).health;
//    x = side.equals(Side.RIGHT) ? 25 : 2;
//    effectiveWallHealth += 0.5 * move.getWallAt(new Coords(x, 13)).health;

    //in the case we use interceptors to bomb then only two walls really matters....
    x = side.equals(Side.RIGHT) ? 27 : 0;
    int x2= side.equals(Side.RIGHT) ? 26 : 1;
    float wall1Health = move.getWallAt(new Coords(x, 14)) != null ? move.getWallAt(new Coords(x, 14)).health : 0;
    float wall2Health = move.getWallAt(new Coords(x2, 14)) != null ? move.getWallAt(new Coords(x2, 14)).health : 0;
    effectiveWallHealth = (int) Math.max(wall1Health, wall2Health);


    Coords[] path = side.equals(Side.RIGHT) ? rightPath : leftPath;
    int effectiveTurretRating = 0; //how much damage we will take in total moving at 1 coord per tick
    for (Coords pathPoint : path) {
      List<Unit> attackers = move.getAttackers(pathPoint);
      for (Unit attacker : attackers) {
        if (attacker.owner == PlayerId.Player2) {
          effectiveTurretRating += attacker.unitInformation.attackDamageWalker.orElse(6);
        }
      }
    }
//    GameIO.debug().println(String.format("enemyCornerSummary:  Side: %s\nEffective Wall Health: %s \nDamage taken: %d\n",
//        side,effectiveWallHealth, effectiveTurretRating));
    return new ExpectedDefense(move, path, effectiveWallHealth, effectiveTurretRating);
  }

  /**
   * Returns the expected amount of MP we will have in X turns if we use none of it and keep all of our factories
   * @param move the game state we are extrapolating from
   * @param turns the number of turn for which we extrapolate
   * @return the MP we will have after some number of turns
   */
  static float extrapolateFutureMP(GameState move, int turns, int expectedBaseCostPerTurn) {
    float currentMP = move.data.p1Stats.bits;
    for(int i = 0; i < turns; i++) {
      float baseMPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * (move.data.turnInfo.turnNumber + i) / move.config.resources.turnIntervalForBitSchedule;
      currentMP *= (1 - move.config.resources.bitDecayPerRound);
      //currentMP -= currentMP%0.01; //TODO: What was this line doing? I commented it out for now...
      currentMP += baseMPIncome - expectedBaseCostPerTurn;
    }
    return currentMP;
  }
}
