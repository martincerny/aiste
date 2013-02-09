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

import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IAgentBody;
import cz.cuni.amis.aiste.environment.IPDDLRepresentableEnvironment;
import cz.cuni.amis.planning4j.*;
import cz.cuni.amis.planning4j.impl.PDDLObjectDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLObjectProblemProvider;
import java.util.ArrayDeque;
import java.util.Queue;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin
 */
public class Planning4JController extends AbstractAgentController<IAgentBody, IAction, IPDDLRepresentableEnvironment<IAgentBody, IAction>> {

    private final Logger logger = Logger.getLogger(Planning4JController.class);

    protected PDDLObjectDomainProvider domainProvider;

    IAsyncPlanner<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> planner;

    IPlanFuture planFuture = null;

    Queue<IAction> currentPlan;

    public Planning4JController(IAsyncPlanner<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> planner) {
        this.planner = planner;
        currentPlan = new ArrayDeque<IAction>();
    }

    @Override
    public void init(IPDDLRepresentableEnvironment<IAgentBody, IAction> environment, IAgentBody body, long stepDelay) {
        super.init(environment, body, stepDelay);
        domainProvider = new PDDLObjectDomainProvider(getEnvironment().getDomain(getBody()));
    }

    @Override
    public boolean isApplicable(IPDDLRepresentableEnvironment<IAgentBody, IAction> environment) {
        return environment instanceof IPDDLRepresentableEnvironment;
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
                        currentPlan = new ArrayDeque<IAction>(getEnvironment().convertPlanToActions(planningResult.getPlan()));
                        if (logger.isDebugEnabled()) {
                            logger.debug("Planning succesful, found plan of length " + currentPlan.size());
                        }
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

        if (currentPlan.isEmpty()) {
            if (planFuture == null || planFuture.isCancelled()) {
                startPlanning();
            }
        } else {
            IAction nextAction = currentPlan.poll();
            getEnvironment().act(getBody(), nextAction);
        }
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
        planFuture = planner.planAsync(domainProvider, new PDDLObjectProblemProvider(getEnvironment().getProblem(getBody())));
    }
}
