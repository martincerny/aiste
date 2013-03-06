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

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.IAgentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutor;
import cz.cuni.amis.experiments.EExperimentRunResult;
import cz.cuni.amis.experiments.ILogDataProvider;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.AbstractExperimentRunner;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.utils.collections.ListConcatenation;
import cz.cuni.amis.utils.objectmanager.IObjectFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class AisteExperimentRunner extends AbstractExperimentRunner<AisteExperiment> {
    private IObjectFactory<IEnvironmentExecutor> environmentExecutorFactory;
    private long maxSteps;

    private IEnvironmentExecutor environmentExecutor;
    private IEnvironmentExecutionResult lastExecutionResult;

    public AisteExperimentRunner(IObjectFactory<IEnvironmentExecutor> environmentExecutorFactory) {
        this(environmentExecutorFactory, 0);
    }

    public AisteExperimentRunner(IObjectFactory<IEnvironmentExecutor> environmentExecutorFactory, long maxSteps) {
        this.environmentExecutorFactory = environmentExecutorFactory;
        this.maxSteps = maxSteps;
    }

    @Override
    protected void prepareExperiment(AisteExperiment experiment) {
        super.prepareExperiment(experiment);
        environmentExecutor = environmentExecutorFactory.newObject();
        environmentExecutor.setEnvironment(experiment.getEnvironment());
        for(IAgentExecutionDescriptor descriptor : experiment.getDescriptors()){
            environmentExecutor.addAgentController(descriptor);
        }
    }

    

    
    
    @Override
    protected EExperimentRunResult runExperimentInternal(AisteExperiment experiment) {
        try {
            lastExecutionResult = environmentExecutor.executeEnvironment(maxSteps);
            return EExperimentRunResult.SUCCESS;
        } finally {
            environmentExecutor.shutdown();
        }
    }

    @Override
    protected List<ILogDataProvider> getAllLogProviders(AisteExperiment experiment) {
        List<ILogDataProvider> providers = new ArrayList<ILogDataProvider>(experiment.getDescriptors().size() + 1);
        providers.add(experiment.getEnvironment());
        for(IAgentExecutionDescriptor desc : experiment.getDescriptors()){
            providers.add(desc.getController());
        }
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
            IAgentController controller = (IAgentController)provider;
            double reward = lastExecutionResult.getPerAgentResults().get(controller).getTotalReward();
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
    
    
}
