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
package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.aiste.environment.impl.SimpleBody;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that describes an agent body in SpyVsSpy.
 * Most of the methods have package visiblity only - so that classes
 * outside the environment cannot look at the internals.
 * @author 
 */
public class SpyVsSpyAgentBody extends SimpleBody {
    
    int locationIndex;
    
    int[] numTrapsCarried;
    
    Set<Integer> itemsCarried;
    
    int[] numTrapRemoversCarried;
    
    SpyVsSpyAgentBody(int id, int initialLocation, int numTraps, int[] numTrapsCarried) {
        super(id, SpyVsSpyAgentType.getInstance());
        locationIndex = initialLocation;
        this.numTrapsCarried = Arrays.copyOf(numTrapsCarried, numTrapsCarried.length);
        numTrapRemoversCarried = new int[numTraps];
        itemsCarried = new HashSet<Integer>();
    }

    @Override
    public String toString() {
        
        return "SpyVsSpyAgentBody id: " + getId() + ", locationIndex: " + locationIndex + ", numTrapsCarried: " + Arrays.toString(numTrapsCarried) + ", itemsCarried: " + itemsCarried + ", numTrapRemoversCarried: " + Arrays.toString(numTrapRemoversCarried);
    }
    
    
    
}
