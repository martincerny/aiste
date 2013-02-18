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

import java.util.Map;

/**
 * An environment that is capable of creating copies of itself for search and simulation purposes.
 * @author Martin Cerny
 */
public interface ISimulableEnvironment<ACTION extends IAction> extends IEnvironment<ACTION> {
    /**
     * Clones this environment for search. Should only actually clone
     * the data that changes while acting.
     * @return 
     */
    ISimulableEnvironment<ACTION> cloneForSimulation();
    
    /**
     * Runs one step of the simulation for search purposes.
     * @return rewards earned by all agents in the simulation
     * @throws SimulationException if an unexpected event occurs
     */
    Map<AgentBody, Double> simulateOneStep(Map<AgentBody, ACTION> actions);
        
}
