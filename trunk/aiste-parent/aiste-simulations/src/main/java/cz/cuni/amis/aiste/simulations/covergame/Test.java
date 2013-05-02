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
package cz.cuni.amis.aiste.simulations.covergame;

import cz.cuni.amis.aiste.environment.impl.DoNothingAgentController;
import cz.cuni.amis.aiste.simulations.fps1.*;
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
        CoverGame.StaticDefs defs = CGMapReader.readMap(Test.class.getResourceAsStream("/cg_map1.txt"));
        
        CoverGame fpsEnvironment = new CoverGame(defs);
        
        SynchronuousEnvironmentExecutor executor = new SynchronuousEnvironmentExecutor();
        executor.setEnvironment(fpsEnvironment);
        executor.addAgentController(new AgentExecutionDescriptor(CGAgentType.getTeam0Instance(), new DoNothingAgentController(), fpsEnvironment));
        executor.addAgentController(new AgentExecutionDescriptor(CGAgentType.getTeam0Instance(), new DoNothingAgentController(), fpsEnvironment));
        executor.addAgentController(new AgentExecutionDescriptor(CGAgentType.getTeam1Instance(), new DoNothingAgentController(), fpsEnvironment));
        executor.addAgentController(new AgentExecutionDescriptor(CGAgentType.getTeam1Instance(), new DoNothingAgentController(), fpsEnvironment));
        
        IEnvironmentExecutionResult result = executor.executeEnvironment(5 /*Max steps*/);
        
        System.out.println("Results: ");
        System.out.println("Player1: "+ result.getAgentResults().get(0).getTotalReward());
        System.out.println("Player2: "+ result.getAgentResults().get(1).getTotalReward());
        System.out.println("Player3: "+ result.getAgentResults().get(2).getTotalReward());
        System.out.println("Player4: "+ result.getAgentResults().get(3).getTotalReward());
    }
}
