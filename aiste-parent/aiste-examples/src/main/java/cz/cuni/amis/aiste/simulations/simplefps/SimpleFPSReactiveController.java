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

import cz.cuni.amis.aiste.environment.impl.AbstractAgentController;

/**
 *
 * @author 
 */
public class SimpleFPSReactiveController extends AbstractAgentController<SimpleFPSAgentBody, SimpleFPSAction, SimpleFPS> {

    @Override
    public boolean isApplicable(SimpleFPS environment) {
        return (environment instanceof SimpleFPS);
    }

    @Override
    public void init(SimpleFPS environment, SimpleFPSAgentBody body, long stepDelay) {
        super.init(environment, body, stepDelay);
        //prepare anything you need here
    }

    @Override
    public void onSimulationStep(double reward) {
        super.onSimulationStep(reward);
        //zde proved logiku agenta, muzes pouzivat libovolne vereje metody SimpleFPS - tvuj agent nemusi byt vubec obecny
        //reward je odmena za posledni provedeny tah
        //akce se provadi takto:
        act(new SimpleFPSAction(/*Tady si asi budes chtit predat nejake info*/));
        
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }


    

}
