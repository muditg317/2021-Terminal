package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
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
    SpawnUtility.placeWalls(move, walls);
    SpawnUtility.placeSupports(move, supportTowers);
    SpawnUtility.placeTurrets(move, turrets);
    SpawnUtility.applyUpgrades(move, supportTowers);
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

    if (numDemolishers == 0) {
      return null;
    }


    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);

    double wallCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);

    for (int x = minX; x <= maxX; x = x == maxX ? maxX+1 : Math.min(x+xIncrement,maxX)) {
      for (int y = minY; y <= maxY; y = y == maxY ? maxY + 1 : Math.min(y + yIncrement, maxY)) {
        if (!MapBounds.ARENA[x][y]) {
          continue;
        }
        if (y == 11) {
          continue;
        }
        int wallsAvailable = (int) (availableSP / wallCost);
        if (wallsAvailable < 0) {
          continue;
        }
        // Build list of walls we need to place for the hook
        List<Coords> neededWalls = new ArrayList<>();

        // Walls of the V
        for (int wallY = 2; wallY <= 13 && wallsAvailable >= 0; ++wallY) {
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
          continue;
        }

        // Walls in the Corners
        for (int wallX = 0; wallX <= 1 && wallsAvailable >= 0; ++wallX) {
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
          continue;
        }
        int placedWalls = neededWalls.size();
        for (int side = 0; side <= 1; ++side) {//0: target RIGHT, 1: target LEFT

          wallsAvailable += neededWalls.size() - placedWalls;
          neededWalls.subList(placedWalls, neededWalls.size()).clear();

          int topWallStartX = side == 0 ? (12+y) : (15-y);
          int wallBuildDir = side * 2 - 1;

          // Walls of the actual hook bar
          for (int wallX = topWallStartX; (side == 0 ? wallX > x : wallX < x) && wallsAvailable >= 0; wallX += wallBuildDir) {
            Coords topWall = new Coords(wallX, y);
            if (move.getWallAt(topWall) == null) {
              neededWalls.add(topWall);
              --wallsAvailable;
            }
          }
          if (wallsAvailable < 0) {
            continue;
          }

          Coords start = new Coords(x, y);
          List<Coords> path;
          try {
            path = move.pathfind(start, side);
          } catch (IllegalPathStartException e) {
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
                }
              }
            }
          }

          damages.put(new Utility.Pair<>(start, side), new Utility.Pair<>(neededWalls, new ExpectedDefense(move, (Coords[]) path.toArray(), spTaken, expectedDamage)));
        }
      }
    }


    Utility.Pair<Coords, Integer> bestAttack = null;
    ExpectedDefense bestED = new ExpectedDefense(move, null, minDamage, 0);

    for (Map.Entry<Utility.Pair<Coords, Integer>, Utility.Pair<List<Coords>, ExpectedDefense>> entry : damages.entrySet()) {
      if (entry.getValue().value.structureHealth > bestED.structureHealth) {
        bestAttack = entry.getKey();
        bestED = entry.getValue().value;
      }
    }
    if (bestAttack == null) {
      return null;
    }

    Coords hookLocation = bestAttack.key;
    int side = bestAttack.value;

    // TODO: compute support locations
    List<Coords> neededWalls = damages.get(bestAttack).key;
    double remainingSP = availableSP - neededWalls.size() * wallCost;
    double supportCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(4);
    double upgradeCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).upgrade.orElse(new Config.UnitInformation()).cost1.orElse(4);

    int minWallsForProtection = (int) Math.ceil(remainingSP * wallCost / (wallCost+supportCost+upgradeCost)) / 2;
    minWallsForProtection = Math.max(minWallsForProtection, 3);
    List<Coords> supportTowers = new ArrayList<>((int) (availableSP / supportCost));
    int wallY = hookLocation.y - 1;
    int wallX = side == 0 ? (15 - wallY) : (wallY - 11);
    int wallBuildDir = side * 2 - 1; //-1 for target right, 1 for target left
    int wallXLimit = hookLocation.x - 2 * wallBuildDir;
    if ((int) Math.signum(wallXLimit - wallX) != wallBuildDir) {
      int wallsPlaced = 0;
      while (remainingSP >= supportCost + wallCost && wallsPlaced < minWallsForProtection) {
        neededWalls.add(new Coords(wallX, wallY));
        remainingSP -= wallCost;
        wallX += wallBuildDir;
        wallsPlaced++;
      }
      if (wallsPlaced == 0) {
        wallXLimit = wallX + wallBuildDir;
      } else {
        wallXLimit = wallX + wallsPlaced;
      }
    }
    while (remainingSP >= supportCost + upgradeCost) {
      Coords supportLoc = new Coords(wallX, wallY);
      if (move.getWallAt(supportLoc) == null) {
        supportTowers.add(supportLoc);
        remainingSP -= supportCost;
        if (remainingSP >= upgradeCost) {
          remainingSP -= upgradeCost;
        }
      }
      if (wallX == wallXLimit) {
        wallY--;
        wallX = side == 0 ? (15 - wallY) : (wallY - 11);
      } else {
        wallX += wallBuildDir;
      }
    }


    Coords[] demolisherLocations = new Coords[(int) (availableMP / demolisherInfo.cost2.orElse(3))];
    Coords demoLoc = new Coords(13 + bestAttack.value, 0);
    Arrays.fill(demolisherLocations, demoLoc);

    return new HookAttack(move, neededWalls.toArray(new Coords[0]),supportTowers.toArray(new Coords[0]), new Coords[]{}, new Coords[]{},new Coords[]{}, demolisherLocations, bestED);
  }


}
