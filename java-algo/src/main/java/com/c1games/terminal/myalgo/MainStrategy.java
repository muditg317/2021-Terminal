package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.*;
import com.c1games.terminal.algo.map.CanSpawn;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import javax.naming.InsufficientResourcesException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    GameIO.debug().println("Turn " + turnNumber+ ": We currently have " + move.data.p1Stats.cores + " SP and " + move.data.p1Stats.bits + " MP!" );


    int scoutRushDefense = StrategyUtility.neededScoutRushDefense(move);
    int reducedScoutRushDefense = (int) Math.max(0, scoutRushDefense - move.data.p1Stats.integrity + 1);

    GameIO.debug().println("scoutRushDefense:" + scoutRushDefense);
    GameIO.debug().println("reducedScoutRushDefense: " + reducedScoutRushDefense);
    GameIO.debug().println("Enemy left corner heuristic: " + Boom.enemyDefenseHeuristic(move, "LEFT"));
    GameIO.debug().println("Enemy right corner heuristic: " + Boom.enemyDefenseHeuristic(move, "RIGHT"));



    //DECIDE TO BOOM OR NOT HERE.==========================
    Boom.evaluate(move, reducedScoutRushDefense);

    setUpEssentialDefense();

    //make sure we have enough for boom wall
    double saveCores = 0;

    if (Boom.awaitingBoom) {
      float ourSPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * turnNumber / move.config.resources.turnIntervalForBitSchedule;
      float growthByBoom = Boom.turnsUntilBoom * ourSPIncome;
      //we need ATLEAST 25 by the time we boom with all walls and a few turrets at the end
      saveCores = Math.max(0, 25 - growthByBoom);
      saveCores = Math.min(Math.max(move.data.p1Stats.cores, 0), saveCores);
    }

    //TODO: Defense spending should be here... This may break the boom save cores thing


//    int prevDamage = algoState.scoredOnLocations.get(algoState.scoredOnLocations.size() - 1).size();
//    float health = move.data.p1Stats.integrity;
//    if (health / (health + prevDamage) >= 0.8) {
//    }
    GameIO.debug().println(saveCores + " saveCores! We currently have " + move.data.p1Stats.cores + " cores!" );


    double defenseBudget = StrategyUtility.neededDefenseSpending(move);
    double attackSpBudget = sp - defenseBudget;
    //Attack
    /*
    For now let's just focus on defending and bomb attacking
     */
    Boom.debugPrint();

    //update mp
    mp = move.data.p1Stats.bits;
    sp = move.data.p1Stats.cores;

    if (Boom.awaitingBoom && Boom.turnsUntilBoom == 0) { // DO THE BOOM
      Boom.execute(move);
      fillHookHoles();

      defenseBudget = Math.min(Math.max(defenseBudget, 0), move.data.p1Stats.cores);
      setUpDefenseWithBudget(defenseBudget, move.data.p1Stats.cores);
    } else { // otherwise do not do the boom, check for it

      if (Boom.awaitingBoom) { // we are going to boom
        spawnDefensiveInters(reducedScoutRushDefense);
        if (Boom.turnsUntilBoom == 1) { //prepare to do the boom next turn
          Boom.clearBoomPath(move, "LEFT");
          Boom.clearBoomPath(move, "RIGHT");
          GameIO.debug().println("clearing path for future BOOM!!");
        }
        Boom.turnsUntilBoom--;
        fillHookHoles();
      } else {

        GameIO.debug().println("CHECK FOR HOOK==================");
        int maxDemos = (int) (mp / move.config.unitInformation.get(UnitType.Demolisher.ordinal()).cost2.orElse(3));
        float minDamagePerDemo = 4;//5 TODO: CHANGE BACK TO 5!!
        HookAttack lowerHookAttack = HookAttack.evaluate(move, attackSpBudget, mp - (move.data.p2Stats.bits > 5 ? (move.data.p2Stats.bits > 12 ? 2 : 1) : 0), 6, 27 - 6, 10, 12, maxDemos*minDamagePerDemo);
        HookAttack upperHookAttack = HookAttack.evaluate(move, attackSpBudget, mp - (move.data.p2Stats.bits > 5 ? (move.data.p2Stats.bits > 12 ? 2 : 1) : 0), 9, 27 - 9, 13, 13, maxDemos*minDamagePerDemo);
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
          DemolisherRun potentialDemolisherRun = DemolisherRun.evaluate(move, mp, maxDemos*minDamagePerDemo);
          algoState.hooking = potentialDemolisherRun != null;
          if (potentialDemolisherRun != null) {
            GameIO.debug().printf("DEMO RUN!\tat:%s\t damage:%.2f\n",potentialDemolisherRun.demolisherLocation, potentialDemolisherRun.expectedDefense.structureHealth);
            potentialDemolisherRun.execute(move);
          } else {
            GameIO.debug().println("Fill in hook holes");
            fillHookHoles();
            ScoutRush potentialScoutRush = ScoutRush.evaluate(move, attackSpBudget);
            if (potentialScoutRush != null && Math.random() > 0.1) {
              GameIO.debug().println("Ping rush!");
              setUpEssentialDefense();
              potentialScoutRush.execute(move);
            } else {
              GameIO.debug().println("Spawn defensive inters!");
              spawnDefensiveInters(Math.min(Math.max(4, reducedScoutRushDefense), Math.max(scoutRushDefense, (int) (move.data.p2Stats.bits / 5))));
            }
          }
        }

      }

      // put up defenses (moved before end of else blck because we were messing up our own booms


      defenseBudget = Math.min(Math.max(defenseBudget, 0), move.data.p1Stats.cores - saveCores);
      setUpDefenseWithBudget(defenseBudget, move.data.p1Stats.cores);
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
   * Spawns defensive inters in front of the wall
   * @param num
   */
  private static void spawnDefensiveInters(int num) {
    Coords leftCoord = new Coords(7, 6);
    Coords rightCoord = new Coords(20, 6);
    if (move.canSpawn(leftCoord, Utility.INTERCEPTOR, 1) != CanSpawn.Yes) {
      leftCoord = new Coords(3, 10);
    }
    if (move.canSpawn(rightCoord, Utility.INTERCEPTOR, 1) != CanSpawn.Yes) {
      rightCoord = new Coords(24, 10);
    }
    int left = (int) Math.ceil(num / 2.0);
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
      //spent += attemptSpawnIfAffordable(Locations.Essentials.firstRightTurret, Utility.TURRET, true, budget - spent);
      //TODO: Let's hold off on upgrading the right turret for now...
      //upgrade walls in front of them
//      spent += attemptSpawnIfAffordable(Locations.Essentials.firstLeftTurretWall, Utility.WALL, true, budget);
//      spent += attemptSpawnIfAffordable(Locations.Essentials.firstRightTurretWall, Utility.WALL, true, budget);

      //get the main wall down
      for (Coords location : Locations.Essentials.mainWallCoords) {
        if (/*algoState.hooking && */ Locations.Essentials.mainWallHookHoles.contains(location)) {
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
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[5].getLineNumber());
      return;
    }
  }
  /**
   * Sets up defense with maximum of budget cores to spend
   * TODO: helper is broken and always autodels
   * @param budget
   */
  private static void setUpDefenseWithBudget(double budget, double totalAllowedSpending) {
    GameIO.debug().printf("Set up defenses!\tBudget: %.2f\tMax spending: %.2f\n", budget, totalAllowedSpending);
    if(budget <= 0 && totalAllowedSpending <= 0) {
      return;
    }
    final int SAVE_AMOUNT = 2;
    int spent = SAVE_AMOUNT; //save 2 for the cap walls
    double initialSP = move.data.p1Stats.cores;
    try {

      spent = placeBudgetedDefenseHelper(spent, budget, false);

    } catch (InsufficientResourcesException e) {
      spent += (initialSP - move.data.p1Stats.cores);
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[5].getLineNumber());
      initialSP = move.data.p1Stats.cores;
      try {
        spent = placeBudgetedDefenseHelper(spent, totalAllowedSpending, true);
      } catch (InsufficientResourcesException e2) {
        spent += (initialSP - move.data.p1Stats.cores);
        GameIO.debug().println("spent an extra: " + (initialSP - move.data.p1Stats.cores) + " of " + totalAllowedSpending + " || finishedBudget @ LINE " + e.getStackTrace()[5].getLineNumber());
      }
    } finally {
      // place the two cap walls and upgrade them if we have the available budget
      //place top line cap
      try {
        spent -= SAVE_AMOUNT;
        Coords lastTopTurretLocation = DefenseUtility.findRightMostTurret(move, Locations.topEntranceTurrets);
        Coords topWallCap = null;
        if (lastTopTurretLocation != null) {
          topWallCap = new Coords(lastTopTurretLocation.x + 1, lastTopTurretLocation.y);

          spent += attemptSpawnIfAffordable(topWallCap, Utility.WALL, false, budget - spent);
          SpawnUtility.removeBuilding(move, topWallCap);
          GameIO.debug().print(String.format("topWallCap: (%s, %s)", topWallCap.x, topWallCap.y));
        }


        //at this point, only the first bottom turret should be down...
        Coords lastBottomTurretLocation = DefenseUtility.findRightMostTurret(move, Locations.bottomEntranceTurrets);
        Coords bottomWallCap = null;
        if (lastBottomTurretLocation != null) {
          bottomWallCap = new Coords(lastBottomTurretLocation.x + 1, lastBottomTurretLocation.y);
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

  private static int placeBudgetedDefenseHelper(int spent, double budget, boolean autoDelete) throws InsufficientResourcesException {
    try {
      Method spawnMethod = MainStrategy.class.getMethod(autoDelete ? "attemptSpawnAndDelete" : "attemptSpawnIfAffordable", Coords.class, UnitType.class, boolean.class, double.class);

      for (int i = 0; i < Locations.initialTopEntranceTurrets.length; i++) {
        Coords towerLocation = Locations.initialTopEntranceTurrets[i];
        Coords topWallLocation = Locations.topEntranceWalls[i];


        spent += (int) spawnMethod.invoke(null, towerLocation, Utility.TURRET, false, budget - spent);
        spent += (int) spawnMethod.invoke(null, topWallLocation, Utility.WALL, false, budget - spent);
        //spent += (int) spawnMethod.invoke(null, towerLocation, Utility.TURRET, true, budget - spent);
      }

      //upgrade corner 2 walls ONLY IF WE HAVE BEEN HIT IN THE CORNER BEFORE
      boolean cornerHasBeenHit = DefenseUtility.cornerHasBeenHit(move);
      if (cornerHasBeenHit) {
        for (int i = Locations.Essentials.leftCornerWalls.length - 1; i > 0; i--) {
          Coords location = Locations.Essentials.leftCornerWalls[i];
          spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
        }
        for (int i = Locations.Essentials.rightCornerWalls.length - 1; i > 0; i--) {
          Coords location = Locations.Essentials.rightCornerWalls[i];
          spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
        }

        //place right turrets down (don't really need right turrets unless they be hittin the corners)
        if (!Boom.awaitingBoom || Boom.turnsUntilBoom != 0) {
          for (Coords location : Locations.rightTurrets) {
            spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, false, budget - spent);
            //spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, true, budget - spent);
          }
        }
      }





      //continue placing entrance turrets and upgrade top entrance walls
      for (int i = 0; i < Locations.topEntranceTurrets.length; i++) {
        //TODO: Make the wall at 5,13 turn into a turret eventually... (how do i do this??)
        Coords topTowerLocation = Locations.topEntranceTurrets[i];
        Coords topWallLocation = Locations.topEntranceWalls[i];
        Coords bottomTowerLocation = Locations.bottomEntranceTurrets[i];

        spent += (int) spawnMethod.invoke(null, topTowerLocation, Utility.TURRET, false, budget - spent);
        spent += (int) spawnMethod.invoke(null, topWallLocation, Utility.WALL, false, budget - spent);
        spent += (int) spawnMethod.invoke(null, bottomTowerLocation, Utility.TURRET, false, budget - spent);
        //spent += (int) spawnMethod.invoke(null, bottomTowerLocation, Utility.TURRET, true, budget - spent);
        //spent += (int) spawnMethod.invoke(null, topTowerLocation, Utility.TURRET, true, budget - spent);
        //spent += (int) spawnMethod.invoke(null, topWallLocation, Utility.WALL, true, budget - spent);

        if (i == 3) { //TODO: this is beyond jank but i (mudit) thought it was needed and its 5am so i not gonna refactor
          if (!Boom.awaitingBoom || Boom.turnsUntilBoom != 0) {
            spent += (int) spawnMethod.invoke(null, new Coords(2, 12), Utility.TURRET, false, budget - spent);
            spent += (int) spawnMethod.invoke(null, new Coords(1, 12), Utility.TURRET, false, budget - spent);
            //spent += (int) spawnMethod.invoke(null, new Coords(2,12), Utility.TURRET, true, budget - spent);
            //spent += (int) spawnMethod.invoke(null, new Coords(1,12), Utility.TURRET, true, budget - spent);
          }
        }
      }
      //prevent right side damage
      Coords extraRightTower = new Coords(23, 13);
      Coords extraRightWall = new Coords(22, 13);
      spent += (int) spawnMethod.invoke(null, extraRightTower, Utility.TURRET, false, budget - spent);
      spent += (int) spawnMethod.invoke(null, extraRightTower, Utility.TURRET, true, budget - spent);
      spent += (int) spawnMethod.invoke(null, extraRightWall, Utility.WALL, false, budget - spent);
      spent += (int) spawnMethod.invoke(null, extraRightWall, Utility.WALL, true, budget - spent);


      //NOW WE DO ALL THE UPGRADES
      //upgrade all corner walls
      for (Coords location : Locations.Essentials.leftCornerWalls) {
        spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
      }
      for (Coords location : Locations.Essentials.rightCornerWalls) {
        spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
      }

      //upgrade right turrets
      if (cornerHasBeenHit) {
        for (Coords location : Locations.rightTurrets) {
          spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, true, budget - spent);
        }
      }


      //upgrade left entrance towers
      for (Coords location : Locations.topEntranceTurrets) {
        spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, true, budget - spent);
      }
      for (Coords location : Locations.bottomEntranceTurrets) {
        spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, true, budget - spent);
      }
      spent += (int) spawnMethod.invoke(null, new Coords(2, 12), Utility.TURRET, true, budget - spent);
      spent += (int) spawnMethod.invoke(null, new Coords(1, 12), Utility.TURRET, true, budget - spent);

      //finall upgrade all the top entrance walls
      for (Coords location : Locations.topEntranceWalls) {
        spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
      }

      //upgrade all walls above a certain y
      final int topY = 13;
      final int bottomY = 8;
      for (int y = topY; y >= bottomY; y--) {
        for (int x = 0; x <= 27; x++) {
          Coords location = new Coords(x, y);
          Unit wall = move.getWallAt(location);
          if (move.getWallAt(location) != null && wall.type == Utility.WALL) {
            spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
          }
        }
      }


      //MOST DEFENSE DONE=====================

      //      for(Coords location : Locations.extraTurretCoords) {
      //        spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, false, budget - spent);
      //        if (autoDelete) SpawnUtility.removeBuilding(move, location);
      //        spent += (int) spawnMethod.invoke(null, location, Utility.TURRET, true, budget - spent);
      //      }
      //
      //      for(Coords location : Locations.extraWallCoords) {
      //        spent += (int) spawnMethod.invoke(null, location, Utility.WALL, false, budget - spent);
      //        if (autoDelete) SpawnUtility.removeBuilding(move, location);
      //        spent += (int) spawnMethod.invoke(null, location, Utility.WALL, true, budget - spent);
      //      }

      return spent;
    } catch (InvocationTargetException e) {
//      e.getCause().printStackTrace(GameIO.debug());
      throw (InsufficientResourcesException) e.getCause();
    } catch (NoSuchMethodException|IllegalAccessException e) {
      GameIO.debug().println("BIG ERROR========================");
      e.printStackTrace(GameIO.debug());
      return spent;
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
  public static int attemptSpawnIfAffordable(Coords location, UnitType unitType, boolean upgrade, double budget) throws InsufficientResourcesException {
    if (Boom.awaitingBoom && Boom.turnsUntilBoom < 2) {
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

  /**
   * Attempts to spawn if it is affordable. Returns the number of monies used.
   * @param location
   * @param unitType
   * @param upgrade
   * @param budget
   * @return the amount of money used
   */
  public static int attemptSpawnAndDelete(Coords location, UnitType unitType, boolean upgrade, double budget) throws InsufficientResourcesException {
    int spent = attemptSpawnIfAffordable(location, unitType, upgrade, budget);
    if (spent > 0) {
      SpawnUtility.removeBuilding(move, location);
    }
    return spent;
  }



}
