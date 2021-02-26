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
    //always set up the essential defenses
    deleteDamagedStructures();


    float mp = move.data.p1Stats.bits;
    float sp = move.data.p1Stats.cores;
    int turnNumber = move.data.turnInfo.turnNumber;


    int scoutRushDefense = StrategyUtility.neededScoutRushDefense(move);
    int reducedScoutRushDefense = (int) Math.max(0, scoutRushDefense - move.data.p2Stats.integrity + 1);

    GameIO.debug().println("Turn Number:" + turnNumber);
    GameIO.debug().println("scoutRushDefense:" + scoutRushDefense);
    GameIO.debug().println("reducedScoutRushDefense: " + reducedScoutRushDefense);
    GameIO.debug().println("Enemy left corner heuristic: " + StrategyUtility.enemyDefenseHeuristic(move, "LEFT"));
    GameIO.debug().println("Enemy right corner heuristic: " + StrategyUtility.enemyDefenseHeuristic(move, "RIGHT"));



    //DECIDE TO BOOM OR NOT HERE.==========================
    GameIO.debug().println("BOOM DECISION: ===========");
    int MAX_EXTRAPOLATION_TURNS = Math.min(3, move.data.turnInfo.turnNumber / 5);
    if (algoState.turnsUntilBoom != 0) { //we WILL boom this turn. no need to check.
      boolean shouldStillBoom = false;
      int turns = 1;
      if (algoState.turnsUntilBoom == -99) {
        algoState.turnsUntilBoom = -1;
        turns = 0;
      }
      for (; turns <= MAX_EXTRAPOLATION_TURNS; turns++) {
        AttackBreakdown futureAttack = StrategyUtility.futureAttackThreshold(move, turns);
        float futureAttackThreshold = futureAttack.units.cost;
        float futureMP = StrategyUtility.extrapolateFutureMP(move, turns, reducedScoutRushDefense);
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
    GameIO.debug().println("Turn " + turnNumber+ ": with " + saveCores + " saveCores! We currently have " + move.data.p1Stats.cores + " SP and " + move.data.p1Stats.bits + " MP!" );

    //TODO: Defense spending should be here... This may break the boom save cores thing


    float remainingCores = move.data.p1Stats.cores;
//    int prevDamage = algoState.scoredOnLocations.get(algoState.scoredOnLocations.size() - 1).size();
//    float health = move.data.p1Stats.integrity;
//    if (health / (health + prevDamage) >= 0.8) {
//    }
    GameIO.debug().println(saveCores + " saveCores! We currently have " + move.data.p1Stats.cores + "cores!" );

    //Attack
    /*
    For now let's just focus on defending and bomb attacking
     */

    GameIO.debug().println("awaitingBoom:" + algoState.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + algoState.turnsUntilBoom);

    AttackBreakdown attackData = StrategyUtility.attackThreshold(move);
    String boomSide = attackData.location;
    UnitCounts attackUnits = attackData.units;
    int attackPoints = attackUnits.cost;
    int numInters = attackUnits.numInterceptors;
    if (algoState.awaitingBoom && algoState.turnsUntilBoom == 0) { // DO THE BOOM
      GameIO.debug().println("Going to boom right now");
      GameIO.debug().println("Cores:" + move.data.p1Stats.cores);
      clearBoomPath(boomSide);
      placeBoomLid(boomSide);

      SpawnUtility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 23 : 4, 9)}, numInters);
      SpawnUtility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 6 : 21, 7)}, (int) move.data.p1Stats.bits);

      SpawnUtility.removeBuilding(move, new Coords(boomSide.equals("RIGHT") ? 6 : 21, 8));
      SpawnUtility.removeBuilding(move, new Coords(3, 12));
      algoState.awaitingBoom = false;
      algoState.turnsUntilBoom = -99;
    } else { // otherwise do not do the boom, check for it

      if (algoState.awaitingBoom) { // we are going to boom
        spawnDefensiveInters(reducedScoutRushDefense);
        if (algoState.turnsUntilBoom == 1) { //prepare to do the boom next turn
          clearBoomPath("LEFT");
          clearBoomPath("RIGHT");
          GameIO.debug().println("clearing path for future BOOM!!");
        }
        algoState.turnsUntilBoom--;
      } else {
        //update mp
        mp = move.data.p1Stats.bits;
        sp = move.data.p1Stats.cores;
        GameIO.debug().println("CHECK FOR HOOK==================");
        int maxDemos = (int) (mp / move.config.unitInformation.get(UnitType.Demolisher.ordinal()).cost2.orElse(3));
        float minDamagePerDemo = 5;
        HookAttack lowerHookAttack = HookAttack.evaluate(move, sp, mp - (move.data.p2Stats.bits > 5 ? (move.data.p2Stats.bits > 12 ? 2 : 1) : 0), 8, 27 - 8, 10, 12, maxDemos*minDamagePerDemo);
        HookAttack upperHookAttack = HookAttack.evaluate(move, sp, mp - (move.data.p2Stats.bits > 5 ? (move.data.p2Stats.bits > 12 ? 2 : 1) : 0), 9, 27 - 9, 13, 13, maxDemos*minDamagePerDemo);
        HookAttack potentialHookAttack = null;
        if (lowerHookAttack != null) {
          potentialHookAttack = lowerHookAttack;
        }
        if (upperHookAttack != null && (lowerHookAttack == null || upperHookAttack.expectedDefense.structureHealth > lowerHookAttack.expectedDefense.structureHealth)) {
          potentialHookAttack = upperHookAttack;
        }
        algoState.hooking = potentialHookAttack != null;
        if (potentialHookAttack != null) {
          GameIO.debug().printf("HOOKING!!!\tx:%d,y:%d,s:%s\n",potentialHookAttack.demolishers[0].x,potentialHookAttack.demolishers[0].y,potentialHookAttack.demolishers[0].x-13 == 0 ? "R" : "L");
          potentialHookAttack.execute(move);
        } else { //hook attack not done
          fillHookHoles();
          ScoutRush potentialScoutRush = ScoutRush.evaluate(move);
          if (potentialScoutRush != null && Math.random() > 0.1) {
            setUpEssentialDefense();
            potentialScoutRush.execute(move);
          } else {
            spawnDefensiveInters(scoutRushDefense);
          }
        }

      }

      // put up defenses (moved before end of else blck because we were messing up our own booms
      setUpEssentialDefense();

      int defenseBudget = StrategyUtility.neededDefenseSpending(move);
      if (defenseBudget > 0) { //we should smartly set up defenses
        defenseBudget = (int) Math.min(defenseBudget, move.data.p1Stats.cores - saveCores);

        setUpDefenseWithBudget(defenseBudget);
      }
    } //end large attack if

    // unconditionally make sure that we can hook next round if we want
    markHookHolesForDeletion();
  }

  /**
   * Marks all damaged structures for deletion.
   */
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
        if (unit.owner == PlayerId.Player1) { //this is our structure
          double startHealth = unit.unitInformation.startHealth.getAsDouble();
          if (unit.health < startHealth) { //thing is damaged
            SpawnUtility.removeBuilding(move, new Coords(x, y));
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
        SpawnUtility.placeTurrets(move, new Coords[]{toClose});
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
        SpawnUtility.placeSupports(move, new Coords[]{location});
        numFactories[0]--;
      } else {
        SpawnUtility.placeWalls(move, new Coords[]{location});
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
      alreadyReady = SpawnUtility.removeBuilding(move, toOpen) == 0 && alreadyReady;
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
   * Fills the main wall hook holes
   */
  private static void fillHookHoles() {
    for (Coords location : Locations.Essentials.mainWallHookHoles) {
      SpawnUtility.placeWall(move, location);
    }
  }

  /**
   * Marks the main wall hook holes for deletion
   */
  private static void markHookHolesForDeletion() {
    for (Coords location : Locations.Essentials.mainWallHookHoles) {
      SpawnUtility.removeBuilding(move, location);
    }
  }

  /**
   * Sets up essential defenses (the triangle and some towers)
   */
  private static void setUpEssentialDefense() {
    int budget = (int) move.data.p1Stats.cores; //can use everything to setup essential defense
    int spent = 0;
    try {

      //Get the core corner turrets down
      spent += attemptSpawnIfAffordable(Locations.Essentials.firstLeftTurret, Utility.TURRET, false, budget - spent);
      spent += attemptSpawnIfAffordable(Locations.Essentials.firstRightTurret, Utility.TURRET, false, budget - spent);

      //get walls in front of them
      spent += attemptSpawnIfAffordable(Locations.Essentials.firstLeftTurretWall, Utility.WALL, false, budget);
      spent += attemptSpawnIfAffordable(Locations.Essentials.firstRightTurretWall, Utility.WALL, false, budget);

      //upgrade two corner turrets
      spent += attemptSpawnIfAffordable(Locations.Essentials.firstLeftTurret, Utility.TURRET, true, budget - spent);
      spent += attemptSpawnIfAffordable(Locations.Essentials.firstRightTurret, Utility.TURRET, true, budget - spent);

      //upgrade walls in front of them
//      spent += attemptSpawnIfAffordable(Locations.Essentials.firstLeftTurretWall, Utility.WALL, true, budget);
//      spent += attemptSpawnIfAffordable(Locations.Essentials.firstRightTurretWall, Utility.WALL, true, budget);

      //get the main wall down
      for (Coords location : Locations.Essentials.mainWallCoords) {
        if (algoState.hooking && Locations.Essentials.mainWallHookHoles.contains(location)) {
          continue;
        }
        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);

      }

      //build left and right corner walls one by one (one left one right)
      int totalCornerWalls =  Locations.Essentials.leftCornerWalls.length + Locations.Essentials.rightCornerWalls.length;
      for (int i = 0; i < totalCornerWalls; i++) {
        Coords location;
        if (i % 2 == 0) {
          location = Locations.Essentials.leftCornerWalls[i / 2];
        } else {
          location = Locations.Essentials.rightCornerWalls[i / 2];
        }
        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);
      }


      //get the entrance left turret down
      spent += attemptSpawnIfAffordable(Locations.Essentials.leftEntranceTurret, Utility.TURRET, false, budget - spent);




    } catch (InsufficientResourcesException e) {
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[1].getLineNumber());
      return;
    }
  }
  /**
   * Sets up defense with maximum of budget cores to spend
   * @param budget
   */
  private static void setUpDefenseWithBudget(int budget) {
    if(budget <= 0) {
      return;
    }
    final int SAVE_AMOUNT = 2;
    int spent = SAVE_AMOUNT; //save 2 for the cap walls
    try {


      for (int i = 0; i < Locations.initialTopEntranceTurrets.length; i++) {
        Coords towerLocation = Locations.initialTopEntranceTurrets[i];
        Coords topWallLocation = Locations.topEntranceWalls[i];


        spent += attemptSpawnIfAffordable(towerLocation, Utility.TURRET, false, budget - spent);
        spent += attemptSpawnIfAffordable(topWallLocation, Utility.WALL, false, budget - spent);
        //spent += attemptSpawnIfAffordable(towerLocation, Utility.TURRET, true, budget - spent);
      }

      //upgrade corner 2 walls
      for (int i = 0; i < 2; i++) {
        Coords location = Locations.Essentials.leftCornerWalls[i];
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }
      for (int i = 0; i < 2; i++) {
        Coords location = Locations.Essentials.rightCornerWalls[i];
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }


      //place right turrets down
      for (Coords location : Locations.rightTurrets) {
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, false, budget - spent);
        //spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
      }

      //continue placing entrance turrets and upgrade top entrance walls
      for (int i = 0; i < Locations.topEntranceTurrets.length; i++) {
        //TODO: Make the wall at 5,13 turn into a turret eventually... (how do i do this??)
        Coords topTowerLocation = Locations.topEntranceTurrets[i];
        Coords topWallLocation = Locations.topEntranceWalls[i];
        Coords bottomTowerLocation = Locations.bottomEntranceTurrets[i];

        spent += attemptSpawnIfAffordable(topTowerLocation, Utility.TURRET, false, budget - spent);
        spent += attemptSpawnIfAffordable(topWallLocation, Utility.WALL, false, budget - spent);
        spent += attemptSpawnIfAffordable(bottomTowerLocation, Utility.TURRET, false, budget - spent);
        //spent += attemptSpawnIfAffordable(bottomTowerLocation, Utility.TURRET, true, budget - spent);
        //spent += attemptSpawnIfAffordable(topTowerLocation, Utility.TURRET, true, budget - spent);
        //spent += attemptSpawnIfAffordable(topWallLocation, Utility.WALL, true, budget - spent);

        if (i == 3) { //TODO: this is beyond jank but i (mudit) thought it was needed and its 5am so i not gonna refactor
          spent += attemptSpawnIfAffordable(new Coords(2,12), Utility.TURRET, false, budget - spent);
          spent += attemptSpawnIfAffordable(new Coords(1,12), Utility.TURRET, false, budget - spent);
          //spent += attemptSpawnIfAffordable(new Coords(2,12), Utility.TURRET, true, budget - spent);
          //spent += attemptSpawnIfAffordable(new Coords(1,12), Utility.TURRET, true, budget - spent);
        }
      }

      //NOW WE DO ALL THE UPGRADES
      //upgrade all corner walls
      for (Coords location : Locations.Essentials.leftCornerWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }
      for (Coords location : Locations.Essentials.rightCornerWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }

      //upgrade right turrets
      for (Coords location : Locations.rightTurrets) {
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
      }

      //upgrade left entrance towers
      for (Coords location : Locations.topEntranceTurrets) {
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
      }
      for (Coords location : Locations.bottomEntranceTurrets) {
        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
      }
      spent += attemptSpawnIfAffordable(new Coords(2,12), Utility.TURRET, true, budget - spent);
      spent += attemptSpawnIfAffordable(new Coords(1,12), Utility.TURRET, true, budget - spent);

      //finall upgrade all the top entrance walls
      for (Coords location : Locations.topEntranceWalls) {
        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
      }




      //MOST DEFENSE DONE=====================

//      for(Coords location : Locations.extraTurretCoords) {
//        spent += attemptSpawnIfAffordable(location, Utility.TURRET, false, budget - spent);
//        spent += attemptSpawnIfAffordable(location, Utility.TURRET, true, budget - spent);
//      }
//
//      for(Coords location : Locations.extraWallCoords) {
//        spent += attemptSpawnIfAffordable(location, Utility.WALL, false, budget - spent);
//        spent += attemptSpawnIfAffordable(location, Utility.WALL, true, budget - spent);
//      }


    } catch (InsufficientResourcesException e) {
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[1].getLineNumber());
      return;
    } finally {
      // place the two cap walls and upgrade them if we have the available budget
      //place top line cap
      try {
        spent -= SAVE_AMOUNT;
        Coords lastTopTurretLocation = DefenseUtility.findRightMostTurret(move, Locations.topEntranceTurrets);

        Coords topWallCap = new Coords(lastTopTurretLocation.x + 1, lastTopTurretLocation.y);
        if (topWallCap != null) {
          spent += attemptSpawnIfAffordable(topWallCap, Utility.WALL, false, budget - spent);
          SpawnUtility.removeBuilding(move, topWallCap);
          GameIO.debug().print(String.format("topWallCap: (%s, %s)", topWallCap.x, topWallCap.y));
        }


        //at this point, only the first bottom turret should be down...
        Coords bottomWallCap = DefenseUtility.findRightMostTurret(move, Locations.bottomEntranceTurrets);
        if (bottomWallCap != null) {
          spent += attemptSpawnIfAffordable(bottomWallCap, Utility.WALL, false, budget - spent);
          SpawnUtility.removeBuilding(move, bottomWallCap);
          GameIO.debug().print(String.format("bottomWallCap: (%s, %s)", bottomWallCap.x, bottomWallCap.y));
        }



        // now if we have done everything possible we should have budget left over to upgrade these caps...
        if (topWallCap != null) {
          spent += attemptSpawnIfAffordable(topWallCap, Utility.WALL, true, budget - spent);

        }
        if (bottomWallCap != null) {
          spent += attemptSpawnIfAffordable(bottomWallCap, Utility.WALL, true, budget - spent);
        }

      } catch(InsufficientResourcesException e) {
        return;
      }
    }
  }

  /**
   * Attempts to spawn if it is affordable. Returns the number of monies used.
   * @param location
   * @param unitType
   * @param upgrade
   * @param budget
   * @return the amount of money used
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
    if (StrategyUtility.numAffordableWithBudget(move, unitType, upgrade, budget) > 0) {
      if (upgrade) {
        return move.attemptUpgrade(location) == 1 ? SpawnUtility.getUnitCost(move, unitType, true) : 0;
      } else {
        return move.attemptSpawn(location, unitType) ? SpawnUtility.getUnitCost(move, unitType, upgrade) : 0;
      }
    }
    throw new InsufficientResourcesException("outta money bruh");

  }



}
