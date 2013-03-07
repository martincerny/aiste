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

    public List<SpyVsSpyPlanningGoal> getRelevantGoals(AgentBody body) {
        List<SpyVsSpyPlanningGoal> goals = new ArrayList<SpyVsSpyPlanningGoal>(2);
        if (environment.getAllBodies().size() > 1) {
            int oponentId;
            if (body.getId() == 0) {
                oponentId = 1;
            } else {
                oponentId = 0;
            }
            goals.add(new SpyVsSpyPlanningGoal(SpyVsSpyPlanningGoal.Type.KILL_OPONENT, oponentId, 100));
        } 
        goals.add(new SpyVsSpyPlanningGoal(SpyVsSpyPlanningGoal.Type.DIRECT_WIN, 50));
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

}
