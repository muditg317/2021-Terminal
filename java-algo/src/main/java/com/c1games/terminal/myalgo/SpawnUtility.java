package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.units.UnitType;

import java.util.Arrays;

public class SpawnUtility {
    /**
     * spawn turrets at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     */
    static int placeTurrets(GameState move, Coords[] locations) {
      return move.attemptSpawnMultiple(Arrays.asList(locations), Utility.TURRET);
    }

    static boolean placeTurret(GameState move, Coords location) {
        return move.attemptSpawn(location, Utility.TURRET);
    }

    /**
     * place walls at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to place them
     */
    static int placeWalls(GameState move, Coords[] locations) {
      return move.attemptSpawnMultiple(Arrays.asList(locations), Utility.WALL);
    }

    static boolean placeWall(GameState move, Coords location) {
        return move.attemptSpawn(location, Utility.WALL);
    }

    /**
     * place supports at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to place them
     */
    static int placeSupports(GameState move, Coords[] locations) {
      return move.attemptSpawnMultiple(Arrays.asList(locations), Utility.SUPPORT);
    }

    /**
     * Place support at the specified location
     * @param move
     * @param location
     * @return
     */
    static boolean placeSupport(GameState move, Coords location) {
        return move.attemptSpawn(location, Utility.SUPPORT);
    }

    /**
     * places upgraded supports at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to place them
     */
    static int placeUpgradedSupports(GameState move, Coords[] locations) {
      return (int) Arrays.stream(locations).filter(coords -> placeSupport(move, coords) && applyUpgrade(move, coords)).count();
    }

    /**
     * apply upgrades at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to apply them to
     */
    static int applyUpgrades(GameState move, Coords[] locations) {
      return move.attemptUpgradeMultiple(Arrays.asList(locations));
    }

    /**
     * Applys an upgrade at the specified location
     * @param move
     * @param location
     * @return
     */
    static boolean applyUpgrade(GameState move, Coords location) {
        return move.attemptUpgrade(location) == 1;
    }

    /**
     * removes one tower at the specified location
     * @param move    the game state on which to deploy moves
     * @param location the location to remove them from
     */
    static int removeBuilding(GameState move, Coords location) {
      return move.attemptRemoveStructure(location);
    }

    /**
     * removes towers at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to remove them from
     */
    static int removeBuildings(GameState move, Coords[] locations) {
      return move.attemptRemoveStructureMultiple(Arrays.asList(locations));
    }

    /**
     * spawn scouts at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     */
    static int spawnScouts(GameState move, Coords[] locations) {
      return move.attemptSpawnMultiple(Arrays.asList(locations), Utility.SCOUT);
    }

    /**
     * spawn demolishers at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     */
    static int spawnScouts(GameState move, Coords[] locations, int num) {
      int count = 0;
      for (int i = 0; i < num; i ++) {
        count += move.attemptSpawnMultiple(Arrays.asList(locations), Utility.SCOUT);
      }
      return count;
    }

    static int spawnScouts(GameState move, Coords location, int num) {
        return spawnScouts(move, new Coords[] {location}, num);
    }

    /**
     * spawn demolishers at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     */
    static int spawnDemolishers(GameState move, Coords[] locations) {
      return move.attemptSpawnMultiple(Arrays.asList(locations), Utility.DEMOLISHER);
    }

    /**
     * spawn demolishers at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     */
    static int spawnDemolishers(GameState move, Coords[] locations, int num) {
      int count = 0;
      for (int i = 0; i < num; i ++) {
        count += move.attemptSpawnMultiple(Arrays.asList(locations), Utility.DEMOLISHER);
      }
      return count;
    }

    static int spawnDemolishers(GameState move, Coords location, int num) {
        return spawnDemolishers(move, new Coords[] {location}, num);
    }

    /**
     * spawn interceptors at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     */
    static int spawnInterceptors(GameState move, Coords[] locations) {
      return move.attemptSpawnMultiple(Arrays.asList(locations), Utility.INTERCEPTOR);
    }

    /**
     * spawn interceptors at the specified locations
     * @param move    the game state on which to deploy moves
     * @param locations the locations to spawn them
     * @param num     number of interceptors to spawn at each location specified
     */
    static int spawnInterceptors(GameState move, Coords[] locations, int num) {
      int count = 0;
      for (int i = 0; i < num; i ++) {
        count += move.attemptSpawnMultiple(Arrays.asList(locations), Utility.INTERCEPTOR);
      }
      return count;
    }

    static int spawnInterceptors(GameState move, Coords location, int num) {
        return spawnInterceptors(move, new Coords[] {location}, num);
    }

    /**
     * get the cost of a unit (API method doesn't work)
     * @param move    the game state for which to check the unit cost
     * @param type    the desired unit type
     * @param upgrade whether of not we are checking upgrade cost or base cost
     * @return
     */
    static int getUnitCost(GameState move, UnitType type, boolean upgrade) {
      if (type == UnitType.Remove) {
        throw new IllegalArgumentException("Cannot query number affordable of remove unit type use removeFirewall");
      }
      if (type == UnitType.Upgrade) {
        throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, put type of unit to upgrade and upgrade=true");
      }

      float[] cost = ((Config.RealUnitInformation) move.config.unitInformation.get(type.ordinal())).cost();
      if (upgrade) {
        Config.UnitInformation upgradeInfo = move.config.unitInformation.get(type.ordinal()).upgrade.orElse(null);
        if (upgradeInfo != null) {
          cost[0] = (float)upgradeInfo.cost1.orElse(cost[0]);
          cost[1] = (float)upgradeInfo.cost2.orElse(cost[1]);
        } else {
          throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, unit not upgradeable");
        }
      }

      return (int) Math.max(cost[0], cost[1]);
    }
}
