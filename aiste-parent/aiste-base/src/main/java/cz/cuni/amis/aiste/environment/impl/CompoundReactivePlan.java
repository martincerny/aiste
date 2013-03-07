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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A reactive plan that consists of multiple reactive plans executed in sequence.
 * @author Martin Cerny
 */
public class CompoundReactivePlan<ACTION extends IAction> implements IReactivePlan<ACTION> {
    List<IReactivePlan<ACTION>> subPlans;
    int subPlanIndex;

    public CompoundReactivePlan(List<IReactivePlan<ACTION>> subPlans) {
        this.subPlans = subPlans;
        subPlanIndex = 0;
    }
    
    public CompoundReactivePlan(IReactivePlan<ACTION> ... subPlans){
        this(Arrays.asList(subPlans));
    }

    @Override
    public ACTION nextAction() {
        skipCompletePlans();
        if(subPlanIndex >= subPlans.size()){
            throw new IllegalStateException("There are no more actions");
        }
        ACTION nextAction = subPlans.get(subPlanIndex).nextAction();
        skipCompletePlans();        
        return nextAction;
    }

    @Override
    public ACTION peek() {
        skipCompletePlans();
        if(subPlanIndex >= subPlans.size()){
            throw new IllegalStateException("There are no more actions");
        }
        return subPlans.get(subPlanIndex).peek();
    }

    @Override
    public ReactivePlanStatus getStatus() {
        skipCompletePlans();
        if(subPlanIndex >= subPlans.size()){
            return ReactivePlanStatus.COMPLETED;
        }
        return subPlans.get(subPlanIndex).getStatus();
    }

    @Override
    public IReactivePlan<ACTION> cloneForSimulation(ISimulableEnvironment<ACTION> environmentCopy) {
        List<IReactivePlan<ACTION>> subPlanClones = new ArrayList<IReactivePlan<ACTION>>(subPlans.size() - subPlanIndex);
        for(int i = subPlanIndex; i < subPlans.size(); i++){
            subPlanClones.add(subPlans.get(i).cloneForSimulation(environmentCopy));
        }
        return new CompoundReactivePlan<ACTION>(subPlanClones);
    }

    protected void skipCompletePlans() {
        while(subPlans.get(subPlanIndex).getStatus() == ReactivePlanStatus.COMPLETED && subPlanIndex < subPlans.size()){
            subPlanIndex++;
        }
    }
    
    
}
