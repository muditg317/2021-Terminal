package com.c1games.terminal.myalgo.attack;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.myalgo.utility.Locations;
import com.c1games.terminal.myalgo.utility.SpawnUtility;
import com.c1games.terminal.myalgo.utility.StrategyUtility;
import com.c1games.terminal.myalgo.utility.Utility;
import com.c1games.terminal.simulation.SimBoard;
import com.c1games.terminal.simulation.Simulator;
import com.c1games.terminal.simulation.units.StructureUnit;

import java.util.*;

public class HookAttack extends Attack {
  static double onlineAdjustment = 1;

  int hookX;
  int hookY;
  Coords hookHole;
  int hookSide;
  int numDemolishers;

  // uses structureHealth as expected structure points taken
  ExpectedDefense expectedDefense;

  public HookAttack(int hookX, int hookY, Coords hookHole, int hookSide, ExpectedDefense expectedDefense, int numDemolishers) {
    this.hookX = hookX;
    this.hookY = hookY;
    this.hookHole = hookHole;
    this.hookSide = hookSide;
    this.expectedDefense = expectedDefense;
    this.clearLocations = new HashSet<>(Arrays.asList(expectedDefense.path));
    this.numDemolishers = numDemolishers;
  }

  @Override
  public double getExpectedAttackValue() {
    return expectedDefense.structureHealth;
  }

  /**
   * Does the hook attack ;o
   * @param move
   */
  public void execute(GameState move) {
//    GameIO.debug().println("ABOUT TO HOOK ON THIS BOARD==================");
//    Utility.printGameBoard(move.allUnits);

    Utility.fillOtherHookHoles(move, hookHole);

    placeHookBar(move, hookX, hookY, hookHole, hookSide);
    placeSupports(move, hookX, hookY, hookHole, hookSide);

    SpawnUtility.spawnDemolishers(move, getDemoStart(move, hookX, hookY, hookHole, hookSide), numDemolishers);
    SpawnUtility.spawnInterceptors(move, getInterStart(move, hookX, hookY, hookHole, hookSide), (int) move.data.p1Stats.bits);
  }

  static Coords getDemoStart(GameState state, int x, int y, Coords hole, int side) {
    return new Coords(13 + side, 0);
  }

  static Coords getInterStart(GameState state, int x, int y, Coords hole, int side) {
    return new Coords(side == 0 ? (y+13) : (14-y), y - 1);
  }

  static List<Coords> placeHookBar(GameState state, int x, int y, Coords hole, int side) {
    int topWallStartX = side == 0 ? (12+y) : (15-y);
    int wallBuildDir = side * 2-1;

    List<Coords> placed = new ArrayList<>();
    for (int wallX = topWallStartX; (side == 0 ? wallX > x : wallX < x); wallX += wallBuildDir) {
      Coords topWall = new Coords(wallX, y);
      if (state.getWallAt(topWall) == null) {
        if (!SpawnUtility.placeWall(state, topWall)) {
          break;
        }
        placed.add(topWall);
        SpawnUtility.removeBuilding(state, topWall);
      }
    }
    return placed;
  }

  static boolean checkHookBar(GameState state, int x, int y, Coords hole, int side) {
    int topWallStartX = side == 0 ? (12+y) : (15-y);
    int wallBuildDir = side * 2-1;

    for (int wallX = topWallStartX; (side == 0 ? wallX > x : wallX < x); wallX += wallBuildDir) {
      if (state.getWallAt(new Coords(wallX, y)) == null) {
        return false;
      }
    }
    return true;
  }

  static List<Coords> placeSupports(GameState state, int x, int y, Coords hole, int side) {
    List<Coords> neededSupports = new ArrayList<>();
    for (int supportY = 1; supportY < 7; supportY++) {
      int supportX;
      for (supportX = x - 3; supportX < x + 3; supportX++) {
        if (supportX == x) {
          continue;
        }
        Coords supportLoc = new Coords(supportX, supportY);
        if (MapBounds.inArena(supportLoc) && state.getWallAt(supportLoc) == null) {
          if (!SpawnUtility.placeSupport(state, supportLoc)) {
            break;
          }
          neededSupports.add(supportLoc);
          SpawnUtility.removeBuilding(state, supportLoc);
        }
      }
      if (supportX < x + 3) {
        break;
      }
    }
    return neededSupports;
  }

  /**
   * finds the optimal hook attack
   * @param move
   * @param availableSP
   * @param availableMP
   * @return
   */
  public static HookAttack evaluate(GameState move, double availableSP, double availableMP, int minX, int maxX, int minY, int maxY, float minDamage) {
    Map<Utility.Pair<Utility.Pair<Integer, Coords>, Integer>, ExpectedDefense> damages = new HashMap<>();

    Config.UnitInformation demolisherInfo = move.config.unitInformation.get(UnitType.Demolisher.ordinal());
    int numDemolishers = (int) (availableMP / demolisherInfo.cost2.orElse(3));

    GameIO.debug().println("================HOOK ANALYSIS========");
    GameIO.debug().printf("SP: %.2f\tMP: %.2f\tminDamage:%.2f\n", availableSP, availableMP, minDamage);
    GameIO.debug().printf("Hook Attack Online Adjustment: %.2f\n", onlineAdjustment);

    if (numDemolishers == 0) {
      GameIO.debug().println("no demos");
      return null;
    }


    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);

    double wallCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);

    int hookY = 13;

    for (Coords hookExit : Locations.Essentials.mainWallHookHoles) {

      GameState hookState = Utility.duplicateState(move);
      int spentOnHookHoles = Utility.fillOtherHookHoles(hookState, hookExit);
      
      for (int hookX = 5; hookX <= 27-5; hookX++) {
        if (!MapBounds.ARENA[hookX][13]) {
          continue;
        }
        // Test hooking right or left
        for (int side = 0; side <= 1; side++) {//0: target RIGHT, 1: target LEFT
          GameState testState = Utility.duplicateState(hookState);


          // Walls of the actual hook bar
          placeHookBar(testState, hookX, hookY, hookExit, side);
          if (!checkHookBar(testState, hookX, hookY, hookExit, side)) {
            continue;
          }

          placeSupports(testState, hookX, hookY, hookExit, side);

          SpawnUtility.spawnDemolishers(testState, getDemoStart(testState, hookX, hookY, hookExit, side), numDemolishers);
          SpawnUtility.spawnInterceptors(testState, getInterStart(testState, hookX, hookY, hookExit, side), (int) (availableMP - numDemolishers*demolisherInfo.cost2.orElse(3)));


          double oldSP = StrategyUtility.enemySPOnBoard(testState.allUnits);
          SimBoard simulationResult = Simulator.simulate(testState);
          double newSP = simulationResult.enemySPOnBoard();

          float spTaken = 0;

          for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
            for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
              Unit structure = testState.getWallAt(new Coords(x,y));
              if (structure != null) {
                double newHealth = 0;
                StructureUnit simStructure = simulationResult.getStructureAt(new Coords(x,y));
                if (simStructure != null) {
                  newHealth = simStructure.getHealth();
                }
                spTaken += Utility.healthToSP(structure.unitInformation, structure.health - newHealth);
              }
            }
          }

          spTaken = (float) (oldSP - newSP);

          spTaken *= onlineAdjustment;
          if (spTaken >= minDamage/2) { // ignore result if it doesn't help
            damages.put(new Utility.Pair<>(new Utility.Pair<>(hookX, hookExit), side), new ExpectedDefense(move, simulationResult.getTraversedPoints().toArray(new Coords[0]), spTaken, 0));
          }
        }
      }
    }

    GameIO.debug().println("================DAMAGES========");
    GameIO.debug().printf("%s: %d options\n", damages.size() > 0 ? "CAN HOOK" : "none", damages.size());
    GameIO.debug().println("================END HOOK ANALYSIS========");


    int hookX = -1;
    Coords hookHole = null;
    int hookSide = -1;
    ExpectedDefense bestED = new ExpectedDefense(move, null, minDamage/2, 0);

    for (Map.Entry<Utility.Pair<Utility.Pair<Integer, Coords>, Integer>, ExpectedDefense> entry : damages.entrySet()) {
      if (entry.getValue().structureHealth > bestED.structureHealth) {
        hookX = entry.getKey().getKey().getKey();
        hookHole = entry.getKey().getKey().getValue();
        hookSide = entry.getKey().getValue();
        bestED = entry.getValue();
      }
    }

    if (hookHole == null || bestED.structureHealth < minDamage) {
      GameIO.debug().println("Not doing enough damage!");
      if (hookHole != null) {
        GameIO.debug().printf("Current best hook: x:%d, hole:%s, %s. damage: %.2f out of %.2f. path_len: %d. ends:%s\n",
            hookX, hookHole, hookSide == 0 ? "R" : "L",
            bestED.structureHealth, minDamage,
            bestED.path.length, bestED.path[bestED.path.length-1].toString());
      }
      return null;
    }

    Coords hookLocation = new Coords(hookX, hookY);

    GameIO.debug().printf("Current best hook: %s, hole:%s, %s. damage: %.2f out of %.2f. path_len: %d. ends:%s\n\tPath: ",
        hookLocation, hookHole, hookSide == 0 ? "R" : "L",
        bestED.structureHealth, minDamage,
        bestED.path.length, bestED.path[bestED.path.length-1].toString());
    Arrays.stream(bestED.path).forEach(coords -> {
      GameIO.debug().printf("%s, ",coords);
    });
    GameIO.debug().println();




    decayLearning();
    return new HookAttack(hookX, hookY, hookHole, hookSide, bestED, numDemolishers);
  }

  public void learn(double actualSpDamage) {
    onlineAdjustment *= actualSpDamage / this.expectedDefense.structureHealth;
  }

  public static void decayLearning() {
    onlineAdjustment = (onlineAdjustment + 1) / 2.0;
  }

  /**
   * Returns some evaluation of this attack. Currently it is the percent of total enemy SP that it takes
   * @param move
   * @return
   */
  public double evaluation(GameState move) {
    return this.expectedDefense.structureHealth / StrategyUtility.totalEnemySp(move);
  }

}
