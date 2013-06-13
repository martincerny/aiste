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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A reactive plan that is actually just a sequence of actions without
 * any further intelligence.
 * @author Martin Cerny
 */
public class SequencePlan<ACTION extends IAction> implements IReactivePlan<ACTION> {
    List<? extends ACTION> actions;
    int currentIndex;

    public SequencePlan(List<? extends ACTION> actions) {
        this.actions = actions;
        currentIndex = 0;
    }
    
    public SequencePlan(ACTION ... actions){
        this(Arrays.asList(actions));
    }

    @Override
    public ACTION nextAction() {
        ACTION act = peek();
        currentIndex++;
        return act;
    }

    @Override
    public ACTION peek() {
        if(currentIndex >= actions.size()){
            throw new IllegalStateException("There are no more actions.");
        }        
        return actions.get(currentIndex);
    }
    
    
    @Override
    public ReactivePlanStatus getStatus() {
        if(currentIndex < actions.size()){
            return ReactivePlanStatus.EXECUTING;
        } else {
            return ReactivePlanStatus.COMPLETED;
        }
    }

    @Override
    public boolean hasActions() {
        return !getStatus().isFinished();
    }
    
    

    @Override
    public IReactivePlan<ACTION> cloneForSimulation(ISimulableEnvironment<ACTION> environmentCopy) {
        return new SequencePlan<ACTION>(actions.subList(currentIndex, actions.size()));
    }
    
    
    
}
