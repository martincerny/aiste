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

import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.execution.IAgentExecutionDescriptor;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.AbstractExperiment;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.utils.collections.ListConcatenation;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class AisteExperiment extends AbstractExperiment {

    IEnvironment environment;
    
    List<IAgentExecutionDescriptor> descriptors;
    
    long stepDelay;

    public AisteExperiment(IEnvironment environment, List<IAgentExecutionDescriptor> descriptors, long stepDelay, long timeout) {
        super(timeout);
        this.environment = environment;
        this.descriptors = descriptors;
        this.stepDelay = stepDelay;
    }

    public List<IAgentExecutionDescriptor> getDescriptors() {
        return descriptors;
    }

    public IEnvironment getEnvironment() {
        return environment;
    }

    public long getStepDelay() {
        return stepDelay;
    }
    
    
    
    @Override
    public ILoggingHeaders getExperimentParametersHeaders() {
        return LoggingHeadersConcatenation.concatenate(new LoggingHeaders("envClass", "agentCount", "stepDelay"), environment.getEnvironmentParametersHeaders());
    }

    @Override
    public List<Object> getExperimentParameters() {
        return new ListConcatenation<Object>(Arrays.asList(new Object[] {environment.getClass().getSimpleName(), descriptors.size(), stepDelay}), environment.getEnvironmentParametersValues());
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("AisteExperiment: ");
        ILoggingHeaders experimentParametersHeaders = getExperimentParametersHeaders();
        List<Object> experimentParameters = getExperimentParameters();
        for(int i = 0; i < experimentParametersHeaders.getColumnCount(); i++){
            sb.append(experimentParametersHeaders.getColumnNames().get(i)).append(" = ").append(experimentParameters.get(i)).append(", ");
        }
        for(int i = 0; i < descriptors.size(); i++){
            sb.append("\nAgent " + i + ": ");
            sb.append(descriptors.get(i).getLoggableReperesentation());
        }
        return sb.toString();
    }

    
}
