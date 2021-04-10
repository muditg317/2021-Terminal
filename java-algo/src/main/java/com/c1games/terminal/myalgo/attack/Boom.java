package com.c1games.terminal.myalgo.attack;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.myalgo.utility.Locations;
import com.c1games.terminal.myalgo.utility.SpawnUtility;
import com.c1games.terminal.myalgo.utility.Utility;
import com.c1games.terminal.simulation.SimBoard;
import com.c1games.terminal.simulation.Simulator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Boom {
  private static boolean LOGGED = false; //TODO: DELETE
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
//  public static Side side;
  public static Boom attack;

  static double onlineAdjustment = 1;

  public Side sideToBoom;
  public int bombUnits;
  public UnitType bombType;
  public int followerUnits;
  public UnitType followerType;
  public int expectedDamage;
  private double cost;

  public Boom(GameState move, Side sideToBoom, int bombUnits, UnitType bombType, int followerUnits) {
    this.sideToBoom = sideToBoom;
    this.bombUnits = bombUnits;
    this.bombType = bombType;
    this.followerUnits = followerUnits;
    this.followerType = UnitType.Scout;
    this.cost = bombUnits * move.config.unitInformation.get(bombType.ordinal()).cost2.orElseThrow() + followerUnits * move.config.unitInformation.get(followerType.ordinal()).cost2.orElseThrow();
  }

  Coords getBombStart() {
    return this.bombType == UnitType.Interceptor
        ? new Coords(this.sideToBoom == Side.RIGHT ? 23 : 27 - 23, 9)
        : new Coords(this.sideToBoom == Side.RIGHT ? 4 : 27 - 4, 9);
  }

  Coords getFollowerStart() {
    return this.bombType == UnitType.Interceptor
        ? new Coords(this.sideToBoom == Side.RIGHT ? 8 : 27 - 8, 5)
        : new Coords(this.sideToBoom == Side.RIGHT ? 5 : 27 - 5, 8);
  }

  public double getCost() {
    return cost;
  }

  @Override
  public String toString() {
    return String.format("Boom: %s\tBomb: %d %ss, Follower: %d %ss, Cost: %.2f",
        sideToBoom, bombUnits, bombType, followerUnits, followerType, cost);
  }

  public static void progress() {
    if (Boom.awaitingBoom && turnsUntilBoom == 0) { //just boomed this turn
      Boom.awaitingBoom = false;
      Boom.turnsUntilBoom = -99;
    }
    if (Boom.awaitingBoom) {
      Boom.turnsUntilBoom--;
    }
  }

  public static void evaluate(GameState move, int expectedMpSpentPerTurn) {
    GameIO.debug().println("BOOM STATE BEFORE DECISION=======");
    Boom.debugPrint();
//    if (Boom.awaitingBoom && Boom.turnsUntilBoom == 0) {
//      awaitingBoom = false;
//      turnsUntilBoom = -99;
//    }
//    if (Boom.awaitingBoom) {
//      Boom.turnsUntilBoom--;
//    }
    if (Boom.turnsUntilBoom != 0) { //we WILL boom this turn. no need to check.
      GameIO.debug().println("BOOM DECISION: ===========");
      float mp = move.data.p1Stats.bits;
      int MAX_EXTRAPOLATION_TURNS = Math.min(3, move.data.turnInfo.turnNumber / 5);

      boolean shouldStillBoom = false;
      int turns = 1;
      if (Boom.turnsUntilBoom == -99) {
        Boom.turnsUntilBoom = -1;
        turns = 0;
      }
      for (; turns <= MAX_EXTRAPOLATION_TURNS; turns++) {
        Boom futureAttack = futureAttackThreshold(move, turns);
        if (futureAttack == null) {
          continue;
        }

        double futureAttackThreshold = futureAttack.cost;
        float futureMP = extrapolateFutureMP(move, turns, expectedMpSpentPerTurn);
//        GameIO.debug().println("futureAttackThreshold: " + futureAttackThreshold);
//        GameIO.debug().println("futureMP: " + futureMP);
        if (futureMP >= futureAttackThreshold) {
          GameIO.debug().printf("Future Boom Attack:\n\t%s\n", futureAttack);
          GameIO.debug().printf("\tcurrent MP: %.2f\tneeded MP: %.2f\texpected: %.2f in %d turns\n", mp, futureAttackThreshold, futureMP, turns);
          Boom.awaitingBoom = true;
          Boom.turnsUntilBoom = turns;
          Boom.attack = futureAttack;
          shouldStillBoom = true;
          break;
        }
      }

      if (!shouldStillBoom) {
        Boom.awaitingBoom = false;
        Boom.turnsUntilBoom = -1;
        Boom.attack = null;
      }
    } else { //booming this turn turnsUntilBoom == 0
      //Boom.attack = attackThreshold(move);
//      Boom.side = attack.side;

    }
    //end boom decision

    Boom.debugPrint();
  }

  /**
   * Does the Boom. Does not set awaitingBoom and turnsUntilBoom.
   * @return True if executed, false if not
   */
  public boolean execute(GameState move) {
    GameIO.debug().println("Going to boom right now");
    GameIO.debug().println("Cores:" + move.data.p1Stats.cores);
    if (!Boom.clearBoomPath(move, sideToBoom)) {
      GameIO.debug().println("BOOM PATH BLOCKED!!");
      return false;
    }
    if (!Boom.placeBoomLid(move, sideToBoom)) {
      GameIO.debug().println("BOOM LID NOT FINISHED!!");
      return false;
    }

    if (this.bombType == UnitType.Interceptor) {
      SpawnUtility.spawnInterceptors(move, getBombStart(), this.bombUnits);
    } else {
      SpawnUtility.spawnScouts(move, getBombStart(), this.bombUnits);
    }
    SpawnUtility.spawnScouts(move, getFollowerStart(), (int) move.data.p1Stats.bits);

    SpawnUtility.removeBuilding(move, new Coords(sideToBoom == Side.RIGHT ? 9 : 27 - 9, 5));
    SpawnUtility.removeBuilding(move, new Coords(4, 11));

    if (!LOGGED) { //TODO: DELETE
      LOGGED = true;
      GameIO.debug().println("EXPECTED BOOM RESULT!!!!!!!!=============");
      Simulator.DEBUG = true;
      Simulator.simulate(move);
      Simulator.DEBUG = false;
    }

    return true;
  }

  /**
   * returns true if the lid is successfully placed
   * @param boomSide
   * @return
   */
  static boolean placeBoomLid(GameState move, Side boomSide) {
    List<Coords> checkWalls = Arrays.stream(Locations.boomCheck_right)
        .map(coords -> new Coords(boomSide == Side.RIGHT ? coords.x : (27 - coords.x), coords.y))
        .filter(coords -> move.getWallAt(coords) == null)
        .collect(Collectors.toList());

    List<Coords> lidWalls = List.of();
//    Arrays.stream(Locations.boomLid_right)
//        .sorted((c1, c2) -> c2.x - c1.x)
//        .sorted(Comparator.comparingInt(c -> c.y))
//        .map(coords -> new Coords(boomSide == Side.RIGHT ? coords.x : (27 - coords.x), coords.y))
//        .filter(coords -> move.getWallAt(coords) == null)
//        .collect(Collectors.toList());

    double wallCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);
    double supportCost = move.config.unitInformation.get(UnitType.Support.ordinal()).cost1.orElse(4);

    // check if we have enough sp to fill the lid
    if (move.data.p1Stats.bits < (checkWalls.size() + lidWalls.size()) * wallCost) return false;

    // place the lid
    checkWalls.forEach(coords -> SpawnUtility.placeWall(move, coords));

    int[] supportsAffordable = new int[]{(int) ((move.data.p1Stats.bits - (lidWalls.size() * wallCost)) / (supportCost - wallCost))};
//    lidWalls.stream().map(coords -> ((supportsAffordable[0]-- > 0 && SpawnUtility.placeSupport(move, coords)) || SpawnUtility.placeWall(move, coords)) && SpawnUtility.removeBuilding(move, coords));
    lidWalls.forEach(coords -> SpawnUtility.placeWall(move, coords));


    return Stream.concat(checkWalls.stream(), lidWalls.stream()).noneMatch(coords -> move.getWallAt(coords) == null);
  }

  /**
   * clears an attack path for a boom attack
   * @param side the side which to hit
   * @return whether the path was already clear
   */
  public static boolean clearBoomPath(GameState move, Side side) {
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
    return alreadyReady;
  }

  /**
   * clears an attack path for a boom attack
   * @param side the side which to hit
   * @return whether the path was already clear
   */
  static boolean clearBoomPathForce(GameState state, Side side) {
    boolean alreadyReady = true;
    for (int i = 0; i < Locations.boomPath_right.length; i++) {
      Coords openLocation = Locations.boomPath_right[i];
      if (openLocation.y > 13) continue;
      int x = side == Side.RIGHT ? openLocation.x : (27 - openLocation.x);
      Coords toOpen = new Coords(x, openLocation.y);
      boolean wasReady = alreadyReady;
      alreadyReady = !SpawnUtility.removeBuilding(state, toOpen) && alreadyReady;
      state.allUnits[toOpen.x][toOpen.y].removeIf(unit -> state.isStructure(unit.type) && unit.owner == PlayerId.Player1);
//      if (wasReady && !alreadyReady) {
//        Unit wall = state.getWallAt(toOpen);
//        GameIO.debug().printf("BOOM PATH NOT CLEARED: %s - %s\n", toOpen, wall == null ? "null" : wall.unitInformation.display.orElse("some tower"));
//      }
    }
    return true; //alreadyReady
  }

  public static void debugPrint() {
    GameIO.debug().printf(Boom.awaitingBoom ? "Will boom in %d turns" : "Not awaiting boom (%d turns)", Boom.turnsUntilBoom);
    if (Boom.attack != null) {
      GameIO.debug().printf("\t[%s]", Boom.attack.sideToBoom);
    }
    GameIO.debug().println();
  }

  /**
   * Returns the weaker side (Side.LEFT, Side.RIGHT) and the num of estimated bits needed to break the wall
   * @param move  the game state for which we're predicting
   * @param useSimulator
   * @return Boom with where to attack and the related units needed
   */
  static Boom attackThreshold(GameState move, boolean useSimulator) {
    if (useSimulator) {
      return bestBoom(move, move.data.p1Stats.bits, move.data.p2Stats.integrity / 4);
    }
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
    return new Boom(move, weakerSide, correctUnitCounts.numInterceptors, UnitType.Scout, correctUnitCounts.numScouts);
  }

  /**
   * Returns the estimated bits needed to break the wall in X turns based on the weaker side.
   * @param move   the game state we are predicting for
   * @param turns  the number of turns we will look into the future
   * @return Boom with where to attack and the related units needed
   */
  static Boom futureAttackThreshold(GameState move, int turns) {
    // TODO: FIX ME
    // TODO: apply some scaling if they have more turns available
    GameState duplicate = Utility.duplicateState(move);

    duplicate.data.p1Stats.cores += 5 * turns;
    duplicate.data.p1Stats.bits = extrapolateFutureMP(move, turns, 0);

    return attackThreshold(duplicate, turns == 1); // only run simulator for deciding the immediate next turn's boom
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

  /**
   * Simulates all possible booms (inter boom + scout boom) + all different combos of num units
   * and returns the best boom
   * @param move
   * @param availableMP
   * @param minDamage
   * @return
   */
  static Boom bestBoom(GameState move, double availableMP, double minDamage) {
    double interCost = move.config.unitInformation.get(UnitType.Interceptor.ordinal()).cost2.orElse(1);
    double scoutCost = move.config.unitInformation.get(UnitType.Scout.ordinal()).cost2.orElse(1);

    GameIO.debug().println("====EVALUATING BOOM====");
    GameIO.debug().printf("MP: %.2f, requiredDamage: %.2f\n", availableMP, minDamage);
    GameIO.debug().printf("Sims run: %d\n", Simulator.simCount);

    Map<Boom, Double> damages = new HashMap<>();
    for (Side side : Side.values()) {
      GameState testState = Utility.duplicateState(move);

      if (!placeBoomLid(testState, side)) {
        continue;
      }
      clearBoomPathForce(testState, side);
      
      for (int bombMP = 1; bombMP < availableMP; bombMP++) {

        GameState boomState = Utility.duplicateState(testState);
        int bombCount = (int) (bombMP / interCost);
        int followerCount = (int) ((availableMP - (bombCount*interCost)) / scoutCost);
        Boom boom = new Boom(move, side, bombCount, UnitType.Interceptor, followerCount);
        SpawnUtility.spawnInterceptors(boomState, boom.getBombStart(), bombCount);

        SimBoard simulationResult = Simulator.simulate(boomState);
        double interBoomDamage = boomState.data.p2Stats.integrity - simulationResult.getP2Health();
        interBoomDamage *= onlineAdjustment;
        if (interBoomDamage > minDamage/2) {
          damages.put(boom, interBoomDamage);
        }

//        boomState = Utility.duplicateState(testState);
//        bombCount = (int) (bombMP / scoutCost);
//        followerCount = (int) ((availableMP - (bombCount*scoutCost)) / scoutCost);
//        boom = new Boom(move, side, bombCount, UnitType.Scout, followerCount);
//        SpawnUtility.spawnScouts(boomState, boom.getBombStart(), bombCount);
//
//        simulationResult = Simulator.simulate(boomState);
//        double scoutBoomDamage = boomState.data.p2Stats.integrity - simulationResult.getP2Health();
//        scoutBoomDamage *= onlineAdjustment;
//        if (scoutBoomDamage > minDamage/2) {
//          damages.put(boom, scoutBoomDamage);
//        }
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
//      damages.forEach((key, value) -> GameIO.debug().printf("%s. damage:%.2f, need:%.2f\n",
//          key,
//          value, minDamage));
      if (bestBoom != null) {
        GameIO.debug().printf("Current best boom: %s. damage:%.2f out of %.2f\n",
            bestBoom,
            bestDamage, minDamage);
      }
      return null;
    }

    GameIO.debug().printf("Using best boom: %s. damage:%.2f out of %.2f\n",
        bestBoom,
        bestDamage, minDamage);
    
    return bestBoom;
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
    for(int i = 1; i <= turns; i++) {
      float baseMPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * (move.data.turnInfo.turnNumber + i) / move.config.resources.turnIntervalForBitSchedule;
      currentMP *= (1 - move.config.resources.bitDecayPerRound);
      //currentMP -= currentMP%0.01; //TODO: What was this line doing? I commented it out for now...
      currentMP += baseMPIncome - expectedBaseCostPerTurn;
    }
    return currentMP;
  }
}
