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
package cz.cuni.amis.aiste.execution.impl;

import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.execution.IAgentExecutionResult;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Martin Cerny
 */
public class EnvironmentExecutionResult implements IEnvironmentExecutionResult {
    List<IAgentExecutionResult> agentResults;
    Map<IAgentController, IAgentExecutionResult> perAgentResults;
    long numberOfStepsElapsed;

    public EnvironmentExecutionResult(List<IAgentExecutionResult> agentResults, long numberOfStepsElapsed) {
        this.agentResults = agentResults;
        this.numberOfStepsElapsed = numberOfStepsElapsed;
        perAgentResults = new HashMap<IAgentController, IAgentExecutionResult>(agentResults.size());
        for(IAgentExecutionResult result : agentResults){
            perAgentResults.put(result.getController(), result);
        }
    }
    
    @Override
    public List<IAgentExecutionResult> getAgentResults() {
        return agentResults;
    }

    @Override
    public long getNumberOfStepsElapsed() {
        return numberOfStepsElapsed;
    }

    @Override
    public Map<IAgentController, IAgentExecutionResult> getPerAgentResults() {
        return perAgentResults;
    }
    
    
}
