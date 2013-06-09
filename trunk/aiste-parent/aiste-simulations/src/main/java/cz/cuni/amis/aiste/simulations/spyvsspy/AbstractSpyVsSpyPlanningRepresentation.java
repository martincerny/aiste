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
package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulablePlanningRepresentation;
import cz.cuni.amis.aiste.environment.impl.SequencePlan;
import cz.cuni.amis.aiste.simulations.utils.RandomUtils;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractSpyVsSpyPlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION> extends AbstractSpyVsSpyRepresentation implements ISimulablePlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, SpyVsSpyAction, SpyVsSpy, SpyVsSpyPlanningGoal> 
{

    @Override
    public void setMarker(AgentBody body) {
        environment.setMarker(body);
    }

    @Override
    public boolean environmentChangedConsiderablySinceLastMarker(AgentBody body) {
        SpyVsSpy.ChangesSinceMarker changes = environment.getChangesSinceMarker(body);
        
        if(changes.agentHasDied){
            return true;
        }
        int numOtherAgents = environment.getActiveBodies().size() - 1;
        if(numOtherAgents == 0){
            return false;
        }
        if(changes.numItemsTaken > numOtherAgents * 2 + 2){
            return true;
        }
        if(changes.numTrapsSet > numOtherAgents + 2){
            return true;
        }
        if(changes.numOtherAgentsDeaths >= numOtherAgents){
            return true;
        }
        if(changes.numRemoversTaken > numOtherAgents * 2 + 2){
            return true;
        }
        if(changes.numWeaponsTaken >= numOtherAgents){
            return true;
        }
        
        return false;
    }

    @Override
    public IReactivePlan<? extends SpyVsSpyAction> evaluateReactiveLayer(AgentBody body) {
        SpyVsSpyAction reactiveAction = SpyVsSpyControllerHelper.evaluateReactiveLayer(environment, body);
        if(reactiveAction != null){
            return new SequencePlan<SpyVsSpyAction>(reactiveAction);
        } else {
            return null;
        }
    }
    
    
    
    
}
