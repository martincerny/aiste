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
 * <p>Class that encapsulates all step rewards.</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockStepReward {
	
	/**
	 * <p>Reward of the empty step (the most usual type of step)</p>
	 */
	public static final Double emptyStep = -1.0;
	
	/**
	 * <p>Reward of the step containing key</p>
	 */
	public static final Double keyStep = emptyStep;
	
	/**
	 * <p>Reward of the start step</p>
	 */
	public static final Double startStep = emptyStep;
	
	/**
	 * <p>Reward of the finish step</p>
	 */
	public static final Double finishStep = 100.0;
	
	/**
	 * <p>Reward of the step containing door that is locked</p>
	 */
	public static final Double doorLockedStep = emptyStep;
	
	/**
	 * <p>Reward of the step containing door that is unlocked</p>
	 */
	public static final Double doorUnlockedStep = emptyStep;
}
