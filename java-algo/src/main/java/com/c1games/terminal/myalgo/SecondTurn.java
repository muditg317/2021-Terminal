package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SecondTurn {
  private static MyAlgo algoState;
  private static GameState move;

  /**
   * Enemy has max 11.75 MP (with 3 non upgraded factories)
   *
   * @param move
   */
  static void execute(MyAlgo algoState, GameState move) {
    SecondTurn.algoState = algoState;
    SecondTurn.move = move;
    float enemyMP = move.data.p2Stats.bits;

    //mobile strategy
    defensiveInters();

    //structure strategy
    greedyFactory();
  }

  /**
   * Upgrade factory (min 9 SP). Saves ATLEAST 1 core (currently 2)
   */
  static void greedyFactory() {
    for (FrameData.PlayerUnit factory : move.data.p1Units.support) {
      boolean isUpgraded = false;
      for (FrameData.PlayerUnit upgrade : move.data.p1Units.upgrade) {
        if (upgrade.x == factory.x && upgrade.y == factory.y) {
          isUpgraded = true;
          break;
        }
      }
      if (!isUpgraded && move.attemptUpgrade(new Coords(factory.x,factory.y)) != 1) {
        break;
      }
    }
  }

  /**
   * put defenses until 1 core is left (used to guarantee factory on next turn)
   */
  static void save1Core() {
    final Coords[] walls = {new Coords(9,9), new Coords(19,9)};
    final Coords[] turrets = {new Coords(9,8), new Coords(19,8)};
    Utility.placeTurrets(move, turrets);
    Utility.placeWalls(move, walls);
  }

  /**
   * Uses the minimum of 8 SP into static defenses
   * interceptor hit
   */
  static void fullDefensiveStructures() {
    final Coords[] walls = {new Coords(8,9), new Coords(9,9), new Coords(19,9), new Coords(20,9)};
    final Coords[] turrets = {new Coords(9,8), new Coords(19,8)};
    Utility.placeTurrets(move, turrets);
    Utility.placeWalls(move, walls);

    //let's just save it  -> commented out for now
    // fullDefensiveStructures_extraCores(move);
  }

  static void fullDefensiveStructures_extraCores() {
    //let's just save it  -> commented out for now
    // if we have extra cores (sp)
    final Coords[] extraWall = {new Coords(7,8)};
    if (move.data.p1Stats.cores > 0) {
      Utility.placeWalls(move, extraWall);
    }
    //if we have one core, just save it
    //at this point, max cores == 4,

    if (move.data.p1Stats.cores > 1) {
      //place the middle turret in the wall
      Coords turretCoord = Locations.mainTurretCoords[Locations.mainTurretCoords.length / 2];
      move.attemptSpawn(turretCoord, Utility.TURRET);
      //if we still have enough cores, put a wall infront
      if (move.data.p1Stats.cores > 0) {
        move.attemptSpawn(new Coords(turretCoord.x, turretCoord.y + 1), Utility.WALL);
      }
    }
  }

  /**
   * hailmaries all mobiles into a corner
   * interceptor hit
   */
  static void hailmaryIntersCorner() {
    int bits = (int) move.data.p1Stats.bits;
    Utility.spawnInterceptors(move, Locations.hailmaryInters, bits);
  }

  /**
   * hailmaries all mobiles into weak location
   * interceptor hit
   */
  static boolean hailmaryIntersSmart() {
    Map<Coords, Float> damagePerStartLocation = Utility.damagePerSpawnLocation(move, null);
    List<Coords> bestLocations = null;
    float minDamage = 99999999;
    for (Entry<Coords, Float> damageEntry : damagePerStartLocation.entrySet()) {
      if (bestLocations == null || damageEntry.getValue() < minDamage) {
        bestLocations = new ArrayList<>(Arrays.asList(damageEntry.getKey()));
        minDamage = damageEntry.getValue();
      } else if (damageEntry.getValue() == minDamage) {
        bestLocations.add(damageEntry.getKey());
      }
    }

    if (bestLocations == null) {
      return false;
    }
    final Coords[] hailmaryInters = {bestLocations.get(0)};
    int bits = (int) move.data.p1Stats.bits;
    Utility.spawnInterceptors(move, hailmaryInters, bits);
    return true;
  }

  /**
   * Puts all SP [8, 13] into static defenses and hailmaries all mobiles into corner
   * interceptor hit
   */
  static void defensiveInters() {
    Utility.spawnInterceptors(move, Locations.spacedInters7, 1);
  }

  /**
   * find optimal demolisher line and send demo if present
   * TODO: doesn't account for killing stuff causing path to change
   */
  static void potentiallySendDemolishers(int minDamage) {
    Map<Coords, Float> damages = new HashMap<>();

    for (Coords location : Utility.friendlyEdges) {
      List<Coords> path;
      try {
        path = move.pathfind(location, MapBounds.getEdgeFromStart(location));
      } catch (IllegalPathStartException e) {
        continue;
      }
      Coords endPoint = path.get(path.size() -1);
      float demolisherPower = 0;
      for (Coords pathPoint : path) {
        List<Unit> attackers = move.getAttackers(pathPoint);
        int additions = 0;
        Coords best = null;
        UnitType bestType = null;
        float bestPercentage = 0;
        Coords second = null;
        UnitType secondType = null;
        float secondPercentage = 0;
        for (FrameData.PlayerUnit unit : move.data.p2Units.support) {
          Coords unitLoc = new Coords(unit.x, unit.y);
          if (pathPoint.distance(unitLoc) < 4.5) {
            if (best == null || pathPoint.distance(unitLoc) <= pathPoint.distance(best)) {
              second = best;
              secondType = bestType;
              secondPercentage = bestPercentage;
              best = unitLoc;
              bestType = Utility.SUPPORT;
              bestPercentage = unit.stability / 30;
            } else if (second == null || pathPoint.distance(unitLoc) <= pathPoint.distance(second)) {
              second = unitLoc;
              secondType = Utility.SUPPORT;
              secondPercentage = unit.stability / 30;
            }
          }
        }
        for (FrameData.PlayerUnit unit : move.data.p2Units.turret) {
          Coords unitLoc = new Coords(unit.x, unit.y);
          if (pathPoint.distance(unitLoc) < 4.5) {
            if (best == null || pathPoint.distance(unitLoc) <= pathPoint.distance(best)) {
              second = best;
              secondType = bestType;
              secondPercentage = bestPercentage;
              best = unitLoc;
              bestType = Utility.TURRET;
              bestPercentage = unit.stability / 95;
            } else if (second == null || pathPoint.distance(unitLoc) <= pathPoint.distance(second)) {
              second = unitLoc;
              secondType = Utility.TURRET;
              secondPercentage = unit.stability / 95;
            }
          }
        }
        for (FrameData.PlayerUnit unit : move.data.p2Units.wall) {
          Coords unitLoc = new Coords(unit.x, unit.y);
          if (pathPoint.distance(unitLoc) < 4.5) {
            if (best == null || pathPoint.distance(unitLoc) <= pathPoint.distance(best)) {
              second = best;
              secondType = bestType;
              secondPercentage = bestPercentage;
              best = unitLoc;
              bestType = Utility.WALL;
              bestPercentage = unit.stability / 150;
            } else if (second == null || pathPoint.distance(unitLoc) <= pathPoint.distance(second)) {
              second = unitLoc;
              secondType = Utility.WALL;
              secondPercentage = unit.stability / 150;
            }
          }
        }
        if (best != null) {
          demolisherPower += bestType == Utility.SUPPORT ? 9 : bestType == Utility.TURRET ? 2 : 1;
        }
        if (second != null) {
          demolisherPower += (1 - bestPercentage) * (bestType == Utility.SUPPORT ? 9 : bestType == Utility.TURRET ? 2 : 1);
        }
        if (!attackers.isEmpty()) {
          break;
        }
      }
//      GameIO.debug().println("Got dmg:" + demolisherPower + " for " + location);
      damages.put(location, demolisherPower);
    }

    Coords bestCoord = null;
    float bestDemolisherDamage = minDamage;
    for (Entry<Coords, Float> entry : damages.entrySet()) {
      if (entry.getValue() > bestDemolisherDamage) {
        bestCoord = entry.getKey();
        bestDemolisherDamage = entry.getValue();
      }
    }

    if (bestCoord != null) {
      Utility.spawnDemolishers(move, new Coords[]{bestCoord}, (int) move.data.p1Stats.bits / 3);
    }
  }
}