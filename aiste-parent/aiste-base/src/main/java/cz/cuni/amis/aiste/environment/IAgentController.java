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

/**
 * An abstract agent controller. To be compatible with default execution and matchmaking
 * all concrete implementing classes should provide a no-argument constructor.
 * @author Martin Cerny
 */
public interface IAgentController<BODY extends IAgentBody, ACTION extends IAction, ENVIRONMENT extends IEnvironment<BODY, ACTION>> {

    /**
     * Check whether this agent controller may control agents in given environment.
     * Called during matchmaking between controllers and environments
     * @param environment
     * @return 
     */
    boolean isApplicable(ENVIRONMENT environment);
    
    /**
     * Initialize the agent for specific body in a specific environment.
     * Agent should initialize any resources necessary for execution in this method. 
     * It is forbidden to issue any actions in this method or at any time prior to call
     * of {@link #start() }
     * @param environment
     * @param body
     * @param stepDelay an informative delay in milliseconds between successive simulation step (e. g. to set a time limit for deliberation)
     */
    void init(ENVIRONMENT environment, BODY body, long stepDelay);
    
    /**
     * Called once, when the simulation starts. If the agent has its own thread of execution,
     * it should be started here.
     */
    void start();

    /**
     * Called once, after the simulation has finished. The controller should release
     * all resources in this method.
     */
    void shutdown();
    
    /**
     * Notifies the agent that a simulation step has happened and that it received a reward.
     * This method is called even if the invocation on previous simulation step has not yet returned.
     * Calls to this method are not synchronized with the environment.
     * @param reward 
     */
    void onSimulationStep(double reward);
    
    /**
     * Gets the body this controller works with.
     * @return 
     */
    BODY getBody();
    
}
