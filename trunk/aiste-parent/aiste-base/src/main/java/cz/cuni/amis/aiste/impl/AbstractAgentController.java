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

package cz.cuni.amis.aiste.impl;

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.IAgentBody;
import cz.cuni.amis.aiste.IAgentController;
import cz.cuni.amis.aiste.IEnvironment;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractAgentController<BODY extends IAgentBody, ACTION> implements IAgentController<BODY, ACTION> {

    protected IEnvironment<BODY, ACTION> environment;
    protected BODY body;
    protected long stepDelay;
    
    @Override
    public void init(IEnvironment<BODY, ACTION> environment, BODY body, long stepDelay) {
        
        if(this.environment != null){
            throw new AisteException("A controller may be initialized only once");
        }
        this.environment = environment;
        this.body = body;
        this.stepDelay = stepDelay;
    }

    @Override
    public void onSimulationStep(double reward) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void start() {
    }

}
