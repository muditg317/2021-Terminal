package com.c1games.terminal.simulation.pathfinding;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.MapBounds;

import java.util.List;
import java.util.Map;

public enum Edge {
  TOP_RIGHT(0), TOP_LEFT(1), BOTTOM_LEFT(2), BOTTOM_RIGHT(3);

  private boolean top;
  private boolean right;

  Edge(int index) {
    top = index % 3 == 0;
    right = index <= 1;
  }

  private static final int[] distanceOffsets = {41,14,-13,14};
  private static final List<Edge> edges = List.of(Edge.values());

  public static Edge byOrdinal(int index) {
    return edges.get(index);
  }

  public static Edge fromStart(Coords start) {
    return byOrdinal(MapBounds.getEdgeFromStart(start));
  }

  public static Edge opposite(Edge edge) {
    return byOrdinal((edge.ordinal() + 2) % 4);
  }

  public static int distToEdge(Coords location) {
    int shortest = Integer.MAX_VALUE;
    for (Edge edge : edges) {
      shortest = Math.min(shortest, edge.manhattanDistance(location));
    }
    return shortest;
  }

  public boolean contains(Coords coords) {
    return MapBounds.IS_ON_EDGE[this.ordinal()][coords.x][coords.y];
  }

  public boolean isTop() {
    return top; // ordinal <= 1
  }

  public boolean isRight() {
    return right; // ordinal%3 == 0
  }

  /**
   targetEdge: [0,1,2,3]
   TR=0: x+y-41=0 -> d=-x-y+41
   TL=1: x-y+14=0 -> d=+x-y+14
   BL=2: x+y-13=0 -> d=+x+y-13
   BR=3: x-y-14=0 -> d=-x+y+14
   target%3==0 -> -x
   target<=1   -> -y
   ignore constants because targetEdge is constant for a certain path finder
   */
  public int manhattanDistance(Coords coords) {
    return distanceOffsets[ordinal()] + (isRight() ? -1 : 1) * coords.x + (isTop() ? -1 : 1) * coords.y;
  }
}
