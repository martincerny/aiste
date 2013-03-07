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
package cz.cuni.amis.aiste.simulations.spyvsspy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyMapNode implements Serializable {

    /**
     * The indices of traps set at the given location
     */
    Set<Integer> traps;

    /**
     * The indices of items that can be pickud up at given location
     */
    Set<Integer> items;

    /**
     * Number of weapons that may be pickud up at given location
     */
    int numWeapons;
    
    /**
     * The indices of trap removers that can be picked up at given location
     */
    int[] numTrapRemovers;

    /**
     * The unique identifier of this node
     */
    int index;
    
    int posX;
    
    int posY;

    /**
     * Create a node with a copy of original data
     * @param original 
     */
    public SpyVsSpyMapNode(SpyVsSpyMapNode original){
        this.index = original.index;
        this.traps = new HashSet<Integer>(original.traps);
        this.items = new HashSet<Integer>(original.items);
        this.posX = original.posX;
        this.posY = original.posY;
        this.numWeapons = original.numWeapons;
        this.numTrapRemovers =  Arrays.copyOf(original.numTrapRemovers, original.numTrapRemovers.length);
    }
    
    public SpyVsSpyMapNode(int index, Set<Integer> traps, Set<Integer> items, Set<Integer> trapRemovers, int numTraps, int numWeapons, int posX, int posY) {
        this.index = index;
        this.traps = new HashSet<Integer>(traps);
        this.items = new HashSet<Integer>(items);
        numTrapRemovers = new int[numTraps];
        for (int trapRemoverIndex : trapRemovers) {
            numTrapRemovers[trapRemoverIndex]++;
        }
        this.posX = posX;
        this.posY = posY;
        this.numWeapons = numWeapons;
    }

    public SpyVsSpyMapNode(int index, int numTraps, int posX, int posY) {
        this.index = index;
        this.traps = new HashSet<Integer>();
        this.items = new HashSet<Integer>();
        numTrapRemovers = new int[numTraps];
        this.posX = posX;
        this.posY = posY;
    }

    public int getIndex() {
        return index;
    }

    public Set<Integer> getItems() {
        return items;
    }

    public int[] getNumTrapRemovers() {
        return numTrapRemovers;
    }

    public Set<Integer> getTraps() {
        return traps;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }   

    public int getNumWeapons() {
        return numWeapons;
    }
    
    @Override
    public String toString() {
        return "MapNode index: " + index + " at [" + posX + "," + posY + "], traps:" + traps + ", items: " + items + ", trapRemovers: " + Arrays.toString(numTrapRemovers) + ", numWeapons: " + numWeapons;
    }
}
