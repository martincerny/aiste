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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractPlanningController;
import cz.cuni.amis.aiste.environment.impl.DoNothingAgentController;
import cz.cuni.amis.aiste.environment.impl.JShop2Controller;
import cz.cuni.amis.aiste.environment.impl.Planning4JController;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.impl.AgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.impl.DefaultEnvironmentExecutorFactory;
import cz.cuni.amis.aiste.experiments.AisteExperiment;
import cz.cuni.amis.aiste.experiments.AisteExperimentRunner;
import cz.cuni.amis.aiste.experiments.AisteExperimentUtils;
import cz.cuni.amis.experiments.IExperimentSuite;
import cz.cuni.amis.experiments.utils.ExperimentUtils;
import cz.cuni.amis.planning4j.IAsyncPlanner;
import cz.cuni.amis.planning4j.IPlanner;
import cz.cuni.amis.planning4j.IValidator;
import cz.cuni.amis.planning4j.external.ExternalPlanner;
import cz.cuni.amis.planning4j.external.impl.itsimple.*;
import cz.cuni.amis.planning4j.external.plannerspack.PlannersPackUtils;
import cz.cuni.amis.planning4j.pddl.PDDLRequirement;
import cz.cuni.amis.planning4j.validation.external.ValValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 *
 * @author Martin Cerny
 */
public class Test {

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        PlannerListManager plannerManager = PlannersPackUtils.getPlannerListManager();

        ItSimplePlannerInformation info;
        if(ItSimpleUtils.getOperatingSystem() == EPlannerPlatform.LINUX){
            info = PlannersPackUtils.getSGPlan6();
        } else {
            info = PlannersPackUtils.getMetricFF();
        }

        File plannersDirectory = new File("target");
        //The planner is extracted (only if it does not exist yet) and exec permissions are set under Linux
        plannerManager.extractAndPreparePlanner(plannersDirectory, info);

        IAsyncPlanner planner = new ExternalPlanner(new ItSimplePlannerExecutor(info, plannersDirectory));

        ValValidator.extractAndPrepareValidator(plannersDirectory);
        //       IValidator validator = new ValValidator(plannersDirectory);
        IValidator validator = null;



//        File envTempFile = new File("env.obj");
//        SpyVsSpyEnvironmentDefinition envDef;
//
//        if (!envTempFile.exists()) {
//            SpyVsSpyGenerator generator = new SpyVsSpyGenerator(2, 100, 3, 5, 8, 0.5, 5, planner);
//            //SpyVsSpyGenerator generator = new SpyVsSpyGenerator(2,8,3,2,2,0.5, 1, planner);
//            //        SpyVsSpyGenerator generator = new SpyVsSpyGenerator(2, 3, 1.4, 1, 1, 0, 0, planner);        
//            generator.setRandomSeed(1745646655);
//
//            envDef = generator.generateEnvironment();
//
//            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(envTempFile));
//            os.writeObject(envDef);
//            os.close();
//        } else {
//            ObjectInputStream is = new ObjectInputStream(new FileInputStream(envTempFile));
//            envDef = (SpyVsSpyEnvironmentDefinition) is.readObject();
//        }
//
//        SpyVsSpy environment = new SpyVsSpy(envDef);
//        environment.setRandomSeed(1234878864L);


//        IAgentController player1 = new JShop2Controller(AbstractPlanningController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN);        
//        IAgentController player1 = new DoNothingAgentController();
        //IAgentController player2 = new Planning4JController(planner, Planning4JController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN);                

        //        executor.addAgentController(SpyVsSpyAgentType.getInstance(), player1, b.getjShop2Representation());        

//        List<IAgentExecutionDescriptor> descriptors = Arrays.asList(new IAgentExecutionDescriptor[] {
//            new AgentExecutionDescriptor(SpyVsSpyAgentType.getInstance(), player1, environment.getjShop2Representation()),
//            new AgentExecutionDescriptor(SpyVsSpyAgentType.getInstance(), player2, environment.getpDDLRepresentation()),
//        });
//
//        AisteExperiment experiment = new AisteExperiment(environment, descriptors, 100000);
//        ExperimentUtils.runExperimentsSingleThreaded(Collections.singletonList(experiment), new AisteExperimentRunner(new DefaultEnvironmentExecutorFactory(300)));        

        
        List<IAgentController> controllers = new ArrayList<IAgentController>();
        controllers.add(new JShop2Controller(AbstractPlanningController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN, 0, new JShop2Controller.StepsSinceFirstPlanInterruptTest(1)));            
        controllers.add(new Planning4JController(planner, Planning4JController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN));
        controllers.add(new JShop2Controller(AbstractPlanningController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN, 1));            
        controllers.add(new JShop2Controller(AbstractPlanningController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN, 2));            
        controllers.add(new JShop2Controller(AbstractPlanningController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN, 0, new JShop2Controller.StepsSinceFirstPlanInterruptTest(2)));            
        controllers.add(new JShop2Controller(AbstractPlanningController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN, 1, new JShop2Controller.StepsSinceFirstPlanInterruptTest(0)));            
        
        List<IEnvironment> environments = new ArrayList<IEnvironment>();
        
        Random rand = new Random(3865983846L);
        for (int i = 0; i < 50; i++) {
            SpyVsSpyEnvironmentDefinition envDef;
            try {
                IPlanner plannerToTestDomain = null;
                if(ItSimpleUtils.getOperatingSystem() == EPlannerPlatform.LINUX){
                    plannerToTestDomain = planner;
                }
                
                //int maxPlayers, int numNodes, double meanNodeDegree, int numItemTypes, int numTrapTypes, double itemTrappedProbability, int numWeapons,
                SpyVsSpyGenerator generator = new SpyVsSpyGenerator(2, 15, 3, 3, 2, 0.3, 5, plannerToTestDomain);
                long generatorSeed = rand.nextLong();
                System.out.println("Seed:" + generatorSeed);
                generator.setRandomSeed(generatorSeed);

                envDef = generator.generateEnvironment();
                environments.add(new SpyVsSpy(envDef));
            } catch (AisteException ex) {
                System.out.println("Generator failnul. i:" + i);
                continue;
            }
        }
        for (int i = 0; i < 50; i++) {
            SpyVsSpyEnvironmentDefinition envDef;
            try {
                IPlanner plannerToTestDomain = null;
                if(ItSimpleUtils.getOperatingSystem() == EPlannerPlatform.LINUX){
                    plannerToTestDomain = planner;
                }
                
                //int maxPlayers, int numNodes, double meanNodeDegree, int numItemTypes, int numTrapTypes, double itemTrappedProbability, int numWeapons,
                SpyVsSpyGenerator generator = new SpyVsSpyGenerator(2, 30 , 4, 4, 2, 0.5, 7, plannerToTestDomain);
                long generatorSeed = rand.nextLong();
                System.out.println("Seed:" + generatorSeed);
                generator.setRandomSeed(generatorSeed);

                envDef = generator.generateEnvironment();
                environments.add(new SpyVsSpy(envDef));
            } catch (AisteException ex) {
                System.out.println("Generator failnul. i:" + i);
                continue;
            }
        }
        
        for (int i = 0; i < 50; i++) {
            SpyVsSpyEnvironmentDefinition envDef;
            try {
                IPlanner plannerToTestDomain = null;
                if(ItSimpleUtils.getOperatingSystem() == EPlannerPlatform.LINUX){
                    plannerToTestDomain = planner;
                }
                
                //int maxPlayers, int numNodes, double meanNodeDegree, int numItemTypes, int numTrapTypes, double itemTrappedProbability, int numWeapons,
                SpyVsSpyGenerator generator = new SpyVsSpyGenerator(2, 70, 5, 5, 2, 0.3, 10, plannerToTestDomain);
                long generatorSeed = rand.nextLong();
                System.out.println("Seed:" + generatorSeed);
                generator.setRandomSeed(generatorSeed);

                envDef = generator.generateEnvironment();
                environments.add(new SpyVsSpy(envDef));
            } catch (AisteException ex) {
                System.out.println("Generator failnul. i:" + i);
                continue;
            }
        }
        
        
        IExperimentSuite<AisteExperiment> suite = AisteExperimentUtils.createAllPossiblePairwiseCombinationsSuite("JSHOPTest", environments, controllers, 100000, rand);

        ExperimentUtils.runSuiteSingleThreaded(suite, new AisteExperimentRunner(new DefaultEnvironmentExecutorFactory(400)));


//        IAgentController player1 = new Planning4JController(planner, Planning4JController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN);        
//        executor.addAgentController(SpyVsSpyAgentType.getInstance(), player1, b.getpDDLRepresentation());        

        //IAgentController player2 = new Planning4JController(planner, Planning4JController.ValidationMethod.ENVIRONMENT_SIMULATION_WHOLE_PLAN);                
        //executor.addAgentController(SpyVsSpyAgentType.getInstance(), player2, b.getpDDLRepresentation());


        /*
         * SpyVsSpy b = new SpyVsSpy(); SpyVsSpyReactiveController player1 = new
         * SpyVsSpyReactiveController(); SpyVsSpyReactiveController player2 =
         * new SpyVsSpyReactiveController();
         *
         * SynchronuousEnvironmentExecutor executor = new
         * SynchronuousEnvironmentExecutor(); executor.setEnvironment(b);
         * executor.addAgentController(SpyVsSpyAgentType.getInstance(),
         * player1);
         * executor.addAgentController(SpyVsSpyAgentType.getInstance(),
         * player2);
         *
         * IEnvironmentExecutionResult result = executor.executeEnvironment();
         *
         * System.out.println("Results: "); System.out.println("Player1: " +
         * result.getAgentResults().get(0).getTotalReward());
         * System.out.println("Player2: "+
         * result.getAgentResults().get(1).getTotalReward());
         */
    }
}
