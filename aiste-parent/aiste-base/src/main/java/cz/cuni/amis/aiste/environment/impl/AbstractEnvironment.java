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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.SimulationException;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;
import cz.cuni.amis.experiments.IBareLoggingOutput;
import cz.cuni.amis.experiments.ILogDataProvider;
import cz.cuni.amis.experiments.ILogIdentifier;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.ClassLogIdentifier;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.NullLoggingOutput;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractEnvironment<ACTION extends IAction> implements IEnvironment<ACTION>, IRandomizable {

    private final Logger logger = Logger.getLogger(AbstractEnvironment.class);
    
    private Class<ACTION> actionClass;

    private boolean finished;
    private List<AgentBody> bodies;
    protected long timeStep;
    private Map<AgentBody, Double> totalRewards;
    
    private final double failureReward;
    private List<IEnvironmentRepresentation> representations;

    protected IBareLoggingOutput runtimeLoggingOutput;

    /**
     * Bodies that are active - ie. have been added and not yet removed.
     */
    private List<AgentBody> activeBodies;
    
    /**
     * Bodies that should no longer be active
     */
    private Set<AgentBody> removedBodies;

    private Map<IAgentType, Integer> instanceCount;
    
    protected Random rand = new Random();

    /**
     * Constructor that creates a shallow copy of specified environment.
     * Namely: everything related to bodies are the same list (changes will propagate)
     * totalRewards are copied, logging output is NOT copied.
     * @param original 
     */
    protected AbstractEnvironment(AbstractEnvironment original){
        this.actionClass = original.actionClass;
        this.finished = original.finished;
        this.bodies = original.bodies;
        this.timeStep = original.timeStep;
        this.failureReward = original.failureReward;
        this.representations = original.representations;
        this.activeBodies = original.activeBodies;
        this.removedBodies = original.removedBodies;
        this.instanceCount = original.instanceCount;
        this.runtimeLoggingOutput = NullLoggingOutput.NULL_LOG;
        
        //total rewards are the only thing that is deeply copied
        this.totalRewards = new HashMap<AgentBody, Double>(original.totalRewards);
    }
    
    public AbstractEnvironment(Class<ACTION> actionClass){
        this(actionClass, Double.NEGATIVE_INFINITY);
    }
    
    public AbstractEnvironment(Class<ACTION> actionClass, double failureReward) {
        this.actionClass = actionClass;
        representations = new ArrayList<IEnvironmentRepresentation>();
        this.failureReward = failureReward;
    }
    
    protected void registerRepresentation(IEnvironmentRepresentation representation){
        representations.add(representation);
    }

    @Override
    public Map<AgentBody, Double> nextStep() {
        if(isFinished()){
            throw new SimulationException("Trying to simulate a finished environment");
        }
        if(activeBodies.isEmpty()){
            logger.info("No more active bodies, finishing simulation");            
            this.setFinished(true);
            return Collections.EMPTY_MAP;
        }
        
        timeStep++;
        Map<AgentBody, Double> result = nextStepInternal();
        for(Map.Entry<AgentBody, Double> rewardEntry : result.entrySet() ){
            if(!removedBodies.contains(rewardEntry.getKey())){
                //removed bodies no longer receive rewards
                totalRewards.put(rewardEntry.getKey(), totalRewards.get(rewardEntry.getKey()) + rewardEntry.getValue());
            }
        }
        return result;
    }
    

    protected abstract Map<AgentBody, Double> nextStepInternal();

    @Override
    public AgentBody createAgentBody(IAgentType type) {
        
        if(timeStep > 0){
            throw new AgentInstantiationException("Instantiation in running environment is not supported");
        }
        
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
        
        AgentBody newBody = createAgentBodyInternal(type);
                
        instanceCount.put(type, instancesSoFar + 1);
        bodies.add(newBody);     
        activeBodies.add(newBody);
        totalRewards.put(newBody, 0d);
                
        afterAgentBodyCreated(newBody);        
        
        return newBody;
        
    }
    
    protected abstract AgentBody createAgentBodyInternal(IAgentType type);
    
    protected void afterAgentBodyCreated(AgentBody body){
        
    }

    @Override
    public void init() {
        finished = false;
        bodies = new ArrayList<AgentBody>();
        activeBodies = new ArrayList<AgentBody>();
        removedBodies =  new HashSet<AgentBody>();
        instanceCount = new HashMap<IAgentType, Integer>();
        timeStep = 0;
        totalRewards = new HashMap<AgentBody, Double>();
    }

    @Override
    public void stop() {
        finished = true;
    }

    
    
    @Override
    public List<AgentBody> getAllBodies() {
        return bodies;
    }

    @Override
    public List<AgentBody> getActiveBodies() {
        return activeBodies;
    }

    protected Set<AgentBody> getRemovedBodies() {
        return removedBodies;
    }

    @Override
    public List<IEnvironmentRepresentation> getRepresentations() {
        return representations;
    }
    

   
    
    @Override
    public long getTimeStep() {
        return timeStep;
    }
    
    @Override
    public double getTotalReward(AgentBody agentBody) {
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
    public void removeAgentBody(AgentBody body) {
        removedBodies.add(body);
        totalRewards.put(body,  totalRewards.get(body) + failureReward);
    }

    /**
     * This implementation just returns null -> per agent logging not supported
     * @return 
     */
    @Override
    public Map<AgentBody, ILogDataProvider> getPerAgentLogDataProviders() {
        return null;
    }

    @Override
    public ILoggingHeaders getEnvironmentParametersHeaders() {
        return LoggingHeaders.EMPTY_LOGGING_HEADERS;
    }

    @Override
    public List<Object> getEnvironmentParametersValues() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public ILoggingHeaders getPerAgentAndExperimentLoggingHeaders() {
        return LoggingHeaders.EMPTY_LOGGING_HEADERS;
    }

    @Override
    public List<Object> getPerAgentAndExperimentLoggingData(AgentBody agentBody) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public ILogIdentifier getIdentifier() {
        return new ClassLogIdentifier(getClass());
    }

    @Override
    public ILoggingHeaders getRuntimeLoggingHeaders() {
        return LoggingHeaders.EMPTY_LOGGING_HEADERS;
    }

    @Override
    public ILoggingHeaders getPerExperimentLoggingHeaders() {
        return new LoggingHeaders("StepsPerformed");
    }

    @Override
    public List<Object> getPerExperimentLoggingData() {
        return Collections.<Object>singletonList(timeStep);
    }

    @Override
    public void setRuntimeLoggingOutput(IBareLoggingOutput loggingOutput) {
        runtimeLoggingOutput = loggingOutput;
    }

    @Override
    public void setRandomSeed(long seed) {
        rand = new Random(seed);
    }
    
 
    
    
}
