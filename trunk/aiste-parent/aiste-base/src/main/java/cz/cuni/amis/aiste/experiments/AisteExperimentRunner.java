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

import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.IAgentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutor;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutorFactory;
import cz.cuni.amis.experiments.*;
import cz.cuni.amis.experiments.impl.AbstractExperimentRunner;
import cz.cuni.amis.experiments.impl.AbstractLogDataProvider;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.experiments.impl.StringLogIdentifier;
import cz.cuni.amis.utils.collections.ListConcatenation;
import cz.cuni.amis.utils.objectmanager.IObjectFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Martin Cerny
 */
public class AisteExperimentRunner extends AbstractExperimentRunner<AisteExperiment> implements IRandomizable{
    private IEnvironmentExecutorFactory environmentExecutorFactory;
    private long maxSteps;
    private Random rand = new Random();

    private IEnvironmentExecutor environmentExecutor;
    private IEnvironmentExecutionResult lastExecutionResult;
    private Map<Class, RankLoggingProvider> rankLoggingProviders;

    public AisteExperimentRunner(IEnvironmentExecutorFactory environmentExecutorFactory) {
        this(environmentExecutorFactory, 0);
    }

    public AisteExperimentRunner(IEnvironmentExecutorFactory environmentExecutorFactory, long maxSteps) {
        this.environmentExecutorFactory = environmentExecutorFactory;
        this.maxSteps = maxSteps;
        rankLoggingProviders = new HashMap<Class, RankLoggingProvider>();
    }

    @Override
    public void setRandomSeed(long seed) {
        rand = new Random(seed);
    }
    
    

    @Override
    protected void prepareExperiment(AisteExperiment experiment) {
        super.prepareExperiment(experiment);
        environmentExecutor = environmentExecutorFactory.createExecutor(experiment);        
        if(environmentExecutor instanceof IRandomizable){
            ((IRandomizable)environmentExecutor).setRandomSeed(rand.nextLong());
        }        
        environmentExecutor.setEnvironment(experiment.getEnvironment());
        for(IAgentExecutionDescriptor descriptor : experiment.getDescriptors()){
            environmentExecutor.addAgentController(descriptor);
        }
        if(!rankLoggingProviders.containsKey(experiment.getEnvironment().getClass())){
            rankLoggingProviders.put(experiment.getEnvironment().getClass(), new RankLoggingProvider(experiment.getEnvironment()));
        }
    }

    

    
    
    @Override
    protected EExperimentRunResult runExperimentInternal(AisteExperiment experiment) {
        try {
            lastExecutionResult = environmentExecutor.executeEnvironment(maxSteps);
            rankLoggingProviders.get(experiment.environment.getClass()).logExperimentResults(lastExecutionResult);  
            return EExperimentRunResult.SUCCESS;
        } finally {
            environmentExecutor.shutdown();            
        }
    }

    @Override
    protected void cancelRunningExperiment() {
        environmentExecutor.shutdown();
    }

    @Override
    protected List<ILogDataProvider> getAllLogProviders(AisteExperiment experiment) {
        List<ILogDataProvider> providers = new ArrayList<ILogDataProvider>(experiment.getDescriptors().size() + 1);
        providers.add(experiment.getEnvironment());
        for(IAgentExecutionDescriptor desc : experiment.getDescriptors()){
            providers.add(desc.getController());
        }
        
        providers.add(rankLoggingProviders.get(experiment.environment.getClass()));
        return providers;
    }

    @Override
    protected ILoggingHeaders getAdditionalLoggingHeaders(AisteExperiment experiment, ILogDataProvider provider) {
        if(! (provider instanceof IAgentController)){
            return super.getAdditionalLoggingHeaders(experiment, provider);
        } else {
            return LoggingHeadersConcatenation.concatenate(super.getAdditionalLoggingHeaders(experiment, provider), new LoggingHeaders("reward", "stepsElapsed") );
        }
    }

    @Override
    protected List<Object> getAdditionalLoggingDataValues(AisteExperiment experiment, ILogDataProvider provider) {
        if(! (provider instanceof IAgentController)){
            return super.getAdditionalLoggingDataValues(experiment, provider);            
        } else {
            if(lastExecutionResult == null){
                throw new ExperimentException("The environment has not provided any result.");
            }
            IAgentController controller = (IAgentController)provider;
            IAgentExecutionResult agentResult = lastExecutionResult.getPerAgentResults().get(controller);
            if(agentResult == null){
                throw new ExperimentException("The environment has not provided reward for controller " + controller);
            }
            double reward = agentResult.getTotalReward();
            List<Object> data = Arrays.asList(new Object[] {reward, lastExecutionResult.getNumberOfStepsElapsed()});
            return ListConcatenation.concatenate(super.getAdditionalLoggingDataValues(experiment, provider), data);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if(environmentExecutor != null){
            environmentExecutor.shutdown();
        }
    }
    
    private class RankLoggingProvider extends AbstractLogDataProvider {

        IEnvironment environment;
        
        public RankLoggingProvider(IEnvironment environment) {
            super(new StringLogIdentifier("Rankings" + environment.getClass().getSimpleName()));
            this.environment = environment;
        }

        @Override
        public ILoggingHeaders getRuntimeLoggingHeaders() {            
            return new LoggingHeadersConcatenation(environment.getPerExperimentLoggingHeaders(), new LoggingHeaders("controller", "oponentController", "reward", "rank"));            
        }

        public void logExperimentResults(IEnvironmentExecutionResult executionResult){
            if(executionResult.getAgentResults().size() != 2){
                throw new UnsupportedOperationException("Only two agents supported");
            }
            IAgentExecutionResult result0 = executionResult.getAgentResults().get(0);
            IAgentExecutionResult result1 = executionResult.getAgentResults().get(1);
            int rank0, rank1;
            if(result0.getTotalReward() == result1.getTotalReward()){
                rank0 = 0;
                rank1 = 0;
            } else if (result0.getTotalReward() < result1.getTotalReward()){
                rank0 = 1;
                rank1 = 0;
            } else {
                rank0 = 0;
                rank1 = 1;
                
            }
            runtimeLoggingOutput.logData(ListConcatenation.concatenate(environment.getPerExperimentLoggingData(), 
                    Arrays.asList(new Object[] { 
                        result0.getController().getLoggableRepresentation(),
                        result1.getController().getLoggableRepresentation(),
                        result0.getTotalReward(),
                        rank0
            })));
            runtimeLoggingOutput.logData(ListConcatenation.concatenate(environment.getPerExperimentLoggingData(), 
                    Arrays.asList(new Object[] { 
                        result1.getController().getLoggableRepresentation(),
                        result0.getController().getLoggableRepresentation(),
                        result1.getTotalReward(),
                        rank1
            })));
        }
        
    }
}
