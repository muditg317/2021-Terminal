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
import com.c1games.terminal.myalgo.attack.Attack;
import com.c1games.terminal.myalgo.attack.DemolisherRun;
import com.c1games.terminal.myalgo.attack.HookAttack;
import com.c1games.terminal.myalgo.attack.ScoutRush;
import com.c1games.terminal.myalgo.strategy.EarlyGame;
import com.c1games.terminal.myalgo.strategy.FirstTurn;
import com.c1games.terminal.myalgo.strategy.MainStrategy;
import com.c1games.terminal.myalgo.strategy.SecondTurn;
import com.c1games.terminal.myalgo.utility.StrategyUtility;
import com.c1games.terminal.myalgo.utility.Utility;
import com.c1games.terminal.simulation.Simulator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * My Algo:
 */
public class MyAlgo implements GameLoop {

  private static final int NUM_EARLY_GAME_TURNS = 0;

  public static final Random rand = new Random();
  public static final HashMap<Coords, Integer> scoredOnLocations = new HashMap<>();
  public static final HashMap<Coords, Double> wallDamage = new LinkedHashMap<>();
  public static Attack lastAttack = null;
  public static double attackActualValue;

  @SuppressWarnings("unchecked") // the list type will be correct
  public static final List<Unit>[][] enemyBaseHistory = new ArrayList[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];

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

//    GameIO.debug().println("Using Config:\n" + config + "\n========");
  }

  /**
   * Make a move in the game.
   */
  @Override
  public void onTurn(GameIO io, GameState move) {
    GameIO.debug().println("Performing turn " + move.data.turnInfo.turnNumber + " of your custom algo strategy");
    GameIO.debug().printf("My stats: %s\n", move.data.p1Stats);
    GameIO.debug().printf("Sims run: %d\n", Simulator.simCount);

    if (lastAttack != null) {
      if (lastAttack instanceof HookAttack || lastAttack instanceof DemolisherRun) {
        List<Unit>[][] oldBoard = new List[MapBounds.BOARD_SIZE][MapBounds.BOARD_SIZE];
        for (int x = 0; x < MapBounds.BOARD_SIZE; x++) {
          for(int y = 0; y < MapBounds.BOARD_SIZE; y++) {
            Unit structure = enemyBaseHistory[x][y].get(enemyBaseHistory[x][y].size() - 1);
            List<Unit> structureAtCoords = structure == null ? List.of() : List.of(structure);
            oldBoard[x][y] = structureAtCoords;
          }
        }
        double spThen = StrategyUtility.enemySPOnBoard(oldBoard);
        double spNow = StrategyUtility.enemySPOnBoard(move.allUnits);
        attackActualValue = spThen - spNow;
      }
      GameIO.debug().printf("Last attack (%s) summary:\n\texpected: %.2f\n\tactual: %.2f\n", lastAttack.getClass().getSimpleName(), lastAttack.getExpectedAttackValue(), attackActualValue);
      lastAttack.learn(attackActualValue);
    }
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

    GameIO.debug().printf("Sims run: %d\n", Simulator.simCount);
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
//      for (FrameData.Events.DamageEvent damage : move.data.events.damage) {
//        if (!MapBounds.inArena(damage.coords)) {
//          continue;
//        }
//        Unit attacked = move.getWallAt(damage.coords);
//        if (attacked != null && damage.unitOwner == PlayerId.Player2) {
//          double damageDone = damage.damage;
//          damageDone = Math.min(damageDone, attacked.health);
//          attackActualValue += Utility.damageToSp(attacked, damageDone);
////          if (move.data.turnInfo.turnNumber == 3) {
////            GameIO.debug().printf("Damage done!\t%.2f to %s. Total SP damage: %.2f\n", damageDone, attacked, attackActualValue);
////          }
//        }
//      }
    }
  }

}
