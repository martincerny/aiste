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

import java.util.Set;

/**
 * A non-changing information traversable square on FPS1 map.
 * @author Martin Cerny
 */
public class FPS1Square {    
    /**
     * The corresponding room or null, if this is only a connecting corridor.
     */
    FPS1Room room;
    
    Loc location;
    
    /**
     * Item spawned at this location, or null if none.
     */
    FPS1.ItemType spawnedItem;    
}
