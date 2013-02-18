/*
 * Copyright (C) 2013 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
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
import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.planning4j.*;
import cz.cuni.amis.planning4j.impl.PDDLObjectDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLObjectProblemProvider;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin
 */
public class Planning4JController extends AbstractAgentController<IAction, IPDDLRepresentation<IAction>> {

    private final Logger logger = Logger.getLogger(Planning4JController.class);

    protected PDDLObjectDomainProvider domainProvider;

    IAsyncPlanner<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> planner;
    
    /**
     * Validator that validates the plans throughout execution, whether they still
     * lead to goal state. If null, no validation is performed.
     */
    IValidator<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> validator;

    IPlanFuture planFuture = null;

    Queue<ActionDescription> currentPlan;
    Queue<IAction> actionsToPerform;

    
    public enum ValidationMethod { NONE, EXTERNAL_VALIDATOR, ENVIRONMENT_SIMULATION_NEXT_STEP, ENVIRONMENT_SIMULATION_WHOLE_PLAN };
    
    private ValidationMethod validationMethod;

    /**
     * Createas a controller without validation
     * @param planner 
     */
    public Planning4JController(IAsyncPlanner planner) {
        this(planner, ValidationMethod.NONE);
    }
    
    /**
     * Creates a controller with specified validation method.
     * @param planner
     * @param validationScope 
     */
    public Planning4JController(IAsyncPlanner planner, ValidationMethod validationMethod) {
        this(planner, validationMethod, null);
    }    
    
    /**
     * Creates a controller with external validator plan validation.
     * @param planner
     * @param validationScope
     * @param validator 
     */
    public Planning4JController(IAsyncPlanner planner, IValidator validator) {
        this(planner, ValidationMethod.EXTERNAL_VALIDATOR, validator);
    }    
    
    public Planning4JController(IAsyncPlanner planner, ValidationMethod validationMethod, IValidator validator) {
        this.planner = Planning4JUtils.getTranslatingAsyncPlanner(planner, IPDDLObjectDomainProvider.class, IPDDLObjectProblemProvider.class);
        if(validator != null){
            this.validator = Planning4JUtils.getTranslatingValidator(validator, IPDDLObjectDomainProvider.class, IPDDLObjectProblemProvider.class);
        } else {
            if(validationMethod == ValidationMethod.EXTERNAL_VALIDATOR){
                throw new IllegalArgumentException("If external validation is required, validator has to be given");
            }
            this.validator = null;
        }

        this.validationMethod = validationMethod;
        currentPlan = new ArrayDeque<ActionDescription>();
        actionsToPerform = new ArrayDeque<IAction>();
    }

    @Override
    public void init(IEnvironment<IAction> environment, IPDDLRepresentation<IAction> representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        domainProvider = new PDDLObjectDomainProvider(representation.getDomain(body));
        if(validationMethod == ValidationMethod.ENVIRONMENT_SIMULATION_NEXT_STEP || validationMethod == ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN){
            if(!(environment instanceof ISimulableEnvironment)){
                throw new AisteException("Validation method set to environment simulation, but the environment is not simulable");
            }
            if(!(representation instanceof ISimulableEnvironmentRepresentation)){
                throw new AisteException("Validation method set to environment simulation, but the representation is not simulable");
            }
            
        }
    }


    @Override
    public void onSimulationStep(double reward) {
        super.onSimulationStep(reward);
        if (planFuture != null) {
            switch (planFuture.getStatus()) {
                case FUTURE_IS_BEING_COMPUTED: //do nothing and wait
                    break;
                case CANCELED: {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Plan calculation cancelled.");
                    }
                    break;
                }
                case COMPUTATION_EXCEPTION: {
                    logger.info("Exception during planning:", planFuture.getException());
                    break;
                }
                case FUTURE_IS_READY: {
                    IPlanningResult planningResult = planFuture.get();
                    if (planningResult.isSuccess()) {
                        if(logger.isTraceEnabled()){
                            StringBuilder planSB = new StringBuilder("Plan before conversion: ");
                            for(ActionDescription act : planningResult.getPlan()){
                                planSB.append(" ").append(act.toString()).append("");
                            }
                            logger.trace(planSB.toString());
                        }
                        currentPlan = new ArrayDeque<ActionDescription>(planningResult.getPlan());                        
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("No plan found.");
                        }
                    }

                }
            }
            
            if (planFuture.isDone()) {
                planFuture = null;
            }
        }

        if(actionsToPerform.isEmpty()){        
            if (currentPlan.isEmpty()) {
                if (planFuture == null || planFuture.isCancelled()) {
                    startPlanning();
                }
            } else {
                if(!validatePlan()){
                    logger.info("Plan invalidated.");                    
                    currentPlan.clear();
                    actionsToPerform.clear();
                }

                if(!currentPlan.isEmpty()){
                    actionsToPerform.addAll(representation.translateAction(currentPlan.poll()));
                }
            }
        }
        if(!actionsToPerform.isEmpty()){
            getEnvironment().act(getBody(), actionsToPerform.poll());            
        }
        
    }

    /**
     * Validate current plan
     * @return true, if plan is valid, false otherwise
     */
    protected boolean validatePlan() {
        switch (validationMethod){
            case NONE :
                return true;
            case EXTERNAL_VALIDATOR: {
                try {
                    long validationStart = System.currentTimeMillis();
                    IValidationResult validationResult = validator.validate(domainProvider, new PDDLObjectProblemProvider(representation.getProblem(getBody())), new ArrayList<ActionDescription>(currentPlan));
                    logger.debug("Validation took " + (System.currentTimeMillis() - validationStart) + "ms");
                    if (!validationResult.isValid()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Validation output:\n" + validationResult.getValidationOutput());
                        }
                        return false;
                    } else {
                        return true;
                    }
                } catch (ValidationException ex) {
                    logger.error("Exception in validating plan." + ex);
                    return true;
                }
            }
            case ENVIRONMENT_SIMULATION_NEXT_STEP: {
                throw new UnsupportedOperationException("One-step validation is not supported yet.");
            }
            case ENVIRONMENT_SIMULATION_WHOLE_PLAN : {
                List<IAction> restOfPlanCopy = new ArrayList<IAction>(actionsToPerform.size() + currentPlan.size());
                
                //those casts are safe, beacause types are enforced in constructor if validation is set to environment simulation
                ISimulableEnvironment environmentCopy = ((ISimulableEnvironment)environment).cloneForSimulation();
                ISimulablePDDLRepresentation simulableRepresentaion = (ISimulablePDDLRepresentation)representation;
                simulableRepresentaion.setEnvironment(environmentCopy);
                
                try {
                    restOfPlanCopy.addAll(actionsToPerform);
                    for(ActionDescription ac : currentPlan){
                        restOfPlanCopy.addAll(representation.translateAction(ac));
                    }
                    for(IAction act : restOfPlanCopy){
                        environmentCopy.simulateOneStep(Collections.singletonMap(body, act));
                        if(simulableRepresentaion instanceof IActionFailureRepresentation && ((IActionFailureRepresentation)simulableRepresentaion).lastActionFailed(body)){
                            return false;
                        }                        
                    }
                    return simulableRepresentaion.isGoalState(body);
                } finally {
                    simulableRepresentaion.setEnvironment(environment);
                }
            }
        }
        
        throw new IllegalStateException("Unrecognized validation method: " + validationMethod);
    }
    
    
    @Override
    public void start() {
        super.start();
        startPlanning();
    }

    protected void startPlanning() {
        if (planFuture != null && !planFuture.isDone()) {
            planFuture.cancel(true);
        }
        planFuture = planner.planAsync(domainProvider, new PDDLObjectProblemProvider(representation.getProblem(getBody())));
    }

    @Override
    public Class getRepresentationClass() {
        return IPDDLRepresentation.class;
    }
    
    
}
