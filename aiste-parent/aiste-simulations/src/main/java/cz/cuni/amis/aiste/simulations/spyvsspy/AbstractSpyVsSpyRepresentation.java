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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IActionFailureRepresentation;
import cz.cuni.amis.aiste.environment.ISimulableEnvironmentRepresentation;
import cz.cuni.amis.aiste.environment.impl.CompoundReactivePlan;
import cz.cuni.amis.aiste.environment.impl.SequencePlan;
import cz.cuni.amis.aiste.simulations.spyvsspy.SpyVsSpy.PursueOponentPlan;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractSpyVsSpyRepresentation implements IActionFailureRepresentation, ISimulableEnvironmentRepresentation<SpyVsSpy> {
    
    protected SpyVsSpy environment;

    public AbstractSpyVsSpyRepresentation() {
    }

    private boolean agentUnableToReachGoal(AgentBody body) {
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        for(int itemType = 0; itemType < environment.defs.numItemTypes; itemType++){
            //If there is an item that I do not have and it is not "laying" somewhere, I am unable to reach the goal
            if(!info.itemsCarried.contains(itemType)){
                boolean itemFoundInEnvironment = false;
                for(SpyVsSpyMapNode node : environment.nodes){
                    if(node.items.contains(itemType)){
                        //the item can be found
                        itemFoundInEnvironment = true;
                        break;
                    }
                }
                if(!itemFoundInEnvironment){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Relevant goals for all planning representations
     * @param body
     * @return 
     */
    public List<SpyVsSpyPlanningGoal> getRelevantGoals(AgentBody body) {
        if(environment.getAllBodies().size() > 2){
            throw new UnsupportedOperationException("Relevant goal selection not supported for more than two agents");
        }
        
        List<SpyVsSpyPlanningGoal> goals = new ArrayList<SpyVsSpyPlanningGoal>(2);
        
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        if(info.numWeapons == 0 && environment.getAllBodies().size() > 1){
            //arming yourself is as important as winning the game: It may easily come to that
            int priorityOfArming = (int)environment.defs.rewardReachGoal;
            goals.add(new SpyVsSpyPlanningGoal(SpyVsSpyPlanningGoal.Type.GET_ARMED, priorityOfArming));
        }
                
        //directly winning is always a good option, unless the agent is easily proven unable to do so
        if(!agentUnableToReachGoal(body)){
            int priorityOfWinning = (int)environment.defs.rewardReachGoal;
            goals.add(new SpyVsSpyPlanningGoal(SpyVsSpyPlanningGoal.Type.DIRECT_WIN, priorityOfWinning));
        }
        
        if (environment.getAllBodies().size() > 1) {
            int oponentId;
            if (body.getId() == 0) {
                oponentId = 1;
            } else {
                oponentId = 0;
            }
            /* By killing an oponnent, I reduce his reward and at the same time grow my chance of winning (because I take his belongings)*/
            int priorityOfKilling = (int) (-environment.defs.rewardDeath + (environment.defs.rewardReachGoal / 3));
            goals.add(new SpyVsSpyPlanningGoal(SpyVsSpyPlanningGoal.Type.KILL_OPONENT, oponentId, priorityOfKilling));
        } 
        return goals;
    }
    
    
    public boolean isGoalState(AgentBody body, SpyVsSpyPlanningGoal goal) {
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        switch (goal.getType()) {
            case DIRECT_WIN: {
                if (info.locationIndex != environment.defs.destination) {
                    return false;
                }
                if (info.itemsCarried.size() != environment.defs.numItemTypes) {
                    return false;
                }
                return true;
            }
            case KILL_OPONENT: {
                return environment.agentsKilledThisRound.contains(environment.getAllBodies().get(goal.getParameter()));
            }
            case GET_ARMED: {
                return info.numWeapons > 0;
            }
            default: {
                throw new AisteException("Unrecognized goal type: " + goal.getType());
            }

        }
    }

    @Override
    public boolean lastActionFailed(AgentBody body) {
        return environment.lastActionFailed(body);
    }

    @Override
    public void setEnvironment(SpyVsSpy env) {
        if (env.defs != this.environment.defs) {
            throw new IllegalArgumentException("Environment could only be set to a copy of the original env.");
        }
        this.environment = env;
    }

    protected CompoundReactivePlan<SpyVsSpyAction> getFollowAndAttackReactivePlan(AgentBody body, int targetOponent) {
        return new CompoundReactivePlan<SpyVsSpyAction>(new PursueOponentPlan(environment, body.getId(), targetOponent), new SequencePlan<SpyVsSpyAction>(new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, targetOponent)));
    }

}
