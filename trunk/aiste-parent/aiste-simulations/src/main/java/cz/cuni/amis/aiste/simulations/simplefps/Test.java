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

import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.impl.AgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.impl.SynchronuousEnvironmentExecutor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;



/**
 *
 * @author Martin Cerny
 */
public class Test 
{
    static int maxSteps = 100;
    static int minP = 2;
    static int maxP = 2;
    static String testmap = "/map1.txt";      
    
    public static void main(String args[]) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException
    {
        System.out.println("Building enviroment ~");
        SimpleFPS fpsEnvironment = new SimpleFPS(minP, maxP, testmap);
        System.out.println("Enviroment was succesfully created ~");
        System.out.println("");
        System.out.println("System strongly recommends to read map before simulating a game.");
        System.out.println("Press Enter to continue.");
        System.in.read();
        
        System.out.println("Establishing controllers to players ~");
        SimpleFPSReactiveController player1 = new SimpleFPSReactiveController();        
        SimpleFPSReactiveController player2 = new SimpleFPSReactiveController();
        System.out.println("Controllers succesfully established ~");
        System.out.println("");
        
        System.out.println("Setting up enviroment executor ~");
        SynchronuousEnvironmentExecutor executor = new SynchronuousEnvironmentExecutor();
        executor.setEnvironment(fpsEnvironment);
        System.out.println("Enviroment added ~");
        executor.addAgentController(new AgentExecutionDescriptor(SimpleFPSAgentType.getInstance(), player1, fpsEnvironment));
        executor.addAgentController(new AgentExecutionDescriptor(SimpleFPSAgentType.getInstance(), player2, fpsEnvironment));
        
        //player1.onSimulationStep(0);
        
        System.out.println("Controllers added ~");
        System.out.println("Enviroment executor was succesfully created ~");
        System.out.println("");
        
        
        IEnvironmentExecutionResult result = executor.executeEnvironment(maxSteps);
        
        System.out.println("Results: ");
        System.out.println("Player1: "+ result.getAgentResults().get(0).getTotalReward());
        System.out.println("Player2: "+ result.getAgentResults().get(1).getTotalReward());
    }
}
