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
 * <p>Tuple of two ints</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class TupleInt implements Comparable<TupleInt> {

	/**
	 * <p>The item</p>
	 */
	private TupleComparable<Integer, Integer> item;
	
	/**
	 * <p>Constructor</p>
	 * @param x First int to store
	 * @param y Second int to store
	 */
	public TupleInt(int x, int y) {
		this.item = new TupleComparable<Integer, Integer>(x, y);
	}
	
	/**
	 * <p>First int getter</p>
	 * @return First int in this tuple
	 */
	public int x() {
		return this.item.x;
	}
	
	/**
	 * <p>Second int getter</p>
	 * @return Second int in this tuple
	 */
	public int y() {
		return this.item.y;
	}
	
	/**
	 * <p>Performs comparison over two {@link TupleInt} instances.</p>
	 * @param tuple TupleInt to be compared with this one
	 * @return Result of the inner item comparison
	 * @see TupleComparable#compareTo(cz.cuni.amis.aiste.simulations.keylockmaze.TupleComparable) 
	 */
	@Override
	public int compareTo(TupleInt tuple) {
		return this.item.compareTo(tuple.item);
	}
}
