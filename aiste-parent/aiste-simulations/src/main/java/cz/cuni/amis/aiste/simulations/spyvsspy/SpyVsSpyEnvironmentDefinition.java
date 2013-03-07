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
import java.util.List;
import java.util.Map;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyEnvironmentDefinition implements Serializable{
    List<SpyVsSpyMapNode> nodes;
    int maxPlayers; 
    List<Integer> startingLocations; 
    Map<Integer, List<Integer>> neighbours; 
    int numTrapTypes; 
    int[] trapCounts; 
    int numItemTypes; 
    int destination;

    public SpyVsSpyEnvironmentDefinition(List<SpyVsSpyMapNode> nodes, int maxPlayers, List<Integer> startingLocations, Map<Integer, List<Integer>> neighbours, int numTrapTypes, int[] trapCounts, int numItemTypes, int destination) {
        this.nodes = nodes;
        this.maxPlayers = maxPlayers;
        this.startingLocations = startingLocations;
        this.neighbours = neighbours;
        this.numTrapTypes = numTrapTypes;
        this.trapCounts = trapCounts;
        this.numItemTypes = numItemTypes;
        this.destination = destination;
    }
    
    
}
