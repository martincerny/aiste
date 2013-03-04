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
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.metrics.IncrementalMetric;
import cz.cuni.amis.experiments.impl.metrics.IntegerAverageMetric;
import cz.cuni.amis.experiments.impl.metrics.TimeMeasuringMetric;
import cz.cuni.amis.utils.future.IFutureWithListeners;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin
 */
public abstract class AbstractPlanningController<DOMAIN, PROBLEM, PLANNER_ACTION,PLANNING_RESULT, REPRESENTATION extends IPlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, IAction>> extends AbstractAgentController<IAction, REPRESENTATION> {

    private final Logger logger = Logger.getLogger(AbstractPlanningController.class);


    private IFutureWithListeners<PLANNING_RESULT> planFuture = null;

    private Queue<PLANNER_ACTION> currentPlan;
    private Queue<IAction> actionsToPerform;

    
    public enum ValidationMethod { NONE, EXTERNAL_VALIDATOR, ENVIRONMENT_SIMULATION_NEXT_STEP, ENVIRONMENT_SIMULATION_WHOLE_PLAN };
    
    private ValidationMethod validationMethod;

 
    protected TimeMeasuringMetric timeSpentPlanning;
    protected TimeMeasuringMetric timeSpentTranslating;
    protected TimeMeasuringMetric timeSpentValidating;
    
    protected IncrementalMetric numPlannerExecutions;
    protected IncrementalMetric numSuccesfulPlanning;
    protected IncrementalMetric numUnsuccesfulPlanning;
    protected IncrementalMetric numPlanningExceptions;
    
    protected IntegerAverageMetric averagePlanLength;
    protected IntegerAverageMetric averageTimePerSuccesfulPlanning;
    protected IntegerAverageMetric averageTimePerUnsuccesfulPlanning;
    
    long lastPlanningStartTime = 0;
    
    public AbstractPlanningController(ValidationMethod validationMethod) {
        this.validationMethod = validationMethod;
        currentPlan = new ArrayDeque<PLANNER_ACTION>();
        actionsToPerform = new ArrayDeque<IAction>();
        
        /**
         * Initialize metrics
         */
        timeSpentPlanning = new TimeMeasuringMetric("planningTime");
        metrics.addMetric(timeSpentPlanning);
        timeSpentTranslating = new TimeMeasuringMetric("translatingTime");
        metrics.addMetric(timeSpentTranslating);
        timeSpentValidating = new TimeMeasuringMetric("validatingTime");
        metrics.addMetric(timeSpentValidating);
        
        numPlannerExecutions = new IncrementalMetric("numPlannerExecutions");
        metrics.addMetric(numPlannerExecutions);
        numSuccesfulPlanning = new IncrementalMetric("numSuccesfulPlanning");
        metrics.addMetric(numSuccesfulPlanning);
        numUnsuccesfulPlanning = new IncrementalMetric("numUnsuccesfulPlanning");
        metrics.addMetric(numUnsuccesfulPlanning);
        numPlanningExceptions = new IncrementalMetric("numPlanningExceptions");
        metrics.addMetric(numPlanningExceptions);

        averagePlanLength = new IntegerAverageMetric("avgPlanLength");
        metrics.addMetric(averagePlanLength);
        averageTimePerSuccesfulPlanning = new IntegerAverageMetric("avgSuccesfulPlanningTime");
        metrics.addMetric(averageTimePerSuccesfulPlanning);
        averageTimePerUnsuccesfulPlanning = new IntegerAverageMetric("avgUnsuccesfulPlanningTime");
        metrics.addMetric(averageTimePerUnsuccesfulPlanning);
    }

    @Override
    public void init(IEnvironment<IAction> environment, REPRESENTATION representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        if(validationMethod == ValidationMethod.ENVIRONMENT_SIMULATION_NEXT_STEP || validationMethod == ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN){
            if(!(environment instanceof ISimulableEnvironment)){
                throw new AisteException("Validation method set to environment simulation, but the environment is not simulable");
            }
            if(!(representation instanceof ISimulableEnvironmentRepresentation)){
                throw new AisteException("Validation method set to environment simulation, but the representation is not simulable");
            }
            
        }
    }

    
    protected abstract boolean isPlanningResultSucces(PLANNING_RESULT result);

    protected abstract List<PLANNER_ACTION> getActionsFromPlanningResult(PLANNING_RESULT result);
    
    protected void getDebugRepresentationOfPlannerActions(List<PLANNER_ACTION> plannerActions, StringBuilder planSB) {
        for(PLANNER_ACTION act : plannerActions){
            planSB.append(" ").append(act.toString()).append("");
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
                    numPlanningExceptions.increment();
                    break;
                }
                case FUTURE_IS_READY: {
                    PLANNING_RESULT planningResult = planFuture.get();
                    long planningTime = System.currentTimeMillis() - lastPlanningStartTime;
                    if (isPlanningResultSucces(planningResult)) {
                        List<PLANNER_ACTION> plannerActions = getActionsFromPlanningResult(planningResult);
                        if(logger.isTraceEnabled()){
                            StringBuilder planSB = new StringBuilder("Plan before conversion: ");
                            getDebugRepresentationOfPlannerActions(plannerActions, planSB);
                            logger.trace(planSB.toString());
                        }

                        numSuccesfulPlanning.increment();
                        averageTimePerSuccesfulPlanning.addSample(planningTime);
                        averagePlanLength.addSample(plannerActions.size());
                        
                        currentPlan = new ArrayDeque<PLANNER_ACTION>(plannerActions);                        
                    } else {
                        numUnsuccesfulPlanning.increment();
                        averageTimePerUnsuccesfulPlanning.addSample(planningTime);                        
                        if (logger.isDebugEnabled()) {
                            logger.debug("No plan found.");
                        }
                    }

                }
            }
            
            if (planFuture.isDone()) {
                timeSpentPlanning.taskFinished();
                planFuture = null;
            }
        }

        if(actionsToPerform.isEmpty()){        
            if (currentPlan.isEmpty()) {
                if (planFuture == null || planFuture.isCancelled()) {
                    startPlanning();
                }
            } else {
                timeSpentValidating.taskStarted();
                if(!validatePlan()){
                    logger.info("Plan invalidated.");                    
                    currentPlan.clear();
                    actionsToPerform.clear();
                }
                timeSpentValidating.taskFinished();

                timeSpentTranslating.taskStarted();
                while(actionsToPerform.isEmpty() && !currentPlan.isEmpty()){
                    actionsToPerform.addAll(representation.translateAction(currentPlan.poll(), body));
                }
                timeSpentTranslating.taskFinished();
            }
        }
        if(!actionsToPerform.isEmpty()){
            getEnvironment().act(getBody(), actionsToPerform.poll());            
        }
        
    }

    protected boolean validateWithExternalValidator(List<PLANNER_ACTION> currentPlan){
        throw new UnsupportedOperationException("Planning controller class " + getClass() + " does not support external validation");
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
                long validationStart = System.currentTimeMillis();
                boolean result = validateWithExternalValidator(null);
                if(logger.isDebugEnabled()){
                    logger.debug("Validation took " + (System.currentTimeMillis() - validationStart) + "ms");
                }
                return result;
            }
            case ENVIRONMENT_SIMULATION_NEXT_STEP: {
                throw new UnsupportedOperationException("One-step validation is not supported yet.");
            }
            case ENVIRONMENT_SIMULATION_WHOLE_PLAN : {
                List<IAction> restOfPlanCopy = new ArrayList<IAction>(actionsToPerform.size() + currentPlan.size());
                
                //those casts are safe, beacause types are enforced in constructor if validation is set to environment simulation
                ISimulableEnvironment environmentCopy = ((ISimulableEnvironment)environment).cloneForSimulation();
                ISimulablePlanningRepresentation simulableRepresentaion = (ISimulablePlanningRepresentation)representation;
                simulableRepresentaion.setEnvironment(environmentCopy);
                
                try {
                    restOfPlanCopy.addAll(actionsToPerform);
                    for(PLANNER_ACTION ac : currentPlan){
                        restOfPlanCopy.addAll(representation.translateAction(ac, body));
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

    protected abstract IFutureWithListeners<PLANNING_RESULT> startPlanningProcess();

    protected final void startPlanning() {
        if (planFuture != null && !planFuture.isDone()) {
            planFuture.cancel(true);
        }        
        lastPlanningStartTime = System.currentTimeMillis();
        timeSpentPlanning.taskStarted();
        numPlannerExecutions.increment();
        
        planFuture = startPlanningProcess();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (planFuture != null && !planFuture.isDone()) {
            planFuture.cancel(true);
        }        
    }
  
    
    
}
