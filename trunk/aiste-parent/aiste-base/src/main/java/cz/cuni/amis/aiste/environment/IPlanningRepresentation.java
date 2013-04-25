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

import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public interface IPlanningRepresentation<DOMAIN, PROBLEM, PLANNER_ACTION, ACTION extends IAction, GOAL extends IPlanningGoal> extends IEnvironmentRepresentation {
    public DOMAIN getDomain(AgentBody body);
    public PROBLEM getProblem(AgentBody body, GOAL goal);
    public IReactivePlan<? extends ACTION> translateAction(PLANNER_ACTION actionFromPlanner, AgentBody body);    
    
    /**
     * Gets a list of goals that are relevant to given body.
     * The list is sorted by priority (highest first).
     * @param body
     * @return 
     */
    public List<GOAL> getRelevantGoals(AgentBody body);
    
    /**
     * Sets a marker for further retrieval with {@link #environmentChangedConsiderablySinceLastMarker() }
     */
    public void setMarker(AgentBody body);
    
    /**
     * True if environment changed so much since the marked time, that plans
     * based on the state at the time of the marker are probably useless.
     * @return 
     */
    public boolean environmentChangedConsiderablySinceLastMarker(AgentBody body);
}