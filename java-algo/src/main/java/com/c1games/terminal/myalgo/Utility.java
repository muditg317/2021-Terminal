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
import com.c1games.terminal.algo.units.UnitTypeAtlas;
import com.google.gson.Gson;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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
   * Calculates the
   * @param unit
   * @param damageDone
   * @return
   */
  static double damageToSp(Unit unit, double damageDone) {
    return (float) (damageDone / unit.unitInformation.startHealth.orElse(2) * unit.unitInformation.cost1.orElse(2) * (unit.upgraded ? 0.97f : 0.90f));

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

  public static <T> void printObjs(T[] objs, Function<T, Boolean> cond, Function<T, String> str) {
    for (T obj : objs) {
      if (cond.apply(obj)) {
        GameIO.debug().println(str.apply(obj));
      }
    }
  }

  public static GameState duplicateState(GameState state) {
    Gson frameDataGSON = FrameData.gson(new UnitTypeAtlas(state.config));
    FrameData copiedData = frameDataGSON.fromJson(frameDataGSON.toJson(state.data), FrameData.class);

    GameState duplicate = new GameState(state.config, copiedData);
    for (int _x = 0; _x < MapBounds.BOARD_SIZE; _x++) {
      for (int _y = 0; _y < MapBounds.BOARD_SIZE; _y++) {
        duplicate.allUnits[_x][_y] = state.allUnits[_x][_y].stream().map(unit -> {
          Unit newUnit = new Unit(unit.type, unit.health, unit.id, unit.owner, duplicate.config);
          if (unit.upgraded) newUnit.upgrade();
          return newUnit;
        }).collect(Collectors.toList());
      }
    }
    return duplicate;
  }

  /**
   * Prints out the board to the console
   * @param board
   */
  public static void printGameBoard(List<Unit>[][] board) {
    GameIO.debug().println("==============BOARD STATE==============");

    for(int y = MapBounds.BOARD_SIZE-1; y >= 0; y--) {
      StringBuilder sb = new StringBuilder();
      for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
        sb.append(boardLocationToChar(board, x, y));
      }
      new PrintStream(GameIO.debug(), true, StandardCharsets.UTF_8).println(sb.toString());
    }
    GameIO.debug().println("============END BOARD STATE============");
  }

  private static String boardLocationToChar(List<Unit>[][] board, int x, int y) {
    if (!MapBounds.ARENA[x][y] || board[x][y] == null) {
      return "  ";
    }
    List<Unit> units = board[x][y];
    if (units.isEmpty()) {
      return " .";
    }
    Unit unit = units.get(0);
    switch (unit.type) {
      case Wall:
        return unit.upgraded ? " O" : " o";
      case Support:
        return unit.upgraded ? " S" : " s";
      case Turret:
        return unit.upgraded ? " X" : " x";
      default:
        return (units.size() < 10 ? " " : "") + units.size();
    }
  }

  public static boolean onEnemyCorner(int x, int y) {
    return y > 13 && (x > 13 ? (x >= (y+9)) : (x <= (18-y)));
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
