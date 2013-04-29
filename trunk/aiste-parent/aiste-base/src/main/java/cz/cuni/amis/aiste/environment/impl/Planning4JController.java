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
package cz.cuni.amis.aiste.environment.impl;

import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.planning4j.*;
import cz.cuni.amis.planning4j.impl.PDDLObjectDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLObjectProblemProvider;
import cz.cuni.amis.planning4j.pddl.PDDLDomain;
import cz.cuni.amis.planning4j.pddl.PDDLProblem;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import cz.cuni.amis.utils.future.IFutureWithListeners;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin
 */
public class Planning4JController extends AbstractPlanningController<PDDLDomain, PDDLProblem, ActionDescription, IPlanningResult, IPDDLRepresentation<IAction, IPlanningGoal>> {

    private final Logger logger = Logger.getLogger(Planning4JController.class);

    protected PDDLObjectDomainProvider domainProvider;

    IAsyncPlanner<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> planner;
    
    /**
     * Validator that validates the plans throughout execution, whether they still
     * lead to goal state. If null, no validation is performed.
     */
    IValidator<IPDDLObjectDomainProvider, IPDDLObjectProblemProvider> validator;


    /**
     * Createas a controller without validation
     * @param planner 
     */
    public Planning4JController(IAsyncPlanner planner) {
        this(planner, ValidationMethod.NONE);
    }
    
    /**
     * Creates a controller with specified validation method.
     * @param planner
     * @param validationScope 
     */
    public Planning4JController(IAsyncPlanner planner, ValidationMethod validationMethod) {
        this(planner, validationMethod, null);
    }    
    
    /**
     * Creates a controller with external validator plan validation.
     * @param planner
     * @param validationScope
     * @param validator 
     */
    public Planning4JController(IAsyncPlanner planner, IValidator validator) {
        this(planner, ValidationMethod.EXTERNAL_VALIDATOR, validator);
    }    
    
    public Planning4JController(IAsyncPlanner planner, ValidationMethod validationMethod, IValidator validator) {
        super(validationMethod, new LoggingHeaders("planner"), planner.getName());
        this.planner = Planning4JUtils.getTranslatingAsyncPlanner(planner, IPDDLObjectDomainProvider.class, IPDDLObjectProblemProvider.class);
        if(validator != null){
            this.validator = Planning4JUtils.getTranslatingValidator(validator, IPDDLObjectDomainProvider.class, IPDDLObjectProblemProvider.class);
        } else {
            if(validationMethod == ValidationMethod.EXTERNAL_VALIDATOR){
                throw new IllegalArgumentException("If external validation is required, validator has to be given");
            }
            this.validator = null;
        }

    }

    @Override
    public void init(IEnvironment<IAction> environment, IPDDLRepresentation<IAction, IPlanningGoal> representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        domainProvider = new PDDLObjectDomainProvider(representation.getDomain(body));
    }

    @Override
    protected List<ActionDescription> getActionsFromPlanningResult(IPlanningResult result) {
        return result.getPlan();
    }

    @Override
    protected boolean isPlanningResultSucces(IPlanningResult result) {
        return result.isSuccess();
    }

    @Override
    protected IFutureWithListeners<IPlanningResult> startPlanningProcess() {
        return planner.planAsync(domainProvider, new PDDLObjectProblemProvider(representation.getProblem(getBody(), goalForPlanning)));
    }

    @Override
    protected boolean validateWithExternalValidator(Queue<ActionDescription> currentPlan, IReactivePlan unexecutedReactivePlan, IPlanningGoal goal) {
        if(!unexecutedReactivePlan.getStatus().isFinished()){
            //validation with unexecuted reactive tasks does not make sense
            return true;
        }
        try {
            IValidationResult validationResult = validator.validate(domainProvider, new PDDLObjectProblemProvider(representation.getProblem(getBody(), goal)), new ArrayList<ActionDescription>(currentPlan));
            if (!validationResult.isValid()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Validation output:\n" + validationResult.getValidationOutput());
                }
                return false;
            } else {
                return true;
            }
        } catch (ValidationException ex) {
            logger.error("Exception in validating plan." + ex);
            return true;
        }
    }  
      

    @Override
    public Class getRepresentationClass() {
        return IPDDLRepresentation.class;
    }
    
    
}
