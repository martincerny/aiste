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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.SimulationException;
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentBody;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.execution.IAgentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractEnvironmentExecutor implements IEnvironmentExecutor{
    private IEnvironment environment = null;
    private Map<IAgentBody, IAgentController> bodyToControllers = new HashMap<IAgentBody, IAgentController>();
    private List<IAgentController> controllers = new ArrayList<IAgentController>();
    private long stepDelay;

    public AbstractEnvironmentExecutor(long stepDelay) {
        this.stepDelay = stepDelay;
    }
    
    
    /**
     * Sets the environment and initializes it.
     * @param environment 
     */
    @Override
    public void setEnvironment(IEnvironment environment) {
        if(this.environment != null){
            throw new AisteException("Environment may be set only once");
        }
        this.environment = environment;
        environment.init();
    }

    @Override
    public void addAgentController(IAgentType type, IAgentController controller) {
        if(environment == null){
            throw new IllegalStateException("Environment not set");
        }
        if(!controller.isApplicable(environment)){
            throw new AgentInstantiationException("Controller " + controller + " is not applicable to environment " + environment);
        }

        IAgentBody newBody = environment.createAgentBody(type);
        controller.init(environment, newBody, stepDelay);
        
        controllers.add(controller);
        bodyToControllers.put(newBody, controller);
    }
    
    /**
     * Descendants may call this method to start all controllers.
     */
    protected void startSimulation(){
        for(IAgentController controller : controllers){
            controller.start();
        }
    }
    
    /**
     * Descendants may call this method to perform a single simulation step. The method
     * is not thread safe.
     */
    protected void performSimulationStep(){
        Map<IAgentBody, Double> stepResult = environment.simulateOneStep();
        for(IAgentController controller : controllers){
            Double reward = stepResult.get(controller.getBody());
            if(reward == null){
                throw new SimulationException("The environment has not produced a reward for body " + controller.getBody() + " (controller: " + controller + ")");
            }
            controller.onSimulationStep(reward);
        }
    }

    
    /**
     * Descendants may call this method to stop the simulation and shutdown all controllers. The method
     * is not thread safe.
     */
    protected void stopSimulation(){
        environment.stop();
        for(IAgentController controller : controllers){
            controller.shutdown();
        }
    }
    
    /**
     * Descendants may call this method to gather rewards from the environment.
     * @return 
     */
    protected IEnvironmentExecutionResult gatherExecutionResult(){
        List<IAgentExecutionResult> agentResults = new ArrayList<IAgentExecutionResult>(controllers.size());
        for(IAgentController controller : controllers){
            agentResults.add(new AgentExecutionResult(controller.getBody().getType(), controller, environment.getTotalReward(controller.getBody())));
        }
        EnvironmentExecutionResult result = new EnvironmentExecutionResult(agentResults, environment.getTimeStep());
        return result;
    }
    
    public IEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public IEnvironmentExecutionResult executeEnvironment() {
        return executeEnvironment(0);
    }

    public Map<IAgentBody, IAgentController> getBodyToControllers() {
        return bodyToControllers;
    }

    public List<IAgentController> getControllers() {
        return controllers;
    }


    public long getStepDelay() {
        return stepDelay;
    }
    
    
}
