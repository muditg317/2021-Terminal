package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.io.GameLoop;
import com.c1games.terminal.algo.io.GameLoopDriver;
import com.c1games.terminal.algo.map.GameState;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

/**
 * Turtle Algo:
 */
public class MyAlgo implements GameLoop {

  final static int NUM_EARLY_GAME_TURNS = 5;

  final Random rand = new Random();
  ArrayList<ArrayList<Coords>> scoredOnLocations = new ArrayList<>();
  int enemySupportTowerCoresInvestment = 0;
  //boolean awaitingBoom = false;
  //int turnsUntilBoom = -1;
  //String boomSide = "";
  boolean hooking = false;

  public static void main(String[] args) {
    new GameLoopDriver(new MyAlgo()).run();
  }


  @Override
  public void initialize(GameIO io, Config config) {
    GameIO.debug().println("Configuring your custom java algo strategy...");
    long seed = rand.nextLong();
    rand.setSeed(seed);
    GameIO.debug().println("Set random seed to: " + seed);
  }

  /**
   * Make a move in the game.
   */
  @Override
  public void onTurn(GameIO io, GameState move) {
    GameIO.debug().println("Performing turn " + move.data.turnInfo.turnNumber + " of your custom algo strategy");

    //Utility.buildReactiveDefenses(move);
    int turnNumber = move.data.turnInfo.turnNumber;

    if (turnNumber == 0) {
      FirstTurn.execute(this, move);
    } else if (turnNumber == 1) {
      SecondTurn.execute(this, move);
    } else if (move.data.turnInfo.turnNumber < NUM_EARLY_GAME_TURNS) {
      EarlyGame.execute(this, move);
    } else {
      MainStrategy.execute(this, move);
//      GameIO.debug().println("finishing main turn with bits: " + move.data.p1Stats.bits);
    }

    scoredOnLocations.add(new ArrayList<Coords>());
  }

  /**
   * Save process action frames. Careful there are many action frames per turn!
   */
  @Override
  public void onActionFrame(GameIO io, GameState move) {
    // Save locations that the enemy scored on against us to reactively build defenses
    for (FrameData.Events.BreachEvent breach : move.data.events.breach) {
      if (breach.unitOwner != PlayerId.Player1) {
        scoredOnLocations.get(scoredOnLocations.size() - 1).add(breach.coords);
      }
    }

    Utility.trackEnemyCoresInvestedInFactories(this, move);
  }

}
