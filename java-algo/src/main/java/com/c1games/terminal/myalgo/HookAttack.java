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

public class HookAttack {
  Coords[] walls;
  Coords[] supportTowers;
  Coords[] turrets;

  UnitCounts units;

  Coords[] scouts;
  Coords[] interceptors;
  Coords[] demolishers;

  // uses structureHealth as expected structure points taken
  ExpectedDefense expectedDefense;

  public HookAttack(GameState move, Coords[] walls, Coords[] supportTowers, Coords[] turrets, Coords[] scouts, Coords[] interceptors, Coords[] demolishers, ExpectedDefense expectedDefense) {
    this.walls = walls == null ? new Coords[0] : walls;
    this.supportTowers = supportTowers == null ? new Coords[0] : supportTowers;
    this.turrets = turrets == null ? new Coords[0] : turrets;
    this.scouts = scouts == null ? new Coords[0] : scouts;
    this.interceptors = interceptors == null ? new Coords[0] : interceptors;
    this.demolishers = demolishers == null ? new Coords[0] : demolishers;
    this.expectedDefense = expectedDefense;
    this.units = new UnitCounts(move, this.scouts.length, this.interceptors.length, this.demolishers.length);
  }

  /**
   * Does the hook attack ;o
   * @param move
   */
  public void execute(GameState move) {
    SpawnUtility.placeUpgradedSupports(move, Arrays.stream(supportTowers).limit(supportTowers.length - 1).toArray(Coords[]::new));
    SpawnUtility.placeWalls(move, walls);
    SpawnUtility.placeUpgradedSupports(move, new Coords[]{supportTowers[supportTowers.length-1]});
    SpawnUtility.placeTurrets(move, turrets);
    if (move.data.p1Stats.cores > 0) {
      List<Coords> wallList = Arrays.asList(walls);
      Collections.reverse(wallList);
      SpawnUtility.applyUpgrades(move, wallList.toArray(new Coords[0]));
    }
    SpawnUtility.spawnDemolishers(move, demolishers, 1);
    SpawnUtility.spawnScouts(move, scouts, 1);
    SpawnUtility.spawnInterceptors(move, interceptors, 1);
    SpawnUtility.removeBuildings(move, walls);
    SpawnUtility.removeBuildings(move, supportTowers);
    SpawnUtility.removeBuildings(move, turrets);
  }

  /**
   * finds the optimal hook attack
   * @param move
   * @param availableSP
   * @param availableMP
   * @return
   */
  static HookAttack evaluate(GameState move, double availableSP, double availableMP, int minX, int maxX, int minY, int maxY, float minDamage) {
    final int xIncrement = 1;
    final int yIncrement = 1;
    Map<Utility.Pair<Coords, Integer>, Utility.Pair<List<Coords>, ExpectedDefense>> damages = new HashMap<>();

    Config.UnitInformation demolisherInfo = move.config.unitInformation.get(UnitType.Demolisher.ordinal());
    int numDemolishers = (int) (availableMP / demolisherInfo.cost2.orElse(3));

    GameIO.debug().println("================HOOK ANALYSIS========");

    if (numDemolishers == 0) {
      GameIO.debug().println("no demos");
      return null;
    }


    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);

    double wallCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);

    for (int x = minX; x <= maxX; x = x == maxX ? maxX+1 : Math.min(x+xIncrement,maxX)) {
      for (int y = minY; y <= maxY; y = y == maxY ? maxY + 1 : Math.min(y + yIncrement, maxY)) {
        if (!MapBounds.ARENA[x][y]) {
          continue;
        }
        int wallsAvailable = (int) (availableSP / wallCost);
//        if (wallsAvailable < 0) {
//          GameIO.debug().printf("x:%d,y:%d. need more walls! sp:%d.\n",x,y,availableSP);
//          continue;
//        }
        // Build list of walls we need to place for the hook
        List<Coords> neededWalls = new ArrayList<>();

        // Walls of the V
        for (int wallY = 2; wallY <= 13 && wallsAvailable >= 0; wallY++) {
          if (wallY == y-1) {
            wallY++;
          }
          Coords leftWall = new Coords(15-wallY,wallY);
          if (move.getWallAt(leftWall) == null) {
            neededWalls.add(leftWall);
            --wallsAvailable;
          }
          Coords rightWall = new Coords(12+wallY, wallY);
          if (move.getWallAt(rightWall) == null) {
            neededWalls.add(rightWall);
            --wallsAvailable;
          }
        }
        if (wallsAvailable < 0) {
//          GameIO.debug().printf("x:%d,y:%d. need more walls! sp:%.2f, walls:%d\n", x, y, availableSP, neededWalls.size());
//          GameIO.debug().println(neededWalls);
          continue;
        }

        // Walls in the Corners
        for (int wallX = 0; wallX <= 1 && wallsAvailable >= 0; wallX++) {
          Coords leftWall = new Coords(wallX,13);
          if (move.getWallAt(leftWall) == null) {
            neededWalls.add(leftWall);
            --wallsAvailable;
          }
          Coords rightWall = new Coords(27-wallX, 13);
          if (move.getWallAt(rightWall) == null) {
            neededWalls.add(rightWall);
            --wallsAvailable;
          }
        }
        if (wallsAvailable < 0) {
//          GameIO.debug().printf("x:%d,y:%d. need more walls! sp:%.2f, walls:%d\n", x, y, availableSP, neededWalls.size());
//          GameIO.debug().println(neededWalls);
          continue;
        }
        int placedWalls = neededWalls.size();

        // Test hooking right or left
        for (int side = 0; side <= 1; side++) {//0: target RIGHT, 1: target LEFT
          if (y == 11 && side == 1) {
            continue;
          }
          wallsAvailable += neededWalls.size() - placedWalls;
//          neededWalls = neededWalls.subList(0, placedWalls);
          neededWalls.subList(placedWalls, neededWalls.size()).clear(); //may be bugged??

          Coords closeGapWall = new Coords(side == 0 ? (16-y) : (y+11), y-1);
          if (move.getWallAt(closeGapWall) == null) {
            neededWalls.add(closeGapWall);
            --wallsAvailable;
          }
          if (wallsAvailable < 0) {
//            GameIO.debug().printf("x:%d,y:%d, %s. need more walls! sp:%.2f, walls:%d\n", x, y, side == 0 ? "R" : "L", availableSP, neededWalls.size());
//            GameIO.debug().println(neededWalls);
            continue;
          }

          int topWallStartX = side == 0 ? (12+y) : (15-y);
          int wallBuildDir = side * 2-1;

          // Walls of the actual hook bar
          for (int wallX = topWallStartX; (side == 0 ? wallX > x : wallX < x) && wallsAvailable >= 0; wallX += wallBuildDir) {
            Coords topWall = new Coords(wallX, y);
            if (move.getWallAt(topWall) == null) {
              neededWalls.add(topWall);
              --wallsAvailable;
            }
          }

          if (wallsAvailable < 0) {
//            GameIO.debug().printf("x:%d,y:%d, %s. need more walls! sp:%.2f, walls:%d\n", x, y, side == 0 ? "R" : "L", availableSP, neededWalls.size());
//            GameIO.debug().println(neededWalls);
            continue;
          }

          Coords start = new Coords(x, y);
          List<Coords> path;
          try {
            path = move.pathfind(start, side);
          } catch (IllegalPathStartException e) {
//            GameIO.debug().printf("x:%d,y:%d. invalid hook exit\n", x, y);
            continue;
          }

          // add the path points along the horizontal travel of the hook path since path planner skips to end of hook
          List<Coords> initialPath = new ArrayList<>();
          for (int pathX = topWallStartX + wallBuildDir; side == 0 ? pathX >= x : pathX <= x; pathX += wallBuildDir) {
            initialPath.add(new Coords(pathX, y-1));
          }
          path.addAll(0, initialPath);

          double remainingSP = availableSP - neededWalls.size() * wallCost;
          double supportAmount = remainingSP > move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(4) ? remainingSP * 3 / 4 : 0;
          supportAmount += move.data.p1Units.support.size() * move.config.unitInformation.get(UnitType.Support.ordinal()).shieldPerUnit.orElse(3);
          double demolisherHealth = demolisherInfo.startHealth.orElse(5) + supportAmount;

          float spTaken = 0;
          float expectedDamage = 0;
          for (Coords pathPoint : path) {
            List<Unit> attackers = move.getAttackers(pathPoint);
            float damageDoneByTowers = 0;
            for (Unit attacker : attackers) {
              if (attacker.owner == PlayerId.Player2) {
                float towerDamage = (float) attacker.unitInformation.attackDamageWalker.orElse(6);
                expectedDamage += towerDamage;
                if (numDemolishers > 0) {
                  float damageDone = (float) Math.min(numDemolishers * demolisherDamage, attacker.health);
                  spTaken += (float) (damageDone / attacker.unitInformation.startHealth.orElse(2) * attacker.unitInformation.cost1.orElse(2) * 0.97f);
                  damageDoneByTowers += towerDamage;
                  if (damageDoneByTowers > demolisherHealth) {
                    numDemolishers--;
                    damageDoneByTowers = 0;
                  }
                  if (MapBounds.IS_ON_EDGE[side][pathPoint.x][pathPoint.y]) {
                    spTaken += numDemolishers * 1.5;
                  }
                }
              }
            }
          }

          damages.put(new Utility.Pair<>(start, side), new Utility.Pair<>(new ArrayList<>(neededWalls), new ExpectedDefense(move, path.toArray(new Coords[0]), spTaken, expectedDamage)));
        }
      }
    }

    GameIO.debug().println("================DAMAGES========");
    GameIO.debug().printf("%s: %d options\n", damages.size() > 0 ? "CAN HOOK" : "none", damages.size());
    GameIO.debug().println("================END HOOK ANALYSIS========");


    Utility.Pair<Coords, Integer> bestAttack = null;
    ExpectedDefense bestED = new ExpectedDefense(move, null, minDamage, 0);

    for (Map.Entry<Utility.Pair<Coords, Integer>, Utility.Pair<List<Coords>, ExpectedDefense>> entry : damages.entrySet()) {
      if (entry.getValue().value.structureHealth > bestED.structureHealth) {
        bestAttack = entry.getKey();
        bestED = entry.getValue().value;
      }
    }
    if (bestAttack == null) {
      GameIO.debug().println("Not doing enough damage!");
      damages.forEach((key, value) -> GameIO.debug().printf("x:%d,y:%d, %s. damage:%.2f, need:%.2f. path_len: %d: ends:%s\n\t%s\n",
          key.key.x, key.key.y, key.value == 0 ? "R" : "L",
          value.value.structureHealth, minDamage,
          value.value.path.length, value.value.path[value.value.path.length-1].toString(),
          Arrays.toString(Arrays.stream(value.value.path).limit(15).map(Coords::toString).toArray(String[]::new))));
      return null;
    }

    Coords hookLocation = bestAttack.key;
    int side = bestAttack.value;

    GameIO.debug().printf("Will hook: %s,s:%s. expected damage: %.2f\n", hookLocation.toString(), side == 0 ? "R" : "L", bestED.structureHealth);


    // TODO: compute support locations
    List<Coords> neededWalls = damages.get(bestAttack).key;
    double remainingSP = availableSP - neededWalls.size() * wallCost;
    double supportCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(4);
    double upgradeCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).upgrade.orElse(new Config.UnitInformation()).cost1.orElse(4);

    GameIO.debug().printf("Remaining SP for support: %.2f\n", remainingSP);

    int minWallsForProtection = (int) Math.ceil(remainingSP * wallCost / (wallCost+supportCost+upgradeCost)) / 2;
    minWallsForProtection = Math.min(Math.max(minWallsForProtection, 4), 6);
    List<Coords> supportTowers = new ArrayList<>((int) (availableSP / supportCost));
    int wallY = hookLocation.y - 2;
    int wallX = side == 0 ? (wallY + 12) : (15 - wallY);
    int wallBuildDir = side * 2 - 1; //-1 for target right, 1 for target left
    int wallXLimit = hookLocation.x - 2 * wallBuildDir;
    GameIO.debug().printf("wallX:%d,wallY:%d,wallBuildDir:%d,wallXLimitL%d\n",wallX,wallY,wallBuildDir,wallXLimit);
    while (remainingSP >= supportCost) {
      if ((int) Math.signum(wallXLimit - wallX) != wallBuildDir) {
        int wallsPlaced = 0;
        GameIO.debug().print("Not enough space for support!\nextra walls:");
        while (remainingSP >= supportCost + wallCost && wallsPlaced < minWallsForProtection) {
          neededWalls.add(new Coords(wallX, wallY));
          GameIO.debug().printf("<%d, %d>,", wallX,wallY);
          remainingSP -= wallCost;
          wallX += wallBuildDir;
          wallsPlaced++;
        }
        if (wallsPlaced == 0) {
          wallXLimit = wallX + wallBuildDir;
        } else {
          wallY--;
          wallX = side == 0 ? (wallY + 12) : (15 - wallY);
          wallXLimit = wallX + (wallsPlaced-2) * wallBuildDir;
        }
      }
      GameIO.debug().println("\nReady for supports");
      GameIO.debug().printf("wallX:%d,wallY:%d,wallBuildDir:%d,wallXLimitL%d\nSupports:",wallX,wallY,wallBuildDir,wallXLimit);
      while (remainingSP >= supportCost + upgradeCost) {
        Coords supportLoc = new Coords(wallX, wallY);
//      if (wallX<0||wallX>=28||wallY<0||wallY>28) {
//        GameIO.debug().printf("BAD COORDINATE! %s",supportLoc);
//      } else
        if (side == 0 ? wallX >= wallXLimit : wallX <= wallXLimit) {
          GameIO.debug().printf("%s,", supportLoc);
          if (move.getWallAt(supportLoc) == null) {
            supportTowers.add(supportLoc);
            remainingSP -= supportCost;
            if (remainingSP >= upgradeCost) {
              remainingSP -= upgradeCost;
            }
          }
        }
        if (side == 0 ? wallX <= wallXLimit : wallX >= wallXLimit) {
          wallY--;
          wallX = side == 0 ? (wallY + 12) : (15 - wallY);
          if (side == 0 ? wallX < wallXLimit : wallX > wallXLimit) {
            break;
          }
        } else {
          wallX += wallBuildDir;
        }
      }
      if (remainingSP < supportCost + upgradeCost) {
        break;
      }
      wallY += 2;
      minWallsForProtection = (int) Math.ceil(remainingSP * wallCost / (wallCost+supportCost+upgradeCost)) / 2;
      minWallsForProtection = Math.min(Math.max(minWallsForProtection, 2), 5);
    }


    GameIO.debug().println("\nSupports ready!");

    Coords[] demolisherLocations = new Coords[(int) (availableMP / demolisherInfo.cost2.orElse(3))];
    Arrays.fill(demolisherLocations, new Coords(13 + bestAttack.value, 0));

    double remainingMP = availableMP - ((int) (availableMP / demolisherInfo.cost2.orElse(3)));
    Coords[] interceptorLocations = new Coords[(int) Math.max((int) (remainingMP / move.config.unitInformation.get(UnitType.Interceptor.ordinal()).cost2.orElse(1)), (int) move.data.p2Stats.bits / demolisherInfo.cost2.orElse(3))];
    if (remainingMP > 0) {
      Arrays.fill(interceptorLocations, new Coords(side == 0 ? (hookLocation.y+13) : (13-hookLocation.y), hookLocation.y - 1));
    }

    return new HookAttack(move, neededWalls.toArray(new Coords[0]),supportTowers.toArray(new Coords[0]), new Coords[]{}, new Coords[]{}, interceptorLocations, demolisherLocations, bestED);
  }


}
