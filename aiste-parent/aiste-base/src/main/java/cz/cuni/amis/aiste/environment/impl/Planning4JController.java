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
import cz.cuni.amis.planning4j.IAsyncPlanner;
import cz.cuni.amis.planning4j.IPDDLObjectDomainProvider;
import cz.cuni.amis.planning4j.IPDDLObjectProblemProvider;
import cz.cuni.amis.planning4j.IPlanFuture;
import cz.cuni.amis.planning4j.IPlanner;
import cz.cuni.amis.planning4j.impl.PDDLObjectDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLObjectProblemProvider;
import java.util.List;

/**
 *
 * @author Martin
 */
public class Planning4JController extends AbstractAgentController<IAgentBody, IAction, IPDDLRepresentableEnvironment<IAgentBody, IAction>> {
    protected PDDLObjectDomainProvider domainProvider;

    IAsyncPlanner<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> planner;

    IPlanFuture planFuture = null;
    
    List<IAction> currentPlan
    
    public Planning4JController(IAsyncPlanner<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> planner) {
        this.planner = planner;
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
    }    
    
    @Override
    public void start() {
        super.start();
        startPlanning();
    }

    protected void startPlanning(){
        if(planFuture != null && !planFuture.isDone()){
            planFuture.cancel(true);
        }
        planFuture = planner.planAsync(domainProvider, new PDDLObjectProblemProvider(getEnvironment().getProblem(getBody())));
    }
    
}
