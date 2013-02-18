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
package cz.cuni.amis.aiste.simulations.keylockmaze;

import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.impl.SynchronuousEnvironmentExecutor;

/**
 *
 * @author Martin Cerny
 */
public class Test {
    public static void main(String args[]){
        KeyLockMaze b = new KeyLockMaze();
        KeyLockNaiveController player = new KeyLockNaiveController();
        
        SynchronuousEnvironmentExecutor executor = new SynchronuousEnvironmentExecutor();
        executor.setEnvironment(b);
        executor.addAgentController(KeyLockAgentType.getInstance(), player, b);
        
        IEnvironmentExecutionResult result = executor.executeEnvironment(300 /*Max steps*/);
        
        System.out.println("Result: "+ result.getAgentResults().get(0).getTotalReward());
    }
}
