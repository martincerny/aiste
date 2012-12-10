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
package cz.cuni.amis.aiste.simulations.simplefps;

import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IStateVariable;
import cz.cuni.amis.aiste.environment.impl.AbstractStateVariableRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.impl.EnumStateVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author 
 */
public class SimpleFPS extends AbstractStateVariableRepresentableSynchronizedEnvironment<SimpleFPSAgentBody, SimpleFPSAction> {

    private int minPlayers;
    private int maxPlayers;

    private List<IStateVariable> itemsAtLocations;
    
    enum ItemType {
        NOTHING, WEAPON_1, MEDIKIT
    }
    
    public SimpleFPS() {
        super(SimpleFPSAgentBody.class, SimpleFPSAction.class);
        //TODO doplnit a zmenit
        minPlayers = 2;
        maxPlayers = 4;
        
        //TODO: prostredi se musi umet reprezentovat pomoci sady state variables, to jsou vlastne pary jmeno-hodnota
        //protoze prostredi je obecne, budes je muset mit v nejakych kolekcich, asi listech nebo mapach, jak se ti to bude hodit
        //je na tobe, jestli budes pro simulaci pouzivat primo tyhle promenne, nebo jestli si budes stav udrzovat jinak 
        //a do promennych budes ten stav pri simulaci mirrorovat
        itemsAtLocations = new ArrayList<IStateVariable>();
        EnumStateVariable newVariable = new EnumStateVariable("ItemAtLocation1", ItemType.class); //jsou tez IntegerStateVariable, pripadne si muzes udelat i vlastni typy
        itemsAtLocations.add(newVariable);
        
        //promennou je potreba zaregistrovat u parent tridy
        addStateVariable(newVariable);
        //a nastavit ji (a v prubehu simulace udrzovat) hodnotu
        setStateVariableValue(newVariable, ItemType.NOTHING);
    }
    
    
    
    @Override
    protected Map<SimpleFPSAgentBody, Double> simulateOneStepInternal(Map<SimpleFPSAgentBody, SimpleFPSAction> actionsToPerform) {
        //krok simulace vraci reward, ktery dostali agenti za provedene akce
        //v nasem pripade je reward +1 za zabiti oponenta, jinak 0 (tj. klasicky frag count)
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected SimpleFPSAgentBody createAgentBodyInternal(IAgentType type) {
        if(type != SimpleFPSAgentType.getInstance()){
            throw new AgentInstantiationException("Illegal agent type");
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(SimpleFPSAgentType.getInstance(), new AgentInstantiationDescriptor(minPlayers, maxPlayers));
    }
    
    
}
