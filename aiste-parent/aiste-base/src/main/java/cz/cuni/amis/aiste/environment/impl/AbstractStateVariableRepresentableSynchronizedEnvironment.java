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
package cz.cuni.amis.aiste.environment.impl;

import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IStateVariable;
import cz.cuni.amis.aiste.environment.IStateVariableRepresentation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractStateVariableRepresentableSynchronizedEnvironment<ACTION extends IAction>
    extends AbstractSynchronizedEnvironment<ACTION> implements IStateVariableRepresentation<ACTION> {

    private Map<IStateVariable, Object> stateVariableValues;
    
    public AbstractStateVariableRepresentableSynchronizedEnvironment(Class<ACTION> actionClass) {
        super( actionClass);
        stateVariableValues = new HashMap<IStateVariable, Object>();
    }
    
    protected void addStateVariable(IStateVariable var){
        if(stateVariableValues.containsKey(var)){
            throw new IllegalArgumentException("Variable " + var.getName() + " already added");
        }
        stateVariableValues.put(var, null);
    }
    
    protected void setStateVariableValue(IStateVariable var, Object value){
        if(!stateVariableValues.containsKey(var)){
            throw new IllegalArgumentException("Variable " + var.getName() + " not found");
        }
        stateVariableValues.put(var, value);
    }

    @Override
    public Object getStateVariableValue(IStateVariable variable) {
        Object value = stateVariableValues.get(variable);
        if(value == null){
            throw new IllegalStateException("Variable " + variable.getName() + " has no defined value");
        }
        return value;
    }

    @Override
    public Collection<IStateVariable> getStateVariables() {
        return stateVariableValues.keySet();
    }
    
    
    
}
