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
package cz.cuni.amis.aiste.impl;

import cz.cuni.amis.aiste.IAgentBody;
import java.util.HashMap;
import java.util.Map;

/**
 * An environment that is synchronized. The actions passed to the environment are gathered and
 * then synchronously retrieved upon performing a simulation step. Only one action per agent body
 * is retained, if multiple actions are invoked for a single body between successive simulation steps,
 * the last one is taken.
 * @author Martin Cerny
 */
public abstract class AbstractSynchronizedEnvironment<BODY extends IAgentBody, ACTION> extends AbstractEnvironment<BODY, ACTION>{
    
    
    private Map<BODY, ACTION> actionsForNextStep = new HashMap<BODY, ACTION>();
    
    /**
     * Mutex for synchronization of access to actionsForNextStep.
     */
    private final Object mutex = new Object();

    public AbstractSynchronizedEnvironment(Class<BODY> bodyClass, Class<ACTION> actionClass) {
        super(bodyClass, actionClass);
    }
    
    
    

    @Override
    protected Map<BODY, Double> simulateOneStepInternal() {
        Map<BODY,ACTION> actionsCopy;
        synchronized (mutex){
            actionsCopy = new HashMap<BODY, ACTION>(actionsForNextStep);
            actionsForNextStep.clear();
        }
        Map<BODY, Double> result = simulateOneStepInternal(actionsCopy);
        return result;
    }

    @Override
    public boolean act(BODY agentBody, ACTION action) {
        if(!isActionRecognized(agentBody, action)){
            return false;
        }
        synchronized(mutex){
            actionsForNextStep.put(agentBody, action);
        }
        return true;
    }

    /**
     * Test, whether the action is recognized. This is useful to get return value for {@link #act(cz.cuni.amis.aiste.IAgentBody, java.lang.Object) }
     * @param agentBody
     * @param action
     * @return the default implementation always returns true.
     */
    protected boolean isActionRecognized(BODY agentBody, ACTION action){
        return true;
    }
    
    /**
     * Simulate one step with the actions gathered for the step.
     * @param actionsToPerform
     * @return 
     */
    protected abstract Map<BODY, Double> simulateOneStepInternal(Map<BODY, ACTION> actionsToPerform);
}
