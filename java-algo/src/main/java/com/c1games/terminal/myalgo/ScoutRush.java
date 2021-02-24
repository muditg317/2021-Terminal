package com.c1games.terminal.myalgo;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.units.UnitType;

import java.util.List;
public class ScoutRush {

  public static void execute(GameState move) {
    int mp = (int) move.data.p1Stats.bits;
    int sp = (int) move.data.p1Stats.cores;
    double scoutBaseHealth = move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);;
    int estimatedScoutHealth = (int) (scoutBaseHealth + (sp * 3.0 / 4.0));
    int leftSurvivingPings = calculateSurvivingScouts(move, new Coords(14, 0), mp, estimatedScoutHealth); //targeting the left side
    int rightSurvivingPings = calculateSurvivingScouts(move, new Coords(13, 0), mp, estimatedScoutHealth);


  }

  /**
   *
   * @param move
   * @param start
   * @param numPings
   * @param pingHealth
   * @return the number of scouts expected to survive and deal damage to enemy health
   */
  public static int calculateSurvivingScouts(GameState move, Coords start, int numScouts, int scoutHealth) {
    int totalDamage = 0;
    int side = start.x <= 13 ? MapBounds.EDGE_TOP_RIGHT : MapBounds.EDGE_TOP_LEFT;
    List<Coords> path = move.pathfind(start, side);
    if (!pathHitsTargetEdge(path, side)) {
      return 0;
    }
  }
  public static int totalDamageTaken(GameState move, Coords start) {


  }



}
