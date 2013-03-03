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

import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutor;
import cz.cuni.amis.experiments.EExperimentRunResult;
import cz.cuni.amis.experiments.ILogDataProvider;
import cz.cuni.amis.experiments.impl.AbstractExperimentRunner;
import cz.cuni.amis.utils.objectmanager.IObjectFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class AisteExperimentRunner extends AbstractExperimentRunner<AisteExperiment> {
    private IObjectFactory<IEnvironmentExecutor> environmentExecutorFactory;
    private long maxSteps;
    
    private IEnvironmentExecutionResult lastExecutionResult;

    public AisteExperimentRunner(IObjectFactory<IEnvironmentExecutor> environmentExecutorFactory) {
        this(environmentExecutorFactory, 0);
    }

    public AisteExperimentRunner(IObjectFactory<IEnvironmentExecutor> environmentExecutorFactory, long maxSteps) {
        this.environmentExecutorFactory = environmentExecutorFactory;
        this.maxSteps = maxSteps;
    }
    

    @Override
    protected EExperimentRunResult runExperimentInternal(AisteExperiment experiment) {
        IEnvironmentExecutor environmentExecutor = environmentExecutorFactory.newObject();
        environmentExecutor.setEnvironment(experiment.getEnvironment());
        for(IAgentExecutionDescriptor descriptor : experiment.getDescriptors()){
            environmentExecutor.addAgentController(descriptor);
        }
        lastExecutionResult = environmentExecutor.executeEnvironment(maxSteps);
        return EExperimentRunResult.SUCCESS;
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
    
    
    
    
}
