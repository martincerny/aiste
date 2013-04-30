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
package cz.cuni.amis.aiste.simulations.fps1;

import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.impl.AgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.impl.SynchronuousEnvironmentExecutor;
import java.io.IOException;

/**
 *
 * @author Martin Cerny
 */
public class Test {
    public static void main(String args[]) throws IOException{
        FPS1.StaticDefs defs = FPS1MapReader.readMap(Test.class.getResourceAsStream("/fps1map1.txt"));
        
        FPS1 fpsEnvironment = new FPS1(defs);
        FPS1ReactiveController player1 = new FPS1ReactiveController();
        FPS1ReactiveController player2 = new FPS1ReactiveController();
        
        SynchronuousEnvironmentExecutor executor = new SynchronuousEnvironmentExecutor();
        executor.setEnvironment(fpsEnvironment);
        executor.addAgentController(new AgentExecutionDescriptor(FPS1AgentType.getInstance(), player1, fpsEnvironment));
        executor.addAgentController(new AgentExecutionDescriptor(FPS1AgentType.getInstance(), player2, fpsEnvironment));
        
        IEnvironmentExecutionResult result = executor.executeEnvironment(50 /*Max steps*/);
        
        System.out.println("Results: ");
        System.out.println("Player1: "+ result.getAgentResults().get(0).getTotalReward());
        System.out.println("Player2: "+ result.getAgentResults().get(1).getTotalReward());
    }
}
