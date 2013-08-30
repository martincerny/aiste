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
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.experiments.impl.metrics.IncrementalMetric;
import cz.cuni.amis.experiments.impl.metrics.IntegerAverageMetric;
import cz.cuni.amis.experiments.impl.metrics.TimeMeasuringMetric;
import cz.cuni.amis.utils.collections.ListConcatenation;
import cz.cuni.amis.utils.future.FutureStatus;
import cz.cuni.amis.utils.future.FutureWithListeners;
import cz.cuni.amis.utils.future.IFutureListener;
import cz.cuni.amis.utils.future.IFutureWithListeners;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin
 */
public abstract class AbstractPlanningController
//sorry for the overuse of generics. But except for this part its pretty convenient
<DOMAIN, PROBLEM, PLANNER_ACTION,PLANNING_RESULT, REPRESENTATION extends IPlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, IAction, IPlanningGoal>> 
extends AbstractAgentController<IAction, REPRESENTATION> 
implements IFutureListener<PLANNING_RESULT>
{

    private final Logger logger = Logger.getLogger(AbstractPlanningController.class);


    private IFutureWithListeners<PLANNING_RESULT> planFuture = null;

    private Queue<PLANNER_ACTION> currentPlan;
    private IReactivePlan activePlannerActionReactivePlan;
    private IReactivePlan activeReactiveLayerPlan;

    protected IPlanningGoal executedGoal;
    protected IPlanningGoal goalForPlanning;

    protected int numFailuresSinceLastImportantEnvChange = 0;

    protected void cancelPlanFutureIfRunning() {
        IFutureWithListeners<PLANNING_RESULT> planFutureCopy = planFuture;
        if (planFutureCopy != null){
            planFutureCopy.removeFutureListener(this);            
            synchronized(planFutureCopy){//synchronized so that setResult cannot be called from within planning process
                 if(!planFutureCopy.isDone()) {
                    planFutureCopy.cancel(true);
                }        
            }
        }
    }

    protected void getNextReactivePlanFromCurrentPlan() {
        timeSpentTranslating.taskStarted();
        activePlannerActionReactivePlan = representation.translateAction(currentPlan, body);
        timeSpentTranslating.taskFinished();
    }
    


    
    public enum ValidationMethod { NONE, EXTERNAL_VALIDATOR, ENVIRONMENT_SIMULATION_NEXT_STEP, ENVIRONMENT_SIMULATION_WHOLE_PLAN };
    
    private ValidationMethod validationMethod;

 
    protected TimeMeasuringMetric timeSpentPlanning;
    protected TimeMeasuringMetric timeSpentTranslating;
    protected TimeMeasuringMetric timeSpentValidating;
    
    protected IncrementalMetric numPlannerExecutions;
    protected IncrementalMetric numSuccesfulPlanning;
    protected IncrementalMetric numUnsuccesfulPlanning;
    protected IncrementalMetric numPlanningExceptions;
    protected IncrementalMetric numStepsIdle;
    
    protected IntegerAverageMetric averagePlanLength;
    protected IntegerAverageMetric averageTimePerSuccesfulPlanning;
    protected IntegerAverageMetric averageTimePerUnsuccesfulPlanning;
    
    long lastPlanningStartTime = 0;
    
    /**
     * If current plan vas validated in this simulation step.
     * There are several places where current plan may get validated: After it
     * was received from planner, or before executing next action. This flag
     * prevents the plan from being validated twice.
     */
    boolean planValidatedForThisStep = false;
    
    /**
     * Whether an action was issued during this simulation step.
     * This allows the planner to issue actions the same step the planning was started, if
     * the planner returns swiftly.
     */
    boolean reactiveActionIssuedThisStep = false;
    
    
    
    public AbstractPlanningController(ValidationMethod validationMethod, ILoggingHeaders controllerParametersHeaders, Object ... controllerParametersValues ) {
        this(validationMethod, LoggingHeaders.EMPTY_LOGGING_HEADERS, controllerParametersHeaders, controllerParametersValues);
    }
    
    public AbstractPlanningController(ValidationMethod validationMethod, ILoggingHeaders runtimeLoggingHeaders, ILoggingHeaders controllerParametersHeaders, Object ... controllerParametersValues ) {
        super(LoggingHeadersConcatenation.concatenate(new LoggingHeaders("planningStatus", "actionIssued"), runtimeLoggingHeaders), 
                LoggingHeadersConcatenation.concatenate(new LoggingHeaders("validationMethod"), controllerParametersHeaders), 
                ListConcatenation.concatenate(Collections.<Object>singletonList(validationMethod), Arrays.asList(controllerParametersValues)));
        this.validationMethod = validationMethod;
        currentPlan = new ArrayDeque<PLANNER_ACTION>();
        activePlannerActionReactivePlan = EmptyReactivePlan.EMPTY_PLAN;
        
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
        numStepsIdle = new IncrementalMetric("numStepsIdle");
        metrics.addMetric(numStepsIdle);        

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
            if(!(representation instanceof ISimulablePlanningRepresentation)){
                throw new AisteException("Validation method set to environment simulation, but the representation is not simulable");
            }
            
        }
        //plan from planner should be kept empty for convenience
        this.activePlannerActionReactivePlan = EmptyReactivePlan.EMPTY_PLAN;
        //while reactive layer plan is tested for null
        this.activeReactiveLayerPlan = null;
        this.currentPlan.clear(); 
        this.numFailuresSinceLastImportantEnvChange = 0;
        this.planFuture = null;
        
    }

    
    protected abstract boolean isPlanningResultSucces(PLANNING_RESULT result);

    protected abstract List<PLANNER_ACTION> getActionsFromPlanningResult(PLANNING_RESULT result);
    
    protected void getDebugRepresentationOfPlannerActions(Collection<PLANNER_ACTION> plannerActions, StringBuilder planSB) {
        for(PLANNER_ACTION act : plannerActions){
            planSB.append(" ").append(act.toString()).append("");
        }
    }
    
    /**
     * Estimate cost of (partially executed) plan.
     * @param actions
     * @return 
     */
    protected double getPlanCost(Queue<PLANNER_ACTION> actions){
        return actions.size();
    }
    
    protected void clearPlan() {
        currentPlan.clear();
        executedGoal = null;
        activePlannerActionReactivePlan = EmptyReactivePlan.EMPTY_PLAN;
    }

    protected void processPlanningFailure() {
        if(!representation.environmentChangedConsiderablySinceLastMarker(body)){
            numFailuresSinceLastImportantEnvChange++;
        } else {
            numFailuresSinceLastImportantEnvChange = 0;
        }
    }    
    
    /**
     * Selects a goal for pursuit. By default, this is the highest priority goal.
     * @return 
     */
    protected IPlanningGoal selectGoal(){        
        List<IPlanningGoal> relevantGoals = representation.getRelevantGoals(body);
        
        //If we have failed to find plans for high priority goals and environment has not changed, lets try some 
        //lower priority ones
        if(numFailuresSinceLastImportantEnvChange < relevantGoals.size()){
            return relevantGoals.get(numFailuresSinceLastImportantEnvChange);
        } else {
            //tried all relevant goals but all failed, lets try it once more
            representation.setMarker(body);
            numFailuresSinceLastImportantEnvChange = 0;
            return relevantGoals.get(0);
        }
    }
    
    @Override
    public void onSimulationStep(double reward) {
        super.onSimulationStep(reward);
        
        if (logger.isTraceEnabled()) {
            StringBuilder planSB = new StringBuilder(body.getId() + ": Current plan: ");
            getDebugRepresentationOfPlannerActions(currentPlan, planSB);
            logger.trace(planSB.toString());
        }
        
        boolean startedPlanningThisStep = false;
        
        if(representation instanceof IActionFailureRepresentation){
            if(((IActionFailureRepresentation)representation).lastActionFailed(body)){
                logger.info(body.getId() + ": Action failed, ivalidating plan.");
                clearPlan();
                startPlanning();;
                startedPlanningThisStep = true;
            }            
        }
        
        
        //The current goal was invalidated by a new one. Lets go for it.
        IPlanningGoal newGoal = selectGoal();
        if(!newGoal.equals(goalForPlanning)){
            goalForPlanning = newGoal;
            if(!startedPlanningThisStep){
                startPlanning();
            }
            startedPlanningThisStep = true;
        }
                
        
        if (planFuture != null && planFuture.getStatus() == FutureStatus.FUTURE_IS_BEING_COMPUTED) {
                    if(representation.environmentChangedConsiderablySinceLastMarker(body)){
                        //the plan currently computed is probably useless. Restart the planning process.
                        if(!startedPlanningThisStep){
                            startPlanning();
                        }
                        startedPlanningThisStep = true;
                    }
 
            
        }
        
        /**
         * Evaluate the reactive layer
         */
        boolean reactiveLayerActive = false;

        
        if(activeReactiveLayerPlan != null){
            switch(activeReactiveLayerPlan.getStatus()){
                case COMPLETED : {
                    activeReactiveLayerPlan = null;
                    break;
                }
                case FAILED : {
                    logger.info(body.getId() + ": Reactive layer plan failed.");
                    activeReactiveLayerPlan = null;
                    break;
                }
            }
        }
        
        if(activeReactiveLayerPlan == null){
            activeReactiveLayerPlan = representation.evaluateReactiveLayer(body);
        }
        
        if(activeReactiveLayerPlan != null && !activeReactiveLayerPlan.getStatus().isFinished()){
            reactiveLayerActive = true;
        }
                

        /**
         * Evaluate actions from plan
         */
        if(logger.isDebugEnabled()){
            logger.debug(body.getId() + ": Reactive plan for planner status:" + activePlannerActionReactivePlan.getStatus());
        }
        findNextAction: do {
            switch (activePlannerActionReactivePlan.getStatus()) {
                case COMPLETED: {
                    if (currentPlan.isEmpty()) {
                        if (planFuture == null || planFuture.isCancelled()) {
                            if(!startedPlanningThisStep){
                                startPlanning();
                            }
                            startedPlanningThisStep = true;
                        }                        
                    } else {
                        if(!planValidatedForThisStep){
                            timeSpentValidating.taskStarted();
                            boolean planValid = validatePlan(currentPlan, activePlannerActionReactivePlan, executedGoal);
                            timeSpentValidating.taskFinished();
                            planValidatedForThisStep = true;
                            if (!planValid) {
                                logger.info(body.getId() + ": Plan invalidated. Clearing plan.");
                                clearPlan();
                                continue;
                            }
                        }
                        getNextReactivePlanFromCurrentPlan();
                    }
                    break;
                }
                case FAILED : {
                    logger.info(body.getId() + ": Reactive plan failed. Clearing plan.");
                    clearPlan();
                    break;
                }
                case EXECUTING : {
                    break findNextAction;
                }
            }
        } while (!currentPlan.isEmpty());        
        
        IAction nextAction = null;
        if(reactiveLayerActive){
            nextAction = activeReactiveLayerPlan.nextAction();
            reactiveActionIssuedThisStep = true;
            logger.info(body.getId() + ": Reactive layer in cotrol, action: " + nextAction.getLoggableRepresentation());            
            if(!activePlannerActionReactivePlan.getStatus().isFinished() && nextAction.equals(activePlannerActionReactivePlan.peek())){
                //if the action is the same as in the original plan, we should advance both reactive plans
                activePlannerActionReactivePlan.nextAction();
            }
        }
        else if(!activePlannerActionReactivePlan.getStatus().isFinished()){
            nextAction = activePlannerActionReactivePlan.nextAction();            
            reactiveActionIssuedThisStep = false;
        } else {
            numStepsIdle.increment();
            IReactivePlan defaultPlan = representation.getDefaultReactivePlan(body);
            if(defaultPlan != null && defaultPlan.getStatus() == ReactivePlanStatus.EXECUTING){
                nextAction = defaultPlan.nextAction();
            }
            reactiveActionIssuedThisStep = false;
        }
        
        if(logger.isDebugEnabled()) {
            logger.debug(body.getId() + ": Current reactive plan: " + activePlannerActionReactivePlan);
        }
        
        if(nextAction != null){
            getEnvironment().act(getBody(), nextAction);                        
        } 
        
        if(reactiveLayerActive){
            logRuntime("REACTIVE_LAYER", nextAction);
        }
        else if(planFuture == null){
            logRuntime("PERFORMING_PLAN", nextAction);
        } else {
            logRuntime(planFuture.getStatus(), nextAction);            
        }
        
        /**
         * There are several places where current plan may get validated:
         * After it was received from planner, or before executing next action.
         * This flag prevents the plan from being validated twice.
         */
        planValidatedForThisStep = false;
        
        
    }

    protected boolean validateWithExternalValidator(Queue<PLANNER_ACTION> planToValidate, IReactivePlan unexecutedReactivePlan, IPlanningGoal goal){
        throw new UnsupportedOperationException("Planning controller class " + getClass() + " does not support external validation");
    }
    
    /**
     * Validate current plan
     * @return true, if plan is valid, false otherwise
     */
    protected boolean validatePlan(Queue<PLANNER_ACTION> planToValidate, IReactivePlan unexecutedReactivePlan, IPlanningGoal goal) {
        switch (validationMethod){
            case NONE :
                return true;
            case EXTERNAL_VALIDATOR: {
                long validationStart = System.currentTimeMillis();
                boolean result = validateWithExternalValidator(planToValidate, unexecutedReactivePlan, goal);
                if(logger.isDebugEnabled()){
                    logger.debug(body.getId() + ": Validation took " + (System.currentTimeMillis() - validationStart) + "ms");
                }
                return result;
            }
            case ENVIRONMENT_SIMULATION_NEXT_STEP: {
                throw new UnsupportedOperationException("One-step validation is not supported yet.");
            }
            case ENVIRONMENT_SIMULATION_WHOLE_PLAN : {
                
                //those casts are safe, beacause types are enforced in constructor if validation is set to environment simulation
                ISimulableEnvironment environmentCopy = ((ISimulableEnvironment)environment).cloneForSimulation();
                if(environment.isFinished()){
                    //the environment has been finished while we have been busy, lets stop the validation
                    //the check has to be AFTER cloning the environment, otherwise a race condition is possible
                    return false;
                }
                ISimulablePlanningRepresentation simulableRepresentaion = (ISimulablePlanningRepresentation)representation;
                
                
                IReactivePlan currentReactivePlan;
                try {
                    currentReactivePlan = unexecutedReactivePlan.cloneForSimulation(environmentCopy);
                } catch (UnsupportedOperationException ex){
                    throw new AisteException(body.getId() + ": Cannot validate plan, because current reactive plan does not support clonning for simulation", ex);
                }

                Queue<PLANNER_ACTION> currentPlanCopy = new ArrayDeque<PLANNER_ACTION>(planToValidate);
                do {
                    while (!currentReactivePlan.getStatus().isFinished()){
                        IAction nextAction = currentReactivePlan.nextAction();
                        environmentCopy.simulateOneStep(Collections.singletonMap(body, nextAction));
                        if(simulableRepresentaion instanceof IActionFailureRepresentation && ((IActionFailureRepresentation)simulableRepresentaion).lastActionFailed(body)){
                            return false;
                        }                        
                    }
                    if(currentReactivePlan.getStatus() == ReactivePlanStatus.FAILED){
                        return false;
                    }

                    if(!currentPlanCopy.isEmpty()){
                        currentReactivePlan = simulableRepresentaion.translateActionForSimulation(environmentCopy, currentPlanCopy, body);
                    }
                } while(!currentPlanCopy.isEmpty() || !currentReactivePlan.getStatus().isFinished());                            
                return simulableRepresentaion.isGoalState(body, goal);
            }
        }
        
        throw new IllegalStateException("Unrecognized validation method: " + validationMethod);
    }
    
    
    @Override
    public void start() {
        super.start();
        goalForPlanning = selectGoal();
        startPlanning();
    }

    protected abstract IFutureWithListeners<PLANNING_RESULT> startPlanningProcess();

    protected synchronized final void startPlanning() {
        cancelPlanFutureIfRunning();
        representation.setMarker(body);
        lastPlanningStartTime = System.currentTimeMillis();
        timeSpentPlanning.taskStarted();
        numPlannerExecutions.increment();
        
        if(logger.isDebugEnabled()){
            logger.debug(body.getId() + ": Starting planning process. Current goal: " + goalForPlanning);
        }
        
        if(planFuture != null){
            planFuture.removeFutureListener(this);
        }
        synchronized(this){
            planFuture = startPlanningProcess();
            
            //for the unlikely event that the future completes before startPlanningProcess() returns 
            if(!planFuture.isDone()){
                planFuture.addFutureListener(this);
            } else {
                futureEvent((FutureWithListeners<PLANNING_RESULT>)planFuture, FutureStatus.FUTURE_IS_BEING_COMPUTED, planFuture.getStatus());
            }
        }
    }

    @Override
    public synchronized void futureEvent(FutureWithListeners<PLANNING_RESULT> fwl, FutureStatus fs, FutureStatus fs1) {
        if(fwl != planFuture){
            //The plan future was changed by a different thread
            fwl.removeFutureListener(this);
            fwl.cancel(true);
            return;
        }        
        //to avoid concurrency issues, all actions are executed on method parameter and not on the class field copy of plan future
        switch (fwl.getStatus()) {
            case FUTURE_IS_BEING_COMPUTED: {
                //Do nothing and wait
                break;
            }
            case CANCELED: {
                if (logger.isDebugEnabled()) {
                    logger.debug(body.getId() + ": Plan calculation cancelled.");
                }
                break;
            }
            case COMPUTATION_EXCEPTION: {
                logger.info(body.getId() + ": Exception during planning:", fwl.getException());
                numPlanningExceptions.increment();
                processPlanningFailure();
                break;
            }
            case FUTURE_IS_READY: {
                PLANNING_RESULT planningResult = fwl.get();
                long planningTime = System.currentTimeMillis() - lastPlanningStartTime;
                if (isPlanningResultSucces(planningResult)) {
                    List<PLANNER_ACTION> plannerActions = getActionsFromPlanningResult(planningResult);
                    if (logger.isDebugEnabled()) {
                        StringBuilder planSB = new StringBuilder(body.getId() + ": Plan before conversion: ");
                        getDebugRepresentationOfPlannerActions(plannerActions, planSB);
                        logger.debug(planSB.toString());
                    }

                    numSuccesfulPlanning.increment();
                    averageTimePerSuccesfulPlanning.addSample(planningTime);
                    averagePlanLength.addSample(plannerActions.size());

                    timeSpentValidating.taskStarted();
                    ArrayDeque<PLANNER_ACTION> newPlanDeque = new ArrayDeque<PLANNER_ACTION>(plannerActions);
                    boolean planValid = validatePlan(newPlanDeque, EmptyReactivePlan.EMPTY_PLAN, goalForPlanning);
                    timeSpentValidating.taskFinished();

                    if (planValid) {
                        boolean overwriteCurrentPlan = false;
                        if (currentPlan.isEmpty() && activePlannerActionReactivePlan.getStatus().isFinished()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(body.getId() + ": No current plan, using new plan.");
                            }
                            overwriteCurrentPlan = true;
                        } else if (goalForPlanning.getPriority() > executedGoal.getPriority()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(body.getId() + ": New plan achieves higher priority goal, using new plan.");
                            }
                            overwriteCurrentPlan = true;
                        } else if (getPlanCost(currentPlan) > getPlanCost(newPlanDeque)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(body.getId() + ": New plan has lower cost, using new plan.");
                            }                            
                            overwriteCurrentPlan = true;
                        } else if (!validatePlan(currentPlan, activePlannerActionReactivePlan, executedGoal)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(body.getId() + ": Current plan is no longer valid, using new plan.");
                            }
                            overwriteCurrentPlan = true;
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug(body.getId() + ": New plan is no better than old plan. Keeping old plan.");
                            }
                            //I have just validated the current plan
                            planValidatedForThisStep = true;
                        }

                        if (overwriteCurrentPlan) {
                            currentPlan.clear();
                            currentPlan.addAll(plannerActions);
                            executedGoal = goalForPlanning;

                            //found plan, reset failure count
                            numFailuresSinceLastImportantEnvChange = 0;

                            //current plan was overwritten with new plan, which was validated
                            planValidatedForThisStep = true;
                            
                            if(!reactiveActionIssuedThisStep){
                                if (logger.isDebugEnabled()) {
                                    logger.debug(body.getId() + ": Overwriting previous non-reactive action.");
                                }
                                //I have finished planning and no action was issued yet in this step -> lets do the first action of our plan
                                getNextReactivePlanFromCurrentPlan();
                                if(!activePlannerActionReactivePlan.getStatus().isFinished()){
                                    environment.act(body, activePlannerActionReactivePlan.nextAction());
                                }                                
                            }
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug(body.getId() + ": Freshly received plan is not valid.");
                        }
                    }
                            

                } else {
                    numUnsuccesfulPlanning.increment();
                    averageTimePerUnsuccesfulPlanning.addSample(planningTime);
                    if (logger.isDebugEnabled()) {
                        logger.debug(body.getId() + ": No plan found.");
                    }
                    processPlanningFailure();
                }

            }
                
        }
        
        if (fwl.isDone()) {
            timeSpentPlanning.taskFinished();
            fwl.removeFutureListener(this);                
            planFuture = null;
        }

    }

    
    
    @Override
    public void shutdown() {
        super.shutdown();
        cancelPlanFutureIfRunning();
    }

    protected IFutureWithListeners<PLANNING_RESULT> getPlanFuture() {
        return planFuture;
    }
  
    
    
}
