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
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IEnvironmentModel;
import java.util.Map;

/**
 * An environment that delegates all its processing to a {@link IEnvironmentModel}
 * @author Martin Cerny
 */
public abstract class AbstractModelEnvironment<ACTION extends IAction> extends AbstractSynchronizedEnvironment<ACTION> {

    public AbstractModelEnvironment(Class<ACTION> actionClass) {
        super( actionClass);
    }
    
    /**
     * Gets the underlying model. Will not be called prior to environment start.
     * @return 
     */
    protected abstract IEnvironmentModel<ACTION> getModel();

    
    
    @Override
    protected Map<AgentBody, Double> simulateOneStepInternal(Map<AgentBody, ACTION> actionsToPerform) {
        return getModel().simulateOneStep(actionsToPerform);
    }

    @Override
    public boolean isFinished() {
        return getModel().isFinished();
    }

    @Override
    public void removeAgentBody(AgentBody body) {
        super.removeAgentBody(body);
        getModel().removeAgentBody(body);        
    }
    
    
    
}
