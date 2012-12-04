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
package cz.cuni.amis.aiste.impl;

import cz.cuni.amis.aiste.IAgentExecutionResult;
import cz.cuni.amis.aiste.IEnvironmentExecutionResult;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class EnvironmentExecutionResult implements IEnvironmentExecutionResult {
    List<IAgentExecutionResult> agentResults;
    long numberOfStepsElapsed;

    public EnvironmentExecutionResult(List<IAgentExecutionResult> agentResults, long bumberOfStepsElapsed) {
        this.agentResults = agentResults;
        this.numberOfStepsElapsed = bumberOfStepsElapsed;
    }
    
    @Override
    public List<IAgentExecutionResult> getAgentResults() {
        return agentResults;
    }

    @Override
    public long getNumberOfStepsElapsed() {
        return numberOfStepsElapsed;
    }
    
    
}
