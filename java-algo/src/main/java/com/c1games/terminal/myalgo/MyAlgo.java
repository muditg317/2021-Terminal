package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.io.GameLoop;
import com.c1games.terminal.algo.io.GameLoopDriver;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Turtle Algo:
 */
public class MyAlgo implements GameLoop {

  final static int NUM_EARLY_GAME_TURNS = 5;

  final Random rand = new Random();
  //ArrayList<ArrayList<Coords>> scoredOnLocations = new ArrayList<>();
  static HashMap<Coords, Integer> scoredOnLocations = new HashMap<>();
  static Attack lastAttack = null;
  static double attackActualValue;
  int enemySupportTowerCoresInvestment = 0;
  //boolean awaitingBoom = false;
  //int turnsUntilBoom = -1;
  //String boomSide = "";
  boolean hooking = false;

  List<Unit>[][] enemyBaseHistory = new ArrayList[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];

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
    lastAttack.learn(attackActualValue);
    attackActualValue = 0;
    lastAttack = null;
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

    // remember their layout
    if (move.data.turnInfo.actionPhaseFrameNumber == 0) {
      for (int i = 13; i < 41; i++) {
        for (int j = 0; j <= 14 - i%2; j++) {
          int x = j + (i-13)/2;
          int y = i - x;
          List<Unit> structureAtCoords = move.allUnits[x][y].stream().filter(unit -> move.isStructure(unit.type)).map(unit -> {
            Unit newUnit = new Unit(unit.type, unit.health, unit.id, unit.owner, move.config);
            if (unit.upgraded) newUnit.upgrade();
            return newUnit;
          }).collect(Collectors.toList());
          enemyBaseHistory[x][y].set(move.data.turnInfo.turnNumber, structureAtCoords.size() > 0 ? structureAtCoords.get(0) : null);
        }
      }
    }


    //evaluate our attacks
    if (lastAttack instanceof ScoutRush) {
      for (FrameData.Events.BreachEvent breach : move.data.events.breach) {
        if (breach.unitOwner == PlayerId.Player1) {
          attackActualValue++;
        }
      }

    } else if (lastAttack instanceof DemolisherRun || lastAttack instanceof HookAttack) {
      for (FrameData.Events.DamageEvent damage : move.data.events.damage) {
        Unit attacked = move.getWallAt(damage.coords);

        if (attacked != null && damage.unitOwner == PlayerId.Player2) {
          double damageDone = damage.damage;
          damageDone = Math.min(damageDone, attacked.health);
          attackActualValue += Utility.damageToSp(attacked, damageDone);
        }
      }
    }
  }

}
