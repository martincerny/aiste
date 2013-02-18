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
import java.util.HashMap;
import java.util.Map;

/**
 * An environment that is synchronized. The actions passed to the environment are gathered and
 * then synchronously retrieved upon performing a simulation step. Only one action per agent body
 * is retained, if multiple actions are invoked for a single body between successive simulation steps,
 * the last one is taken.
 * @author Martin Cerny
 */
public abstract class AbstractSynchronizedEnvironment<ACTION extends IAction> extends AbstractEnvironment<ACTION>{
    
    
    //TODO - change to array indexed by body id
    private Map<AgentBody, ACTION> actionsForNextStep = new HashMap<AgentBody, ACTION>();
    
    /**
     * Mutex for synchronization of access to actionsForNextStep.
     */
    private final Object mutex = new Object();

    /**
     * Constructor that creates a shallow copy of data using {@link AbstractEnvironment#AbstractEnvironment(cz.cuni.amis.aiste.environment.impl.AbstractEnvironment) }
     * @param original 
     */
    protected AbstractSynchronizedEnvironment(AbstractSynchronizedEnvironment original){
        super(original);
    }
    
    public AbstractSynchronizedEnvironment(Class<ACTION> actionClass) {
        super( actionClass);
    }
    
    
    

    @Override
    protected Map<AgentBody, Double> nextStepInternal() {
        Map<AgentBody,ACTION> actionsCopy;
        synchronized (mutex){
            actionsCopy = new HashMap<AgentBody, ACTION>(actionsForNextStep);
            actionsForNextStep.clear();
        }
        Map<AgentBody, Double> result = nextStepInternal(actionsCopy);
        return result;
    }

    @Override
    public boolean act(AgentBody agentBody, ACTION action) {
        if(!isActionRecognized(agentBody, action)){
            return false;
        }
        if(getRemovedBodies().contains(agentBody)){
            return false;
        }
        synchronized(mutex){
            actionsForNextStep.put(agentBody, action);
        }
        return true;
    }

    @Override
    public void init() {
        super.init();
        synchronized(mutex){
            actionsForNextStep = new HashMap<AgentBody, ACTION>();
        }
    }

    @Override
    public void stop() {
        super.stop();
        synchronized(mutex){
            actionsForNextStep.clear();
        }
    }

    
    
    /**
     * Test, whether the action is recognized. This is useful to get return value for {@link #act(cz.cuni.amis.aiste.AgentBody, java.lang.Object) }
     * @param agentBody
     * @param action
     * @return the default implementation always returns true.
     */
    protected boolean isActionRecognized(AgentBody agentBody, ACTION action){
        return true;
    }
    
    /**
     * Simulate one step with the actions gathered for the step.
     * @param actionsToPerform
     * @return 
     */
    protected abstract Map<AgentBody, Double> nextStepInternal(Map<AgentBody, ACTION> actionsToPerform);
}
