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
package cz.cuni.amis.aiste.environment.impl;

import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulableEnvironment;
import cz.cuni.amis.aiste.environment.ReactivePlanStatus;

/**
 *
 * @author Martin Cerny
 */
public class EmptyReactivePlan<ACTION extends IAction> implements IReactivePlan<ACTION> {

    public static final EmptyReactivePlan EMPTY_PLAN = new EmptyReactivePlan();
    
    public static <ACTION extends IAction> EmptyReactivePlan<ACTION> emptyPlan(){
        return EMPTY_PLAN;
    }
    
    @Override
    public ACTION nextAction() {
        throw new IllegalStateException("Empty plan cannot yield actions.");
    }

    @Override
    public ACTION peek() {
        throw new IllegalStateException("Empty plan cannot yield actions.");
    }

    @Override
    public ReactivePlanStatus getStatus() {
        //empty plan is always completed;
        return ReactivePlanStatus.COMPLETED;
    }

    @Override
    public IReactivePlan<ACTION> cloneForSimulation(ISimulableEnvironment<ACTION> environmentCopy) {
        return this;
    }

    @Override
    public boolean hasActions() {
        return false;
    }
    
    
    
    
}
