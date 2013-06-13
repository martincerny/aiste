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
import cz.cuni.amis.pathfinding.alg.astar.AStar;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public abstract class CGRolePlan extends AbstractReactivePlan<CGAction> {

    
    protected CoverGame env;
    protected int bodyId;

    private List<Loc> path;
    private long lastPathRefresh = -1;
    
    protected AStar<Loc> astar;

    
    public CGRolePlan(CoverGame env, int bodyId) {
        this.env = env;
        this.bodyId = bodyId;
        astar = new AStar<Loc>(env.defs.navGraphMap);
    }        
    
    
    /**
     * Action to be triggerred, when theres nothing left to do :-)
     * @return 
     */
    protected CGAction defaultAction(){
        int bestTarget = env.getBestTarget(bodyId);
        if(bestTarget >= 0){
            return new CGAction(CGAction.Action.SHOOT, bestTarget);
        } else if(env.isThereNeighbouringCover(getBodyInfo().loc)) {
            return CGAction.TAKE_FULL_COVER_ACTION;
        } else {
            return CGAction.NO_OP_ACTION;
        }
    }
    
    @Override
    protected void updateStepForNextAction(){}

    @Override
    public boolean hasActions() {
        return true; //role plans always have actions
    }
    
    
    
    
    protected List<Loc> computePath(){
        return null;
    }
    
    protected void resetPath(){
        path = null;
        lastPathRefresh = -1;
    }
    
    protected List<Loc> getPath(){
        if(env.getTimeStep() > lastPathRefresh){
            path = computePath();
            lastPathRefresh = env.getTimeStep();
        }
        return path;
    }
    
    CoverGame.CGBodyInfo getBodyInfo() {
        return env.bodyInfos.get(bodyId);
    }
    
    /**
     * If there is a path found, returns action that takes the first step.
     * @return  The first step action or null, if no path is found
     */
    CGAction followPath(){
        List<Loc> path = getPath();
        if(path != null && path.size() > 1){
            return new CGAction(CGAction.Action.MOVE, path.get(1));
        }
        else {
            return null;
        }
    }
    
    
    
}
