package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.io.GameLoop;
import com.c1games.terminal.algo.io.GameLoopDriver;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;

import java.util.*;

/**
 * Turtle Algo:
 */
public class MyAlgo implements GameLoop {

  final static int NUM_EARLY_GAME_TURNS = 5;

  final Random rand = new Random();
  //ArrayList<ArrayList<Coords>> scoredOnLocations = new ArrayList<>();
  static HashMap<Coords, Integer> scoredOnLocations = new HashMap<>();
  static Attack lastAttack = null;
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

    //scoredOnLocations.add(new ArrayList<Coords>());
  }

  /**
   * Save process action frames. Careful there are many action frames per turn!
   */
  @Override
  public void onActionFrame(GameIO io, GameState move) {
    // Save locations that the enemy scored on against us to reactively build defenses
    for (FrameData.Events.BreachEvent breach : move.data.events.breach) {
      if (breach.unitOwner != PlayerId.Player1) {
        //scoredOnLocations.get(scoredOnLocations.size() - 1).add(breach.coords);
        scoredOnLocations.put(breach.coords, scoredOnLocations.getOrDefault(breach.coords, 0) + 1);
        scoredOnLocations.forEach((key, value) -> GameIO.debug().printf("We got scored on at %s %d times.\n",
            key, value));
      }
    }
    //evaluate our attacks
    if (lastAttack instanceof ScoutRush) {
      ScoutRush sr = (ScoutRush) lastAttack;
      int lastDamage = 0;
      for (FrameData.Events.BreachEvent breach : move.data.events.breach) {
        if (breach.unitOwner == PlayerId.Player1) {
          lastDamage++;
        }
      }
      GameIO.debug().printf("SR: Expected Damage was %d: Actual damage was: %d\n", sr.expectedDamage, lastDamage);
      sr.learn(lastDamage);
    } else if (lastAttack instanceof DemolisherRun || lastAttack instanceof HookAttack) {
      int actualSpDamage = 0;
      for (FrameData.Events.DamageEvent damage : move.data.events.damage) {
        Unit attacked = move.getWallAt(damage.coords);

        if (attacked != null && damage.unitOwner == PlayerId.Player2) {
          double damageDone = damage.damage;
          damageDone = Math.min(damageDone, attacked.health);
          actualSpDamage += Utility.damageToSp(attacked, damageDone);
        }
      }
      lastAttack.learn(actualSpDamage);
    }
  }

}
