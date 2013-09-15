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
package cz.cuni.amis.aiste.environment;

import java.util.Queue;

/**
 *
 * @author Martin Cerny
 */
public interface ISimulablePlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, ACTION extends IAction, ENVIRONMENT extends IEnvironment, GOAL extends IPlanningGoal> 
extends IPlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, ACTION, GOAL> {    
    /**
     * Check whether given body is in goal state. Useful especially for simulations.
     * @param body
     * @return 
     */
    public boolean isGoalState(ENVIRONMENT environment, AgentBody body, GOAL goal);
    
    /**
     * Translates an initial segment of a plan into actions for a specified simulated copy of the environment. The processed actions are removed from the respective queue
     * @param actionFromPlanner
     * @param body
     * @return 
     */
    public IReactivePlan<? extends ACTION> translateActionForSimulation(ENVIRONMENT environment, Queue<PLANNER_ACTION> actionsFromPlanner, AgentBody body);    
    
}
