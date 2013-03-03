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
package cz.cuni.amis.aiste.execution;

import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;

/**
 * Interface for classes that execute environments and agent controllers. 
 * Executors are generally for single use only. This method is intentionally not
 * generic, as there is no benefit to it.
 * @author Martin Cerny
 */
public interface IEnvironmentExecutor {
    
    /**
     * Set the environment for the executor. Can be called only once on a specific
     * executor instance.
     * @param environment 
     */
    void setEnvironment(IEnvironment environment);
    
    /**
     * Adds a controller to the list of managed controllers and calls {@link IAgentController#init(cz.cuni.amis.aiste.environment.IEnvironment, cz.cuni.amis.aiste.environment.IAgentBody, long) }
     * for it.
     * @param type
     * @param controller 
     */
    void addAgentController(IAgentExecutionDescriptor executionDescriptor);
    
    /**
     * Executes the environment, until it reaches a terminal state.
     */
    IEnvironmentExecutionResult executeEnvironment();
    
    /**
     * Executes the environment, until it reaches a terminal state or a maximum number
     * of simulation steps is performed.
     * @param maxSteps maximum number of steps or zero for infinity
     */
    IEnvironmentExecutionResult executeEnvironment(long maxSteps);
}
