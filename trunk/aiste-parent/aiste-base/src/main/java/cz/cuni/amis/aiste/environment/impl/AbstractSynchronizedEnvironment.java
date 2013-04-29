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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * An environment that is synchronized. The actions passed to the environment are gathered and
 * then synchronously retrieved upon performing a simulation step. Only one action per agent body
 * is retained, if multiple actions are invoked for a single body between successive simulation steps,
 * the last one is taken.
 * Any code that does not wish to be interrupted by environment updates should synchronize on the 
 * environment instance.
 * @author Martin Cerny
 */
public abstract class AbstractSynchronizedEnvironment<ACTION extends IAction> extends AbstractEnvironment<ACTION>{
    
    private final Logger logger = Logger.getLogger(AbstractSynchronizedEnvironment.class);
    
    private Map<AgentBody, ACTION> actionsForNextStep = new HashMap<AgentBody, ACTION>();
    
    /**
     * Mutex for synchronization of access to actionsForNextStep.
     */
    private final Object actionsMutex = new Object();

    /**
     * Helper to support action failures
     */
    private Map<AgentBody, Long> lastAgentActionFailure;

    /**
     * Constructor that creates a shallow copy of data using {@link AbstractEnvironment#AbstractEnvironment(cz.cuni.amis.aiste.environment.impl.AbstractEnvironment) }
     * @param original 
     */
    protected AbstractSynchronizedEnvironment(AbstractSynchronizedEnvironment original){
        super(original);
        this.lastAgentActionFailure = new HashMap<AgentBody, Long>(original.lastAgentActionFailure);
    }
    
    public AbstractSynchronizedEnvironment(Class<ACTION> actionClass) {
        super( actionClass);
    }
    
    
    

    @Override
    protected Map<AgentBody, Double> nextStepInternal() {
        Map<AgentBody,ACTION> actionsCopy;
        synchronized (actionsMutex){
            actionsCopy = new HashMap<AgentBody, ACTION>(actionsForNextStep);
            actionsForNextStep.clear();
        }
        synchronized(this){
            Map<AgentBody, Double> result = nextStepInternal(actionsCopy);
            return result;
        }
    }

    @Override
    public boolean act(AgentBody agentBody, ACTION action) {
        if(!isActionRecognized(agentBody, action)){
            return false;
        }
        if(getRemovedBodies().contains(agentBody)){
            return false;
        }
        synchronized(actionsMutex){
            actionsForNextStep.put(agentBody, action);
        }
        return true;
    }

    @Override
    public void init() {
        super.init();
        lastAgentActionFailure  = new HashMap<AgentBody, Long>();
        synchronized(actionsMutex){
            actionsForNextStep = new HashMap<AgentBody, ACTION>();
        }        
    }

    @Override
    public void stop() {
        super.stop();
        synchronized(actionsMutex){
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
     * Implementations may call this to facilitate action failure detection.
     * This method is really thread-safe only when called for a properly registered agent body.
     * @param body 
     */
    protected void agentFailedAction(AgentBody body){
        lastAgentActionFailure.put(body, getTimeStep());
    }
    
    /**
     * If environment uses {@link #agentFailedAction(cz.cuni.amis.aiste.environment.AgentBody) } to notify
     * of failed actions, this method returns, whether action of an agent in last step failed.
     * @param body
     * @return 
     */       
    public boolean lastActionFailed(AgentBody body){
        //TODO should actually check, whether this is larger than time step of last action
        return lastAgentActionFailure.get(body) == getTimeStep();
    }

    @Override
    protected void afterAgentBodyCreated(AgentBody body) {
        super.afterAgentBodyCreated(body);
        //initialize the value, so that I do not need modify the map during executin, which is not thread safe
        lastAgentActionFailure.put(body, -1L);
    }
    
    
    
    /**
     * Simulate one step with the actions gathered for the step.
     * @param actionsToPerform
     * @return 
     */
    protected abstract Map<AgentBody, Double> nextStepInternal(Map<AgentBody, ACTION> actionsToPerform);
}
