package com.c1games.terminal.simulation.pathfinding;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.simulation.SimBoard;
import com.c1games.terminal.simulation.Simulator;
import com.c1games.terminal.simulation.units.MobileUnit;

import java.util.*;


public class PathFinder {
  private static final boolean USE_DEFENSIVE_FINDERS = false;

  private static final Set<Coords> blocked = new HashSet<>();

  private static final EnumMap<Edge, PathFinder> finders = new EnumMap<>(Map.ofEntries(
      new AbstractMap.SimpleImmutableEntry<>(Edge.TOP_RIGHT, new PathFinder(Edge.TOP_RIGHT)),
      new AbstractMap.SimpleImmutableEntry<>(Edge.TOP_LEFT, new PathFinder(Edge.TOP_LEFT)),
      new AbstractMap.SimpleImmutableEntry<>(Edge.BOTTOM_LEFT, new PathFinder(Edge.BOTTOM_LEFT)),
      new AbstractMap.SimpleImmutableEntry<>(Edge.BOTTOM_RIGHT, new PathFinder(Edge.BOTTOM_RIGHT))
  ));
//  private static final Map<Edge, PathFinder> finders = Map.ofEntries(
//      new AbstractMap.SimpleEntry<>(Edge.TOP_RIGHT, new PathFinder(Edge.TOP_RIGHT)),
//      new AbstractMap.SimpleEntry<>(Edge.TOP_LEFT, new PathFinder(Edge.TOP_LEFT)),
//      new AbstractMap.SimpleEntry<>(Edge.BOTTOM_LEFT, new PathFinder(Edge.BOTTOM_LEFT)),
//      new AbstractMap.SimpleEntry<>(Edge.BOTTOM_RIGHT, new PathFinder(Edge.BOTTOM_RIGHT))
//  );

  Edge startEdge;
  Edge targetEdge;
  int[][] pathLengths;

  public PathFinder(Edge targetEdge) {
    this.startEdge = Edge.opposite(targetEdge);
    this.targetEdge = targetEdge;
    this.pathLengths = new int[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];
  }

  /**
   * updates the set of blocked coordinates based on a board
   */
  private static void updateBlockedListFromBoard(SimBoard board) {
    blocked.clear();
    for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        if (MapBounds.ARENA[x][y]) {
          Coords coords = new Coords(x,y);
          if (board.hasWall(coords)) {
            blocked.add(coords);
          }
        }
      }
    }
  }

  /**
   * runs the path analyzers on the specified board
   */
  static void processBoard(SimBoard board) {
    updateBlockedListFromBoard(board);
    for (Map.Entry<Edge, PathFinder> finderEntry : finders.entrySet()) {
      if (finderEntry.getKey().isTop() || USE_DEFENSIVE_FINDERS) {
        finderEntry.getValue().analyzeBoard();
      }
    }
  }

  /**
   * whether or not the structure layout on the board has changed
   */
  private static boolean needsToUpdate(SimBoard board) {
    if (!blocked.stream().allMatch(board::hasWall)) {
      return true;
    }
    for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        if (MapBounds.ARENA[x][y]) {
          Coords coords = new Coords(x,y);
          if (board.hasWall(coords) != blocked.contains(coords)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * checks the structure layout of the board and decides whether or not to update the board
   */
  public static void updateIfNecessary(SimBoard board) {
    if (needsToUpdate(board)) {
      forceUpdate(board);
    }
  }

  /**
   * forces the path finder to update with the specified board
   */
  public static void forceUpdate(SimBoard board) {
    processBoard(board);
    if (Simulator.DEBUG) finders.get(Edge.TOP_LEFT).debugPrint();
  }

  /**
   * processes the board state if the blocked set is changed, then returns the desired path
   */
  public static List<Coords> findPath(SimBoard board, Coords start, Edge target) {
    updateIfNecessary(board);
    return findPathStrict(start, target);
  }

  /**
   * returns the path from start to the specified target edge (assumes no board updates are necessary)
   */
  static List<Coords> findPathStrict(Coords start, Edge target) {
    return finders.get(target).getPath(start);
  }

  /**
   * gets the next move for a certain unit based on its position, target, and prevDirection
   */
  public static Coords getNextMove(MobileUnit unit) {
    return finders.get(unit.getTargetEdge()).getNextMove(unit.getLocation(), unit.getPrevDirection());
  }

  /**
   * perform the coordinate analysis (double BFS)
   */
  private void analyzeBoard() {
    List<Coords> reachableCoords = evaluate();
    populateLengths(reachableCoords);
  }

  /**
   * Finds the coordinates reachable from the starting edge towards the target edge
   * will return a list of the single most ideal coordinate if the target edge is unreachable
   */
  private List<Coords> evaluate() {
    List<Coords> reachable = new ArrayList<>();
    Coords fallbackCoord = MapBounds.EDGE_LISTS[startEdge.ordinal()][5]; // 5 is arbitrary but must be less than the length of the edge
    int fallbackCoordValue = coordinateIdealnessValue(fallbackCoord);

    // run A* to find the best reachable point
    Set<Coords> visited = new HashSet<>();
    PriorityQueue<Coords> findEdgeQueue = new PriorityQueue<>(Comparator.comparingInt(this::distanceToEdge));
    findEdgeQueue.addAll(Arrays.asList(MapBounds.EDGE_LISTS[startEdge.ordinal()]));
    while (!findEdgeQueue.isEmpty()) {
      Coords curr = findEdgeQueue.poll();
      if (!MapBounds.inArena(curr) || visited.contains(curr) || blocked.contains(curr)) {
        continue;
      }
      visited.add(curr);
      if (targetEdge.contains(curr)) {
        reachable.add(curr);
      }
      int currCoordValue = coordinateIdealnessValue(curr);
      if (currCoordValue > fallbackCoordValue) {
        fallbackCoord = curr;
        fallbackCoordValue = currCoordValue;
      }
      findEdgeQueue.addAll(curr.neighbors());
    }
    if (reachable.isEmpty()) {
      reachable.add(fallbackCoord);
    }
    return reachable;
  }

  /** A* heuristic for pathing towards an edge
    targetEdge: [0,1,2,3]
    TR=0: x+y-41=0 -> d=-x-y+41
    TL=1: x-y+14=0 -> d=+x-y+14
    BL=2: x+y-13=0 -> d=+x+y-13
    BR=3: x-y-14=0 -> d=-x+y+14
    target%3==0 -> -x
    target<=1   -> -y
    ignore constants because targetEdge is constant for a certain path finder
   */
  private int distanceToEdge(Coords coords) {
    return (targetEdge.isRight() ? -1 : 1) * coords.x + (targetEdge.isTop() ? -1 : 1) * coords.y;
  }

  /**
   * Stupid idealness function from their Pathfinder that ig i have to use
   */
  private int coordinateIdealnessValue(Coords coords) {
    if (targetEdge.contains(coords)) {
      return Integer.MAX_VALUE;
    } else {
      int idealness = 0;
      if (targetEdge.isTop()) //TR or TL
        idealness += MapBounds.BOARD_SIZE * coords.y;
      else
        idealness += MapBounds.BOARD_SIZE * (MapBounds.BOARD_SIZE - 1 - coords.y);
      if (targetEdge.isRight()) // TR or BR
        idealness += coords.x;
      else
        idealness += (MapBounds.BOARD_SIZE - 1 - coords.x);
      return idealness;
    }
  }

  /**
   * mark all coordinates as unreachable from the target edge/most ideal coordinate
   */
  private void markAllUnreachable() {
    for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        if (MapBounds.ARENA[x][y]) {
          pathLengths[x][y] = Integer.MAX_VALUE; // marked as unreachable
        }
      }
    }
  }

  /**
   * populates the pathLengths[][] with the min distance to a reachable coordinate (BFS)
   */
  private void populateLengths(List<Coords> reachableCoords) {
    markAllUnreachable();
    Set<Coords> visited = new HashSet<>();
    Queue<Coords> backtrackingQueue = new LinkedList<>();
    for (Coords reachableCoord : reachableCoords) {
      pathLengths[reachableCoord.x][reachableCoord.y] = 0;
      backtrackingQueue.add(reachableCoord);
    }
    while (!backtrackingQueue.isEmpty()) {
      Coords curr = backtrackingQueue.poll();
      if (!MapBounds.inArena(curr) || visited.contains(curr) || blocked.contains(curr)) {
        continue;
      }
      visited.add(curr);
      for (Coords neighbor : curr.neighbors()) {
        if (MapBounds.inArena(neighbor) && !blocked.contains(neighbor) && !visited.contains(neighbor)) {
          pathLengths[neighbor.x][neighbor.y] = pathLengths[curr.x][curr.y] + 1;
          backtrackingQueue.add(neighbor);
        }
      }
    }
  }

  /**
   * returns a path from the start to the targetEdge
   */
  private List<Coords> getPath(Coords start) {
    if (pathLengths[start.x][start.y] == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Cannot start from " + start + ". Coordinate is unreachable!");
    }
    debugPrint();
    List<Coords> path = new ArrayList<>();
    path.add(start);
    Coords curr = start;
    Direction prevDirection = Direction.SPAWNED;

    // just follow the best path based on computed data until the end of the path is reached
    while (pathLengths[curr.x][curr.y] != 0) {
      // get the next tile
      Coords next = getNextMove(curr, prevDirection);

      // update curr direction
      prevDirection = Direction.fromCoordinates(curr, next);

      // build the list
      path.add(next);
      curr = next;
    }

    return path;
  }

  private void debugPrint() {
    final String WALL_TEXT = " # ";
    final String INVALID_TEXT = " . ";
    final String OUTSIDE_TEXT = "   ";
    for (int y = MapBounds.BOARD_SIZE - 1; y >= 0; y--) {
      GameIO.debug().printf("%2d ", y);
      for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
        Coords c = new Coords(x, y);
        if (!MapBounds.inArena(c))
          GameIO.debug().print(OUTSIDE_TEXT);
        else if (blocked.contains(c))
          GameIO.debug().print(WALL_TEXT);
        else if (pathLengths[c.x][c.y] < Integer.MAX_VALUE)
          GameIO.debug().printf("%2d ", pathLengths[c.x][c.y]);
        else
          GameIO.debug().print(INVALID_TEXT);
      }
      GameIO.debug().println();
    }
    GameIO.debug().print("   ");
    for(int i = 0; i < MapBounds.BOARD_SIZE; i++){
      GameIO.debug().printf("%2d ", i);
    }
    GameIO.debug().println();
  }

  /**
   * gets the next move for a certain coordinate based on the previous direction we just moved
   * @return the same coords as curr if there is no move that improves the position
   */
  private Coords getNextMove(Coords curr, Direction prevDirection) {
    Coords bestNeighbor = curr;
    int bestPathlength = pathLengths[curr.x][curr.y];
    List<Coords> neighbors = curr.neighbors();
    for (int i = 0, neighborsSize = neighbors.size(); i < neighborsSize; i++) {
      Coords neighbor = neighbors.get(i);
      if (MapBounds.inArena(neighbor) && !blocked.contains(neighbor) && (pathLengths[neighbor.x][neighbor.y] < bestPathlength || pathLengths[neighbor.x][neighbor.y] == bestPathlength && (bestNeighbor == curr || compareDirections(curr, prevDirection, neighbor, bestNeighbor) < 0))) {
        bestNeighbor = neighbor;
        bestPathlength = pathLengths[neighbor.x][neighbor.y];
      }
    }
    return bestNeighbor;
  }

  /**
   * compares two potential neighbors for the ideal next mvoe based on current location and previous direction
   * @return -1 when neighbor1 is better than neighbor2, else +1
   */
  private int compareDirections(Coords curr, Direction prevDirection, Coords neighbor1, Coords neighbor2) {
    Direction neighbor1Dir = Direction.fromCoordinates(curr, neighbor1);
    Direction neighbor2Dir = Direction.fromCoordinates(curr, neighbor2);
    boolean neighbor1Good, neighbor2Good;

    // rule 3: move vertically first
    neighbor1Good = prevDirection == Direction.SPAWNED && neighbor1Dir == Direction.VERTICAL;
    neighbor2Good = prevDirection == Direction.SPAWNED && neighbor2Dir == Direction.VERTICAL;
    if (neighbor1Good && !neighbor2Good) {
//      GameIO.debug().println("case 1a " + curr + " -> " + neighbor1);
      return -1;
    } else if (!neighbor1Good && neighbor2Good) {
//      GameIO.debug().println("case 1b " + curr + " -> " + neighbor2);
      return 1;
    }

    // rule 2: change directions
    neighbor1Good = prevDirection != neighbor1Dir;
    neighbor2Good = prevDirection != neighbor2Dir;
    if (neighbor1Good && !neighbor2Good) {
//      GameIO.debug().println("case 2a " + curr + " -> " + neighbor1);
      return -1;
    } else if (!neighbor1Good && neighbor2Good) {
//      GameIO.debug().println("case 2b " + curr + " -> " + neighbor2);
      return 1;
    }

    // rule 4: move towards the target edge
    if ((targetEdge == Edge.TOP_RIGHT && (neighbor1.x > neighbor2.x || neighbor1.y > neighbor2.y)) ||
        (targetEdge == Edge.TOP_LEFT && (neighbor1.x < neighbor2.x || neighbor1.y > neighbor2.y)) ||
        (targetEdge == Edge.BOTTOM_RIGHT && (neighbor1.x > neighbor2.x || neighbor1.y < neighbor2.y)) ||
        (targetEdge == Edge.BOTTOM_LEFT && (neighbor1.x < neighbor2.x || neighbor1.y < neighbor2.y))) {
//      GameIO.debug().println("case 3a " + curr + " -> " + neighbor1);
      return -1;
    } else {
//      GameIO.debug().println("case 3b " + curr + " -> " + neighbor2);
      return 1;
    }
  }
}
