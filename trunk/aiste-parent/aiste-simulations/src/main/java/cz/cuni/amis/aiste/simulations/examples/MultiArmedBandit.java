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
package cz.cuni.amis.aiste.simulations.examples;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IPercept;
import cz.cuni.amis.aiste.environment.impl.AbstractModelLessRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.EmptyPercept;
import cz.cuni.amis.aiste.environment.impl.IntegerAction;
import cz.cuni.amis.aiste.environment.impl.IntegerPercept;
import cz.cuni.amis.aiste.environment.impl.SimpleAgentType;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.impl.SingletonAgentInstantiationDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

//TODO decouple representation and environment
/**
 * 
 * @author Martin Cerny
 */
public class MultiArmedBandit extends AbstractModelLessRepresentableSynchronizedEnvironment<IntegerAction, IPercept> {
    int numArms;
    int[] armMeans;
    
    AgentBody theBody;
    IAgentType theType;
    
    Random rnd;
    
    List<IntegerAction> theActions;
    
    public MultiArmedBandit(int numArms, int[] armMeans) {
        super(IntegerAction.class, IPercept.class);
        this.numArms = numArms;
        this.armMeans = armMeans;
        theType = new SimpleAgentType("MultiArmedBanditPlayer");
        theActions = new ArrayList<IntegerAction>(numArms);
        for(int i = 0; i < numArms; i++){
            theActions.add(new IntegerAction(i));
        }
    }

    @Override
    public void init() {
        super.init();;
        rnd = new Random();
        theBody = null;
    }
    
    

    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, IntegerAction> actionsToPerform) {
        IntegerAction action = actionsToPerform.get(theBody);
        double reward;
        if(action == null){
            reward = 0;
        } else {
            reward = rnd.nextInt(armMeans[action.getCommand()] * 2);
        }
        return Collections.singletonMap(theBody, reward);
    }

    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) {
        theBody = new AgentBody(0, type);
        return theBody;
    }

    @Override
    public Map<IAgentType, IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(theType, (IAgentInstantiationDescriptor)new SingletonAgentInstantiationDescriptor());
    }

    @Override
    public IPercept getPercept(AgentBody agentBody) {
        return new EmptyPercept();
    }

    @Override
    public Collection<IntegerAction> getPossibleActions(IAgentType agentType) {
        return theActions;
    }

    @Override
    public String getLoggableRepresentation() {
        return "Default";
    }
    
    
}
