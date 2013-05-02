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

package cz.cuni.amis.aiste.simulations.covergame;

import cz.cuni.amis.aiste.simulations.fps1.*;

/**
 *
 * @author Martin Cerny
 */
public class Loc {
    public final int x;
    public final int y;

    public Loc(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double distanceTo(Loc other){
        return CGUtils.distance(x, y, other.x, other.y);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Loc other = (Loc) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + this.x;
        hash = 73 * hash + this.y;
        return hash;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + "]";
    }
    
    
    
}
