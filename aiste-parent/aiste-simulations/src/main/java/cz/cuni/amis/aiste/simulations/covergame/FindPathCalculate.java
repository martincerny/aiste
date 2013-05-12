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
package cz.cuni.amis.aiste.simulations.covergame;

import JSHOP2.Calculate;
import JSHOP2.List;
import JSHOP2.Term;
import cz.cuni.amis.aiste.environment.AgentBody;

/**
 *
 * @author Martin
 */
public class FindPathCalculate implements Calculate {
    private CGJSHOPRepresentation representation;
    private AgentBody body;

    public FindPathCalculate(CGJSHOPRepresentation representation, AgentBody body) {
        this.representation = representation;
        this.body = body;
    }

    @Override
    public Term call(List l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
}
