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
package cz.cuni.amis.aiste.environment;

import java.util.List;
import java.util.Map;

/**
 * A class that represents a model of an environment. This is useful especifally
 * for predicting outcomes of agent actions and search.
 * @author Martin Cerny
 */
public interface IEnvironmentModel<ACTION extends IAction> extends Cloneable{ 
    /**
     * Get all agent bodies present in the environment
     * @return 
     */
    List<AgentBody> getAllBodies();
    
    Map<AgentBody, Double> simulateOneStep(Map<AgentBody, ACTION> actionsToPerform);    
    
    /**
     * Clone the model for search purposes. 
     * Should only clone information that may actually change. This method should be
     * as fast as possible, since it might be called very often.
     * @return 
     */
    IEnvironmentModel<? extends ACTION> clone();
    
    /**
     * Whether the simulation has reached a terminal state
     * @return 
     */
    boolean isFinished();  
    
    /**
     * Removes agent body from the simulation. An environment specific failure-reward should
     * be given to the agent. This is usually -Inf for environments where there is
     * no lower bound on reward agent may accumulate or -SOME_INTEGER for environments
     * where doing nothing does not hurt the agent.
     * Useful especially to remove bodies of agents that performed some sort of illegal operation
     * (e. g. exception in agent logic).
     */
    void removeAgentBody(AgentBody body);    
    
}
