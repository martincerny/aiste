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

import cz.cuni.amis.experiments.ILogDataProvider;

/**
 * An abstract agent controller. To be compatible with default execution and matchmaking
 * all concrete implementing classes should provide a no-argument constructor. The controller
 * logs data and is responsible for logging any neccessary data from its associated environment representation.
 * Controllers may be reused across several environments. A call to {@link #shutdown() } is always issued before another 
 * call to {@link #init(cz.cuni.amis.aiste.environment.IEnvironment, cz.cuni.amis.aiste.environment.IEnvironmentRepresentation, cz.cuni.amis.aiste.environment.AgentBody, long) }.
 * @author Martin Cerny
 */
public interface IAgentController<ACTION extends IAction, REPRESENTATION extends IEnvironmentRepresentation> extends ILogDataProvider {

    
    /**
     * Initialize the agent for specific body in a specific environment and representation.
     * Agent should initialize any resources necessary for execution in this method. 
     * It is forbidden to issue any actions in this method or at any time prior to call
     * of {@link #start() }
     * @param environment
     * @param body
     * @param stepDelay an informative delay in milliseconds between successive simulation step (e. g. to set a time limit for deliberation)
     */
    void init(IEnvironment<ACTION> environment, REPRESENTATION representation, AgentBody body, long stepDelay);
    
    /**
     * Return the representation the agent works with (the one last set by {@link #init(cz.cuni.amis.aiste.environment.IEnvironment, cz.cuni.amis.aiste.environment.IEnvironmentRepresentation, cz.cuni.amis.aiste.environment.AgentBody, long) }).
     * @return 
     */
    REPRESENTATION getCurrentEnvironmentRepresentation();
    
    /**
     * Called once, when the simulation starts. If the agent has its own thread of execution,
     * it should be started here. The simulation does not start before this method returns. Time taken 
     */
    void start();

    /**
     * Called once, after the simulation has finished. The controller should release
     * all resources in this method.
     */
    void shutdown();
    
    /**
     * Notifies the agent that a simulation step has happened and that it received a reward.
     * If execution of this method takes more time than a single simulation step,
     * it may be called again even if the invocation on previous simulation step has not yet returned.
     * Calls to this method are not synchronized with the environment.
     * @param reward 
     */
    void onSimulationStep(double reward);
    
    /**
     * Gets the body this controller works with.
     * @return 
     */
    AgentBody getBody();

    /**
     * Gets the representation class this controller works with - used during automatic matchmaking.
     * @return 
     */
    Class getRepresentationClass();
    
    /**
     * Get representation uniquely representing the controller and all its parameters.
     * @return 
     */
    String getLoggableRepresentation();
}
