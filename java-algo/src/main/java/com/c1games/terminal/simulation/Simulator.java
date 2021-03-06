package com.c1games.terminal.simulation;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.SpawnCommand;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.myalgo.Utility;
import com.c1games.terminal.simulation.units.SimUnit;

import java.util.List;
import java.util.Map;

public class Simulator {

//  SimBoard board;
//
//  public Simulator(GameState move) {
//    this.board = new SimBoard(move);
//  }

  public SimBoard simulate(GameState state) {
    // prepare board object
    SimBoard board = new SimBoard(state, false);
//    for (Map.Entry<Coords, Utility.Pair<UnitType, Integer>> spawnEvent : spawnEvents.entrySet()) {
//      Coords spawnLocation = spawnEvent.getKey();
//      for (int num = spawnEvent.getValue().value; num >= 0; num--) {
//        board.spawnUnit(spawnLocation, spawnEvent.getValue().key);
//      }
//    }

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

    board.simulate();

    return board;
  }
}
