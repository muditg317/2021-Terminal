package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Utility {

  static final UnitType TURRET = UnitType.Turret;
  static final UnitType WALL = UnitType.Wall;
  static final UnitType SUPPORT = UnitType.Support;
  static final UnitType UPGRADE = UnitType.Upgrade;
  static final UnitType REMOVE = UnitType.Remove;
  static final UnitType SCOUT = UnitType.Scout;
  static final UnitType DEMOLISHER = UnitType.Demolisher;
  static final UnitType INTERCEPTOR = UnitType.Interceptor;

  static final List<Coords> friendlyEdges = new ArrayList<>();
  static {
    friendlyEdges.addAll(Arrays.asList(MapBounds.EDGE_LISTS[MapBounds.EDGE_BOTTOM_LEFT]));
    friendlyEdges.addAll(Arrays.asList(MapBounds.EDGE_LISTS[MapBounds.EDGE_BOTTOM_RIGHT]));
  }

  /**
   * Updates the instance variable recording the number of cores that the enemy has invested into factories
   * @param move
   * @return the number of cores that were ever invested
   */
  static void trackEnemyCoresInvestedInFactories(MyAlgo algoState, GameState move) {
    List<FrameData.Events.SpawnEvent> spawnEvents = move.data.events.spawn;
    for(FrameData.Events.SpawnEvent spawnEvent : spawnEvents) {
      if (spawnEvent.owner == PlayerId.Player2) {
        if (spawnEvent.spawningUnitType == SUPPORT) {
          algoState.enemySupportTowerCoresInvestment += 9;
        }
        else if (spawnEvent.spawningUnitType == UnitType.Upgrade) {
          Coords coords = spawnEvent.spawnLocation;
          if (move.getWallAt(coords).type == SUPPORT) {
            algoState.enemySupportTowerCoresInvestment += 9;
          }
        }
      }
    }
  }


  /**
   * Goes through the list of locations, gets the path taken from them,
   * and loosely calculates how much damage will be taken by traveling that path assuming speed of 1.
   * @param move
   * @param locations
   * @return
   */
  static Map<Coords, Float> damagePerSpawnLocation(GameState move, List<Coords> locations) {
    if (locations == null) {
      locations = friendlyEdges;
    }

    Map<Coords, Float> damages = new HashMap<>();

    for (Coords location : locations) {
      List<Coords> path;
      try {
        path = move.pathfind(location, MapBounds.getEdgeFromStart(location));
      } catch (IllegalPathStartException e) {
        continue;
      }
      Coords endPoint = path.get(path.size() -1);
      if (MapBounds.IS_ON_EDGE[MapBounds.EDGE_TOP_LEFT][endPoint.x][endPoint.y] || MapBounds.IS_ON_EDGE[MapBounds.EDGE_TOP_LEFT][endPoint.x][endPoint.y]) {
        float totalDamage = 0;
        for (Coords dmgLoc : path) {
          List<Unit> attackers = move.getAttackers(dmgLoc);
          for (Unit unit : attackers) {
            totalDamage += unit.unitInformation.attackDamageWalker.orElse(0);
          }
        }
//        GameIO.debug().println("Got dmg:" + totalDamage + " for " + location);
        damages.put(location, totalDamage);
      }
    }

    return damages;
  }


  /**
   * Goes through the list of locations, gets the path taken from them,
   * and loosely calculates how much damage will be taken by traveling that path assuming speed of 1.
   * @param move
   * @param locations
   * @return the location with the least damaging path
   */
  static Coords leastDamageSpawnLocation(GameState move, List<Coords> locations) {
    List<Float> damages = new ArrayList<>();

    for (Coords location : locations) {
      List<Coords> path = move.pathfind(location, MapBounds.getEdgeFromStart(location));
      float totalDamage = 0;
      for (Coords dmgLoc : path) {
        List<Unit> attackers = move.getAttackers(dmgLoc);
        for (Unit unit : attackers) {
          totalDamage += unit.unitInformation.attackDamageWalker.orElse(0);
        }
      }
      GameIO.debug().println("Got dmg:" + totalDamage + " for " + location);
      damages.add(totalDamage);
    }

    int minIndex = 0;
    float minDamage = 9999999;
    for (int i = 0; i < damages.size(); i++) {
      if (damages.get(i) <= minDamage) {
        minDamage = damages.get(i);
        minIndex = i;
      }
    }
    return locations.get(minIndex);
  }

  /**
   * Counts the number of a units found with optional parameters to specify what locations and unit types to count.
   * @param move GameState
   * @param xLocations Can be null, list of x locations to check for units
   * @param yLocations Can be null, list of y locations to check for units
   * @param units Can be null, list of units to look for, null will check all
   * @return count of the number of units seen at the specified locations
   */
  static int detectEnemyUnits(GameState move, List<Integer> xLocations, List<Integer> yLocations, List<UnitType> units) {
    if (xLocations == null) {
      xLocations = new ArrayList<Integer>();
      for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
        xLocations.add(x);
      }
    }
    if (yLocations == null) {
      yLocations = new ArrayList<Integer>();
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        yLocations.add(y);
      }
    }

    if (units == null) {
      units = new ArrayList<>();
      for (Config.UnitInformation unit : move.config.unitInformation) {
        if (unit.startHealth.isPresent()) {
          units.add(move.unitTypeFromShorthand(unit.shorthand.get()));
        }
      }
    }

    int count = 0;
    for (int x : xLocations) {
      for (int y : yLocations) {
        Coords loc = new Coords(x,y);
        if (MapBounds.inArena(loc)) {
          for (Unit u : move.allUnits[x][y]) {
            if (units.contains(u.type)) {
              count++;
            }
          }
        }
      }
    }
    return count;
  }


  /**
   * Build defenses reactively based on where we got scored on
   */
  static void buildReactiveDefenses(MyAlgo algoState, GameState move) {
    for (int i = algoState.scoredOnLocations.size() - 1; i > 0; i--) {
      ArrayList<Coords> locations = algoState.scoredOnLocations.get(i);
      for (Coords loc : locations) {
        // Build 1 space above the breach location so that it doesn't block our spawn locations
        move.attemptSpawn(new Coords(loc.x, loc.y + 1), Utility.TURRET);
      }
    }
  }

  static void empLineStrategy(GameState move) {
    /*
    First lets fine the cheapest type of structure  stationary unit. We could hardcode this to FILTER probably
    depending on the config but lets demonstrate how to use java-algo features.
     */
    Config.UnitInformation cheapestUnit = null;
    for (Config.UnitInformation uinfo : move.config.unitInformation) {
      if (uinfo.unitCategory.isPresent() && move.isStructure(uinfo.unitCategory.getAsInt())) {
        float[] costUnit = uinfo.cost();
        if((cheapestUnit == null || costUnit[0] + costUnit[1] <= cheapestUnit.cost()[0] + cheapestUnit.cost()[1])) {
          cheapestUnit = uinfo;
        }
      }
    }
    if (cheapestUnit == null) {
      GameIO.debug().println("There are no structure s?");
    }

    for (int x = 27; x>=5; x--) {
      move.attemptSpawn(new Coords(x, 11), move.unitTypeFromShorthand(cheapestUnit.shorthand.get()));
    }

    for (int i = 0; i<22; i++) {
      move.attemptSpawn(new Coords(24, 10), DEMOLISHER);
    }
  }

  public static class Pair<T,K> {
    public T key;
    public K value;

    public Pair(T key, K value) {
      this.key = key;
      this.value = value;
    }
  }

}
