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
 * <p>Tuple of two comparable objects.</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 * @param <X> {@link Comparable} class
 * @param <Y> {@link Comparable} class
 */
public class TupleComparable<X extends Comparable<? super X>, Y extends Comparable<? super Y>> implements Comparable<TupleComparable<X, Y>> {
	
	/**
	 * <p>First item</p>
	 */
	public final X x;
	
	/**
	 * <p>Second item</p>
	 */
	public final Y y;
	
	/**
	 * <p>Constructor</p>
	 * @param x First item
	 * @param y Second item
	 */
	public TupleComparable(X x, Y y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * <p>Compares two instances of {@link TupleComparable}. Returns result of comparison of inner items.</p>
	 * @param t Another TupleComparable to be compared with this one.
	 * @return Zero if both items comparison returned zero.
	 */
	@Override
	public int compareTo(TupleComparable<X, Y> t) {
		int d = this.x.compareTo(t.x);
		if (d == 0) {
			return this.y.compareTo(t.y);
		}
		return d;
	}
	
}
