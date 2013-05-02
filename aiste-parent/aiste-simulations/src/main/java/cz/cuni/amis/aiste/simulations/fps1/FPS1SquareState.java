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

package cz.cuni.amis.aiste.simulations.fps1;

import cz.cuni.amis.aiste.simulations.fps1.FPS1.ItemType;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public class FPS1SquareState {
    Set<FPS1.ItemType> items;
    
    FPS1Square square;

    public FPS1SquareState() {
        items = EnumSet.noneOf(FPS1.ItemType.class);
    }
    
    public FPS1SquareState(FPS1.ItemType item1, FPS1.ItemType ... items){
        this.items = EnumSet.of(item1, items);
    }
    
    public FPS1SquareState(FPS1SquareState original){
        this.items = EnumSet.copyOf(original.items);
        this.square = original.square;
    }
}
