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
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;

/**
 * Information needed to execute an agent in an environment.
 * This interface is deliberately not generic, as there is no benefit of it.
 * @author Martin Cerny
 */
public interface IAgentExecutionDescriptor {
    /**
     * Get the (environment specific) type of the agent 
     */
    IAgentType getAgentType();
    
    /**
     * Get the class of the controller to be instantiated
     * @return 
     */
    IAgentController getController();
    
    IEnvironmentRepresentation getRepresentation();
    
    /**
     * String for logging purposes
     * @return 
     */
    public String getLoggableReperesentation();
}
