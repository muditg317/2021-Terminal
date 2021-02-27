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

public class DemolisherRun {
  Coords demolisherLocation;
  int numDemolishers;

  // uses structureHealth as expected structure points taken
  ExpectedDefense expectedDefense;

  public DemolisherRun(GameState move, Coords demolisherLocation, ExpectedDefense expectedDefense, int numDemolishers) {
    this.demolisherLocation = demolisherLocation;
    this.expectedDefense = expectedDefense;
    this.numDemolishers = numDemolishers;
  }


  /**
   * Does the demolisher run ;o
   * @param move
   */
  public void execute(GameState move) {
    SpawnUtility.spawnDemolishers(move, demolisherLocation, numDemolishers);
  }

  /**
   * finds the optimal hook attack
   * @param move
   * @param availableMP
   * @return
   */
  static DemolisherRun evaluate(GameState move, double availableMP, float minDamage) {
    Map<Coords, ExpectedDefense> damages = new HashMap<>();

    Config.UnitInformation demolisherInfo = move.config.unitInformation.get(UnitType.Demolisher.ordinal());
    int numDemolishers = (int) (availableMP / demolisherInfo.cost2.orElse(3));

    GameIO.debug().println("================DEMO ANALYSIS========");

    if (numDemolishers == 0) {
      GameIO.debug().println("no demos");
      return null;
    }

    List<Coords> spawnLocations = Arrays.stream(MapBounds.EDGE_LISTS).skip(2).map(Arrays::asList).flatMap(List::stream).collect(Collectors.toList());

    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);

    for (int i = 0; i < spawnLocations.size(); i++) {
      Coords start = spawnLocations.get(i);
      List<Coords> path;
      int targetEdge = MapBounds.getEdgeFromStart(start);
      try {
        path = move.pathfind(start, targetEdge);
      } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid hook exit\n", x, y);
        continue;
      }

      GameState testState = new GameState(move.config, move.data);
      for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
        for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
          testState.allUnits[x][y] = move.allUnits[x][y].stream().map(unit -> {
            Unit newUnit = new Unit(unit.type, unit.health, unit.id, unit.owner, move.config);
            if (unit.upgraded) newUnit.upgrade();
            return newUnit;
          }).collect(Collectors.toList());
        }
      }

      int demolishersRemaining = numDemolishers;
      double demolisherHealth = demolisherInfo.startHealth.orElse(5);
      List<Double> demolisherHealths = new ArrayList<>(demolishersRemaining);
      for (int d = 0; d < demolishersRemaining; d++) {
        demolisherHealths.add(demolisherHealth);
      }

      float spTaken = 0;
      float expectedDamage = 0;
      for (int p = 0; p < path.size() && p < 100; p++) {
        Coords pathPoint = path.get(p);
        Map<Unit, Coords> attackersLocations = StrategyUtility.getAttackerLocations(testState, pathPoint);
        List<Unit> attackers = new ArrayList<>(attackersLocations.keySet());
        attackers.sort(new Comparator<Unit>() {
          @Override
          public int compare(Unit o1, Unit o2) {
            return (int) Math.signum(pathPoint.distance(attackersLocations.get(o2)) - pathPoint.distance(attackersLocations.get(o1)));
          }
        });
        boolean needToRepath = false;
        int numDemolishersToAttack = demolishersRemaining;
        for (Unit attacker : attackers) {
          if (attacker.owner == PlayerId.Player1) {
            if (attacker.type == UnitType.Support) {
              double shieldAmount = attacker.unitInformation.shieldPerUnit.orElse(attacker.upgraded ? 5 : 3) + (attacker.upgraded ? (attacker.unitInformation.shieldBonusPerY.orElse(0.3) * attackersLocations.get(attacker).y) : 0);
              for (int d = 0; d < demolishersRemaining; d++) {
                demolisherHealths.set(d, demolisherHealths.get(d) + shieldAmount);
              }
            }
          } else {
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
            spTaken += (float) (damageDone / attacker.unitInformation.startHealth.orElse(2) * attacker.unitInformation.cost1.orElse(2) * 0.97f);

            if (demolisherHealths.size() > 0) {
              double initialDemoHealth = demolisherHealths.get(demolisherHealths.size() - 1);
              double towerDamage = attacker.unitInformation.attackDamageWalker.orElse(attacker.upgraded ? 20 : 6);
              demolisherHealths.set(demolisherHealths.size() - 1, Math.max(0, initialDemoHealth - towerDamage));
              double afterDemoHealth = demolisherHealths.get(demolisherHealths.size() - 1);
              double damageToDemos = initialDemoHealth - afterDemoHealth;
              if (afterDemoHealth == 0) {
                demolishersRemaining--;
                demolisherHealths.remove(demolisherHealths.size() - 1);
              }
              expectedDamage += damageToDemos;
            }
          }
        }
        if (needToRepath) {
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
      if (spTaken >= minDamage) { // ignore result if it doesn't help
        damages.put(start, new ExpectedDefense(move, path.toArray(new Coords[0]), spTaken, expectedDamage));
      }
    }


    GameIO.debug().println("================DAMAGES========");
    GameIO.debug().printf("%s: %d options\n", damages.size() > 0 ? "CAN DEMO" : "none", damages.size());
    GameIO.debug().println("================END DEMO ANALYSIS========");


    Coords bestAttack = null;
    ExpectedDefense bestED = new ExpectedDefense(move, null, minDamage, 0);

    for (Map.Entry<Coords, ExpectedDefense> entry : damages.entrySet()) {
      if (entry.getValue().structureHealth > bestED.structureHealth) {
        bestAttack = entry.getKey();
        bestED = entry.getValue();
      }
    }
    if (bestAttack == null) {
      GameIO.debug().println("Not doing enough damage!");
      damages.forEach((key, value) -> GameIO.debug().printf("x:%d,y:%d. damage:%.2f, need:%.2f. path_len: %d: ends:%s\n\t%s\n",
          key.x, key.y,
          value.structureHealth, minDamage,
          value.path.length, value.path[value.path.length-1].toString(),
          Arrays.toString(Arrays.stream(value.path).limit(15).map(Coords::toString).toArray(String[]::new))));
      return null;
    }

    Coords hookLocation = bestAttack;

    GameIO.debug().printf("Will run demo: %s. expected damage: %.2f\n", hookLocation.toString(), bestED.structureHealth);

    return new DemolisherRun(move, hookLocation, bestED, (int) (availableMP / demolisherInfo.cost2.orElse(3)));
  }


}
