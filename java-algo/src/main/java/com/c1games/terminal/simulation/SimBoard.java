package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.units.UnitType;

import java.util.*;
import java.util.stream.Collectors;

public class SimBoard {
  final static double ROOT2 = 1.41421356237;
  final static int yPosXWidth = 7;
  final static int maxYPosX = 3;
  final static int yNegXWidth = 7;
  final static int maxYNegX = 3;
  final static int numBuckets = (maxYNegX+1) * (maxYPosX+1);

  HashMap<Integer, HashSet<SimUnit>> buckets;


  public SimBoard() {
    buckets = new HashMap<>(numBuckets);
    for (int i = 0; i < numBuckets; i++) {
      buckets.put(i, new HashSet<>());
    }
  }

  public SimBoard(GameState move) {
    this();
    for (int i = 13; i < 41; i++) {
      for (int j = 0; j <= 14 - i%2; j++) {
        int x = j + (i-13)/2;
        int y = i - x;
        move.allUnits[x][y].stream().filter(unit -> unit.type != UnitType.Remove && unit.type != UnitType.Upgrade).forEach(unit -> addUnit(SimUnit.fromAPIUnit(unit, x,y)));
      }
    }
  }

  // makes tilted spatial hashing for the map
  private int getHashIndex(Coords location) {
    int yPosX = Math.min((location.x + location.y - 13) / yPosXWidth, maxYPosX);
    int yNegX = Math.min((location.x - location.y + 14) / yNegXWidth, maxYNegX);
    return yNegX + yPosX * (maxYNegX + 1);
  }
  private int getHashIndex(SimUnit unit) {
    return getHashIndex(unit.location);
  }

  public void addUnit(SimUnit unit) {
    buckets.get(getHashIndex(unit)).add(unit);
  }

  public void updateUnitBucket(SimUnit unit, Coords prevLocation) {
    int oldIndex = getHashIndex(prevLocation);
    int unitIndex = getHashIndex(unit);
    if (oldIndex == unitIndex) {
      return;
    }
    buckets.get(oldIndex).remove(unit);
    buckets.get(unitIndex).add(unit);
  }

  public List<SimUnit> interactableUnits(SimUnit unit) {
    if (unit.range == 0) {
      return new ArrayList<>(0);
    }

    // determine which buckets to check in
    // in the worst case reduces by only half
    Set<Integer> bucketIndices = new HashSet<>(9);
    int unitYP = Math.min((unit.location.x + unit.location.y - 13) / yPosXWidth, maxYPosX);
    int unitYN = Math.min((unit.location.x - unit.location.y + 14) / yNegXWidth, maxYNegX);
    int tiltedRange = (int) (2 * Math.floor(unit.range / ROOT2));
    int minYP = Math.max(0, unitYP-tiltedRange);
    int maxYP = Math.min(maxYPosX, unitYP+tiltedRange);
    int minYN = Math.max(0, unitYN-tiltedRange);
    int maxYN = Math.min(maxYNegX, unitYN+tiltedRange);
    for (int yPosX = minYP; yPosX <= maxYP; yPosX++) {
      for (int yNegX = minYN; yNegX <= maxYN; yNegX++) {
        bucketIndices.add(yNegX + yPosX * (maxYNegX + 1));
      }
    }

    // Check each bucket
    return bucketIndices.stream().flatMap(hashIndex -> buckets.get(hashIndex).stream()).filter(unit::testUnitInteractable).collect(Collectors.toList());
  }


}
