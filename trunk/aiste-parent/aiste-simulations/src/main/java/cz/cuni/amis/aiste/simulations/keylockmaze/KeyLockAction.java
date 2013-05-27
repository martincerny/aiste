/*
 * Copyright (C) 2012 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
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

import cz.cuni.amis.aiste.environment.IAction;

/**
 * <p>Action performed by a spy in {@link KeyLockMaze}.</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockAction implements IAction{

	/**
	 * <p>Direction of spy's move</p>
	 */
	private KeyLockDirection direction;
	
	/**
	 * <p>Constructor</p>
	 * @param directionToGoTo Direction of spy's move
	 */
	public KeyLockAction(KeyLockDirection directionToGoTo) {
		direction = directionToGoTo;
	}

	/**
	 * <p>Returns direction of spy's move</p>
	 * @return Spy's move direction
	 */
	public KeyLockDirection getDirection() {
		return direction;
	}
    
    @Override
    public String getLoggableRepresentation() {
        return "ACTION -> direction '" + direction + "' ";
    }
    
}
