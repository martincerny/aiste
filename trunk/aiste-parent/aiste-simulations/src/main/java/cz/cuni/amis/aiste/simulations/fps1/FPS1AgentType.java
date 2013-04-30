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
package cz.cuni.amis.aiste.simulations.fps1;

import cz.cuni.amis.aiste.environment.impl.SimpleAgentType;

/**
 * Simple FPS has only one agent type, this one.
 * @author 
 */
public class FPS1AgentType extends SimpleAgentType {

    private static FPS1AgentType instance = new FPS1AgentType();
    
    private FPS1AgentType() {
        super("DefaultSimpleFPSAgent");
    }

    public static FPS1AgentType getInstance() {
        return instance;
    }
    
    
    
}
