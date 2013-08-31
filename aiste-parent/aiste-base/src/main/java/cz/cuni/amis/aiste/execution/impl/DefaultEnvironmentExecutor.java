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
package cz.cuni.amis.aiste.execution.impl;

import cz.cuni.amis.aiste.SimulationException;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.execution.IEnvironmentExecutionResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class DefaultEnvironmentExecutor extends AbstractEnvironmentExecutor {

    private final Logger logger = Logger.getLogger(DefaultEnvironmentExecutor.class);

    private RunningStepNotificationsMonitor stepNotificationsMonitor = new RunningStepNotificationsMonitor();

    private final ExecutorService agentStepNotificationExecutorService;

    private final int maxNotificationInstancesPerController;

    private boolean debugMode;
    
    CountDownLatch simulationStoppedLatch;
    
    boolean cancelled = false;
    
    /**
     * If actual execution delay between successive environment steps is greater by a factor larger
     * than this, it is assumed, that the environment does not react fast enough
     * for current stepDelay and an exception is thrown.
     */
    private static final double STEP_TOLERANCE_FACTOR = 1.2;

    public DefaultEnvironmentExecutor(long stepDelay, int maxNotificationInstancesPerController) {
        super(stepDelay);
        this.maxNotificationInstancesPerController = maxNotificationInstancesPerController;
        agentStepNotificationExecutorService = Executors.newCachedThreadPool();
    }

    public DefaultEnvironmentExecutor(long stepDelay) {
        this(stepDelay, 2 );
    }

    @Override
    public IEnvironmentExecutionResult executeEnvironment(long maxSteps) {

        long stepsPerformed = 0;
        long lastExecutionTime = -1;
        long startTime = System.currentTimeMillis();
        
        cancelled = false;
        simulationStoppedLatch = new CountDownLatch(1);
        
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);        
        
        try {
            while (!getEnvironment().isFinished() && !cancelled) {
                if((System.currentTimeMillis() - startTime) / getStepDelay() > stepsPerformed){
                    //time for next step
                    if (logger.isTraceEnabled()) {
                        logger.trace("Starting simulation step");
                        /*                if (!Thread.holdsLock(getEnvironment())) {
                         outer: 
                         for (java.lang.management.ThreadInfo ti :
                         java.lang.management.ManagementFactory.getThreadMXBean()
                         .dumpAllThreads(true, false)) {
                         for (java.lang.management.MonitorInfo mi : ti.getLockedMonitors()) {
                         if (mi.getIdentityHashCode() == System.identityHashCode(getEnvironment())) {
                         logger.trace("Monitor held by: " + mi.getClassName() + ": " + mi.getLockedStackFrame().getLineNumber());
                         break outer;
                         }
                         }
                         }
                         }*/

                    }



                    synchronized (getEnvironment()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Acquired environment monitor");
                        }
                        if (lastExecutionTime > 0) {
                            long delay = System.currentTimeMillis() - lastExecutionTime;
                            if (!isDebugMode() && delay > getStepDelay() * STEP_TOLERANCE_FACTOR) {
                                throw new SimulationException("Two succesive simulation steps were run after " + delay + " ms, but should be " + getStepDelay() + " ms");
                            }
                        }
                        lastExecutionTime = System.currentTimeMillis();
                        if (getEnvironment().isFinished() || (maxSteps != 0 && stepsPerformed >= maxSteps)) {
                            break;
                        }

                        performSimulationStep();
                        stepsPerformed++;
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Simulation step done");
                    }
                } else {
                    //busy wait
                    try {
                        Thread.sleep(1);                    
                    } catch(InterruptedException ex){} //I don't care
                }
            }
                
        } catch (Exception ex) {
            onException(ex);
            logger.error("Exception during environment execution. ", ex);
        }
        
        /**
         * Wait for two simulation steps to avoid most of terrible concurrency issues when
         * a controller has not finished shutdown for an environment before a second environment is started
         */
        try {
            Thread.sleep(getStepDelay() * 2);
        } catch(InterruptedException ex){
            logger.warn("Waiting at the end of experiment interrupted.", ex);
        }
        
        simulationStoppedLatch.countDown();
        
        return gatherExecutionResult();
    }

    @Override
    protected void notifyControllerOfSimulationStep(IAgentController controller, double reward) {
        synchronized(agentStepNotificationExecutorService) {
            agentStepNotificationExecutorService.submit(new NotifyControllerOfSimulationStepTask(controller, reward));
        }
    }

    @Override
    protected void stopSimulation() {
        cancelled = true;
        super.stopSimulation();
        synchronized(agentStepNotificationExecutorService) {
            agentStepNotificationExecutorService.shutdownNow();
        }
        try {
            simulationStoppedLatch.await();
        } catch (InterruptedException ex) {
            logger.warn("Waiting for simulation stop interrupted.", ex);            
        }
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
       

    private class NotifyControllerOfSimulationStepTask implements Runnable {

        private IAgentController controller;

        private double reward;

        public NotifyControllerOfSimulationStepTask(IAgentController controller, double reward) {
            this.controller = controller;
            this.reward = reward;
        }

        @Override
        public void run() {
            try {
                if (stepNotificationsMonitor.stepNotificationStarted(controller) > maxNotificationInstancesPerController) {
                    logger.info("Controller " + controller + " has exceeded maximum allowed parallel logic instances (" + maxNotificationInstancesPerController + "). It has been stopped.");
                    controllerFailed(controller);
                    return;
                }
                try {
                    controller.onSimulationStep(reward);
                } catch (Exception ex) {
                    logger.warn("Controller " + controller + " has raised exception during onSimulationStep(). It has been stopped.", ex);
                    controllerFailed(controller);
                }
            } finally {
                stepNotificationsMonitor.stepNotificationEnded(controller);
            }
        }
    }

    private static class RunningStepNotificationsMonitor {

        private final Object mutex = new Object();

        private Map<IAgentController, Integer> runningInstances = new HashMap<IAgentController, Integer>();

        /**
         * Called when task is started for a controller.
         *
         * @param controller
         * @return The number of running instances for this controller,
         * including the instance just started
         */
        public int stepNotificationStarted(IAgentController controller) {
            synchronized (mutex) {
                Integer previousCount = runningInstances.get(controller);
                if (previousCount == null) {
                    previousCount = 0;
                }
                runningInstances.put(controller, previousCount + 1);
                return previousCount + 1;
            }
        }

        /**
         * Called when task is finished for a controller.
         *
         * @param controller
         * @return The number of running instances for this controller,
         * excluding the instance just ended
         */
        public int stepNotificationEnded(IAgentController controller) {
            synchronized (mutex) {
                Integer previousCount = runningInstances.get(controller);
                if (previousCount == null) {
                    previousCount = 0;
                }
                if (previousCount <= 0) {
                    throw new IllegalStateException("Termination of task that has not yet started for controller " + controller);
                }
                runningInstances.put(controller, previousCount - 1);
                return previousCount - 1;
            }
        }
    }
}
