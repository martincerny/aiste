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
 * <p>This is base class for all {@link KeyLockMaze} parts. It contains ID and four pointers to
 * the neighbours.</p>
 * 
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockFourWayPoint implements Comparable<KeyLockFourWayPoint> {
	
	/**
	 * <p>ID of current point <b>(should be unique because of comparison)</b></p>
	 */
	protected int id;
	
	/**
	 * <p>Pointer to northern neighbour</p>
	 */
	public KeyLockFourWayPoint north = null;	// Code: 0
	
	/**
	 * <p>Pointer to southern neighbour</p>
	 */
	public KeyLockFourWayPoint south = null;	// Code: 1
	
	/**
	 * <p>Pointer to western neighbour</p>
	 */
	public KeyLockFourWayPoint west = null;	// Code: 2
	
	/**
	 * <p>Pointer to eastern neighbour</p>
	 */
	public KeyLockFourWayPoint east = null;	// Code: 3
	
	/**
	 * <p>Constructor, that sets ID of currently created KeyLockFourWayPoint.</p>
	 * @param id 
	 */
	public KeyLockFourWayPoint(int id) {
		this.id = id;
	}

	/**
	 * <p>Determines whether the specified neighbour slot is free or not.</p>
	 * @param direction Direction code of neighbour that you want to check<br/>(0 = north, 1 = south, 2 = west, 3 = east)
	 * @return True, if specified directionCode is in range 0-3 & slot in that direction is free (&lt;=the pointer is null). False otherwise.
	 */
	public boolean isNeighbourSlotFree(KeyLockDirection direction) {
		switch (direction.getCode()) {
			case 0:
				return this.north == null;
			case 1:
				return this.south == null;
			case 2:
				return this.west == null;
			case 3:
				return this.east == null;
		}
		return false;
	}
	
	/**
	 * <p>Checks if current {@link FourWayPoint} has specified FourWayPoint as it's neighbour and returns {@link KeyLockDirection} where that point is connected.</p>
	 * <p>Example:<br />
	 * <code>
	 * &nbsp;&nbsp;&nbsp;FourWayPoint p1 = new FourWayPoint(0);<br />
	 * &nbsp;&nbsp;&nbsp;FourWayPoint p2 = new FourWayPoint(1);<br />
	 * &nbsp;&nbsp;&nbsp;// p1.hasNeighbourOnIndex(p2) returns null<br />
	 * &nbsp;&nbsp;&nbsp;p1.south = p2;<br />
	 * &nbsp;&nbsp;&nbsp;// p1.hasNeighbourOnIndex(p2) returns KeyLockDirection.SOUTH</code></p>
	 * 
	 * @param point FourWayPoint to check
	 * @return Direction, where the specified point is connected or null (if it's not connected)
	 */
	public KeyLockDirection hasNeighbourInDirection(KeyLockFourWayPoint point) {
		if (this.north != null && this.north.compareTo(point) == 0) { return KeyLockDirection.NORTH; }
		if (this.south != null && this.south.compareTo(point) == 0) { return KeyLockDirection.SOUTH; }
		if (this.west != null && this.west.compareTo(point) == 0) { return KeyLockDirection.WEST; }
		if (this.east != null && this.east.compareTo(point) == 0) { return KeyLockDirection.EAST; }
		return null;
	}

	/**
	 * <p>Sets given point as neighbour to this point in specified direction. And also sets this as neighbour to the given one.</p>
	 * @param neighbour Point, that will be new neighbour
	 * @param direction Direction in which new neighbour will be connected
	 */
	public void setNeighbour(KeyLockFourWayPoint neighbour, KeyLockDirection direction) {
		// North
		if (direction.compareTo(KeyLockDirection.NORTH) == 0) {
			if (this.north == null) {
				this.north = neighbour;
			}
			neighbour.addNeighbourOnly(this, KeyLockDirection.SOUTH);
		}
		// South
		else if (direction.compareTo(KeyLockDirection.SOUTH) == 0) {
			if (this.south == null) {
				this.south = neighbour;
			}
			neighbour.addNeighbourOnly(this, KeyLockDirection.NORTH);
		}
		// West
		else if (direction.compareTo(KeyLockDirection.WEST) == 0) {
			if (this.west == null) {
				this.west = neighbour;
			}
			neighbour.addNeighbourOnly(this, KeyLockDirection.EAST);
		}
		// East
		else if (direction.compareTo(KeyLockDirection.EAST) == 0) {
			if (this.east == null) {
				this.east = neighbour;
			}
			neighbour.addNeighbourOnly(this, KeyLockDirection.WEST);
		}
	}

	/**
	 * <p>Sets given point as neighbour to this point in specified direction. But does NOT set this as neighbour to the given one.</p>
	 * @param neighbour Point, that will be new neighbour
	 * @param direction Direction in which new neighbour will be connected
	 */
	private void addNeighbourOnly(KeyLockFourWayPoint neighbour, KeyLockDirection direction) {
		// North
		if (direction.compareTo(KeyLockDirection.NORTH) == 0) {
			if (this.north == null) {
				this.north = neighbour;
			}
		}
		// South
		else if (direction.compareTo(KeyLockDirection.SOUTH) == 0) {
			if (this.south == null) {
				this.south = neighbour;
			}
		}
		// West
		else if (direction.compareTo(KeyLockDirection.WEST) == 0) {
			if (this.west == null) {
				this.west = neighbour;
			}
		}
		// East
		else if (direction.compareTo(KeyLockDirection.EAST) == 0) {
			if (this.east == null) {
				this.east = neighbour;
			}
		}
	}
	
	/**
	 * <p>Returns {@link KeyLockFourWayPoint} that is held in specified direction of this point.</p>
	 * @param direction Direction that we want the point from
	 * @return KeyLockFourWayPoint that is held in specified direction
	 */
	public KeyLockFourWayPoint getNeighbour(KeyLockDirection direction) {
		switch (direction.getCode()) {
			
			case 0:	// North
				return this.north;
			
			case 1:	// South
				return this.south;
			
			case 2:	// West
				return this.west;
			
			case 3:	// East
			default:
				return this.east;
		}
	}

	
	/***************************************************************
	 * Implementation of Comparable<KeyLockFourWayPoint> functions *
	 ***************************************************************/
	
	/**
	 * Checks if two instances of FourWayPoint are equal (compares their ID's).
	 * 
	 * @param p Another FourWayPoint to compare with the current one
	 * @return Zero if ID's are equal, something else otherwise.
	 */
	@Override
	public int compareTo(KeyLockFourWayPoint p) {
		return this.id - p.id;
	}
}
