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
package cz.cuni.amis.aiste.simulations.simplefps;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractAgentController;
import cz.cuni.amis.pathfinding.alg.astar.AStar;

/**
 *
 * @author 
 */
public class SimpleFPSReactiveController extends AbstractAgentController<SimpleFPSAction, SimpleFPS> {

    @Override
    public void init(IEnvironment<SimpleFPSAction> environment, SimpleFPS representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        //prepare everything you need in here
    }



    @Override
    public void onSimulationStep(double reward) {
        super.onSimulationStep(reward);
        //zde proved logiku agenta, muzes pouzivat libovolne vereje metody SimpleFPS - tvuj agent nemusi byt vubec obecny
        //reward je odmena za posledni provedeny tah
        //akce se provadi takto:
        act(new SimpleFPSAction(/*Tady si asi budes chtit predat nejake info*/));
        
        //pro hledani cesty muzes pouzit treba (s doplnenim parametru)
        AStar astar = new AStar(null);
        astar.findPath(null);
        
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public Class getRepresentationClass() {
        return SimpleFPS.class;
    }


    

}
