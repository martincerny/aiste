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
package cz.cuni.amis.aiste.environment;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IAgentType;

/**
 * A handle to an agent body.
 * @author Martin Cerny
 */
public class AgentBody {

   
    /**
     * Id must be unique withini single environment instance.
     * It is the single member used in equals and hashCode.
     */
    private int id;
    
    private IAgentType type;


    /**
     * @param type 
     */
    public AgentBody(int id, IAgentType type) {
        this.id = id;
        this.type = type;
    }
    
    
    
    public IAgentType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return type.getName() + ": " + id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AgentBody other = (AgentBody) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }


        
}
