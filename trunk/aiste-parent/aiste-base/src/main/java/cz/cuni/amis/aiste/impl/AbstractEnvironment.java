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

import cz.cuni.amis.aiste.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractEnvironment<BODY extends IAgentBody, ACTION> implements IEnvironment<BODY, ACTION> {
    private Class<BODY> bodyClass;
    private Class<ACTION> actionClass;

    private boolean finished = false;
    private List<BODY> bodies = new ArrayList<BODY>();
    private long timeStep = 0;
    private Map<BODY, Double> totalRewards = new HashMap<BODY, Double>();

    private Map<IAgentType, Integer> instanceCount = new HashMap<IAgentType,Integer>();

    public AbstractEnvironment(Class<BODY> bodyClass, Class<ACTION> actionClass) {
        this.bodyClass = bodyClass;
        this.actionClass = actionClass;
    }

    @Override
    public Map<BODY, Double> simulateOneStep() {
        if(isFinished()){
            throw new SimulationException("Trying to simulate a finished environment");
        }
        Map<BODY, Double> result = simulateOneStepInternal();
        for(Map.Entry<BODY, Double> rewardEntry : result.entrySet() ){
            totalRewards.put(rewardEntry.getKey(), totalRewards.get(rewardEntry.getKey()) + rewardEntry.getValue());
        }
        timeStep++;
        return result;
    }
    

    protected abstract Map<BODY, Double> simulateOneStepInternal();

    @Override
    public BODY createAgentBody(IAgentType type) {
        IAgentInstantiationDescriptor desc = getInstantiationDescriptors().get(type);
        
        if(desc == null){
            throw new AgentInstantiationException("Trying to instantiate an unregistered type: " + type);
        }
        
        Integer instancesSoFar = instanceCount.get(type);
        if(instancesSoFar == null){
            instancesSoFar = 0;
        }
        
        if(instancesSoFar >= desc.getMaxInstances()){
            throw new AgentInstantiationException("Trying to instantiate more agents of type " + type + " than allowed. The maxCount is " + desc.getMaxInstances() );
        }
        
        BODY newBody = createAgentBodyInternal(type);
                
        instanceCount.put(type, instancesSoFar + 1);
        bodies.add(newBody);        
        
        return newBody;
        
    }
    
    protected abstract BODY createAgentBodyInternal(IAgentType type);

    @Override
    public List<BODY> getAllBodies() {
        return bodies;
    }
    

    
    
    @Override
    public long getTimeStep() {
        return timeStep;
    }
    
    @Override
    public double getTotalReward(BODY agentBody) {
        return totalRewards.get(agentBody);
    }
    
    
    @Override
    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    
    
    
    @Override
    public Class<ACTION> getActionClass() {
        return actionClass;
    }

    @Override
    public Class<BODY> getBodyClass() {
        return bodyClass;
    }
    
    
    
}
