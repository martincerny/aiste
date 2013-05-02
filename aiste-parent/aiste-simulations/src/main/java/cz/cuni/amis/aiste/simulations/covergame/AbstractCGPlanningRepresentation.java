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

import com.sun.xml.internal.messaging.saaj.soap.ver1_1.Body1_1Impl;
import cz.cuni.amis.aiste.simulations.fps1.*;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulablePlanningRepresentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractCGPlanningRepresentation <DOMAIN, PROBLEM, PLANNER_ACTION>  implements ISimulablePlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, CGPairAction, CoverGame, CGPlanningGoal>{

    protected CoverGame env;

    public AbstractCGPlanningRepresentation(CoverGame env) {
        this.env = env;
    }

    
    
    protected int[] getOponentIds(int teamNo){
        int[] ids = new int[2];
        int idIndex = 0;
        for(CoverGame.CGBodyInfo bodyInfo : env.bodyInfos){
            if(bodyInfo.getTeamId() != teamNo){
                ids[idIndex] = bodyInfo.id;
                idIndex++;
            }
        }
        return ids;
    }
    
    protected OponentData[] getOponentData(int teamNo){
        int [] ids = getOponentIds(teamNo);
        OponentData[] data = new OponentData[ids.length];
        for(int i = 0; i < ids.length; i++){
            data[i] = new OponentData();
            Loc oponentLocation = env.bodyInfos.get(ids[i]).loc;
            for(Loc navPoint : env.defs.navGraph.keySet()){
                if(env.isVisible(navPoint, oponentLocation)){
                    data[i].visibleNavpoints.add(navPoint);
                    if(!env.isCovered(navPoint, oponentLocation)){
                        data[i].navpointsInvalidatingCover.add(navPoint);
                    }
                    if(!env.isCovered(oponentLocation, navPoint)){
                        data[i].uncoveredNavpoints.add(navPoint);
                    }
                }
            }
        }
        return data;
        
    }
    
    @Override
    public boolean isGoalState(AgentBody body, CGPlanningGoal goal) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean environmentChangedConsiderablySinceLastMarker(AgentBody body) {
        //TODO
        return false;
    }

    @Override
    public IReactivePlan<? extends CGPairAction> evaluateReactiveLayer(AgentBody body) {
        return null;
    }

    @Override
    public List<CGPlanningGoal> getRelevantGoals(AgentBody body) {
        return Collections.singletonList(new CGPlanningGoal(CGPlanningGoal.Type.FIND_COVER, 10));
    }

    @Override
    public void setMarker(AgentBody body) {
        //TODO
    }

    @Override
    public void setEnvironment(CoverGame env) {
        this.env = env;
    }

    protected static class OponentData {
        /**
         * Navpoints that are not covered when oponent attacks
         */
        List<Loc> uncoveredNavpoints = new ArrayList<Loc>();
        
        /**
         * Navpoints that are visible to oponent (and oponent is visible from there)
         */
        List<Loc> visibleNavpoints = new ArrayList<Loc>();
        
        /**
         * Navpoints from which the oponent has no cover
         */
        List<Loc> navpointsInvalidatingCover = new ArrayList<Loc>();
    }
}
