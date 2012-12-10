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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IAgentBody;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IEnvironment;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractAgentController<BODY extends IAgentBody, ACTION extends IAction, ENVIRONMENT extends IEnvironment<BODY, ACTION>> implements IAgentController<BODY, ACTION, ENVIRONMENT> {

    private ENVIRONMENT environment;
    private BODY body;
    private long stepDelay;
    
    @Override
    public void init(ENVIRONMENT environment, BODY body, long stepDelay) {
        
        if(this.environment != null){
            throw new AisteException("A controller may be initialized only once");
        }
        this.environment = environment;
        this.body = body;
        this.stepDelay = stepDelay;
    }

    /**
     * Act for this agent's body.
     * @param action 
     */
    protected void act(ACTION action){
        getEnvironment().act(body, action);
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

    @Override
    public BODY getBody() {
        return body;
    }

    public ENVIRONMENT getEnvironment() {
        return environment;
    }

    public long getStepDelay() {
        return stepDelay;
    }

  
    
}
