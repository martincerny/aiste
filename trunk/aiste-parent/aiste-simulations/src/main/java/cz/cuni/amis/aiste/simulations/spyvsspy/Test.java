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

import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.impl.SynchronuousEnvironmentExecutor;

/**
 *
 * @author Martin Cerny
 */
public class Test {

    public static void main(String args[]) {
        SpyVsSpy b = new SpyVsSpy();
        SpyVsSpyReactiveController player1 = new SpyVsSpyReactiveController();
        SpyVsSpyReactiveController player2 = new SpyVsSpyReactiveController();

        SynchronuousEnvironmentExecutor executor = new SynchronuousEnvironmentExecutor();
        executor.setEnvironment(b);
        executor.addAgentController(SpyVsSpyAgentType.getInstance(), player1);
        executor.addAgentController(SpyVsSpyAgentType.getInstance(), player2);

        IEnvironmentExecutionResult result = executor.executeEnvironment(50 /*
                 * Max steps
                 */);

        System.out.println("Results: ");
        System.out.println("Player1: " + result.getAgentResults().get(0).getTotalReward());
        System.out.println("Player2: "+ result.getAgentResults().get(1).getTotalReward());
    }
}
