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

import JSHOP2.*;
import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.utils.collections.ListConcatenation;
import cz.cuni.amis.utils.future.FutureStatus;
import cz.cuni.amis.utils.future.FutureWithListeners;
import cz.cuni.amis.utils.future.IFutureWithListeners;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Martin Cerny
 */
public class JShop2Controller extends AbstractPlanningController<JSHOP2, IJShop2Problem, Predicate, Plan, IJShop2Representation<IAction, IPlanningGoal>> {

    private JSHOP2 jshop;
    
    /**
     * Maximum number of succesively improving plans to evaluate before returning from
     * JSHOP algorithm. 0 for no limit.
     */
    private int maxEvaluatedPlans = 0;
    
    private double lastBestPlanCost = Double.POSITIVE_INFINITY;
    private double currentBestPlanCost = Double.POSITIVE_INFINITY;
    
    private int stepsSinceFirstPlanFound = -1;
    
    private PlannerInterruptTest plannerInterruptTest;
    
    public JShop2Controller(ValidationMethod validationMethod) {
        this(validationMethod, 0);    
    }
    public JShop2Controller(ValidationMethod validationMethod, int maxEvaluatedPlans){
        this(validationMethod, maxEvaluatedPlans, null);
    }
    
    public JShop2Controller(ValidationMethod validationMethod, int maxEvaluatedPlans, PlannerInterruptTest plannerInterruptTest) {
        super(validationMethod, new LoggingHeaders("maxEvaluatedPlans", "interruptTest"), new Object[] {maxEvaluatedPlans, plannerInterruptTest == null ? "None" : plannerInterruptTest.getLoggableRepresentation()});
        this.maxEvaluatedPlans = maxEvaluatedPlans;
        this.plannerInterruptTest = plannerInterruptTest;
    }

    @Override
    public void init(IEnvironment<IAction> environment, IJShop2Representation<IAction, IPlanningGoal> representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        jshop = representation.getDomain(body);
    }

    @Override
    public void onSimulationStep(double reward) {
        if(getPlanFuture() != null && getPlanFuture().getStatus() == FutureStatus.FUTURE_IS_BEING_COMPUTED){
            /**
            * Check whether it is worth interrupting the planning prematurely
            */
            if(jshop.getNumPlansFound() > 0){
                stepsSinceFirstPlanFound++;
                if(plannerInterruptTest != null && plannerInterruptTest.shouldInterruptPrematurely(goalForPlanning, lastBestPlanCost, stepsSinceFirstPlanFound, jshop)){
                    jshop.cancel();
                    getPlanFuture().get(10, TimeUnit.MILLISECONDS);
                }
                if(jshop.getBestPlan().getCost() < currentBestPlanCost){
                    lastBestPlanCost = currentBestPlanCost;
                    currentBestPlanCost = jshop.getBestPlan().getCost();
                }
            }
            
        } 
        super.onSimulationStep(reward);
    }
    
    

    @Override
    protected List<Predicate> getActionsFromPlanningResult(Plan result) {
        if(result == null){
            return null;
        } else {
            return result.getOps();
        }
    }

    @Override
    protected boolean isPlanningResultSucces(Plan result) {
        return result != null;
    }

    @Override
    protected void getDebugRepresentationOfPlannerActions(Collection<Predicate> plannerActions, StringBuilder planSB) {
        for(Predicate act : plannerActions){
            JShop2Utils.GroundActionInfo info = JShop2Utils.getGroundInfo(act);
            planSB.append(" (").append(jshop.getDomain().getPrimitiveTasks()[info.actionId]);
            for(int constantIndex  : info.params){
                planSB.append(" ").append(jshop.getConstant(constantIndex).toString(jshop));
            }
            planSB.append(")");
        }
    }

    
    
    @Override
    protected IFutureWithListeners<Plan> startPlanningProcess() {
        final JShop2PlanningProcess planningProcess = new JShop2PlanningProcess(jshop, representation.getProblem(body, goalForPlanning));
        final JShop2PlanningFuture future = new JShop2PlanningFuture(planningProcess);
        currentBestPlanCost = Double.POSITIVE_INFINITY;
        lastBestPlanCost = Double.POSITIVE_INFINITY;
        stepsSinceFirstPlanFound = -1;
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Plan planningResult = planningProcess.execute();
                    synchronized (future) {
                        if (!future.isCancelled()) {
                            if (planningResult != null) {
                                future.setResult(planningResult);
                            } else {
                                //null result signals cancellation
                                future.cancel(true);
                            }
                        }
                    }
                } catch (Exception ex) {
                    if (future.getStatus() == FutureStatus.FUTURE_IS_BEING_COMPUTED) {
                        future.computationException(ex);
                    } else {
                        throw new AisteException("Exception occurred in processing planning future result", ex);
                    }
                }
            }
        }).start();
        
        return future;
    }

    @Override
    public ILoggingHeaders getPerExperimentLoggingHeaders() {
        return new LoggingHeadersConcatenation(super.getPerExperimentLoggingHeaders(), new LoggingHeaders("maxEvaluatedPlans", "interruptTest"));        
    }

    @Override
    public List<Object> getPerExperimentLoggingData() {
        return new ListConcatenation<Object>(super.getPerExperimentLoggingData(), Arrays.asList(new Object[] {maxEvaluatedPlans, plannerInterruptTest}));
    }

    @Override
    public String toString() {
        return "JShop2Controller{" + "jshop=" + jshop + ", maxEvaluatedPlans=" + maxEvaluatedPlans + ", plannerInterruptTest=" + plannerInterruptTest + '}';
    }

    @Override
    public String getLoggableRepresentation() {
        return "JShop2_maxEval_" + maxEvaluatedPlans + "_" + (plannerInterruptTest == null ? "NoTest" : plannerInterruptTest.getLoggableRepresentation());
    }


    
    

    @Override
    public Class getRepresentationClass() {
        return IJShop2Representation.class;
    }

    private class JShop2PlanningFuture extends FutureWithListeners<Plan> {

        JShop2PlanningProcess process;

        public JShop2PlanningFuture(JShop2PlanningProcess process) {
            this.process = process;
        }
                
        @Override
        protected boolean cancelComputation(boolean mayInterruptIfRunning) {
            if(!mayInterruptIfRunning){
                return super.cancelComputation(mayInterruptIfRunning);
            } else {
                process.cancel();
                return true;
            }
        }
        
    }

    private class JShop2PlanningProcess {

        JSHOP2 jshop;
        IJShop2Problem problem;
        boolean cancelled;

        public JShop2PlanningProcess(JSHOP2 jshop, IJShop2Problem problem) {
            this.jshop = jshop;
            this.problem = problem;
            cancelled = false;
        }



        public Plan execute() {
                //initialization is now done in representation.getDomain()... Curse static objects!

		jshop.getDomain().setProblemConstants(problem.getProblemConstants());
                State s = problem.getInitialState();
		jshop.setState(s);
                
                Plan plan = jshop.branchAndBound(problem.getTaskList(), maxEvaluatedPlans);                                
                return plan;                
	}
        
        public void cancel(){
            if(!cancelled){
                jshop.cancel();
            } 
            cancelled = true;
        }
    }
    
    public static interface PlannerInterruptTest {
        public boolean shouldInterruptPrematurely(IPlanningGoal goal, double lastBestPlanCost, int numStepsSinceFirstPlan, JSHOP2 jshop);
        public String getLoggableRepresentation();
    }
    
    public static class StepsSinceFirstPlanInterruptTest implements PlannerInterruptTest {

        private int numStepsToTerminate;

        public StepsSinceFirstPlanInterruptTest(int numStepsToTerminate) {
            this.numStepsToTerminate = numStepsToTerminate;
        }                
        
        @Override
        public boolean shouldInterruptPrematurely(IPlanningGoal goal, double lastBestPlanCost, int numStepsSinceFirstPlan, JSHOP2 jshop) {
            return (numStepsSinceFirstPlan >= numStepsToTerminate);
        }

        @Override
        public String toString() {
            return "StepsSinceFirstPlanInterruptTest{" + "numStepsToTerminate=" + numStepsToTerminate + '}';
        }

        @Override
        public String getLoggableRepresentation() {
            return "NumSteps_" + numStepsToTerminate;
        }
        
        
        
    }
}
