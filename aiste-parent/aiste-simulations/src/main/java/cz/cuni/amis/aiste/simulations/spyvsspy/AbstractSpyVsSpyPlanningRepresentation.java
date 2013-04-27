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
        if(environment.getActiveBodies().size() <= 1){
            //no reactive layer applies when there are no opponents
            return null;
        }
        
        //rule 1: I have weapon and enemy is at the same square. Attack him!
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        if(info.numWeapons > 0){
            for(int oponentId = 0; oponentId < environment.bodyInfos.size(); oponentId++){
                if(oponentId != body.getId() && environment.bodyInfos.get(oponentId).locationIndex == info.locationIndex){
                    return new SequencePlan<SpyVsSpyAction>(new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, oponentId));
                }
            }
        }
        
        //rule 2: If there is armed oponent and I am not armed, run!
        if(info.numWeapons <= 0){
            for(SpyVsSpyBodyInfo oponentInfo : environment.bodyInfos){
                if(oponentInfo.body.getId() == body.getId()){
                    continue;
                }
                if(oponentInfo.numWeapons > 0 && oponentInfo.locationIndex == info.locationIndex){
                    int randomNeighbouringLocation = RandomUtils.randomElementLinearAccess(environment.defs.neighbours.get(info.locationIndex), environment.rand);
                    return new SequencePlan<SpyVsSpyAction>(new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, randomNeighbouringLocation));                    
                }
            }
        }
        
        //rule 3: weapons are useful, if there is an unguarded weapon, lets pick it up
        SpyVsSpyMapNode node = environment.nodes.get(info.locationIndex);
        if(node.numWeapons > 0 && node.traps.isEmpty()){
            return new SequencePlan<SpyVsSpyAction>(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_WEAPON, -1));            
        }
        
        return null;
    }
    
    
    
    
}
