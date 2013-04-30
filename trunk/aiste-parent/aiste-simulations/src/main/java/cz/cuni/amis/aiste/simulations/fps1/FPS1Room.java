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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class FPS1Room {
    
    int id;
    List<FPS1Room> neighbours;
    List<Integer> neighboursDistances;

    public static void connect(FPS1Room r1, FPS1Room r2, int distance){
        r1.addNeighbour(r2, distance);
        r2.addNeighbour(r1, distance);        
    }
    
    public FPS1Room(int id) {
        this.id = id;
        neighbours = new ArrayList<FPS1Room>();
        neighboursDistances = new ArrayList<Integer>();
    }

    
    public void addNeighbour(FPS1Room neighbour, int distance){
        neighbours.add(neighbour);
        neighboursDistances.add(distance);
    }


    public List<FPS1Room> getNeighbours() {
        return neighbours;
    }
    
}
