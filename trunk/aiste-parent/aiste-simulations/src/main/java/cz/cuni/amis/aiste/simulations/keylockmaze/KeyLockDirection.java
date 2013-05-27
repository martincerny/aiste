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

/**
 * <p>Enum to represent direction in {@link KeyLockMaze}.</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public enum KeyLockDirection {
	
	/**
	 * <p>North</p>
	 */
	NORTH(0, new int[]{0, 1}),
	/**
	 * <p>South</p>
	 */
	SOUTH(1, new int[]{0, -1}),
	/**
	 * <p>West</p>
	 */
	WEST(2, new int[]{-1, 0}),
	/**
	 * <p>East</p>
	 */
	EAST(3, new int[]{1, 0});
	
	
	/**
	 * <p>Code of the direction</p>
	 */
	private final int code;
	/**
	 * <p>Coordinate differences for each direction.</p>
	 */
	private final int[] diff;
	
	/**
	 * <p>Constructor defining the direction code and direction coordinate differences.</p>
	 * @param c Code of the direction
	 * @param dirDiff Coordinate difference of the direction
	 */
	private KeyLockDirection(int c, int[] dirDiff) {
		this.code = c;
		this.diff = dirDiff;
	}
	
	/**
	 * <p>Direction code getter.</p>
	 * @return int that represents code of the direction
	 */
	public int getCode() {
		return this.code;
	}
	
	/**
	 * <p>X-axis coordinate difference getter</p>
	 * @return int that is needed to add to actual point's x-axis coord to get coordinates of next point in specified direction
	 */
	public int getDiffX() {
		return this.diff[0];
	}
	
	/**
	 * <p>Y-axis coordinate difference getter</p>
	 * @return int that is needed to add to actual point's y-axis coord to get coordinates of next point in specified direction
	 */
	public int getDiffY() {
		return this.diff[1];
	}
}
