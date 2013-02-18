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

import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import java.util.List;
import java.util.Map;

/**
 * An abstract ancestor for all environments. A specific environment should inherit
 * one or more IXXXXRepresentableEnvironment interfaces. In a typical scenario environments
 * should inherit from {@link AbstractSynchronizedEnvironment} ore one of its descendants.
 * The environment should be reusable - i. e. after call to {@link #stop() } and {@link #init()} it
 * should be able to start afresh (without keeping the registered executors).
 * @author Martin Cerny
 */
public interface IEnvironment<ACTION extends IAction> {
    
    /**
     * Initializes the environment for use.
     */
    void init();
    
    /**
     * Shutdowns the environment and prepares it to be reused.
     */
    void stop();
    
    /**
     * Get information about possible agent types that may by instantiated
     * @return 
     */
    Map<? extends IAgentType,? extends IAgentInstantiationDescriptor> getInstantiationDescriptors();
    
    /**
     * Instantiate a new agent body of specified type
     * @param type
     * @return 
     * @throws AgentInstantiationException if it was not possible to instantiate such agent
     */
    AgentBody createAgentBody(IAgentType type);
    
    /**
     * Perform an action by the specified body.
     * This method must not block and it must be possible to call it repeatedly between two
     * successive simulation steps or not call it at all (resulting in a NO_OP action).
     * @param agentBody
     * @param action 
     * @return true, if the action was recognized and is valid in current context, false otherwise. 
     * If this method returns true, the action may still fail. Returning true means, that such action
     * is merely available to the agent. In a typical implementation this method returns true, unless
     * the action is found to belong to different agent type or not recognized at all.
     * 
     */
    boolean act(AgentBody agentBody, ACTION action);
    
    /**
     * Get all agent bodies present in the environment
     * @return 
     */
    List<AgentBody> getAllBodies();

    /**
     * Get all agent bodies present in the environment that have not yet been removed.
     * @return 
     */
    List<AgentBody> getActiveBodies();
    
    /**
     * Runs one step of the simulation.
     * @return rewards earned by all agents in the simulation
     * @throws SimulationException if an unexpected event occurs
     */
    Map<AgentBody, Double> simulateOneStep();
    
    /**
     * Returns the number of steps that were simulated.
     * @return 
     */
    long getTimeStep();
    
    /**
     * Gets the sum of all rewards gained by specified agent during the whole course of the simulation
     * @param agentBody
     * @return 
     */
    double getTotalReward(AgentBody agentBody);
    
    /**
     * Whether the simulation has reached a terminal state
     * @return 
     */
    boolean isFinished();
    
    Class<ACTION> getActionClass();
    
    /**
     * Removes agent body from the simulation. An environment specific failure-reward should
     * be given to the agent. This is usually -Inf for environments where there is
     * no lower bound on reward agent may accumulate or -SOME_INTEGER for environments
     * where doing nothing does not hurt the agent.
     * Useful especially to remove bodies of agents that performed some sort of illegal operation
     * (e. g. exception in agent logic).
     */
    void removeAgentBody(AgentBody body);    

    
    /**
     * Gets all representations this environment has.
     */
    List<IEnvironmentRepresentation> getRepresentations();
}
