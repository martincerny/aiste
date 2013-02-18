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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IPDDLRepresentation;
import cz.cuni.amis.aiste.environment.impl.AbstractModelEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractStateVariableRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.pddl.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpy extends AbstractModelEnvironment<SpyVsSpyAction>
        implements IRandomizable {

    private final Logger logger = Logger.getLogger(SpyVsSpy.class);

    private SpyVsSpyModel model;
    


    public SpyVsSpy(List<MapNode> nodes, int maxPlayers, List<Integer> startingLocations, Map<Integer, List<Integer>> neighbours, int numTrapTypes, int[]trapCounts, int numItemTypes, int destination) {
        super(SpyVsSpyAction.class);

        model = new SpyVsSpyModel(nodes, maxPlayers, startingLocations, neighbours, numTrapTypes, trapCounts, numItemTypes, destination);
        
    }

    @Override
    public void setRandomSeed(long seed) {
        model.setRandomSeed(seed);        
    }
    
    

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(SpyVsSpyAgentType.getInstance(), new AgentInstantiationDescriptor(1, maxPlayers));
    }

}
