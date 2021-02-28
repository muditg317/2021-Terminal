package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.io.GameLoop;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

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
  static DemolisherRun evaluate(GameState move, double availableMP, float minDamage) {
    Map<Utility.Pair<Coords, Coords>, ExpectedDefense> damages = new HashMap<>();

    Config.UnitInformation demolisherInfo = move.config.unitInformation.get(UnitType.Demolisher.ordinal());
    int numDemolishers = (int) (availableMP / demolisherInfo.cost2.orElse(3));

    GameIO.debug().println("================DEMO ANALYSIS========");
    GameIO.debug().printf("MP: %.2f\tminDamage:%.2f\n", availableMP, minDamage);
    GameIO.debug().printf("DEMO RUN Online Adjustment: %.2f\n", onlineAdjustment);

    if (numDemolishers == 0) {
      GameIO.debug().println("no demos");
      return null;
    }

    List<Coords> spawnLocations = Arrays.stream(MapBounds.EDGE_LISTS).skip(2).map(Arrays::asList).flatMap(List::stream).collect(Collectors.toList());

    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);

    for (Coords hookExit : Locations.Essentials.mainWallHookHoles) {

      GameState demoRunState = Utility.duplicateState(move);
      int spentOnHookHoles = Utility.fillOtherHookHoles(demoRunState, hookExit);

      for (int i = 0; i < spawnLocations.size(); i++) {
        Coords start = spawnLocations.get(i);
        List<Coords> path;
        int targetEdge = MapBounds.getEdgeFromStart(start);
        try {
          path = demoRunState.pathfind(start, targetEdge);
        } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid hook exit\n", x, y);
          continue;
        }

        GameState testState = Utility.duplicateState(demoRunState);
        //if (i % 10 == 0) {
        //GameIO.debug().printf("TESTING DEMO RUN from %s:\n", start);
        //Utility.printGameBoard(testState.allUnits);
        //}

        double demolisherHealth = demolisherInfo.startHealth.orElse(5);
        List<Double> demolisherHealths = new ArrayList<>(numDemolishers);
        for (int d = 0; d < numDemolishers; d++) {
          demolisherHealths.add(demolisherHealth);
        }

        float spTaken = 0;
        float expectedDamage = 0;
        for (int p = 0; p < path.size() && p < 100; p++) {
          Coords pathPoint = path.get(p);
          Map<Unit, Coords> attackersLocations = StrategyUtility.getTowerLocations(testState, pathPoint, demolisherInfo.attackRange.orElse(4.5));
          List<Unit> attackers = new ArrayList<>(attackersLocations.keySet());
          attackers.sort(new Comparator<Unit>() {
            @Override
            public int compare(Unit o1, Unit o2) {
              return (int) Math.signum(pathPoint.distance(attackersLocations.get(o2)) - pathPoint.distance(attackersLocations.get(o1)));
            }
          });
          boolean needToRepath = false;
          int numDemolishersToAttack = demolisherHealths.size();
          for (int frame = 0; frame < (1/demolisherInfo.speed.orElse(0.5)); frame++) { //run each path point twice since demolishers move every 2 frames
            for (Unit attacker : attackers) {
              if (attacker.owner == PlayerId.Player1) {
                if (attacker.type == UnitType.Support) {
                  if (frame == 0) {
                    double shieldAmount = attacker.unitInformation.shieldPerUnit.orElse(attacker.upgraded ? 5 : 3) + (attacker.upgraded ? (attacker.unitInformation.shieldBonusPerY.orElse(0.3) * attackersLocations.get(attacker).y) : 0);
                    for (int d = 0; d < demolisherHealths.size(); d++) {
                      demolisherHealths.set(d, demolisherHealths.get(d) + shieldAmount);
                    }
                  }
                }
              } else {
                if (attackersLocations.get(attacker).distance(pathPoint) <= demolisherInfo.attackRange.orElse(4.5)) {
                  float initialTowerHealth = attacker.health;
                  while (numDemolishersToAttack > 0 && attacker.health > 0) {
                    attacker.health -= demolisherDamage;
                    numDemolishersToAttack--;
                    if (attacker.health <= 0) {
                      needToRepath = true;
                      break;
                    }
                  }
                  float damageDone = initialTowerHealth - attacker.health;
                  spTaken += (float) Utility.damageToSp(attacker, damageDone);
                }

                if (demolisherHealths.size() > 0) {
                  if (attackersLocations.get(attacker).distance(pathPoint) <= attacker.unitInformation.attackRange.orElse(attacker.upgraded ? 3.5 : 2.5)) {
                    double initialDemoHealth = demolisherHealths.get(demolisherHealths.size() - 1);
                    double towerDamage = attacker.unitInformation.attackDamageWalker.orElse(0);
                    demolisherHealths.set(demolisherHealths.size() - 1, Math.max(0, initialDemoHealth - towerDamage));
                    double afterDemoHealth = demolisherHealths.get(demolisherHealths.size() - 1);
                    double damageToDemos = initialDemoHealth - afterDemoHealth;
                    if (afterDemoHealth == 0) {
                      demolisherHealths.remove(demolisherHealths.size() - 1);
                    }
                    expectedDamage += damageToDemos;
                  }
                } else {
                  break;
                }
              }
            }
          }
          if (needToRepath) {
//          GameIO.debug().printf("REPATHING!for:%s,at:%s\n",start,pathPoint);
            List<Coords> newPath;
            try {
              newPath = testState.pathfind(pathPoint, targetEdge);
            } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid hook exit\n", x, y);
              continue;
            }
            path.subList(p, path.size()).clear();
            path.addAll(newPath);
          }
        }
        spTaken *= onlineAdjustment;
        if (spTaken >= minDamage/2) { // ignore result if it doesn't help
          damages.put(new Utility.Pair<>(start, hookExit), new ExpectedDefense(demoRunState, path.toArray(new Coords[0]), spTaken, expectedDamage));
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
        bestAttack = entry.getKey().key;
        hookHole = entry.getKey().value;
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
