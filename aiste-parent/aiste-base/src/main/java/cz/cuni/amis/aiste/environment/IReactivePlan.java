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

/**<p>
 * A simple prescription of actions that can be executed without complex deliberation.
 * Primarily used when translating abstract actions from planners and others into
 * low-level actions that may be carried out in the environment.
 * In many cases it is just a sequence of actions to perform, but may contain
 * loops and other. </p>
 * <p>
 * The methods are allowed to query underlying environment for
 * current status, but should not perform any complex computation. For example,
 * they are not expected to take significant amount of per-step deliberation time.
 *</p>
 * @author Martin Cerny
 */
public interface IReactivePlan<ACTION extends IAction> {       
    /**
     * Gets action to perform just now and change the state of the plan.
     * @return next action 
     * @throws IllegalStateException if there are no more actions (as indicated by {@link #getStatus() }
     */
    public ACTION nextAction();
    
    
    /**
     * Gets action to perform just now, but does not change the state of the plan.
     * @return 
     * @throws IllegalStateException if there are no more actions (as indicated by {@link #getStatus() }
     */
    public ACTION peek();
    
    /**
     * Return the status of the plan.
     * @return 
     */
    public ReactivePlanStatus getStatus();
    
    /**
     * This is needed in order to allow plan validation by environment simulation.
     * This method should create a copy of the original plan, that retains all state,
     * but queries given environment instead of the original environment.
     * @param environmentCopy
     * @return copy of the action
     * @throws UnsupportedOperationException if this reactive plan does not support cloning for simulation
     */
    public IReactivePlan<ACTION> cloneForSimulation(ISimulableEnvironment<ACTION> environmentCopy);
}
