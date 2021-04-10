package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.SpawnCommand;
import com.c1games.terminal.algo.units.UnitType;

public class Simulator {
  public static boolean DEBUG = false;

  public static int simCount = 0;

  public static SimBoard simulate(GameState state) {
    simCount++;
    GameIO.debug().printf("sim:%d|",simCount);
    // prepare board object
    SimBoard board = new SimBoard(state, false);

    // run spawn commands in order (used to maintain good IDs on the units)
    for (SpawnCommand spawnCommand : state.buildStack) {
      if (spawnCommand.type != UnitType.Upgrade && spawnCommand.type != UnitType.Remove) {
        board.spawnUnit(new Coords(spawnCommand.x, spawnCommand.y), spawnCommand.type);
      }
    }
    for (SpawnCommand spawnCommand : state.deployStack) {
      if (spawnCommand.type != UnitType.Upgrade && spawnCommand.type != UnitType.Remove) {
        board.spawnUnit(new Coords(spawnCommand.x, spawnCommand.y), spawnCommand.type);
      }
    }

    // simulate on the board
    board.simulate();
    return board;
  }
}
