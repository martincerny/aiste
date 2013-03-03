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
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.IAgentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractEnvironmentExecutor implements IEnvironmentExecutor{
    private final Logger logger = Logger.getLogger(AbstractEnvironmentExecutor.class);
    
    private IEnvironment environment = null;
    private Map<AgentBody, IAgentController> bodyToControllers = new HashMap<AgentBody, IAgentController>();
    private List<IAgentController> controllers = new ArrayList<IAgentController>();
    private List<IAgentController> activeControllers = new CopyOnWriteArrayList<IAgentController>();
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
    public void addAgentController(IAgentExecutionDescriptor descriptor) {
        if(environment == null){
            throw new IllegalStateException("Environment not set");
        }
        IEnvironmentRepresentation representation = descriptor.getRepresentation();
        IAgentController controller = descriptor.getController();
        
        if(!environment.getRepresentations().contains(representation)){
            throw new AgentInstantiationException("Trying to instantiate controller with representation that does not belong to the environment");
        }
        if(!controller.getRepresentationClass().isAssignableFrom(representation.getClass())){
            throw new AgentInstantiationException("Controller " + controller + " is not applicable to representation " + representation);
        }

        AgentBody newBody = environment.createAgentBody(descriptor.getAgentType());
        controller.init(environment, representation, newBody, stepDelay);
        
        controllers.add(controller);        
        activeControllers.add(controller);
        bodyToControllers.put(newBody, controller);
    }

    
    /**
     * Descendants may call this method to start all controllers.
     */
    protected void startSimulation(){
        for(IAgentController controller :   activeControllers){
            try {
                controller.start();
            } catch (Exception ex){
                logger.info("Controller " + controller + " has raised exception during start(). It has been stopped.", ex);
                controllerFailed(controller);
            }
        }
    }
    
    /**
     * Descendants may call this method to perform a single simulation step. The method
     * is not thread safe.
     */
    protected void performSimulationStep(){
        Map<AgentBody, Double> stepResult = environment.nextStep();
        for(IAgentController controller : activeControllers){
            Double reward = stepResult.get(controller.getBody());
            if(reward == null){
                throw new SimulationException("The environment has not produced a reward for body " + controller.getBody() + " (controller: " + controller + ")");
            }
            notifyControllerOfSimulationStep(controller, reward);
        }
    }

    /**
     * Called by {@link #performSimulationStep() } to notify an individual controller that
     * a simulation step has happened.
     * @param controller
     * @param reward 
     */
    protected abstract void notifyControllerOfSimulationStep(IAgentController controller, double reward);
    
    protected void controllerFailed(IAgentController controller){
        getEnvironment().removeAgentBody(controller.getBody());        
        activeControllers.remove(controller);        
        try {
            controller.shutdown();
        } catch (Exception ex){
            logger.info("Exception during controller shutdown. Controller: " + controller, ex);
        }
    }
    
    /**
     * Descendants may call this method to stop the simulation and shutdown all controllers. The method
     * is not thread safe.
     */
    protected void stopSimulation(){
        try {
            environment.stop();
        } catch (Exception ex){
            logger.info("Exception during environment shutdown." , ex);            
        }
        for(IAgentController controller : activeControllers){
            try {
                controller.shutdown();
            } catch (Exception ex){
                logger.info("Exception during controller shutdown. Controller: " + controller, ex);
            }
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

    public Map<AgentBody, IAgentController> getBodyToControllers() {
        return bodyToControllers;
    }

    public List<IAgentController> getAllControllers() {
        return controllers;
    }
    
    public List<IAgentController> getActiveControllers() {
        return activeControllers;
    }


    public long getStepDelay() {
        return stepDelay;
    }


    
}
