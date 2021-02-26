package com.c1games.terminal.algo;

import com.c1games.terminal.algo.map.MapBounds;

import java.util.List;

/**
 * Two-dimensional, integer coordinates.
 */
public class Coords {
    public int x;
    public int y;

    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public List<Coords> neighbors() {
        return List.of(
                new Coords(x + 1, y),
                new Coords(x - 1, y),
                new Coords(x, y + 1),
                new Coords(x, y - 1)
        );
    }

    @Override
    public String toString() {
        return "<" + x + ", " + y + ">";
    }

    public float distance(Coords other) {
        return (float)Math.sqrt(((x - other.x) * (x - other.x)) + ((y - other.y) * (y - other.y)));
    }

    @Override
    public int hashCode() {
        return x + y * MapBounds.BOARD_SIZE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Coords && ((Coords) obj).x == x && ((Coords) obj).y == y;
    }
}
