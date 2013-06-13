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
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulablePlanningRepresentation;
import cz.cuni.amis.aiste.environment.ReactivePlanStatus;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.CGBodyPair;
import cz.cuni.amis.aiste.simulations.covergame.Loc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractCGPlanningRepresentation <DOMAIN, PROBLEM, PLANNER_ACTION>  implements ISimulablePlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, CGPairAction, CoverGame, CGPlanningGoal>{

    protected CoverGame env;

    public AbstractCGPlanningRepresentation(CoverGame env) {
        this.env = env;
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

    @Override
    public IReactivePlan<? extends CGPairAction> getDefaultReactivePlan(AgentBody body) {
        CGBodyPair bodyPair = env.bodyPairs.get(body.getId());
        int body0Id = bodyPair.bodyInfo0.id;
        int body1Id = bodyPair.bodyInfo1.id;
        List<CGRolePlan> plan0 = getDefaultRolesForSingleAgent(body0Id);
        List<CGRolePlan> plan1 = getDefaultRolesForSingleAgent(body1Id);
        return new CGPairRolePlan(plan0, plan1);
    }

    public List<CGRolePlan> getDefaultRolesForSingleAgent(int bodyId) {
        return Arrays.asList(new CGRolePlan[] {new CGRoleDefensive(env, bodyId), new CGRoleOverWatch(env, bodyId, true)});
    }

    
}
