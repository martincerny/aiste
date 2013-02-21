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

package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IAction;
import cz.cuni.amis.aiste.environment.IActionFailureRepresentation;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.ISimulableEnvironment;
import cz.cuni.amis.aiste.environment.ISimulableEnvironmentRepresentation;

/**
 *
 * @author Martin Cerny
 */
public abstract class AbstractSpyVsSpyRepresentation implements IActionFailureRepresentation, ISimulableEnvironmentRepresentation<SpyVsSpy> {
    
    protected SpyVsSpy environment;

    public AbstractSpyVsSpyRepresentation() {
    }

    public boolean isGoalState(AgentBody body) {
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        if (info.locationIndex != environment.defs.destination) {
            return false;
        }
        if (info.itemsCarried.size() != environment.defs.numItemTypes) {
            return false;
        }
        return true;
    }

    @Override
    public boolean lastActionFailed(AgentBody body) {
        return environment.lastActionFailed(body);
    }

    @Override
    public void setEnvironment(SpyVsSpy env) {
        if (env.defs != this.environment.defs) {
            throw new IllegalArgumentException("Environment could only be set to a copy of the original env.");
        }
        this.environment = env;
    }

}
