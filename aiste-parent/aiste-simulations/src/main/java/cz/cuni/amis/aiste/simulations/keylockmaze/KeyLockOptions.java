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
 * <p>This class encapsulates all options, that can be used in {@link KeyLockMaze} and it's related classes.</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockOptions {
	
	/**
	 * <p>Defines the width of maze (how many rooms will be the maze wide)..<br />
	 * <b>Minimal value is 3!</b> (otherwise, exception is thrown while generating maze)</p>
	 */
	public int roomsWide = 3;
	
	/**
	 * <p>Defines the height of maze (how many rooms will be the maze high)..<br />
	 * <b>Minimal value is 3!</b> (otherwise, exception is thrown while generating maze)</p>
	 */
	public int roomsHigh = 3;
	
	/**
	 * <p>Defines the width of each room (how many steps will be the room wide)..<br />
	 * <b>Minimal value is 3!</b> (otherwise, exception is thrown while generating maze)</p>
	 */
	public int roomWidth = 5;
	
	/**
	 * <p>Defines the height of each room (how many steps will be the room high)..<br />
	 * <b>Minimal value is 3!</b> (otherwise, exception is thrown while generating maze)</p>
	 */
	public int roomHeight = 5;
	
	/**
	 * <p>Represents the numerator of locked door probability.<br />
	 * i.e. Doors will be locked with probability <code>lockedRatio / lockedRatioMax</code>.</p>
	 * <p><code>IF lockedRatio &ge; lockedRatioMax THEN</code> all door will be locked.</p>
	 * @see #lockedRatioMax
	 * @see KeyLockMaze#lockedOrUnlocked() use
	 */
	public int lockedRatio = 10;
	
	/**
	 * <p>Represents the divisor of locked door probability.<br />
	 * i.e. Doors will be locked with probability <code>lockedRatio / lockedRatioMax</code>.</p>
	 * @see #lockedRatio
	 * @see KeyLockMaze#lockedOrUnlocked() use
	 */
	public int lockedRatioMax = 10;
	
	/**
	 * <p>Seed of the random generator. If set to null, the generator will get random seed</p>
	 */
	public Long randomSeed = null;
	
	/**
	 * <p>Flag that means: "Should the program print debug messages to stdout?"</p>
	 */
	public static final boolean debug = true;
}
