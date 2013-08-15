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
package cz.cuni.amis.aiste.simulations.covergame;

import cz.cuni.amis.aiste.environment.ReactivePlanStatus;
import cz.cuni.amis.aiste.environment.impl.AbstractReactivePlan;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.CGBodyInfo;
import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import cz.cuni.amis.pathfinding.map.IPFMapView;
import cz.cuni.amis.utils.astar.AStarGoal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class CGRoleAggressive extends CGRolePlan {

    private int maxThreatsToAdvance;
    private int preferredTargetIndex;

    public CGRoleAggressive(CoverGame env, int bodyId, int preferredTargetIndex){
        this(env, bodyId, preferredTargetIndex, 0);
    }
    
    public CGRoleAggressive(CoverGame env, int bodyId, int preferredTargetIndex, int maxThreatsToAdvance) {
        super(env, bodyId);
        this.maxThreatsToAdvance = maxThreatsToAdvance;
        this.preferredTargetIndex = preferredTargetIndex;
    }
    
    
    @Override
    protected void updateStepForNextAction() {
    }

    
    @Override
    public CGAction peek() {
        int target = env.getBestTarget(bodyId);
        if(target < 0){
            CGAction followPathAction = followPath();
            if(followPathAction != null){
                return followPathAction;
            } else {
                return defaultAction();
            }
        }
        return new CGAction(CGAction.Action.SHOOT, target);
    }

    
    
    
    @Override
    public ReactivePlanStatus getStatus() {
        CGBodyInfo bodyInfo = getBodyInfo();        
        
        boolean enemyKilledLastRound = false;
        for(CGBodyInfo killedInfo : env.agentsKilledLastRound){
            if(killedInfo.getTeamId() != bodyInfo.getTeamId()){
                enemyKilledLastRound = true;
                break;
            }
        }
        
        if(env.getNumThreats(bodyId, bodyInfo.loc) > maxThreatsToAdvance){
            return ReactivePlanStatus.FAILED;
        } else if(enemyKilledLastRound) {
            return ReactivePlanStatus.COMPLETED;
        } else {
            return ReactivePlanStatus.EXECUTING;            
        }
    }

    /**
     * Aggressive path computation - unused
     * @return 
     */
    @Override
    protected List<Loc> computePath() {
        final CGBodyInfo bodyInfo = getBodyInfo();
        AStarResult<Loc> result = astar.findPath(new CGAStarGoal(env, bodyInfo.loc) {

            @Override
            public boolean isGoalReached(Loc actualNode) {
                return env.getOpponentTeamData(bodyInfo.getTeamId()).allVisibleNavPoints.contains(actualNode);
            }

            @Override
            public int getEstimatedCostToGoal(Loc node) {
                if(isGoalReached(node)){
                    return 0;
                } else {
                    return 1;
                }
            }
                        
        }, new IPFMapView<Loc>() {

            @Override
            public Collection<Loc> getExtraNeighbors(Loc node, Collection<Loc> mapNeighbors) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public int getNodeExtraCost(Loc node, int mapCost) {
                return 0;
            }

            @Override
            public int getArcExtraCost(Loc nodeFrom, Loc nodeTo, int mapCost) {
                return 0;
            }

            @Override
            public boolean isNodeOpened(Loc node) {
                return env.getNumThreats(bodyId, node) < maxThreatsToAdvance;
            }

            @Override
            public boolean isArcOpened(Loc nodeFrom, Loc nodeTo) {
                return true;
            }
            
        });
        
        if(result.isSuccess()){
            return result.getPath();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Aggressive{maxThreatsToAdvance=" + maxThreatsToAdvance + '}';
    }

    
    
}
