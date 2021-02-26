package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.map.CanSpawn;
import com.c1games.terminal.algo.map.GameState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Boom {
  public static boolean awaitingBoom = false;
  public static int turnsUntilBoom = -1;

  static void evaluate(GameState move, int expectedMpSpentPerTurn) {
    GameIO.debug().println("BOOM DECISION: ===========");
    float mp = move.data.p1Stats.bits;
    int MAX_EXTRAPOLATION_TURNS = Math.min(3, move.data.turnInfo.turnNumber / 5);
    if (Boom.turnsUntilBoom != 0) { //we WILL boom this turn. no need to check.
      boolean shouldStillBoom = false;
      int turns = 1;
      if (Boom.turnsUntilBoom == -99) {
        Boom.turnsUntilBoom = -1;
        turns = 0;
      }
      for (; turns <= MAX_EXTRAPOLATION_TURNS; turns++) {
        AttackBreakdown futureAttack = StrategyUtility.futureAttackThreshold(move, turns);
        float futureAttackThreshold = futureAttack.units.cost;
        float futureMP = StrategyUtility.extrapolateFutureMP(move, turns, expectedMpSpentPerTurn);
        GameIO.debug().println("futureAttackThreshold: " + futureAttackThreshold);
        GameIO.debug().println("futureMP: " + futureMP);
        if (futureMP >= futureAttackThreshold) {
          GameIO.debug().println("about to boom..." + mp + " / " + futureAttackThreshold + " reached -- expecting: " + futureMP +" in " + turns +" turns ||| started turn with: " + mp);
          Boom.awaitingBoom = true;
          Boom.turnsUntilBoom = turns;
          shouldStillBoom = true;
          break;
        }
      }

      if (!shouldStillBoom) {
        Boom.awaitingBoom = false;
        Boom.turnsUntilBoom = -1;
      }
    } //end boom decision
    GameIO.debug().println("awaitingBoom:" + Boom.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + Boom.turnsUntilBoom);
  }

  /**
   * Does the Boom and sets awaitingBoom to false and turnsUntilBoom to -99
   * @return True if executed, false if not
   */
  static boolean execute(GameState move) {
    AttackBreakdown attackData = StrategyUtility.attackThreshold(move);
    String boomSide = attackData.location;
    UnitCounts attackUnits = attackData.units;
    int attackPoints = attackUnits.cost;
    int numInters = attackUnits.numInterceptors;
    GameIO.debug().println("Going to boom right now");
    GameIO.debug().println("Cores:" + move.data.p1Stats.cores);
    Boom.clearBoomPath(move, boomSide);
    Boom.placeBoomLid(move, boomSide);

    SpawnUtility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 23 : 4, 9)}, numInters);
    SpawnUtility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 6 : 21, 7)}, (int) move.data.p1Stats.bits);

    SpawnUtility.removeBuilding(move, new Coords(boomSide.equals("RIGHT") ? 6 : 21, 8));
    SpawnUtility.removeBuilding(move, new Coords(4, 11));
    Boom.awaitingBoom = false;
    Boom.turnsUntilBoom = -99;
    return true;
  }
  /**
   * returns true if the lid is successfully placed
   * @param boomSide
   * @return
   */
  static boolean placeBoomLid(GameState move, String boomSide) {
    List<Coords> toPlace = new ArrayList<>();
    for (int i = 0; i < Locations.boomLid_right.length; i++) {
      Coords closeLocation = Locations.boomLid_right[i];
      int x = boomSide.equals("RIGHT") ? closeLocation.x : (27 - closeLocation.x);
      Coords toClose = new Coords(x, closeLocation.y);
      if (toClose.y < 8) {
        if (move.canSpawn(toClose, Utility.WALL, 1) == CanSpawn.Yes) {
          toPlace.add(toClose);
        }
      } else if (toClose.y == 13) {
        if (move.canSpawn(toClose, Utility.WALL, 1) == CanSpawn.Yes) {
          toPlace.add(toClose);
        }
      } else {
        SpawnUtility.placeWalls(move, new Coords[]{toClose});
      }
    }
    final int[] numFactories = {0}; //TODO: I'm a bit confused about this legacy code... going to just make it place walls
    //final int[] numFactories = {(int) ((move.data.p1Stats.bits - toPlace.size()) / 9)};
    toPlace.stream().sorted((o1, o2) -> o1.y - o2.y).forEach(location -> {
      if (numFactories[0] > 0 && location.y < 11) {
        SpawnUtility.placeSupports(move, new Coords[]{location});
        numFactories[0]--;
      } else {
        SpawnUtility.placeWalls(move, new Coords[]{location});
      }
    });

    //now put down supports
    SpawnUtility.placeSupports(move, Locations.safeSupportLocations);
    SpawnUtility.removeBuildings(move, Locations.safeSupportLocations);
    return true;
  }

  /**
   * clears an attack path for a boom attack
   * @param side the side which to hit
   * @return whether the path was already clear
   */
  static boolean clearBoomPath(GameState move, String side) {
    boolean alreadyReady = true;
    for (int i = 0; i < Locations.boomPath_right.length; i++) {
      Coords openLocation = Locations.boomPath_right[i];
      int x = side.equals("RIGHT") ? openLocation.x : (27 - openLocation.x);
      Coords toOpen = new Coords(x, openLocation.y);
      alreadyReady = SpawnUtility.removeBuilding(move, toOpen) == 0 && alreadyReady;
    }
    return alreadyReady;
  }

  static void debugPrint() {
    GameIO.debug().println("awaitingBoom:" + Boom.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + Boom.turnsUntilBoom);
  }
}
