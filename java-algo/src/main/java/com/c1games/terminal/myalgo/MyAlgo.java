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
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.simulation.Simulator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * My Algo:
 */
public class MyAlgo implements GameLoop {

  private static final int NUM_EARLY_GAME_TURNS = 5;

  static final Random rand = new Random();
  static final HashMap<Coords, Integer> scoredOnLocations = new HashMap<>();
  static final HashMap<Coords, Double> wallDamage = new LinkedHashMap<>();
  static Attack lastAttack = null;
  static double attackActualValue;

  @SuppressWarnings("unchecked") // the list type will be correct
  static final List<Unit>[][] enemyBaseHistory = new ArrayList[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];

  public static void main(String[] args) {
    new GameLoopDriver(new MyAlgo()).run();
  }


  @Override
  public void initialize(GameIO io, Config config) {
    GameIO.debug().println("Configuring your custom java algo strategy...");
    long seed = rand.nextLong();
    rand.setSeed(seed);
    GameIO.debug().println("Set random seed to: " + seed);

    for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
      for (int y = 0; y < MapBounds.BOARD_SIZE; y++) {
        enemyBaseHistory[x][y] = new ArrayList<>();
      }
    }
  }

  /**
   * Make a move in the game.
   */
  @Override
  public void onTurn(GameIO io, GameState move) {
    GameIO.debug().println("Performing turn " + move.data.turnInfo.turnNumber + " of your custom algo strategy");


    if (lastAttack != null) {
      lastAttack.learn(attackActualValue);
    }
    attackActualValue = 0;
    lastAttack = null;
    //Utility.buildReactiveDefenses(move);
    int turnNumber = move.data.turnInfo.turnNumber;

    for (int x = 0; x < 28; x++) {
      move.attemptSpawn(new Coords(x, 13), UnitType.Wall);
    }

    /*

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

    GameIO.debug().printf("Sims run: %d\n", Simulator.simCount);


     */
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

    //Remember where we take wall damage
    for (FrameData.Events.DamageEvent damage : move.data.events.damage) {
      if (damage.unitOwner == PlayerId.Player1 && MapBounds.inArena(damage.coords)) {
//        GameIO.debug().println("Damage event!\n"+damage);
        Unit wall = move.getWallAt(damage.coords);
        if (wall != null && wall.type == Utility.WALL) {
          wallDamage.put(damage.coords, wallDamage.getOrDefault(damage.coords, 0.0) + damage.damage);
        }
      }
    }

    // remember their layout
    if (move.data.turnInfo.actionPhaseFrameNumber == 0) {
      @SuppressWarnings("unchecked") // the type will be correct
      List<Unit>[][] currentBoard = new ArrayList[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];
      for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
        for(int y = 0; y < MapBounds.BOARD_SIZE; y++) {
          List<Unit> structureAtCoords = move.allUnits[x][y].stream().filter(unit -> move.isStructure(unit.type)).map(unit -> {
            Unit newUnit = new Unit(unit.type, unit.health, unit.id, unit.owner, move.config);
            if (unit.upgraded) newUnit.upgrade();
            return newUnit;
          }).collect(Collectors.toList());
          currentBoard[x][y] = structureAtCoords;
          enemyBaseHistory[x][y].add(structureAtCoords.size() > 0 ? structureAtCoords.get(0) : null);
        }
      }

//      Utility.printGameBoard(currentBoard);
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
        if (!MapBounds.inArena(damage.coords)) {
          continue;
        }
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
