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

  private static GameState predictedEnemyBaseLayout;

  static void execute(MyAlgo algoState, GameState move) {
    MainStrategy.algoState = algoState;
    MainStrategy.move = move;

    predictedEnemyBaseLayout = StrategyUtility.predictGameState(move, algoState.enemyBaseHistory);

    executeHelper();

    // do unconditional things
    // unconditionally make sure that we can hook next round if we want
    markHookHolesForDeletion();
    Boom.progress();
  }

  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  static void executeHelper() {
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
    //GameIO.debug().println("Enemy left corner heuristic: " + Boom.enemyDefenseHeuristic(move, Side.LEFT));
    //GameIO.debug().println("Enemy right corner heuristic: " + Boom.enemyDefenseHeuristic(move, Side.RIGHT));




    //setUpEssentialDefense(move, move.data.p1Stats.cores - Locations.Essentials.mainWallHookHoles.size() * move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1));
    //setUpEssentialDefense(predictedEnemyBaseLayout, predictedEnemyBaseLayout.data.p1Stats.cores - Locations.Essentials.mainWallHookHoles.size() * predictedEnemyBaseLayout.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1));
    setUpEssentialDefense(move, move.data.p1Stats.cores);
    setUpEssentialDefense(predictedEnemyBaseLayout, predictedEnemyBaseLayout.data.p1Stats.cores);

    //DECIDE TO BOOM OR NOT HERE.==========================
    Boom.evaluate(predictedEnemyBaseLayout, reducedScoutRushDefense);

    GameIO.debug().println("PREDICTED GAME STATE=========");
    Utility.printGameBoard(predictedEnemyBaseLayout.allUnits);

    //make sure we have enough for boom wall
    double saveCores = 0;

    if (Boom.awaitingBoom) {
      float ourSPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * turnNumber / move.config.resources.turnIntervalForBitSchedule;
      float growthByBoom = Boom.turnsUntilBoom * ourSPIncome;
      //we need ATLEAST 25 by the time we boom with all walls and a few turrets at the end
      saveCores = Math.max(0, 25 - growthByBoom);
      saveCores = Math.min(Math.max(move.data.p1Stats.cores, 0), saveCores);
    }

//    int prevDamage = algoState.scoredOnLocations.get(algoState.scoredOnLocations.size() - 1).size();
//    float health = move.data.p1Stats.integrity;
//    if (health / (health + prevDamage) >= 0.8) {
//    }
    GameIO.debug().println(saveCores + " saveCores! We currently have " + move.data.p1Stats.cores + " cores!" );


    double defenseBudget = StrategyUtility.neededDefenseSpending(move);
    GameIO.debug().printf("Defense budget: %.2f", defenseBudget);
    defenseBudget = Math.min(Math.max(defenseBudget, 0), move.data.p1Stats.cores);
    double attackSpBudget = sp - defenseBudget;
    /*
    ATTACKING ==================
     */

    //update mp
    mp = move.data.p1Stats.bits;
    sp = move.data.p1Stats.cores;

    if (Boom.awaitingBoom && Boom.turnsUntilBoom == 0 && Boom.attack != null) { // DO THE BOOM
      if (Boom.attack.execute(move)) {
        GameIO.debug().printf("DOING THE BOOM: %s\n", Boom.attack.sideToBoom.toString());
        fillHookHoles(move); //done by the Boom execution
        setUpEssentialDefense(move, move.data.p1Stats.cores);
        defenseBudget = Math.min(Math.max(defenseBudget, 0), move.data.p1Stats.cores);
        setUpDefenseWithBudget(move, defenseBudget, move.data.p1Stats.cores);
        return;
      }
    }
    // otherwise do not do the boom, check for it

    if (Boom.awaitingBoom) { // we are going to boom
      spawnDefensiveInters(reducedScoutRushDefense);
      if (Boom.turnsUntilBoom == 1) { //prepare to do the boom next turn
        Boom.clearBoomPath(move, Side.LEFT);
        Boom.clearBoomPath(move, Side.RIGHT);
        GameIO.debug().println("clearing path for future BOOM!!");
      }
      fillHookHoles(move);

      // put up defenses
      setUpEssentialDefense(move, move.data.p1Stats.cores);
      defenseBudget = Math.min(Math.max(defenseBudget, 0), move.data.p1Stats.cores - saveCores);
      setUpDefenseWithBudget(move, defenseBudget, move.data.p1Stats.cores);
      return;
    }
    Attack chosenAttack = chooseAttack(attackSpBudget, defenseBudget);
    if (chosenAttack != null) {
      MyAlgo.lastAttack = chosenAttack;
      if (!(chosenAttack instanceof HookAttack)) {
       //fillHookHoles(move);
      }
      chosenAttack.execute(move);
    } else {
      fillHookHoles(move);
      GameIO.debug().println("Spawn defensive inters!");
      spawnDefensiveInters(scoutRushDefense);
    }
    // put up defenses
    setUpEssentialDefense(move, move.data.p1Stats.cores);
    defenseBudget = Math.min(Math.max(defenseBudget, 0), move.data.p1Stats.cores - saveCores);
    setUpDefenseWithBudget(move, defenseBudget, move.data.p1Stats.cores);
  }

  /**
   * Decides the best attack and returns it. Nullable
   * If null, best attack not decided
   * @return
   */
  private static Attack chooseAttack(double attackSpBudget, double defenseSPBudget) {
    List<Attack> potentialAttacks = new ArrayList<>();
    float mp = move.data.p1Stats.bits;
    //GameIO.debug().println("CHECK FOR HOOK==================");
    int maxDemos = (int) (mp / move.config.unitInformation.get(UnitType.Demolisher.ordinal()).cost2.orElse(3));
    double enemySP = StrategyUtility.enemySPOnBoard(move) + move.data.p2Stats.cores;
    float minDamagePerDemo = (float) (enemySP * 0.2 / maxDemos);//5 TODO: CHANGE BACK TO 5!!
//        if (maxDemos < 2) {
//          minDamagePerDemo *= 1.5;
//        }
    HookAttack upperHookAttack = HookAttack.evaluate(predictedEnemyBaseLayout, attackSpBudget, mp - (move.data.p2Stats.bits > 5 ? (move.data.p2Stats.bits > 12 ? 2 : 1) : 0), 9, 27 - 9, 13, 13, maxDemos*minDamagePerDemo);
    HookAttack potentialHookAttack = null;

    if (upperHookAttack != null) {
      potentialHookAttack = upperHookAttack;
    }

    if (potentialHookAttack != null) {
      GameIO.debug().printf("HOOKING!!!\tx:%d,y:%d,s:%s\n",potentialHookAttack.hookX,potentialHookAttack.hookY,potentialHookAttack.hookSide == 0 ? "R" : "L");
      potentialAttacks.add(potentialHookAttack);
    }

    DemolisherRun potentialDemolisherRun = DemolisherRun.evaluate(predictedEnemyBaseLayout, mp, maxDemos * minDamagePerDemo);
    if (potentialDemolisherRun != null) {
      GameIO.debug().printf("DEMO RUN!\tat:%s\t damage:%.2f\n",potentialDemolisherRun.demolisherLocation, potentialDemolisherRun.expectedDefense.structureHealth);
      potentialAttacks.add(potentialDemolisherRun);
    }

    GameState defendedState = Utility.duplicateState(move);
    setUpDefenseWithBudget(defendedState, defenseSPBudget, defenseSPBudget);
    double scoutRushSPBudet = attackSpBudget - fillHookHoles(defendedState);


    ScoutRush potentialScoutRush = ScoutRush.evaluate(predictedEnemyBaseLayout, attackSpBudget);
    if (potentialScoutRush != null) {
      GameIO.debug().printf("SENDING SCOUT RUSH!\n\tSpBudget:%.2f\tNumScouts:%d\tScoutHealth: %d\n\tExpected Damage:%d\tOnline Adjustment:%.2f\n",
          potentialScoutRush.spBudget, potentialScoutRush.numScouts, potentialScoutRush.scoutHealth, potentialScoutRush.expectedDamage, ScoutRush.onlineAdjustment);
      potentialAttacks.add(potentialScoutRush);
    }


    if (potentialAttacks.isEmpty() && move.data.p1Stats.bits > StrategyUtility.mpCapacity(move, move.data.turnInfo.turnNumber) * 0.8) {
      upperHookAttack = HookAttack.evaluate(predictedEnemyBaseLayout, attackSpBudget, mp - (move.data.p2Stats.bits > 5 ? (move.data.p2Stats.bits > 12 ? 2 : 1) : 0), 9, 27 - 9, 13, 13, 0);
      potentialHookAttack = null;

      if (upperHookAttack != null) {
        potentialHookAttack = upperHookAttack;
      }

      if (potentialHookAttack != null) {
        GameIO.debug().printf("fall back hook::\n\tx:%d,y:%d,s:%s\n",potentialHookAttack.hookX,potentialHookAttack.hookY,potentialHookAttack.hookSide == 0 ? "R" : "L");
        potentialAttacks.add(potentialHookAttack);
      }

      potentialDemolisherRun = DemolisherRun.evaluate(predictedEnemyBaseLayout, mp, 0);
      if (potentialDemolisherRun != null) {
        GameIO.debug().printf("fallback demo run::\n\tat:%s\t damage:%.2f\n", potentialDemolisherRun.demolisherLocation, potentialDemolisherRun.expectedDefense.structureHealth);
        potentialAttacks.add(potentialDemolisherRun);
      }


      defendedState = Utility.duplicateState(move);
      setUpDefenseWithBudget(defendedState, defenseSPBudget, defenseSPBudget);
      scoutRushSPBudet = attackSpBudget - fillHookHoles(defendedState);

      potentialScoutRush = ScoutRush.evaluate(defendedState, scoutRushSPBudet);
      if (potentialScoutRush != null) {
        GameIO.debug().printf("fallback scout rush::\n\tSpBudget:%.2f\tNumScouts:%d\tScoutHealth: %d\n\tExpected Damage:%d\tOnline Adjustment:%.2f\n",
            potentialScoutRush.spBudget, potentialScoutRush.numScouts, potentialScoutRush.scoutHealth, potentialScoutRush.expectedDamage, ScoutRush.onlineAdjustment);
        potentialAttacks.add(potentialScoutRush);
      }
    }
    if (potentialAttacks.isEmpty()) return null;


    //sort attacks by some criteria
    potentialAttacks.sort((a1,a2) -> {
      return (int) Math.signum(a2.evaluation(move) - a1.evaluation(move));
    });


    // return best attack
    return potentialAttacks.get(0);
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
          if (unit.health < (startHealth * (unit.type == UnitType.Turret && unit.upgraded ? 0.5 : 1))) { //thing is damaged
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
   * @return the total cores spent
   */
  private static int fillHookHoles(GameState gameState) {
    int spent = 0;
    for (Coords location : Locations.Essentials.mainWallHookHoles) {
      if (SpawnUtility.placeWall(gameState, location)) {
        spent++;
      }
    }
    return spent;
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
  private static void setUpEssentialDefense(GameState move, double budget) {
//    int budget = (int) move.data.p1Stats.cores; //can use everything to setup essential defense
    double spent = 0;
    try {

      for (Coords loc : Locations.Essentials.mainTurrets) {
        spent += attemptSpawnIfAffordable(move, loc, Utility.TURRET, false, budget - spent);

      }

      //place corner walls one by one
      int totalCornerWalls =  Locations.Essentials.leftCornerWalls.length + Locations.Essentials.rightCornerWalls.length;
      for (int i = 0; i < totalCornerWalls; i++) {
        Coords location;
        if (i % 2 == 0) {
          location = Locations.Essentials.leftCornerWalls[i / 2];
        } else {
          location = Locations.Essentials.rightCornerWalls[i / 2];
        }
        spent += attemptSpawnIfAffordable(move, location, Utility.WALL, false, budget - spent);
      }

      //place walls in front of main Turrets
      for (Coords loc : Locations.Essentials.mainTurretWalls) {
        spent += attemptSpawnIfAffordable(move, loc, Utility.WALL, false, budget - spent);
      }

      //upgrade every other turret in main turrets
      for (int i = 0; i < Locations.Essentials.mainTurrets.length; i += 2) {
        Coords loc = Locations.Essentials.mainTurrets[i];
        spent += attemptSpawnIfAffordable(move, loc, Utility.TURRET, true, budget - spent);

      }

      //get the main wall down
      for (Coords location : Locations.Essentials.mainWallCoords) {
        if (/*algoState.hooking && */ Locations.Essentials.mainWallHookHoles.contains(location)) {
          continue;
        }
        spent += attemptSpawnIfAffordable(move, location, Utility.WALL, false, budget - spent);

      }

      //upgrade every other mainWallTurret

      for (int i = 0; i < Locations.Essentials.mainTurretWalls.length; i += 2) {
        Coords loc = Locations.Essentials.mainTurrets[i];
        spent += attemptSpawnIfAffordable(move, loc, Utility.WALL, true, budget - spent);
      }



      /*
      ADAPTIVE DEFENSES BELOW ==========================
       */
      //if we have been hit in the corners hard this is now considered essential...
      int cornerDamage = DefenseUtility.cornerDamageTaken(move);
      if (cornerDamage >= move.data.p1Stats.integrity / 5) {
        for (int i = Locations.Essentials.leftCornerWalls.length - 1; i > 0; i--) {
          Coords location = Locations.Essentials.leftCornerWalls[i];
          spent += attemptSpawnIfAffordable(move, location, Utility.WALL, true, budget - spent);
        }
        for (int i = Locations.Essentials.rightCornerWalls.length - 1; i > 0; i--) {
          Coords location = Locations.Essentials.rightCornerWalls[i];
          spent += (int) attemptSpawnIfAffordable(move, location, Utility.WALL, true, budget - spent);
        }

        //place corner turrets down
        for (Coords loc : Locations.cornerTurrets) {
          spent += attemptSpawnIfAffordable(move, loc, Utility.TURRET, false, budget - spent);
        }

        if (cornerDamage > 6) { //larger threshold to upgrade these towers
          for (Coords loc : Locations.cornerTurrets) {
            spent += attemptSpawnIfAffordable(move, loc, Utility.TURRET, true, budget - spent);
          }
        }

      } //end corner placing

      List<Coords> highPriorityWalls = DefenseUtility.getHighPriorityWalls(move);
      GameIO.debug().println("List of High Priority Walls: " + highPriorityWalls);
      if (highPriorityWalls != null) {
        for (Coords loc : highPriorityWalls) {
          spent += attemptSpawnIfAffordable(move, loc, Utility.WALL, true, budget - spent);
        }
      }


    } catch (InsufficientResourcesException e) {
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[5].getLineNumber());
      return;
    }
  }
  /**
   * Sets up defense with maximum of budget cores to spend
   * @param gameState the gameState to set up on (needed for hypothetical scenarios)
   * @param budget
   */
  private static void setUpDefenseWithBudget(GameState gameState, double budget, double totalAllowedSpending) {
    GameIO.debug().printf("Set up defenses!\tBudget: %.2f\tMax spending: %.2f\n", budget, totalAllowedSpending);
    if (budget <= 0 && totalAllowedSpending <= 0) {
      return;
    }
    int SAVE_AMOUNT = 0;
    budget -= SAVE_AMOUNT;
    totalAllowedSpending -= SAVE_AMOUNT; //save 2 for the cap walls
    int spent = 0;
    double initialSP = gameState.data.p1Stats.cores;
    try {

      spent += placeBudgetedDefenseHelper(gameState, budget, false);

    } catch (InsufficientResourcesException e) {
      spent += (initialSP - gameState.data.p1Stats.cores);
      GameIO.debug().println("spent: " + spent + " of " + budget + " || finishedBudget @ LINE " + e.getStackTrace()[5].getLineNumber());
      initialSP = gameState.data.p1Stats.cores;

      try {
        spent += placeBudgetedDefenseHelper(gameState, totalAllowedSpending, true);
      } catch (InsufficientResourcesException e2) {
        spent += (initialSP - gameState.data.p1Stats.cores);
        GameIO.debug().println("spent an extra: " + (initialSP - gameState.data.p1Stats.cores) + " of " + totalAllowedSpending + " || finishedBudget @ LINE " + e.getStackTrace()[5].getLineNumber());
      }
    } finally {
      // place the two cap walls and upgrade them if we have the available budget
      //place top line cap

    }
  }

  private static int placeBudgetedDefenseHelper(GameState gameState, double budget, boolean autoDelete) throws InsufficientResourcesException {
    int spent = 0;
    try {
      Method spawnMethod = MainStrategy.class.getMethod(autoDelete ? "attemptSpawnAndDelete" : "attemptSpawnIfAffordable", GameState.class, Coords.class, UnitType.class, boolean.class, double.class);

      //upgrade all main turrets
      for (Coords loc : Locations.Essentials.mainTurrets) {
        spent += attemptSpawnIfAffordable(move, loc, Utility.TURRET, true, budget - spent);
      }

      //upgrade walls infront of middle mainTurrets
      for (int i = 1; i < Locations.Essentials.mainTurretWalls.length - 1; i++) {
        Coords loc = Locations.Essentials.mainTurretWalls[i];
        spent += attemptSpawnIfAffordable(move, loc, Utility.WALL, true, budget - spent);
      }

      //upgrade all turret walls
      for (Coords loc : Locations.Essentials.mainTurretWalls) {
        spent += (int) spawnMethod.invoke(null, gameState, loc, Utility.WALL, true, budget - spent);
        //spent += (int) spawnMethod.invoke(null, gameState, towerLocation, Utility.TURRET, true, budget - spent);
      }


      //upgrade only the very corner walls
      Coords leftCorner = new Coords(0, 13);
      spent += (int) spawnMethod.invoke(null, gameState, leftCorner, Utility.WALL, true, budget - spent);

      Coords rightCorner = new Coords(27, 13);
      spent += (int) spawnMethod.invoke(null, gameState, rightCorner, Utility.WALL, true, budget - spent);

      //place extra turrets and upgrade them
      for (Coords loc : Locations.extraTurrets) {
        spent += (int) spawnMethod.invoke(null, gameState, loc, Utility.TURRET, false, budget - spent);
        spent += (int) spawnMethod.invoke(null, gameState, loc, Utility.TURRET, true, budget - spent);
      }

      //NOW WE DO ALL THE UPGRADES
      //upgrade all walls above a certain y
      final int topY = 13;
      final int bottomY = 8;
      for (int y = topY; y >= bottomY; y--) {
        for (int x = 0; x <= 27; x++) {
          Coords location = new Coords(x, y);
          Unit wall = move.getWallAt(location);
          if (move.getWallAt(location) != null && wall.type == Utility.WALL) {
            spent += (int) spawnMethod.invoke(null, gameState, location, Utility.WALL, true, budget - spent);
          }
        }
      }

      //upgrade all turrets above a certain Y
      for (int y = topY; y >= bottomY; y--) {
        for (int x = 0; x <= 27; x++) {
          Coords location = new Coords(x, y);
          Unit wall = move.getWallAt(location);
          if (move.getWallAt(location) != null && wall.type == Utility.TURRET) {
            spent += (int) spawnMethod.invoke(null, gameState, location, Utility.TURRET, true, budget - spent);
          }
        }
      }


      //MOST DEFENSE DONE=====================

      //      for(Coords location : Locations.extraTurretCoords) {
      //        spent += (int) spawnMethod.invoke(null, gameState, location, Utility.TURRET, false, budget - spent);
      //        if (autoDelete) SpawnUtility.removeBuilding(move, location);
      //        spent += (int) spawnMethod.invoke(null, gameState, location, Utility.TURRET, true, budget - spent);
      //      }
      //
      //      for(Coords location : Locations.extraWallCoords) {
      //        spent += (int) spawnMethod.invoke(null, gameState, location, Utility.WALL, false, budget - spent);
      //        if (autoDelete) SpawnUtility.removeBuilding(move, location);
      //        spent += (int) spawnMethod.invoke(null, gameState, location, Utility.WALL, true, budget - spent);
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
   *
   * @param gameState
   * @param location
   * @param unitType
   * @param upgrade
   * @param budget
   * @return the amount of money used
   */
  public static int attemptSpawnIfAffordable(GameState gameState, Coords location, UnitType unitType, boolean upgrade, double budget) throws InsufficientResourcesException {

    boolean markForDeletion = false;
    if (algoState.lastAttack != null && algoState.lastAttack.clearLocations.contains(location)) {
      return 0;
    }
    if (Boom.awaitingBoom) {
      if (Boom.turnsUntilBoom == 0) {
        for (Coords openLocation : Locations.boomPath_right) {
          if (Boom.attack != null) {
            Coords adjustedLoc = new Coords(Boom.attack.sideToBoom == Side.RIGHT ? openLocation.x : 27 - openLocation.x, openLocation.y);
            if (location.equals(adjustedLoc)) {
              return 0;
            }
          }

        }
      }
      else if (Boom.turnsUntilBoom < 2) {
        //GameIO.debug().println("Prevented spawn at" +location);
        for (Coords openLocation : Locations.boomPath_right) {
          if ((openLocation.x == location.x || (27 - openLocation.x) == location.x) && openLocation.y == location.y) {

            markForDeletion = true; // turnsUntilBoom == 1
          }
        }
      }
    }



    if (StrategyUtility.numAffordableWithBudget(gameState, unitType, upgrade, budget) > 0) {
      int spent = 0;
      if (upgrade) {
        spent = gameState.attemptUpgrade(location) == 1 ? SpawnUtility.getUnitCost(gameState, unitType, true) : 0;
      } else {
        spent = gameState.attemptSpawn(location, unitType) ? SpawnUtility.getUnitCost(gameState, unitType, upgrade) : 0;
      }
      if (markForDeletion && spent > 0) {
        SpawnUtility.removeBuilding(gameState, location);
      }
      return spent;
    }
    throw new InsufficientResourcesException("outta money bruh");

  }

  /**
   * Attempts to spawn if it is affordable. Returns the number of monies used.
   *
   * @param gameState
   * @param location
   * @param unitType
   * @param upgrade
   * @param budget
   * @return the amount of money used
   */
  public static int attemptSpawnAndDelete(GameState gameState, Coords location, UnitType unitType, boolean upgrade, double budget) throws InsufficientResourcesException {
    if (upgrade == true) {
      return 0;
    }
    int spent = attemptSpawnIfAffordable(gameState, location, unitType, upgrade, budget);
    if (spent > 0) {
      SpawnUtility.removeBuilding(gameState, location);
    }
    return spent;
  }
}
