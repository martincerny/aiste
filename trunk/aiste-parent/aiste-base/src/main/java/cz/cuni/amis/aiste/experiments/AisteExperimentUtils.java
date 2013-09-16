/*
 * Copyright (C) 2013 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
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

package cz.cuni.amis.aiste.experiments;

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.impl.AgentExecutionDescriptor;
import cz.cuni.amis.experiments.IExperimentSuite;
import cz.cuni.amis.experiments.impl.ExperimentSuite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class AisteExperimentUtils {
    public static IExperimentSuite<AisteExperiment> createAllPossiblePairwiseCombinationsSuite(String name, List<? extends IEnvironment> environments, List<? extends IAgentController> controllers, long stepDelay, long stepsToTimeout){
        return createAllPossiblePairwiseCombinationsSuite(name, environments, controllers, Collections.singletonList(stepDelay), stepsToTimeout);
    }
    public static IExperimentSuite<AisteExperiment> createAllPossiblePairwiseCombinationsSuite(String name, List<? extends IEnvironment> environments, List<? extends IAgentController> controllers, List<Long> stepDelays, long stepsToTimeout){
        return createAllPossiblePairwiseCombinationsSuite(name, environments, controllers, stepDelays, stepsToTimeout, 1);
    }
    
    public static IExperimentSuite<AisteExperiment> createAllPossiblePairwiseCombinationsSuite(String name, List<? extends IEnvironment> environments, List<? extends IAgentController> controllers, List<Long> stepDelays, long stepsToTimeout, int numRepetitions){
        List<AisteExperiment> experiments = new ArrayList<AisteExperiment>();        
        for (int i = 0; i < numRepetitions; i++) {
            for (IEnvironment env : environments) {
                if (env.getInstantiationDescriptors().size() != 1) {
                    throw new AisteException("Single agent type expected");
                }
                IAgentType agentType = (IAgentType) env.getInstantiationDescriptors().keySet().iterator().next();


                for (IAgentController controller1 : controllers) {
                    for (Object representation1 : env.getRepresentations()) {
                        if (controller1.getRepresentationClass().isAssignableFrom(representation1.getClass())) {
                            for (IAgentController controller2 : controllers) {
                                for (Object representation2 : env.getRepresentations()) {
                                    if (controller2.getRepresentationClass().isAssignableFrom(representation2.getClass())) {
                                        if (controller1 == controller2 && representation1 == representation2) {
                                            continue;
                                        }
                                        List<IAgentExecutionDescriptor> descriptors = Arrays.asList(new IAgentExecutionDescriptor[]{
                                            new AgentExecutionDescriptor(agentType, controller1, (IEnvironmentRepresentation) representation1),
                                            new AgentExecutionDescriptor(agentType, controller2, (IEnvironmentRepresentation) representation2)
                                        });
/*                                        List<IAgentExecutionDescriptor> descriptors2 = Arrays.asList(new IAgentExecutionDescriptor[]{
                                            new AgentExecutionDescriptor(agentType, controller2, (IEnvironmentRepresentation) representation2),
                                            new AgentExecutionDescriptor(agentType, controller1, (IEnvironmentRepresentation) representation1),});
*/
                                        for (long stepDelay : stepDelays) {
                                            long timeout = (stepsToTimeout + 5) * stepDelay + 1000/* Just a little reserve for startup and shutdown*/;
                                            experiments.add(new AisteExperiment(env, descriptors, stepDelay, timeout));
  //                                          experiments.add(new AisteExperiment(env, descriptors2, stepDelay, timeout));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ExperimentSuite<AisteExperiment>(name, experiments);
    }
    
    public static IExperimentSuite<AisteExperiment> createAllPossiblePairwiseCombinationsSuiteHack(String name, List<? extends IEnvironment> environments, List<? extends IAgentController> controllers, List<Long> stepDelays, long stepsToTimeout, int numRepetitions){
        List<AisteExperiment> experiments = new ArrayList<AisteExperiment>();        
        for (int i = 0; i < numRepetitions; i++) {
            for (IEnvironment env : environments) {
                if (env.getInstantiationDescriptors().size() != 1) {
                    throw new AisteException("Single agent type expected");
                }
                IAgentType agentType = (IAgentType) env.getInstantiationDescriptors().keySet().iterator().next();


                for (IAgentController controller1 : controllers) {
                    for (Object representation1 : env.getRepresentations()) {
                        if (controller1.getRepresentationClass().isAssignableFrom(representation1.getClass())) {
                            for (IAgentController controller2 : controllers) {
                                if (controller1 != controller2) {
                                    continue;
                                }
                                for (Object representation2 : env.getRepresentations()) {
                                    if(representation1 == representation2){
                                        continue;
                                    }
                                    if (controller2.getRepresentationClass().isAssignableFrom(representation2.getClass())) {
                                        List<IAgentExecutionDescriptor> descriptors = Arrays.asList(new IAgentExecutionDescriptor[]{
                                            new AgentExecutionDescriptor(agentType, controller1, (IEnvironmentRepresentation) representation1),
                                            new AgentExecutionDescriptor(agentType, controller2, (IEnvironmentRepresentation) representation2)
                                        });
/*                                        List<IAgentExecutionDescriptor> descriptors2 = Arrays.asList(new IAgentExecutionDescriptor[]{
                                            new AgentExecutionDescriptor(agentType, controller2, (IEnvironmentRepresentation) representation2),
                                            new AgentExecutionDescriptor(agentType, controller1, (IEnvironmentRepresentation) representation1),});
*/
                                        for (long stepDelay : stepDelays) {
                                            long timeout = (stepsToTimeout + 5) * stepDelay + 1000/* Just a little reserve for startup and shutdown*/;
                                            experiments.add(new AisteExperiment(env, descriptors, stepDelay, timeout));
  //                                          experiments.add(new AisteExperiment(env, descriptors2, stepDelay, timeout));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ExperimentSuite<AisteExperiment>(name, experiments);
    }
    
}
