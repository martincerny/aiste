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

package cz.cuni.amis.aiste.environment.impl;

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IAgentController;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;
import cz.cuni.amis.experiments.IBareLoggingOutput;
import cz.cuni.amis.experiments.ILogIdentifier;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.ClassLogIdentifier;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.metrics.MetricCollection;
import cz.cuni.amis.utils.collections.ListConcatenation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractAgentController<ACTION extends IAction, REPRESENTATION extends IEnvironmentRepresentation> implements IAgentController<ACTION, REPRESENTATION> {

    protected IEnvironment<ACTION> environment;
    protected REPRESENTATION representation;
    protected AgentBody body;
    protected long stepDelay;    
    private IBareLoggingOutput runtimeLoggingOutput;
    protected final ILoggingHeaders runtimeLoggingHeaders;
    protected final ILogIdentifier logIdentifier;
    
    private boolean inUse = false;
    
    /**
     * Controller parameters are added to both runtime and per-experiment logs.
     */
    protected final ILoggingHeaders controllerParametersHeaders;
    protected final List<Object> controllerParametersValues;

    /**
     * Add all metrics used by this controller to this member in contructor.
     */
    protected MetricCollection metrics;
    
    public AbstractAgentController(){
        this(LoggingHeaders.EMPTY_LOGGING_HEADERS);
    }
    
    public AbstractAgentController(ILoggingHeaders runtimeLoggingHeaders) {
        this(runtimeLoggingHeaders, LoggingHeaders.EMPTY_LOGGING_HEADERS, Collections.EMPTY_LIST);
    }        
    public AbstractAgentController(ILoggingHeaders runtimeLoggingHeaders, ILoggingHeaders controllerParametersHeaders, Object ... controllerParameterValues) {
        this(runtimeLoggingHeaders, controllerParametersHeaders, Arrays.<Object>asList(controllerParameterValues));
    }
        
    public AbstractAgentController(ILoggingHeaders runtimeLoggingHeaders, ILoggingHeaders controllerParametersHeaders, List<Object> controllerParameterValues) {
        if(runtimeLoggingHeaders.getColumnCount() > 0){
            this.runtimeLoggingHeaders = LoggingHeadersConcatenation.concatenate(new LoggingHeaders("controllerClass", "step"), runtimeLoggingHeaders);
        } else {
            //if runtime logging headers from subclass are empty, keep it empty
            this.runtimeLoggingHeaders = runtimeLoggingHeaders;
        }
        this.controllerParametersHeaders = LoggingHeadersConcatenation.concatenate(new LoggingHeaders("controllerClass"), controllerParametersHeaders);
        this.controllerParametersValues = ListConcatenation.concatenate(Collections.<Object>singletonList(getClass().getSimpleName()), controllerParameterValues);
        
        this.logIdentifier = new ClassLogIdentifier(getClass());
        metrics = new MetricCollection();
    }
    
    
    @Override
    public void init(IEnvironment<ACTION> environment, REPRESENTATION representation, AgentBody body, long stepDelay) {
        
        if(inUse){
            throw new AisteException("The controller is already in use.");
        }
        inUse = true;
        this.environment = environment;
        this.representation = representation;
        this.body = body;
        this.stepDelay = stepDelay;
        metrics.reset();
    }

    /**
     * Act for this agent's body.
     * @param action 
     */
    protected void act(ACTION action){
        getEnvironment().act(body, action);
    }
    
    @Override
    public void onSimulationStep(double reward) {
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    

    @Override
    public void shutdown() {
        inUse = false;
        metrics.stopMeasurement();
    }

    @Override
    public void start() {
    }

    @Override
    public AgentBody getBody() {
        return body;
    }

    public IEnvironment<ACTION> getEnvironment() {
        return environment;
    }

    public long getStepDelay() {
        return stepDelay;
    }
    
    public REPRESENTATION getRepresentation(){
        return representation;
    }

    @Override
    public ILoggingHeaders getRuntimeLoggingHeaders() {
        return runtimeLoggingHeaders;
    }

    @Override
    public void setRuntimeLoggingOutput(IBareLoggingOutput loggingOutput) {
        this.runtimeLoggingOutput = loggingOutput;
    }

    @Override
    public ILogIdentifier getIdentifier() {
        return logIdentifier;
    }

    @Override
    public List<Object> getPerExperimentLoggingData() {
        return new ListConcatenation<Object>(controllerParametersValues, metrics.getValues());
    }

    @Override
    public ILoggingHeaders getPerExperimentLoggingHeaders() {
        return LoggingHeadersConcatenation.concatenate(controllerParametersHeaders, metrics.getHeaders());
    }

    /**
     * Access to log only through this method, so that data added by superclasses is transparently logged.
     * @param values 
     */
    protected void logRuntime(Object ... values){
        runtimeLoggingOutput.logData(Arrays.asList(new Object[] {getClass().getSimpleName(), environment.getTimeStep()}), Arrays.asList(values));        
    }
    
}
