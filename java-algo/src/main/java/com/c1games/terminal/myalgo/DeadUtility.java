package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeadUtility {

  /**
   * Calculates some rating of our defense..
   * Cuts the board in half and counts the total SP value of the structures on the left and right side and then
   * returns the value of the lowest side
   * @param move the game state
   * @return the defensive rating
   */
  private static float calculateDefenseRating(GameState move) {

    float leftRating = 0;
    float rightRating = 0;

    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (List<Unit> units : row) {
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player1) { //this is our structure
          float unitValue = 0;
          unitValue += unit.unitInformation.cost()[0] * unit.health / unit.unitInformation.startHealth.getAsDouble();
          if (x < 13) {
            leftRating += unitValue;
          } else {
            rightRating += unitValue;
          }
        }
      }
    }
    return 2 * Math.min(leftRating, rightRating);
  }

  /**
   * OLD ALGO
   * @param move
   * @return the estimated number of inters needed to defend a scout rush this turn given our defenses
   */
  static int neededScoutRushDefense(GameState move) {
    float mp = move.data.p1Stats.bits;
    float sp = move.data.p1Stats.cores;
    int turnNumber = move.data.turnInfo.turnNumber;
    int enemyMPCapacity = (int) move.data.p2Stats.bits * 5;
    double enemyMPPercentCapacity = move.data.p2Stats.bits / enemyMPCapacity;
    int scoutRushDefense = (int) ((2.0 * (move.data.p2Stats.bits) / 5) - (turnNumber / 2));
    if (enemyMPPercentCapacity < 0.5) {
      scoutRushDefense = 0;
    }

    scoutRushDefense = (int) Math.min(mp, scoutRushDefense);
    return scoutRushDefense;
  }

  /**
   * find optimal demolisher line and send demo if present
   * TODO: doesn't account for killing stuff causing path to change
   */
  static Coords potentiallySendDemolishers(GameState move, int minDamage) {
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
    for (Map.Entry<Coords, Float> entry : damages.entrySet()) {
      if (entry.getValue() > bestDemolisherDamage) {
        bestCoord = entry.getKey();
        bestDemolisherDamage = entry.getValue();
      }
    }

    return bestCoord;
  }
}
