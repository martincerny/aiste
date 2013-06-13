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
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.CGBodyInfo;

/**
 *
 * @author Martin Cerny
 */
public class CGRoleOverWatch extends CGRolePlan {

    /**
     * Whether to prefer suppression to shooting, whenever possible
     */
    private boolean preferSuppression;

    public CGRoleOverWatch(CoverGame env, int bodyId){
        this(env, bodyId, true);
    }
    
    public CGRoleOverWatch(CoverGame env, int bodyId, boolean preferSuppression) {
        super(env, bodyId);
        this.preferSuppression = preferSuppression;
    }
    
    
    @Override
    protected void updateStepForNextAction() {
    }

    
    
    @Override
    public CGAction peek() {
        CGBodyInfo bodyInfo = getBodyInfo();
        if(preferSuppression && bodyInfo.suppressCooldown <= 1){
            CGBodyInfo partnerInfo = env.bodyPairs.get(bodyInfo.getTeamId()).getOtherInfo(bodyInfo);
            int largestThreat = -1;
            double largestThreatAim = 0;            
            for(int opp : env.getOponentIds(bodyInfo.getTeamId())){
                CGBodyInfo opponentInfo = env.bodyInfos.get(opp);
                if(env.isVisible(partnerInfo.loc, opponentInfo.loc)) {
                    double aim = env.getHitProbability(opponentInfo, partnerInfo);
                    if(aim > largestThreatAim){
                        largestThreat = opp;
                        largestThreatAim = aim;
                    }
                }
            }
            if(largestThreat > 0){
                return new CGAction(CGAction.Action.SUPPRESS, largestThreat);
            }
        }
        int target = env.getBestTarget(bodyId);
        if(target < 0){
            return defaultAction();
        } else {
            return new CGAction(CGAction.Action.SHOOT, target);            
        }
    }

    
    
    @Override
    public ReactivePlanStatus getStatus() {
        CGBodyInfo bodyInfo = getBodyInfo();
        if(env.getNumThreats(bodyId, bodyInfo.loc) > 0){
            return ReactivePlanStatus.FAILED;
        } else { 
            //overwatch is always completed - only waits for the other action to complete
            return ReactivePlanStatus.COMPLETED;
        } 
    }

    @Override
    public String toString() {
        return "OverWatch{" + "preferSuppression=" + preferSuppression + '}';
    }

  

    
    
}
