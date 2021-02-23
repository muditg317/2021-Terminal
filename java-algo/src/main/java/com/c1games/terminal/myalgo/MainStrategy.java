package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.*;
import com.c1games.terminal.algo.map.CanSpawn;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

import javax.naming.InsufficientResourcesException;
import java.util.*;

public class MainStrategy {
  private static MyAlgo algoState;
  private static GameState move;
  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  static void execute(MyAlgo algoState, GameState move) {
    MainStrategy.algoState = algoState;
    MainStrategy.move = move;
    // Defenses
    /*
    We need a good heuristic that considers the enemy MP and factories to decide when to put more defenses vs more factories
     */
    float mp = move.data.p1Stats.bits;
    int turnNumber = move.data.turnInfo.turnNumber;
    int enemyMPCapacity = (int) move.data.p2Stats.bits * 5;
    double enemyMPPercentCapacity = move.data.p2Stats.bits / enemyMPCapacity;
    int pingRushDefense = (int) ((2.0 * (move.data.p2Stats.bits) / 5) - (turnNumber / 2));
    if (enemyMPPercentCapacity < 0.3) {
      pingRushDefense = 0;
    }
    pingRushDefense = (int) Math.min(mp, pingRushDefense);
    if (turnNumber < 6) {
      pingRushDefense = (int) mp;
    }

    int reducedPingRushDefense = (int) Math.max(0, pingRushDefense - move.data.p2Stats.integrity + 1);

    GameIO.debug().println("Turn Number:" + turnNumber);
    GameIO.debug().println("pingRushDefense:" + pingRushDefense);
    GameIO.debug().println("reducedPingRushDefense: " + reducedPingRushDefense);
    GameIO.debug().println("Enemy left corner heuristic: " + Arrays.toString(enemyDefenseHeuristic("LEFT")));
    GameIO.debug().println("Enemy right corner heuristic: " + Arrays.toString(enemyDefenseHeuristic("RIGHT")));


    //
    deleteDamagedStructures();
    //DECIDE TO BOOM OR NOT HERE.

    GameIO.debug().println("BOOM DECISION: ===========");
    int MAX_EXTRAPOLATION_TURNS = Math.min(10, move.data.turnInfo.turnNumber);
    if (algoState.turnsUntilBoom != 0) { //we WILL boom this turn. no need to check.
      boolean shouldStillBoom = false;
      int turns = 1;
      if (algoState.turnsUntilBoom == -99) {
        algoState.turnsUntilBoom = -1;
        turns = 0;
      }
      for (; turns <= MAX_EXTRAPOLATION_TURNS; turns++) {
        float futureAttackThreshold = futureAttackThreshold(turns);
        float futureMP = extrapolateFutureMP(turns, reducedPingRushDefense);
        GameIO.debug().println("futureAttackThreshold: " + futureAttackThreshold);
        GameIO.debug().println("futureMP: " + futureMP);
        if (futureMP >= futureAttackThreshold) {
          GameIO.debug().println("about to boom..." + mp + " / " + futureAttackThreshold + " reached -- expecting: " + futureMP +" in " + turns +" turns ||| started turn with: " + mp);
          algoState.awaitingBoom = true;
          algoState.turnsUntilBoom = turns;
          shouldStillBoom = true;
          break;
        }
      }

      if (!shouldStillBoom) {
        algoState.awaitingBoom = false;
        algoState.turnsUntilBoom = -1;
      }
    } //end boom decision
    GameIO.debug().println("awaitingBoom:" + algoState.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + algoState.turnsUntilBoom);


    //TODO: if buildings die, replace all then upgrade all

    //make sure we have enough for boom wall
    int saveCores = 0;

    if (algoState.awaitingBoom) {
      int ourSPIncome = 5;
      int growthByBoom = algoState.turnsUntilBoom * ourSPIncome;
      //we need ATLEAST 25 by the time we boom with all walls and a few turrets at the end
      saveCores = Math.max(0, 25 - growthByBoom);
      saveCores = (int) Math.min(move.data.p1Stats.cores, saveCores);
      if (saveCores < 0) {
        saveCores = 0;
      }
    }
    GameIO.debug().println("Turn " + turnNumber+ ": with" + saveCores + " saveCores! We currently have " + move.data.p1Stats.cores + "cores!" );
    int defenseBudget = defenseHeuristic();//TODO: need to get a bit more factories bc they're always getting more factories than us

    if (defenseBudget > 0) { //we should smartly set up defenses
      defenseBudget = (int) Math.min(defenseBudget, move.data.p1Stats.cores - saveCores);

//      if (move.data.p1Stats.cores > 9 && (move.data.p1Stats.cores - defenseBudget) % 9 >= 7) {
//        defenseBudget -= 9;
//      }
      setUpDefenseWithBudget(defenseBudget);
    }



    float remainingCores = move.data.p1Stats.cores;
//    int prevDamage = algoState.scoredOnLocations.get(algoState.scoredOnLocations.size() - 1).size();
//    float health = move.data.p1Stats.integrity;
//    if (health / (health + prevDamage) >= 0.8) {
    upgradeOrBuildFactoriesWithBudget((int) remainingCores - saveCores);
//    }
    GameIO.debug().println(saveCores + " saveCores! We currently have " + move.data.p1Stats.cores + "cores!" );

    //Attack
    /*
    For now let's just focus on defending and bomb attacking
     */

    GameIO.debug().println("awaitingBoom:" + algoState.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + algoState.turnsUntilBoom);

    Object[] attackData = attackThreshold();
    String boomSide = (String) attackData[0];
    int attackPoints = (int) attackData[1];
    int numInters = (int) attackData[2];
    if (algoState.awaitingBoom && algoState.turnsUntilBoom == 0) { // DO THE BOOM
      GameIO.debug().println("Going to boom right now");
      GameIO.debug().println("Cores:" + move.data.p1Stats.cores);
      clearBoomPath(boomSide);
      placeBoomLid(boomSide);
      if (mp >= 106) {
        Utility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 20 : 7, 6)}, 16);
        Utility.spawnDemolishers(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 11 : 16, 2)}, 25);
        Utility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 11 : 16, 2)}, 15);
      } else {
        Utility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 23 : 4, 9)}, numInters);
        Utility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 6 : 21, 7)}, (int) move.data.p1Stats.bits);
      }
      Utility.removeBuilding(move, new Coords(boomSide.equals("RIGHT") ? 6 : 21, 8));
      Utility.removeBuilding(move, new Coords(3, 12));
      algoState.awaitingBoom = false;
      algoState.turnsUntilBoom = -99;
    } else { // otherwise do not do the boom, check for it

      if (algoState.awaitingBoom) { // we are going to boom
        spawnDefensiveInters(reducedPingRushDefense);
        if (algoState.turnsUntilBoom == 1) {
          clearBoomPath("LEFT");
          clearBoomPath("RIGHT");
          GameIO.debug().println("clearing path for future BOOM!!");
        }
        algoState.turnsUntilBoom--;
      } else {
        spawnDefensiveInters(pingRushDefense);
      }
    }
  }
  private static void deleteDamagedStructures() {
    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (int y = 0; y < row.length; y++) {
        List<Unit> units = row[y];
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player1) { //this is our firewall
          double startHealth = unit.unitInformation.startHealth.getAsDouble();
          float enemyMP = move.data.p2Stats.bits;
          if (enemyMP > move.data.p1Stats.integrity) {
            if (y == 13 && unit.health < startHealth) {
              Utility.removeBuilding(move, new Coords(x, y));
            }
          }
          else if (unit.health * 2 < startHealth) { //thing is damaged
            Utility.removeBuilding(move, new Coords(x, y));
          }
        }
      }
    }
  }
  /**
   * returns true if the lid is successfully placed
   * @param boomSide
   * @return
   */
  private static boolean placeBoomLid(String boomSide) {
    List<Coords> toPlace = new ArrayList<>();
    for (int i = 0; i < Locations.boomLid_right.length; i++) {
      Coords closeLocation = Locations.boomLid_right[i];
      int x = boomSide.equals("RIGHT") ? closeLocation.x : (27 - closeLocation.x);
      Coords toClose = new Coords(x, closeLocation.y);
      if (toClose.y < 8) {
        if (move.canSpawn(toClose, Utility.WALL, 1) == CanSpawn.Yes) {
          toPlace.add(toClose);
        }
      } else if (toClose.y == 13) {
        if (move.canSpawn(toClose, Utility.WALL, 1) == CanSpawn.Yes) {
          toPlace.add(toClose);
        }
      } else {
        Utility.placeTurrets(move, new Coords[]{toClose});
      }
    }
    final int[] numFactories = {(int) ((move.data.p1Stats.bits - toPlace.size()) / 9)};
    toPlace.stream().sorted(new Comparator<Coords>() {
      @Override
      public int compare(Coords o1, Coords o2) {
        return o1.y - o2.y;
      }
    }).forEach(location -> {
      if (numFactories[0] > 0 && location.y < 11) {
        Utility.placeSupports(move, new Coords[]{location});
        numFactories[0]--;
      } else {
        Utility.placeWalls(move, new Coords[]{location});
      }
    });
    return true;
  }

  private static int calculateHitsSinceTurns(int turns) {
    int hits = 0;
    for (int i = algoState.scoredOnLocations.size() - turns; i >= 0 && i < algoState.scoredOnLocations.size(); i++) {
      hits += algoState.scoredOnLocations.get(i).size();
    }
    return hits;
  }

  /**
   * Returns the weaker side ("LEFT", "RIGHT") and the num of estimated bits needed to break the wall
   * returns Side, cost, and num interceptors needed
   * @return [Side, cost, num interceptors needed]
   */
  private static Object[] attackThreshold() {
    int[] leftHeuristic = enemyDefenseHeuristic("LEFT");
    int leftCost = leftHeuristic[0] + leftHeuristic[1];

    int[] rightHeuristic = enemyDefenseHeuristic("RIGHT");
    int rightCost = rightHeuristic[0] + rightHeuristic[1];

    String weakerSide = leftCost < rightCost ? "LEFT" : "RIGHT";
    int correctCost = leftCost < rightCost ? leftCost : rightCost;
    int[] correctHeuristic = leftCost < rightCost ? leftHeuristic : rightHeuristic;
    return new Object[]{weakerSide, correctCost, correctHeuristic[1]};
  }

  /**
   * Returns the estimated bits needed to break the wall in X turns based on the weaker side.
   * @return
   */
  private static int futureAttackThreshold(int turns) {
    int[] leftHeuristic = enemyDefenseHeuristic("LEFT");
    int leftCost = leftHeuristic[0] + leftHeuristic[1];

    int[] rightHeuristic = enemyDefenseHeuristic("RIGHT");
    int rightCost = rightHeuristic[0] + rightHeuristic[1];

    int cheaperCost = Math.min(leftCost, rightCost);

    return cheaperCost;
  }

  /**
   * Guess of how strong their corner defense is on a certain side
   * @param side
   * @return the guessed amount of pings and interceptor tanks needed
   * return[0] = num of pings
   * return[1] = num of interceptors
   */
  private static int[] enemyDefenseHeuristic(String side) {
    int enemyHealth = (int) move.data.p2Stats.integrity; //min amount we need to hit them by
    //walls only matter in a few locations -> we include turret healths since they are "walls"

    int[] cornerSummary = enemyCornerSummary(side);
    int intersNeeded = ((cornerSummary[0]) / 40) + (cornerSummary[1] * 4) / 40;
    if (cornerSummary[0] == 0) {
      intersNeeded = 0;
    }
    intersNeeded = (int) Math.ceil(0.9 * intersNeeded);
    //intersNeeded = Math.min(intersNeeded, 18);
    int pingsNeeded = cornerSummary[1] / 15 + enemyHealth;

    return new int[]{pingsNeeded, intersNeeded};
  }

  /**
   * Returns an array of the wall value and the turret value
   * Wall Value = return[0] = sum of the building healths in the corner with some discounts for edge walls
   * Turret Value = return[1] = total damage we will taking moving at 1 speed (ping)
   * @param side
   * @return
   */
  private static int[] enemyCornerSummary(String side) {
    int effectiveWallHealth = 0;
    //walls only matter in a few locations
    int x = side.equals("RIGHT") ? 27 : 0;
//    effectiveWallHealth += move.getWallAt(new Coords(x, 14)).health;
//    x = side.equals("RIGHT") ? 26 : 1;
//    effectiveWallHealth += move.getWallAt(new Coords(x, 14)).health;
//    //these walls are discounted since they are worth less
//    effectiveWallHealth += 0.5 * move.getWallAt(new Coords(x, 15)).health;
//    x = side.equals("RIGHT") ? 25 : 2;
//    effectiveWallHealth += 0.5 * move.getWallAt(new Coords(x, 13)).health;

    //in the case we use interceptors to bomb then only two walls really matters....
    x = side.equals("RIGHT") ? 27 : 0;
    int x2= side.equals("RIGHT") ? 26 : 1;
    float wall1Health = move.getWallAt(new Coords(x, 14)) != null ? move.getWallAt(new Coords(x, 14)).health : 0;
    float wall2Health = move.getWallAt(new Coords(x2, 14)) != null ? move.getWallAt(new Coords(x2, 14)).health : 0;
    effectiveWallHealth = (int) Math.max(wall1Health, wall2Health);

    final Coords[] leftPath = {
        new Coords(3, 11),
        new Coords(2, 11),
        new Coords(2, 12),
        new Coords(1, 12),
        new Coords(1, 13),
        new Coords(1, 14),
        new Coords(0 ,14)
    };
    final Coords[] rightPath = {
        new Coords(24, 11),
        new Coords(25, 11),
        new Coords(25, 12),
        new Coords(26, 12),
        new Coords(26, 13),
        new Coords(26, 14),
        new Coords(27 ,14)
    };
    Coords[] path = side.equals("RIGHT") ? rightPath : leftPath;
    int effectiveTurretRating = 0; //how much damage we will take in total moving at 1 coord per tick
    for (Coords pathPoint : path) {
      List<Unit> attackers = move.getAttackers(pathPoint);
      for (Unit attacker : attackers) {
        if (attacker.owner == PlayerId.Player2) {
          if (attacker.upgraded) {
            effectiveTurretRating += 20;
          } else {
            effectiveTurretRating += 6;
          }
        }
      }
    }
    return new int[]{effectiveWallHealth, effectiveTurretRating};
  }
  /**
   * Returns the expected amount of MP we will have in X turns if we use none of it and keep all of our factories
   * @param turns
   * @return
   */
  private static float extrapolateFutureMP(int turns, int expectedBaseCostPerTurn) {
    float currentMP = move.data.p1Stats.bits;
    for(int i = 0; i < turns; i++) {
      int baseMPIncome = 4 + (move.data.turnInfo.turnNumber + i) / 10;
      currentMP *= 0.75;
      currentMP += baseMPIncome - expectedBaseCostPerTurn;
    }
    return currentMP;
  }

  /**
   * clears an attack path for a boom attack
   * @param side the side which to hit
   * @return whether the path was already clear
   */
  private static boolean clearBoomPath(String side) {
    boolean alreadyReady = true;
    for (int i = 0; i < Locations.boomPath_right.length; i++) {
      Coords openLocation = Locations.boomPath_right[i];
      int x = side.equals("RIGHT") ? openLocation.x : (27 - openLocation.x);
      Coords toOpen = new Coords(x, openLocation.y);
      alreadyReady = Utility.removeBuilding(move, toOpen) == 0 && alreadyReady;
    }
    return alreadyReady;
  }

  /**
   * Spawns defensive inters in front of the wall
   * @param num
   */
  private static void spawnDefensiveInters(int num) {
    Coords leftCoord = new Coords(7, 6);
    Coords rightCoord = new Coords(15, 1);
    if (move.canSpawn(leftCoord, Utility.INTERCEPTOR, 1) != CanSpawn.Yes) {
      leftCoord = new Coords(3, 10);
    }
    if (move.canSpawn(rightCoord, Utility.INTERCEPTOR, 1) != CanSpawn.Yes) {
      rightCoord = new Coords(24, 10);
    }
    int left = num / 2;
    int right = num - left;
    for (int i = 0; i < left; i++) {
      move.attemptSpawn(leftCoord, Utility.INTERCEPTOR);
    }
    for (int i = 0; i < right; i++) {
      move.attemptSpawn(rightCoord, Utility.INTERCEPTOR);
    }
  }
  /**
   * Spends as much of the budget as it can on factory upgrades
   * @param budget
   */
  private static void upgradeOrBuildFactoriesWithBudget(int budget) {
    upgradeOrBuildFactoriesWithBudget(budget, Locations.factoryCoords);
  }

  private static void upgradeOrBuildFactoriesWithBudget(int budget, Coords[] factoryLocations) {
    //first we want to prioritize upgrading already existing factories
    Coords lastPlaced = null;
    int spent = 0;
    try {
      spent += attemptSpawnIfAffordable(new Coords(13, 0), Utility.SUPPORT, true, budget - spent);
      spent += attemptSpawnIfAffordable(new Coords(14, 0), Utility.SUPPORT, true, budget - spent);

      for (Coords location : factoryLocations) {
        spent += attemptSpawnIfAffordable(location, Utility.SUPPORT, false, budget - spent);
        if (!algoState.awaitingBoom) {
          spent += attemptSpawnIfAffordable(location, Utility.SUPPORT, true, budget - spent);
        }
        lastPlaced = location;
      }
    } catch (InsufficientResourcesException e) {
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LOCATION " + lastPlaced);
      return;
    }
  }
  /**
   * Sets up defense with maximum of budget cores to spend
   * @param budget
   */
  private static void setUpDefenseWithBudget(int budget) {
    int spent = 0;
    try {

      //Get the core corner turrets down
      Coords firstLeftTurret = new Coords(3, 13);
      spent += attemptSpawnIfAffordable(firstLeftTurret, Utility.TURRET, false, budget - spent);

      Coords firstRightTurret = new Coords(24, 13);
      spent += attemptSpawnIfAffordable(firstRightTurret, Utility.TURRET, false, budget - spent);

      //get the main wall down
      for (Coords location : Locations.mainWallCoords) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);
      }

      spent += attemptSpawnIfAffordable(firstLeftTurret, Utility.TURRET, true, budget - spent);
      spent += attemptSpawnIfAffordable(firstRightTurret, Utility.TURRET, true, budget - spent);

      //get the entrance left turret down
      Coords leftEntranceTurret = new Coords(4, 11);
      spent += attemptSpawnIfAffordable(leftEntranceTurret, Utility.TURRET, false, budget - spent);

      //get right wall turrets down
      Coords rightWallTurret1 = new Coords(25, 13);
      Coords rightWallTurret2 = new Coords(24, 12);
      spent += attemptSpawnIfAffordable(rightWallTurret1, Utility.TURRET, false, budget - spent);
      spent += attemptSpawnIfAffordable(rightWallTurret2, Utility.TURRET, false, budget - spent);

      Coords secondLeftTurret = new Coords(2, 13);
      spent += attemptSpawnIfAffordable(secondLeftTurret, Utility.TURRET, false, budget - spent);

      // build left corner walls
      for (Coords location : Locations.leftCornerWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);
      }

      // build right corner walls
      for (Coords location : Locations.rightCornerWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);
      }

      //upgrade left entrance towers
      spent += attemptSpawnIfAffordable(leftEntranceTurret, Utility.TURRET, true, budget - spent);

      //upgrade corner walls
      for (Coords location : Locations.leftCornerWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }
      for (Coords location : Locations.rightCornerWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }

      //place all turrets and upgrade
      for(Coords location : Locations.cornerTurrets) {
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, false, budget - spent);
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
      }

      //add left entrance wall
      Coords leftEntranceWall = new Coords(6, 11);
      spent += attemptSpawnIfAffordable(leftEntranceWall, Utility.TURRET, false, budget - spent);
      spent += attemptSpawnIfAffordable(leftEntranceWall, Utility.TURRET, true, budget - spent);



      //MOST DEFENSE DONE=====================


      if (move.data.p1Units.support.size() < Locations.factoryCoords.length / 2) {
        throw new InsufficientResourcesException("place factories before extra stuff!");
      }

      for(Coords location : Locations.extraTurretCoords) {
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, false, budget - spent);
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
      }

      for(Coords location : Locations.extraWallCoords) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }

//      for(Coords location : Locations.finalExtraTurretCoords) {
//        spent += attemptSpawnIfAffordable(location, Utility.TURRET, false, budget - spent);
//        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
//      }


//
//      // EXTRA LAYER OF TOWER DUUDES
//      for(Coords location : Locations.mainTurretCoords) {
//        Coords newLocation = new Coords(location.x, location.y - 1);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, false, budget - spent);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, true, budget - spent);
//      }
//
//      //well i guess now we just win ;D
////      for(Coords location : Locations.mainTurretCoords) {
////        Coords newLocation = new Coords(location.x, location.y + 2);
////        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, false, budget - spent);
////        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, true, budget - spent);
////      }
//
//      for(Coords location : Locations.mainTurretCoords) {
//        Coords newLocation = new Coords(location.x, location.y + 3);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, false, budget - spent);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, true, budget - spent);
//      }
//
//      for(Coords location : Locations.finalExtraTurretCoords) {
//        spent += attemptSpawnIfAffordable(location, Utility.TURRET, false, budget - spent);
//        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
//      }
//
//      for(Coords location : Locations.mainTurretCoords) {
//        Coords newLocation = new Coords(location.x, location.y + 4);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.WALL, false, budget - spent);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.WALL, true, budget - spent);
//      }
//
//      for(Coords location : Locations.mainTurretCoords) {
//        Coords newLocation = new Coords(location.x, location.y + 5);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, false, budget - spent);
//        spent += attemptSpawnIfAffordable(newLocation, Utility.TURRET, true, budget - spent);
//      }

    } catch (InsufficientResourcesException e) {
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[1].getLineNumber());
      return;
    }
  }

  /**
   * Attempts to spawn if it is affordable. Returns the number of monies used.
   * @param location
   * @param unitType
   * @param upgrade
   * @param budget
   * @return
   */
  private static int attemptSpawnIfAffordable(Coords location, UnitType unitType, boolean upgrade, int budget) throws InsufficientResourcesException {
    if (algoState.awaitingBoom && algoState.turnsUntilBoom < 2) {
      //GameIO.debug().println("Prevented spawn at" +location);
      for (Coords openLocation : Locations.boomPath_right) {
        if ((openLocation.x == location.x || (27 - openLocation.x) == location.x) && openLocation.y == location.y) {
          return 0;
        }
      }
    }
    if (numAffordableWithBudget(unitType, upgrade, budget) > 0) {
      if (upgrade) {
        return move.attemptUpgrade(location) == 1 ? getUnitCost(unitType, true) : 0;
      } else {
        return move.attemptSpawn(location, unitType) ? getUnitCost(unitType, upgrade) : 0;
      }
    }
    throw new InsufficientResourcesException("outta money bruh");

  }
  /**
   * Based on the current board state, enemy MP, and our defenses returns the number of cores it thinks we need to spend into defenses
   * @return the number of cores we should spend in defenses
   */
  private static int defenseHeuristic() {
    float enemyMP = move.data.p2Stats.bits;
    float ourHealth = move.data.p1Stats.integrity;
    int baseMPIncome = 5 + move.data.turnInfo.turnNumber / 10;
    float enemyMPIncome = baseMPIncome + move.data.p2Units.support.size();
    float defenseRating = calculateDefenseRating();


    //we want atleast 1 rating per 1 MP they have
    double enemyAttackRating = 1.7 * enemyMP + (0 * enemyMPIncome) - (ourHealth * 0.1) + 15;
    return (int) Math.ceil(enemyAttackRating - defenseRating);
  }

  /**
   * Calculates some rating of our defense...
   * @return
   */
  private static float calculateDefenseRating() {

   float leftRating = 0;
   float rightRating = 0;

    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (List<Unit> units : row) {
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player1) { //this is our firewall
          float unitValue = 0;
          if(unit.type == Utility.TURRET) {
            if (unit.upgraded) {
              unitValue += 4 * unit.health / 200;
            } else {
              unitValue += 2 * unit.health / 100;
            }
          }
          else if (unit.type == Utility.WALL) {
            if (unit.upgraded) {
              unitValue += 3 * (unit.health / 300);
            } else {
              unitValue += 1 * (unit.health / 75);
            }
          }
          if (x < 13) {
            leftRating += unitValue;
          } else {
            rightRating += unitValue;
          }
        }
      }
    }
    return 2 * Math.min(leftRating, rightRating);
  }


  /**
   * How many of a certain unit type we can afford. Copied and modified from GameState.java
   */
  private static int numAffordableWithBudget(UnitType type, boolean upgrade, int budget) {
    if (type == UnitType.Remove) {
      throw new IllegalArgumentException("Cannot query number affordable of remove unit type use removeFirewall");
    }
    if (type == UnitType.Upgrade) {
      throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, put type of unit to upgrade and upgrade=true");
    }

    if (budget <= 0) {
      return 0;
    }

    float[] cost = ((Config.RealUnitInformation) move.config.unitInformation.get(type.ordinal())).cost();
    if (upgrade) {
      Config.UnitInformation upgradeInfo = move.config.unitInformation.get(type.ordinal()).upgrade.orElse(null);
      if (upgradeInfo != null) {
        cost[0] = (float)upgradeInfo.cost1.orElse(cost[0]);
        cost[1] = (float)upgradeInfo.cost2.orElse(cost[1]);
      } else {
        throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, unit not upgradeable");
      }
    }

    float[] wealth = new float[] { budget, budget };
    int[] afford = new int[] {
            cost[0] > 0 ?  (int) (wealth[0]/cost[0]) : 99,
            cost[1] > 0 ?  (int) (wealth[1]/cost[1]) : 99,
    };

    return Math.min(afford[0], afford[1]);
  }

  private static int getUnitCost(UnitType type, boolean upgrade) {
    if (type == UnitType.Remove) {
      throw new IllegalArgumentException("Cannot query number affordable of remove unit type use removeFirewall");
    }
    if (type == UnitType.Upgrade) {
      throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, put type of unit to upgrade and upgrade=true");
    }

    float[] cost = ((Config.RealUnitInformation) move.config.unitInformation.get(type.ordinal())).cost();
    if (upgrade) {
      Config.UnitInformation upgradeInfo = move.config.unitInformation.get(type.ordinal()).upgrade.orElse(null);
      if (upgradeInfo != null) {
        cost[0] = (float)upgradeInfo.cost1.orElse(cost[0]);
        cost[1] = (float)upgradeInfo.cost2.orElse(cost[1]);
      } else {
        throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, unit not upgradeable");
      }
    }

    return (int) Math.max(cost[0], cost[1]);
  }

  /**
   * find optimal demolisher line and send demo if present
   * TODO: doesn't account for killing stuff causing path to change
   */
  static Coords potentiallySendDemolishers(int minDamage) {
    Map<Coords, Float> damages = new HashMap<>();

    for (Coords location : Utility.friendlyEdges) {
      List<Coords> path;
      try {
        path = move.pathfind(location, MapBounds.getEdgeFromStart(location));
      } catch (IllegalPathStartException e) {
        continue;
      }
      Coords endPoint = path.get(path.size() -1);
      float demolisherPower = 0;
      for (Coords pathPoint : path) {
        List<Unit> attackers = move.getAttackers(pathPoint);
        int additions = 0;
        Coords best = null;
        UnitType bestType = null;
        float bestPercentage = 0;
        Coords second = null;
        UnitType secondType = null;
        float secondPercentage = 0;
        for (FrameData.PlayerUnit unit : move.data.p2Units.support) {
          Coords unitLoc = new Coords(unit.x, unit.y);
          if (pathPoint.distance(unitLoc) < 4.5) {
            if (best == null || pathPoint.distance(unitLoc) <= pathPoint.distance(best)) {
              second = best;
              secondType = bestType;
              secondPercentage = bestPercentage;
              best = unitLoc;
              bestType = Utility.SUPPORT;
              bestPercentage = unit.stability / 30;
            } else if (second == null || pathPoint.distance(unitLoc) <= pathPoint.distance(second)) {
              second = unitLoc;
              secondType = Utility.SUPPORT;
              secondPercentage = unit.stability / 30;
            }
          }
        }
        for (FrameData.PlayerUnit unit : move.data.p2Units.turret) {
          Coords unitLoc = new Coords(unit.x, unit.y);
          if (pathPoint.distance(unitLoc) < 4.5) {
            if (best == null || pathPoint.distance(unitLoc) <= pathPoint.distance(best)) {
              second = best;
              secondType = bestType;
              secondPercentage = bestPercentage;
              best = unitLoc;
              bestType = Utility.TURRET;
              bestPercentage = unit.stability / 95;
            } else if (second == null || pathPoint.distance(unitLoc) <= pathPoint.distance(second)) {
              second = unitLoc;
              secondType = Utility.TURRET;
              secondPercentage = unit.stability / 95;
            }
          }
        }
        for (FrameData.PlayerUnit unit : move.data.p2Units.wall) {
          Coords unitLoc = new Coords(unit.x, unit.y);
          if (pathPoint.distance(unitLoc) < 4.5) {
            if (best == null || pathPoint.distance(unitLoc) <= pathPoint.distance(best)) {
              second = best;
              secondType = bestType;
              secondPercentage = bestPercentage;
              best = unitLoc;
              bestType = Utility.WALL;
              bestPercentage = unit.stability / 150;
            } else if (second == null || pathPoint.distance(unitLoc) <= pathPoint.distance(second)) {
              second = unitLoc;
              secondType = Utility.WALL;
              secondPercentage = unit.stability / 150;
            }
          }
        }
        if (best != null) {
          demolisherPower += bestType == Utility.SUPPORT ? 9 : bestType == Utility.TURRET ? 2 : 1;
        }
        if (second != null) {
          demolisherPower += (1 - bestPercentage) * (bestType == Utility.SUPPORT ? 9 : bestType == Utility.TURRET ? 2 : 1);
        }
        if (!attackers.isEmpty()) {
          break;
        }
      }
//      GameIO.debug().println("Got dmg:" + demolisherPower + " for " + location);
      damages.put(location, demolisherPower);
    }

    Coords bestCoord = null;
    float bestDemolisherDamage = minDamage;
    for (Map.Entry<Coords, Float> entry : damages.entrySet()) {
      if (entry.getValue() > bestDemolisherDamage) {
        bestCoord = entry.getKey();
        bestDemolisherDamage = entry.getValue();
      }
    }

    return bestCoord;
  }
}
