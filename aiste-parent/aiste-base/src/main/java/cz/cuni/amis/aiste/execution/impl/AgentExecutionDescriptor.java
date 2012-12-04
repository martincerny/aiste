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
package cz.cuni.amis.aiste.execution.impl;

import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;

/**
 *
 * @author Martin Cerny
 */
public class AgentExecutionDescriptor implements IAgentExecutionDescriptor{
    private IAgentType agentType;
    
    private Class<IAgentController> controllerClass;
    
    private int count;

    public AgentExecutionDescriptor(IAgentType agentType, Class<IAgentController> controllerClass) {
        this(agentType, controllerClass, 1);
    }

    
    
    public AgentExecutionDescriptor(IAgentType agentType, Class<IAgentController> controllerClass, int count) {
        this.agentType = agentType;
        this.controllerClass = controllerClass;
        this.count = count;
    }

    @Override
    public IAgentType getAgentType() {
        return agentType;
    }

    @Override
    public Class<IAgentController> getControllerClass() {
        return controllerClass;
    }

    @Override
    public int getCount() {
        return count;
    }
    
    
}
