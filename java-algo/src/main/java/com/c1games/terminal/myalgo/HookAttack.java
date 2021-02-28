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

  /**
   * Does the hook attack ;o
   * @param move
   */
  public void execute(GameState move) {
    placeHookBar(move, hookX, hookY, hookHole, hookSide);
    placeSupports(move, hookX, hookY, hookHole, hookSide);

    SpawnUtility.spawnDemolishers(move, getDemoStart(move, hookX, hookY, hookHole, hookSide), numDemolishers);
    SpawnUtility.spawnInterceptors(move, getDemoStart(move, hookX, hookY, hookHole, hookSide), (int) move.data.p1Stats.bits);

    Utility.fillOtherHookHoles(move, hookHole);
  }

  static Coords getDemoStart(GameState state, int x, int y, Coords hole, int side) {
    return new Coords(13 + side, 0);
  }

  static Coords getInterStart(GameState state, int x, int y, Coords hole, int side) {
    return new Coords(13 + side, 0);
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

  static List<Coords> placeSupports(GameState state, int x, int y, Coords hole, int side) {
    List<Coords> neededSupports = new ArrayList<>();
    for (int supportY = 1; supportY < 7; supportY++) {
      int supportX;
      for (supportX = x - 3; supportX < x + 3; supportX++) {
        if (supportX == x) {
          continue;
        }
        Coords supportLoc = new Coords(supportX, supportY);
        if (state.getWallAt(supportLoc) == null) {
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
  static HookAttack evaluate(GameState move, double availableSP, double availableMP, int minX, int maxX, int minY, int maxY, float minDamage) {
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

          placeSupports(testState, hookX, hookY, hookExit, side);

          Coords start = getDemoStart(testState, hookX, hookY, hookExit, side);
          int targetEdge = MapBounds.getEdgeFromStart(start);
          List<Coords> path = move.pathfind(start, targetEdge);


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
            for (int frame = 0; frame < 2 && demolisherHealths.size() > 0; frame++) { //run each path point twice since demolishers move every 2 frames
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
                }
              }
              for (Unit attacker : attackers) {
                if (attacker.owner == PlayerId.Player2) {
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
//              GameIO.debug().printf("REPATHING!for:%s,%s,at:%s\n",start,side==0?"R":"L",pathPoint);
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
            damages.put(new Utility.Pair<>(new Utility.Pair<>(hookX, hookExit), side), new ExpectedDefense(move, path.toArray(new Coords[0]), spTaken, expectedDamage));
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
        hookX = entry.getKey().key.key;
        hookHole = entry.getKey().key.value;
        hookSide = entry.getKey().value;
        bestED = entry.getValue();
      }
    }
    if (hookHole == null || bestED.structureHealth < minDamage) {
      GameIO.debug().println("Not doing enough damage!");
//      damages.forEach((key, value) -> GameIO.debug().printf("x:%d,y:%d, %s. damage:%.2f, need:%.2f. path_len: %d: ends:%s\n",
//          key.key.x, key.key.y, key.value == 0 ? "R" : "L",
//          value.value.structureHealth, minDamage,
//          value.value.path.length, value.value.path[value.value.path.length-1].toString()));
//          Arrays.toString(Arrays.stream(value.value.path).limit(15).map(Coords::toString).toArray(String[]::new))));
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
