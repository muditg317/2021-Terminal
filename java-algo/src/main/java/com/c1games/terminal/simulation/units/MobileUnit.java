package com.c1games.terminal.simulation.units;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.simulation.pathfinding.Direction;
import com.c1games.terminal.simulation.pathfinding.Edge;
import com.c1games.terminal.simulation.pathfinding.PathFinder;

import java.util.HashSet;
import java.util.Set;

public abstract class MobileUnit extends SimUnit {
  private final int speed; // num frames to move

  private final Edge targetEdge;
  private Direction prevDirection;
  private Set<StructureUnit> previouslyInRange;
  private int framesSinceMoved;
  private int timesMoved;

  protected MobileUnit(boolean isEnemy, String id, double startHealth, double range, double walkerDamage, double structureDamage, double speed, Coords location, double health, Direction prevDirection) {
    super(isEnemy, id, startHealth, range, walkerDamage, structureDamage, location, health);
    this.speed = (int) Math.round(1/speed);
    this.prevDirection = prevDirection;
    this.previouslyInRange = new HashSet<>();
    this.framesSinceMoved = this.speed;
    this.timesMoved = 0;
    this.targetEdge = Edge.fromStart(location);
  }

  @Override
  public boolean testUnitInteractable(SimUnit other) {
    // TODO: optimization for when this boi hasn't moved
    return super.testUnitInteractable(other);
  }

  public void applyShielding(double shieldAmount) {
    super.addHealth(shieldAmount);
  }

  public double getSpeed() {
    return speed;
  }

  public Edge getTargetEdge() {
    return targetEdge;
  }

  public Direction getPrevDirection() {
    return prevDirection;
  }

  public int getFramesSinceMoved() {
    return framesSinceMoved;
  }

  public int getTimesMoved() {
    return timesMoved;
  }

  /**
   * attempts to move the unit based on its framesSinceMoved
   * @return the coordinates to which it moved (will be the same if it cannot move/is on an edge) (will be null if no move attempt was made)
   */
  public Coords attemptMove() {
    Coords prev = null;
    if (--framesSinceMoved == 0) {
      prev = super.getLocation();
      super.setLocation(PathFinder.getNextMove(this));
      this.prevDirection = Direction.fromCoordinates(prev, super.getLocation());
      timesMoved++;
      framesSinceMoved = speed;
    }
    return prev;
  }

  public boolean onTargetEdge() {
    return targetEdge.contains(super.getLocation());
  }
}
