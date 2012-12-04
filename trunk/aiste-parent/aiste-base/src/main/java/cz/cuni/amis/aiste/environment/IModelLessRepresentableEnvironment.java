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
package cz.cuni.amis.aiste.environment;

import java.util.Collection;

/**
 * An environment, where agents may be controlled in a model-less way. Model-less
 * means that agents do not need to construct an explicit model of the environment to act
 * upon it. In such an environment, there is a finite number of possible actions and the
 * same actions are possible for a specific agent type at all simulation steps.
 * @author Martin Cerny
 */
public interface IModelLessRepresentableEnvironment<BODY extends IAgentBody, ACTION extends IAction, PERCEPT extends IPercept>
extends IEnvironment<BODY, ACTION> {    
    
    /**
     * Gets the class of the percept in this simulation. This value is important
     * to check whether a specific ModelLessController may be run with this environment.
     * @return 
     */
    Class<PERCEPT> getPerceptClass();
    
    /**
     * Get the current percept of the agent. May be called asynchronously to simulation execution (has to be thread-safe)
     * @param agentBody
     * @return 
     */
    PERCEPT getPercept(BODY agentBody);
    
    /**
     * Gets the actions possible for a specific agent type
     * @param agentType
     * @return 
     */
    Collection<ACTION> getPossibleActions(IAgentType agentType);
}
