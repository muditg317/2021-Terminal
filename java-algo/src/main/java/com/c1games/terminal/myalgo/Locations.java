package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    static final Coords leftEntranceTurret = new Coords(5, 10);

    //THE MAIN WALL ======================
    static final Coords[] mainWallCoords = {
        new Coords(13, 2),
        new Coords(14, 2),

        new Coords(12,3),
        new Coords(15, 3),

        new Coords(11,4),
        new Coords(16, 4),

        new Coords(10,5),
        new Coords(17, 5),

        new Coords(9,6),
        new Coords(18, 6),

        new Coords(8,7),
        new Coords(19, 7),

        new Coords(7,8),
        new Coords(20, 8),

        new Coords(6, 9),
        new Coords(21, 9),

        //new Coords(5, 10),
        new Coords(22, 10),

        new Coords(23,11),
    };

    static final Set<Coords> mainWallHookHoles = new HashSet<Coords>(
        Arrays.asList(
            new Coords(7,8),
            new Coords(20, 8),
            new Coords(6,9),
            new Coords(21, 9),
            new Coords(22, 10),
            new Coords(23,11)));

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
  }


  static final Coords[] cornerTurrets = {

      new Coords(5, 11),
      new Coords(24, 13),

      new Coords(3, 13),
      new Coords(23, 13),
      new Coords(4, 11),

      new Coords(4, 13),
      new Coords(5, 13),
      new Coords(24, 12)

  };

  static final Coords[] rightTurrets = {
      new Coords(25, 12),
      new Coords(26, 12),
  };

  static final Coords[] initialTopEntranceTurrets = {
      new Coords(4, 12),
      new Coords(5, 12),
  };

  static final Coords[] topEntranceWalls = {
      new Coords(3, 13),
      new Coords(4, 13),
      new Coords(5, 13), //this will be the turret boi
      new Coords(6, 13),
      new Coords(7, 13),
      new Coords(8, 13)
  };

  static final Coords[] topEntranceTurrets = {
      new Coords(3 ,12),
      new Coords(4, 12),
      new Coords(5, 12),
      new Coords(6, 12),
      new Coords(7, 12),
      new Coords(8, 12),
  };

  static final Coords[] bottomEntranceTurrets = {
      new Coords(5, 10),
      new Coords(5, 10),
      new Coords(5, 10),
      new Coords(6, 10),
      new Coords(7, 10),
      new Coords(8, 10),
  };




  static final Coords[] extraTurretCoords = {
      new Coords(1, 12),
      new Coords(26, 12),
      new Coords(2, 12),
      new Coords(25, 12),
      new Coords(2, 11),
      new Coords(25, 11),
      new Coords(6, 13),
      new Coords(7, 11),
      new Coords(7, 13),
      new Coords(8, 11),
      new Coords(9, 11),
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

  static final Coords[] encryptorLocations = {
      new Coords(13, 2),
      new Coords(14, 2),
      new Coords(13, 3),
      new Coords(14, 3)
  };

  static final Coords[] boomPath_right = {
          new Coords(6,7),
          new Coords(7,7),
          new Coords(7,6),
          new Coords(8,6),
          new Coords(8,5),
          new Coords(9,5),
          new Coords(9,4),
          new Coords(10,4),
          new Coords(10,3),
          new Coords(11,3),
          new Coords(11,2),
          new Coords(12,2),
          new Coords(12,1),
          new Coords(13,1),
          new Coords(14,1),
          new Coords(15,1),
          new Coords(15,2),
          new Coords(16,2),
          new Coords(16,3),
          new Coords(17,3),
          new Coords(17,4),
          new Coords(18,4),
          new Coords(18,5),
          new Coords(19,5),
          new Coords(19,6),
          new Coords(20,6),
          new Coords(20,7),
          new Coords(21,7),
          new Coords(21,8),
          new Coords(22,8),
          new Coords(22,9),
          new Coords(23,9),
          new Coords(23,10),
          new Coords(24,10),
          new Coords(24,11),
          new Coords(24,12),
          new Coords(25,11),
          new Coords(25,12),
          new Coords(26,12),
          new Coords(26,13),
          new Coords(27,13),
  };
  
  static final Coords[] boomLid_right = {
        new Coords(0, 13),
        new Coords(1, 13),
        new Coords(2, 13),
        new Coords(4, 11),
        new Coords(5, 10),
        new Coords(6, 9),
        new Coords(6, 8),
        new Coords(7, 8),
        new Coords(8, 7),
        new Coords(9, 6),
        new Coords(10, 5),
        new Coords(11, 4),
        new Coords(12, 3),
        new Coords(13, 2),
        new Coords(14, 2),
        new Coords(15, 3),
        new Coords(16, 4),
        new Coords(17, 5),
        new Coords(18, 6),
        new Coords(19, 7),
        new Coords(20, 8),
        new Coords(21, 9),
        new Coords(22, 10),
        new Coords(23, 11),
        new Coords(24, 12),
        new Coords(25, 13)
  };
}
