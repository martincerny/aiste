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
package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractAgentController;

/**
 *
 * @author 
 */
public class SpyVsSpyReactiveController extends AbstractAgentController<SpyVsSpyAction, SpyVsSpy> {



    
    int scriptIndex = 0;
    SpyVsSpyAction[] script;

    @Override
    public void init(IEnvironment<SpyVsSpyAction> environment, SpyVsSpy representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        //prepare anything you need here
        int otherId = 1 - body.getId();
        script = new SpyVsSpyAction[] {
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, otherId),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, 2),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, otherId),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, 0),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, otherId),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, 0),
            
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, otherId),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, 3),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.ATTACK_AGENT, otherId),
            new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, 1),
        };
    }

    
    @Override
    public void onSimulationStep(double reward) {
        super.onSimulationStep(reward);
        if(reward < -10){
            scriptIndex = 0;
        }
        if(scriptIndex < script.length){
            act(script[scriptIndex]);
            scriptIndex++;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public Class getRepresentationClass() {
        return SpyVsSpy.class;
    }


    

}
