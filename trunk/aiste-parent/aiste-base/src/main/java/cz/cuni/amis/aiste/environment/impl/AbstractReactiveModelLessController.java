/*
 * Copyright (C) 2012 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
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

import cz.cuni.amis.aiste.environment.IAgentBody;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.IModelLessRepresentableEnvironment;

/**
 * A controller for model-less environments that deliberates instantly and does
 * not have its own thread of execution.
 * @author Martin Cerny
 * @see IModelLessRepresentableEnvironment
 */
public abstract class AbstractReactiveModelLessController<BODY extends IAgentBody, ACTION, PERCEPT> extends AbstractAgentController<BODY, ACTION, IModelLessRepresentableEnvironment<BODY, ACTION, PERCEPT>> {

    @Override
    public boolean isApplicable(IModelLessRepresentableEnvironment<BODY, ACTION, PERCEPT> environment) {
        return environment instanceof IModelLessRepresentableEnvironment;
    }

    @Override
    public void onSimulationStep(double reward) {
        super.onSimulationStep(reward);
        ACTION nextAction = getActionForPercept(getEnvironment().getPercept(getBody()));
        getEnvironment().act(getBody(), nextAction);        
    }
    
    protected abstract ACTION getActionForPercept(PERCEPT percept);

    
}
