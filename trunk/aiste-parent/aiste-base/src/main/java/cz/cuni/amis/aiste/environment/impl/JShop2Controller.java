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
import cz.cuni.amis.utils.future.FutureStatus;
import cz.cuni.amis.utils.future.FutureWithListeners;
import cz.cuni.amis.utils.future.IFutureWithListeners;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class JShop2Controller extends AbstractPlanningController<JSHOP2, IJShop2Problem, Predicate, List<Plan>, IJShop2Representation<IAction>> {

    private JSHOP2 jshop;
    
    public JShop2Controller(ValidationMethod validationMethod) {
        super(validationMethod);
    }

    @Override
    public void init(IEnvironment<IAction> environment, IJShop2Representation<IAction> representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        jshop = representation.getDomain(body);
    }
    
    

    @Override
    protected List<Predicate> getActionsFromPlanningResult(List<Plan> result) {
        double minimalCost = Double.POSITIVE_INFINITY;
        List<Predicate> bestPlan = null;
        for (Plan p : result) {
            if (p.getCost() < minimalCost) {
                minimalCost = p.getCost();
                bestPlan = p.getOps();
            }
        }
        return bestPlan;
    }

    @Override
    protected boolean isPlanningResultSucces(List<Plan> result) {
        return !result.isEmpty();
    }

    @Override
    protected void getDebugRepresentationOfPlannerActions(List<Predicate> plannerActions, StringBuilder planSB) {
        for(Predicate act : plannerActions){
            JShop2Utils.GroundActionInfo info = JShop2Utils.getGroundInfo(act);
            planSB.append(" (").append(jshop.getDomain().getPrimitiveTasks()[info.actionId]);
            for(int constantIndex  : info.params){
                planSB.append(" ").append(jshop.getConstant(constantIndex));
            }
            planSB.append(")");
        }
    }

    
    
    @Override
    protected IFutureWithListeners<List<Plan>> startPlanningProcess() {
        final JShop2PlanningFuture future = new JShop2PlanningFuture();
        final JShop2PlanningProcess planningProcess = new JShop2PlanningProcess(jshop, representation.getProblem(body));
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    List<Plan> planningResult = planningProcess.execute();
                    synchronized (future) {
                        if (!future.isCancelled()) {
                            if (planningResult != null) {
                                future.setResult(planningResult);
                            } else {
                                //null result signalls cancellation
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
    public Class getRepresentationClass() {
        return IJShop2Representation.class;
    }

    private class JShop2PlanningFuture extends FutureWithListeners<List<Plan>> {
    }

    private class JShop2PlanningProcess {

        JSHOP2 jshop;
        IJShop2Problem problem;

        public JShop2PlanningProcess(JSHOP2 jshop, IJShop2Problem problem) {
            this.jshop = jshop;
            this.problem = problem;
        }



        public List<Plan> execute() {
                //initialization is now done in representation.getDomain()... Curse static objects!
		//TermConstant.initialize(domain.getDomainConstantCount() + problem.getProblemConstants().length);

		jshop.getDomain().setProblemConstants(problem.getProblemConstants());

		State s = problem.getInitialState();

		jshop.setState(s);

                LinkedList p = jshop.findPlans(problem.getTaskList(), 1 /*Number of plans*/);
                
                return p;
	}
    }
}
