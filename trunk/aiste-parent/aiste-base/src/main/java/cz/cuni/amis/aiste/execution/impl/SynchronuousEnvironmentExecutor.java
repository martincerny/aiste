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
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import java.util.concurrent.Executor;
import org.apache.log4j.Logger;

/**
 * A simple executor, that runs the agent controllers and environment
 * synchronously, one step after other. This executor is mainly for testing
 * purposes and should be used only when agent controllers do not rely on a
 * specific simulation speed.
 *
 * @author Martin Cerny
 */
public class SynchronuousEnvironmentExecutor extends AbstractEnvironmentExecutor {

    private final Logger logger = Logger.getLogger(SynchronuousEnvironmentExecutor.class);

    private boolean simulationStopped = false;
    
    public SynchronuousEnvironmentExecutor() {
        super(1);
    }

    @Override
    public IEnvironmentExecutionResult executeEnvironment(long maxSteps) {
        this.startSimulation();
        simulationStopped = false;
        long step = 0;
        while (!getEnvironment().isFinished() && (maxSteps == 0 || step < maxSteps) && !simulationStopped) {
            performSimulationStep();
            step++;
        }
        IEnvironmentExecutionResult result = gatherExecutionResult();
        stopSimulation();
        return result;
    }

    @Override
    protected void notifyControllerOfSimulationStep(IAgentController controller, double reward) {
        //we run controllers synchronously
        try {
            controller.onSimulationStep(reward);
        } catch (Exception ex) {
            logger.warn("Controller " + controller + " has raised exception during onSimulationStep(). It has been stopped.", ex);
            controllerFailed(controller);
        }
    }

    @Override
    protected void stopSimulation() {
        super.stopSimulation();
        simulationStopped = true;
    }
    
    
}
