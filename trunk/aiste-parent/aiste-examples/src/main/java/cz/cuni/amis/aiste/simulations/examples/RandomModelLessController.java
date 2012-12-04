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

import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IAgentBody;
import cz.cuni.amis.aiste.environment.IModelLessRepresentableEnvironment;
import cz.cuni.amis.aiste.environment.IPercept;
import cz.cuni.amis.aiste.environment.impl.AbstractReactiveModelLessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Martin Cerny
 */
public class RandomModelLessController extends AbstractReactiveModelLessController<IAgentBody, IAction, IPercept> {

    List<IAction> possibleActions;
    
    Random rnd;

    @Override
    public void init(IModelLessRepresentableEnvironment<IAgentBody, IAction, IPercept> environment, IAgentBody body, long stepDelay) {
        super.init(environment, body, stepDelay);
        possibleActions = new ArrayList<IAction>(environment.getPossibleActions(body.getType()));
        rnd = new Random();
    }
    
    
    
    @Override
    protected IAction getActionForPercept(IPercept percept) {
        return possibleActions.get(rnd.nextInt(possibleActions.size()));
    }

}
