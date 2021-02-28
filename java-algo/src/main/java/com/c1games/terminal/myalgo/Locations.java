package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;

import java.util.*;
import java.util.stream.Stream;

/**
 * Holds the hard coded locations of stuff for our algorithm.
 */
class Locations {
  // OFFENSIVE ===================
  static final Coords[] hailmaryInters = {
    new Coords(15,1)
  };

  static final Coords[] spacedInters5 = {
    new Coords(3,10),
    new Coords(9,4),
    new Coords(17,3),
    new Coords(21,7),////////////////////// WAS 12,1 but that seems silly...
    new Coords(25,11)
  };

  static final Coords[] spacedInters7 = {
    new Coords(3,10),
    new Coords(9,4),
    new Coords(17,3),
    new Coords(21,7),
    new Coords(25,11)
  };


  static class Essentials {
    static final Coords firstLeftTurret = new Coords(3, 12);
    static final Coords firstRightTurret = new Coords(24, 12);
    static final Coords firstLeftTurretWall = new Coords(firstLeftTurret.x, firstLeftTurret.y + 1);
    static final Coords firstRightTurretWall = new Coords(firstRightTurret.x, firstRightTurret.y + 1);

    static final Coords[] mainTurrets = {
        new Coords(3, 12),

        new Coords(7, 10),
        new Coords(11, 10),
        new Coords(16, 9),
        new Coords(20, 10),

        new Coords(24, 12),

    };


    static final Coords[] mainTurretWalls;
    static {
      mainTurretWalls = new Coords[mainTurrets.length];
      for (int i = 0; i < mainTurretWalls.length; i++) {
        Coords turret = mainTurrets[i];
        mainTurretWalls[i] = new Coords(turret.x, turret.y + 1);
      }
    }

    static final Coords leftEntranceTurret = new Coords(5, 10);

    //THE MAIN WALL ======================
    static final Coords[] mainWallCoords = {
        new Coords(4, 12),
        new Coords(23, 12),

        new Coords(6, 11),
        new Coords(21, 11),

        new Coords(10, 11),
        new Coords(17, 11),

        new Coords(11, 11),
        new Coords(18, 11),

        new Coords(12, 10),
        new Coords(15, 10)

    };

    static final Set<Coords> mainWallHookHoles = new LinkedHashSet<Coords>(
        Arrays.asList(
            new Coords(5, 11),
            new Coords(8, 10),
            new Coords(13, 9),
            new Coords(14, 9),
            new Coords(19, 10),
            new Coords(22, 11)
        ));

    static final Coords[] leftCornerWalls = {
        new Coords(2, 13),
        new Coords(1, 13),
        new Coords(0, 13),


    };
    static final Coords[] rightCornerWalls = {
        new Coords(25, 13),
        new Coords(26, 13),
        new Coords(27, 13),

    };

    static final Coords[] cornerWalls;
    static {
      cornerWalls = new Coords[leftCornerWalls.length + rightCornerWalls.length];
      int i = 0;
      for (; i < leftCornerWalls.length; i++) {
        cornerWalls[i] = leftCornerWalls[i];
      }
      for (int j = 0; j < rightCornerWalls.length; j++) {
        cornerWalls[i+j] = rightCornerWalls[j];
      }
    }
  }



  static final Coords[] cornerTurrets = {

      new Coords(2, 12),
      new Coords(25, 12),

      new Coords(1, 12),
      new Coords(26, 12)
  };

  static final Coords[] rightTurrets = {
      new Coords(25, 12),
      new Coords(26, 12),
  };


  static final Coords[] extraTurrets = {
      new Coords(6, 10),
      new Coords(21, 10),
      new Coords(4, 11),
      new Coords(23, 11),
      new Coords( 10, 10),
      new Coords(17, 10),
  };

  static final Coords[] extraWallCoords = {
      new Coords(8,13),
      new Coords(9, 13),
      new Coords(10, 13),
      new Coords(10, 11),
      new Coords(11, 13),
      new Coords(11, 11)
  };

  static final Coords[] extraMainWallUpgrades = {
      new Coords(23,11),
      new Coords(22,10),
      new Coords(21,9),
      new Coords(20,8),
  };



  //SUPPORT TOWER LOCATIONS


  //ENEMY LOCATIONS
  static final Coords[] enemyLeftCornerCoords = {
          new Coords(13, 2),
          new Coords(14, 2),
  };
  //DEFAULT GARBAGE =====================

  static final Coords[] boomPath_right = {

      new Coords(23,10),
      new Coords(24,10),
      new Coords(24,11),
      new Coords(25,11),
      new Coords(25,12),
      new Coords(26,12),
      new Coords(26,13),
      //new Coords(27,13),
  };
  
  static final Coords[] boomLid_right;
  static{
    boomLid_right = Stream.concat(Stream.concat(Arrays.stream(
        Essentials.leftCornerWalls),
        Arrays.stream(Essentials.mainWallCoords)),
        Essentials.mainWallHookHoles.stream()).toArray(Coords[]::new);
  }

  static final Coords[] safeSupportLocations = {
      new Coords(13, 3),
      new Coords(14, 3),

      new Coords(12, 4),
      new Coords(13, 4),
      new Coords(14, 4),
      new Coords(15, 4),

      new Coords(11, 5),
      new Coords(12, 5),
      new Coords(13, 5),
      new Coords(14, 5),
      new Coords(15, 5),
      new Coords(16, 5),

      new Coords(10, 6),
      new Coords(11, 6),
      new Coords(12, 6),
     // new Coords(13, 6), //these don't reach the path if unupgraded
      //new Coords(14, 6),
      new Coords(15, 6),
      new Coords(16, 6),
      new Coords(17, 6),



  };
}
