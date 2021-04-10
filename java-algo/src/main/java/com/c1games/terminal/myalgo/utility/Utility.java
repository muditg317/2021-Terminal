package com.c1games.terminal.myalgo.utility;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.algo.units.UnitTypeAtlas;
import com.google.gson.Gson;

import java.awt.*;
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

  public static final UnitType TURRET = UnitType.Turret;
  public static final UnitType WALL = UnitType.Wall;
  public static final UnitType SUPPORT = UnitType.Support;
  public static final UnitType UPGRADE = UnitType.Upgrade;
  public static final UnitType REMOVE = UnitType.Remove;
  public static final UnitType SCOUT = UnitType.Scout;
  public static final UnitType DEMOLISHER = UnitType.Demolisher;
  public static final UnitType INTERCEPTOR = UnitType.Interceptor;

  public static final List<Coords> friendlyEdges = new ArrayList<>();
  static {
    friendlyEdges.addAll(Arrays.asList(MapBounds.EDGE_LISTS[MapBounds.EDGE_BOTTOM_LEFT]));
    friendlyEdges.addAll(Arrays.asList(MapBounds.EDGE_LISTS[MapBounds.EDGE_BOTTOM_RIGHT]));
  }




  /**
   * Fills the other hook holes for this scout attack but leaves the one needed open
   * @param gameState
   * @return the amount spent
   */
  public static int fillOtherHookHoles(GameState gameState, Coords noFill) {
    int spent = 0;
    for (Coords loc : Locations.Essentials.mainWallHookHoles) {
      if (loc.equals(noFill)) {
        continue;
      }
      if (SpawnUtility.placeWall(gameState, loc)) {
        spent++;
      }
    }
    return spent;
  }

  /**
   * Goes through the list of locations, gets the path taken from them,
   * and loosely calculates how much damage will be taken by traveling that path assuming speed of 1.
   * @param move
   * @param locations
   * @return
   */
  public static Map<Coords, Float> damagePerSpawnLocation(GameState move, List<Coords> locations) {
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
  public static Coords leastDamageSpawnLocation(GameState move, List<Coords> locations) {
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
  public static int detectEnemyUnits(GameState move, List<Integer> xLocations, List<Integer> yLocations, List<UnitType> units) {
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
   * Calculates the SP of a certain unit based on its health and refund percentage
   * @return
   */
  public static double healthToSP(Config.UnitInformation unitInformation, double health) {
    return (float) (health / unitInformation.startHealth.orElseThrow() * unitInformation.cost1.orElseThrow() * unitInformation.refundPercentage.orElseThrow());
  }

  public static <T> void printObjs(T[] objs, Function<T, Boolean> cond, Function<T, String> str) {
    for (T obj : objs) {
      if (cond.apply(obj)) {
        GameIO.debug().println(str.apply(obj));
      }
    }
  }

  public static FrameData duplicateFrameData(FrameData data) {
//    Gson frameDataGSON = FrameData.gson(new UnitTypeAtlas(state.config));
//    FrameData copiedData = frameDataGSON.fromJson(frameDataGSON.toJson(data), FrameData.class);

    FrameData copiedData = new FrameData();
    copiedData.turnInfo = new FrameData.TurnInfo();
    copiedData.turnInfo.phase = data.turnInfo.phase;
    copiedData.turnInfo.turnNumber = data.turnInfo.turnNumber;
    copiedData.turnInfo.actionPhaseFrameNumber = data.turnInfo.actionPhaseFrameNumber;

    copiedData.p1Stats = new FrameData.PlayerStats();
    copiedData.p1Stats.integrity = data.p1Stats.integrity;
    copiedData.p1Stats.cores = data.p1Stats.cores;
    copiedData.p1Stats.bits = data.p1Stats.bits;
    copiedData.p1Stats.timeTakenLastTurnMillis = data.p1Stats.timeTakenLastTurnMillis;

    copiedData.p2Stats = new FrameData.PlayerStats();
    copiedData.p2Stats.integrity = data.p2Stats.integrity;
    copiedData.p2Stats.cores = data.p2Stats.cores;
    copiedData.p2Stats.bits = data.p2Stats.bits;
    copiedData.p2Stats.timeTakenLastTurnMillis = data.p2Stats.timeTakenLastTurnMillis;

    // TODO: duplicate the other data as well if desired

    return copiedData;
  }

  public static GameState duplicateState(GameState state) {

    FrameData copiedData = duplicateFrameData(state.data);

    GameState duplicate = new GameState(state.config, copiedData, false);
    for (int _x = 0; _x < MapBounds.BOARD_SIZE; _x++) {
      for (int _y = 0; _y < MapBounds.BOARD_SIZE; _y++) {
        List<Unit> unitList = new ArrayList<>(state.allUnits[_x][_y].size());
        List<Unit> units = state.allUnits[_x][_y];
        for (int i = 0, unitsSize = units.size(); i < unitsSize; i++) {
          unitList.add(new Unit(units.get(i)));
        }
        duplicate.allUnits[_x][_y] = unitList;
      }
    }
    duplicate.buildStack.addAll(state.buildStack);
    duplicate.deployStack.addAll(state.deployStack);

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
      return "   ";
    }
    List<Unit> units = board[x][y];
    if (units.isEmpty()) {
      return " . ";
    }
    Unit unit = units.get(0);
    switch (unit.type) {
      case Wall:
        return unit.upgraded ? " O " : " o ";
      case Support:
        return unit.upgraded ? " S " : " s ";
      case Turret:
        return unit.upgraded ? " X " : " x ";
      default:
        return String.format("%2d ", units.size());
    }
  }

  public static boolean onEnemyCorner(int x, int y) {
    return y > 13 && (x > 13 ? (x >= (y+9)) : (x <= (18-y)));
  }

  public static class Pair<T,K> {
    private T key;
    private K value;

    public Pair(T key, K value) {
      this.key = key;
      this.value = value;
    }

    public T getKey() {
      return key;
    }

    public K getValue() {
      return value;
    }
  }

}
