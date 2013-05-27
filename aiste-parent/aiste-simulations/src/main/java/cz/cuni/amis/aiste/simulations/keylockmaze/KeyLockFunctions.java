/*
 * Copyright (C) 2013 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cuni.amis.aiste.simulations.keylockmaze;

import java.util.Random;

/**
 * This class encapsulates all static functions (function, that are not object dependent).
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockFunctions {
	
	/**
	 * <p>Generates maze of simple objects named {@link FourWayPoint}s. Function uses Side-winder algorithm.</p>
	 * @param stepsWide How many steps should be the result wide?
	 * @param stepsLong How many steps should be the result high?
	 * @param rand Random generator.
	 * @return Maze of FourWayPoints.
	 * @throws IllegalArgumentException 
	 */
	public static KeyLockFourWayPoint[][] generateMazeMap(int stepsWide, int stepsLong, Random rand) throws IllegalArgumentException {
		if (stepsWide < 3 || stepsLong < 3) {
			throw new IllegalArgumentException("The size must be at least 3x3.");
		}
		
		/* 
		 * Let's use Side-winder algorithm with smaller upgrade. We will divide 
		 * the array into two halves and we will generate separate maze in both
		 * of them. One will be generated from top to bottom, and the other will 
		 * be generated from bottom to top. Then we will randomly pick one wall 
		 * that will be broken to connect those two halves.
		 */
		
		
		// Construct maze with all walls
		KeyLockFourWayPoint[][] map = new KeyLockFourWayPoint[stepsWide][stepsLong];
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				map[i][j] = new KeyLockFourWayPoint((i * map.length) + j);
			}
		}
		
		// First half borders
		int fromX1 = 0;
		int fromY1 = 0;
		int toX1 = stepsWide / 2 - 1;
		int toY1 = stepsLong - 1;
		
		// Second half borders
		int fromX2 = toX1 + 1;
		int fromY2 = 0;
		int toX2 = stepsWide - 1;
		int toY2 = stepsLong - 1;
		
		// If the maze is not long enough to generate it as two halves...
		if (stepsWide < 4) {
			// First half borders will cover whole maze
			fromX1 = 0;
			fromY1 = 0;
			toX1 = stepsWide - 1;
			toY1 = stepsLong - 1;

			// Second half borders will be zero
			fromX2 = 0;
			fromY2 = 0;
			toX2 = 0;
			toY2 = 0;
		}
		
		
		// ---------------->
		// -- First half -->
		// ---------------->
		
		// Lower line is without walls
		for (int i = fromX1; i < toX1; i++) {
			map[i][fromY1].setNeighbour(map[i + 1][fromY1], KeyLockDirection.EAST);
		}
		// Generate the rest of maze
		// Go through all lines (excluding the first one)...
		for (int y = fromY1 + 1; y <= toY1; y++) {
			// Get block of random length and pick one of it's points. Destroy
			// southern wall of that point to connect it to the rest of maze.
			int blockLength = 0;
			for (int x = fromX1; x < toX1; x++) {
				// If connect to the next...
				if (rand.nextBoolean()) {
					// Connect next step to current block
					map[x][y].setNeighbour(map[x + 1][y], KeyLockDirection.EAST);
					blockLength++;
				} else {
					// Pick one of steps in the block and destroy it's southern 
					// wall.
					int offset = (blockLength > 0) ? rand.nextInt(blockLength) : 0;
					map[x - offset][y].setNeighbour(map[x - offset][y - 1], KeyLockDirection.SOUTH);
					blockLength = 0;
				}
			}
			
			// Last cycle iteration is a special case
			if (blockLength > 0) {
				// Pick one of steps in the block and destroy it's southern 
				// wall.
				int offset = rand.nextInt(blockLength);
				map[toX1 - offset][y].setNeighbour(map[toX1 - offset][y - 1], KeyLockDirection.SOUTH);
			} else {
				map[toX1][y].setNeighbour(map[toX1][y - 1], KeyLockDirection.SOUTH);
			}
		}
		
		
		// <----------------
		// <-- First half --
		// <----------------
		
		// ----------------->
		// -- Second half -->
		// ----------------->
		
		// Top line si without walls
		for (int i = fromX2; i < toX2; i++) {
			map[i][toY2].setNeighbour(map[i + 1][toY2], KeyLockDirection.EAST);
		}
		// Generate the rest of maze
		// Go through all lines (excluding the last one)...
		for (int y = toY2 - 1; y >= fromY2; y--) {
			// Get block of random length and pick one of it's points. Destroy
			// northern wall of that point to connect it to the rest of maze.
			int blockLength = 0;
			for (int x = fromX2; x < toX2; x++) {
				// If connect to the next...
				if (rand.nextBoolean()) {
					// Connect next step to current block
					map[x][y].setNeighbour(map[x + 1][y], KeyLockDirection.EAST);
					blockLength++;
				} else {
					// Pick one of steps in the block and destroy it's northern 
					// wall.
					int offset = (blockLength > 0) ? rand.nextInt(blockLength) : 0;
					map[x - offset][y].setNeighbour(map[x - offset][y + 1], KeyLockDirection.NORTH);
					blockLength = 0;
				}
			}
			
			// Last cycle iteration is a special case
			if (blockLength > 0) {
				// Pick one of steps in the block and destroy it's northern 
				// wall.
				int offset = rand.nextInt(blockLength);
				map[toX2 - offset][y].setNeighbour(map[toX2 - offset][y + 1], KeyLockDirection.NORTH);
			} else {
				map[toX2][y].setNeighbour(map[toX2][y + 1], KeyLockDirection.NORTH);
			}
		}
		
		// If the maze consists of two halves
		if (toY2 > 0) {
			// Connect randomly both halves
			int randomY = rand.nextInt(toY2);
			map[toX1][randomY].setNeighbour(map[fromX2][randomY], KeyLockDirection.EAST);
		}
		
		// <-----------------
		// <-- Second half --
		// <-----------------
		
		return map;
	}
	
	/**
	 * Gives true if <code>min &lt;= value &lt= max</code>
	 * @param value Value to examine.
	 * @param min Lower bound (inclusive).
	 * @param max Upper bound (inclusive).
	 * @return True if <code>min &lt;= value &lt= max</code>
	 */
	public static boolean isBetweenOrEqual(Comparable value, Comparable min, Comparable max) {
		return min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
	}
	
	/**
	 * <p>Randomly sorts items of array.</p>
	 * <p><b>HINT:</b> If you want to sort array of any other objects than int, use array of indices instead of array of objects.</p>
	 * @param array Array of integers to be sorted
	 * @param rand Random generator
	 * @return Array of int (containing same items as array given as param), but randomly sorted.
	 */
	public static int[] randomSort(int[] array, Random rand) {
		// Make copy of input array
		int[] result = new int[array.length];
		System.arraycopy(array, 0, result, 0, array.length);
		
		// Sort the result array randomly
		for (int i = 1; i < result.length; i++) {
			int y = rand.nextInt(i);
			int tmp = result[i];
			result[i] = result[y];
			result[y] = tmp;
		}
		return result;
	}
	
	/**
	 * <p>Gets direction of shortest way from one point to another.</p>
	 * @param coordsFrom Coordinates of start point
	 * @param coordsTo Coordinates of finish point
	 * @return <p>{@link KeyLockDirection}
	 * <ul>
	 *	<li>KeyLockDirection.NORTH when (x, y + 1)</li>
	 *	<li>KeyLockDirection.SOUTH when (x, y - 1)</li>
	 *	<li>KeyLockDirection.WEST when (x - 1, y)</li>
	 *	<li>KeyLockDirection.EAST when (x + 1, y)</li>
	 *	<li>null otherwise</li>
	 * </ul>
	 * where (x, y) are coordinates of start point and modified coordinates are coordinates of finish point.
	 */
	public static KeyLockDirection getDirection(TupleInt coordsFrom, TupleInt coordsTo) {
		if (coordsFrom.x() < coordsTo.x()) {
			if (coordsFrom.y() == coordsTo.y()) {
				return KeyLockDirection.EAST;
			}
		} else if (coordsFrom.x() > coordsTo.x()) {
			if (coordsFrom.y() == coordsTo.y()) {
				return KeyLockDirection.WEST;
			}
		} else {
			if (coordsFrom.y() < coordsTo.y()) {
				return KeyLockDirection.NORTH;
			} else if (coordsFrom.y() > coordsTo.y()) {
				return KeyLockDirection.SOUTH;
			}
		}
		return null;
	}
	
	/**
	 * <p>If the program is in debug mode, prints given message to standard output.</p>
	 * @param msg Message to be printed to stdout
	 */
	public static void printDebugMsg(String msg) {
		if (KeyLockOptions.debug) {
			System.out.println("DEBUG: " + msg);
		}
	}
}
