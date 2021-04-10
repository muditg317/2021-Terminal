package com.c1games.terminal.myalgo.attack;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.myalgo.utility.Locations;
import com.c1games.terminal.myalgo.utility.SpawnUtility;
import com.c1games.terminal.myalgo.utility.StrategyUtility;
import com.c1games.terminal.myalgo.utility.Utility;
import com.c1games.terminal.simulation.SimBoard;
import com.c1games.terminal.simulation.Simulator;

import java.util.*;
import java.util.stream.Collectors;

public class DemolisherRun extends Attack {
  static double onlineAdjustment = 1;

  Coords demolisherLocation;
  Coords hookHole;
  int numDemolishers;

  // uses structureHealth as expected structure points taken
  ExpectedDefense expectedDefense;

  public DemolisherRun(GameState move, Coords demolisherLocation, Coords hookHole, ExpectedDefense expectedDefense, int numDemolishers) {
    this.demolisherLocation = demolisherLocation;
    this.hookHole = hookHole;
    this.expectedDefense = expectedDefense;
    this.numDemolishers = numDemolishers;
    this.clearLocations = new HashSet<>(Arrays.asList(expectedDefense.path));
    this.clearLocations.add(hookHole);
  }

  @Override
  public double getExpectedAttackValue() {
    return expectedDefense.structureHealth;
  }


  /**
   * Does the demolisher run ;o
   * @param move
   */
  public void execute(GameState move) {
    Utility.fillOtherHookHoles(move, hookHole);
    SpawnUtility.spawnDemolishers(move, demolisherLocation, numDemolishers);
//    for (int wallY = 2; wallY <= 13; wallY++) {
//      if (wallY == 11) continue;
//      Coords leftWall = new Coords(15-wallY,wallY);
//      if (demolisherLocation.x > 13 || wallY < demolisherLocation.y) {
//        if (SpawnUtility.placeWall(move, leftWall)) SpawnUtility.removeBuilding(move, leftWall);
//      }
//      Coords rightWall = new Coords(12+wallY, wallY);
//      if (demolisherLocation.x < 14 || wallY < demolisherLocation.y) {
//        if (SpawnUtility.placeWall(move, rightWall)) SpawnUtility.removeBuilding(move, rightWall);
//      }
//    }
    SpawnUtility.spawnInterceptors(move, demolisherLocation, (int) move.data.p1Stats.bits);
  }

  /**
   * finds the optimal hook attack
   * @param move
   * @param availableMP
   * @return
   */
  public static DemolisherRun evaluate(GameState move, double availableMP, float minDamage) {
    Map<Utility.Pair<Coords, Coords>, ExpectedDefense> damages = new HashMap<>();

    Config.UnitInformation demolisherInfo = move.config.unitInformation.get(UnitType.Demolisher.ordinal());
    double demolisherCost = demolisherInfo.cost2.orElseThrow();
    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);
    int numDemolishers = (int) (availableMP / demolisherCost);

    GameIO.debug().println("================DEMO ANALYSIS========");
    GameIO.debug().printf("MP: %.2f\tminDamage:%.2f\n", availableMP, minDamage);
    GameIO.debug().printf("DEMO RUN Online Adjustment: %.2f\n", onlineAdjustment);

    if (numDemolishers == 0) {
      GameIO.debug().println("no demos");
      return null;
    }

    List<Coords> spawnLocations = Arrays.stream(MapBounds.EDGE_LISTS).skip(2).map(Arrays::asList).flatMap(List::stream).collect(Collectors.toList());


    for (Coords hookExit : Locations.Essentials.mainWallHookHoles) {

      GameState demoRunState = Utility.duplicateState(move);
      int spentOnHookHoles = Utility.fillOtherHookHoles(demoRunState, hookExit);

      for (int i = 0; i < spawnLocations.size(); i++) {
        Coords start = spawnLocations.get(i);

        GameState testState = Utility.duplicateState(demoRunState);

        SpawnUtility.spawnDemolishers(testState, start, numDemolishers);
        SpawnUtility.spawnInterceptors(testState, start, (int) (availableMP - numDemolishers*demolisherCost));

        double oldSP = StrategyUtility.enemySPOnBoard(testState.allUnits);
        SimBoard simulationResult = Simulator.simulate(testState);
        double newSP = simulationResult.enemySPOnBoard();

        float spTaken = 0;

//          for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
//            for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
//              Unit structure = testState.getWallAt(new Coords(x,y));
//              if (structure != null) {
//                double newHealth = 0;
//                StructureUnit simStructure = simulationResult.getStructureAt(new Coords(x,y));
//                if (simStructure != null) {
//                  newHealth = simStructure.getHealth();
//                }
//                spTaken += Utility.healthToSP(structure.unitInformation, structure.health - newHealth);
//              }
//            }
//          }

        spTaken = (float) (oldSP - newSP);

        spTaken *= onlineAdjustment;
        if (spTaken >= minDamage/2) { // ignore result if it doesn't help
          damages.put(new Utility.Pair<>(start, hookExit), new ExpectedDefense(move, simulationResult.getTraversedPoints().toArray(new Coords[0]), spTaken, 0));
        }
      }
    }


    GameIO.debug().println("================DAMAGES========");
    GameIO.debug().printf("%s: %d options\n", damages.size() > 0 ? "CAN DEMO" : "none", damages.size());
//    damages.forEach((key, value) -> GameIO.debug().printf("from:%s hole:%s. damage:%.2f, need:%.2f. path_len: %d: ends:%s\n\t%s\n",
//        key.key,key.value,
//        value.structureHealth, minDamage,
//        value.path.length, value.path[value.path.length-1].toString(),
//        Arrays.toString(Arrays.stream(value.path).limit(15).map(Coords::toString).toArray(String[]::new))));
    GameIO.debug().println("================END DEMO ANALYSIS========");

    decayLearning();
    Coords bestAttack = null;
    Coords hookHole = null;
    ExpectedDefense bestED = new ExpectedDefense(move, null, minDamage/2, 0);

    for (Map.Entry<Utility.Pair<Coords,Coords>, ExpectedDefense> entry : damages.entrySet()) {
      if (entry.getValue().structureHealth > bestED.structureHealth) {
        bestAttack = entry.getKey().getKey();
        hookHole = entry.getKey().getValue();
        bestED = entry.getValue();
      }
    }
    if (bestAttack == null || bestED.structureHealth < minDamage) {
      if (bestAttack != null) {
        GameIO.debug().printf("Current best hook: %s. damage: %.2f out of %.2f. path_len: %d. ends:%s\n",
            bestAttack,
            bestED.structureHealth, minDamage,
            bestED.path.length, bestED.path[bestED.path.length-1].toString());
      }
      return null;
    }

    Coords hookLocation = bestAttack;

    GameIO.debug().printf("Will run demo: %s. expected damage: %.2f\n", hookLocation.toString(), bestED.structureHealth);
    GameIO.debug().printf("from:%s hole:%s. damage:%.2f, need:%.2f. path_len: %d: ends:%s\n\t%s\n",
        bestAttack,hookLocation,
        bestED.structureHealth, minDamage,
        bestED.path.length, bestED.path[bestED.path.length-1].toString(),
        Arrays.toString(Arrays.stream(bestED.path).map(Coords::toString).toArray(String[]::new)));
    return new DemolisherRun(move, hookLocation, hookHole, bestED, (int) (availableMP / demolisherInfo.cost2.orElse(3)));
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
