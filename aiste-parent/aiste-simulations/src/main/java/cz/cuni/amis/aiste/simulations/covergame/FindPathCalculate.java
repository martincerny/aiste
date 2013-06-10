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
import JSHOP2.TermConstant;
import JSHOP2.TermList;
import cz.cuni.amis.aiste.environment.AgentBody;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin
 */
public class FindPathCalculate implements Calculate {
    private CGJSHOPRepresentation representation;
    private AgentBody body;
    
    private final Logger logger = Logger.getLogger(FindPathCalculate.class);

    public FindPathCalculate(CGJSHOPRepresentation representation, AgentBody body) {
        this.representation = representation;
        this.body = body;
    }

    @Override
    public Term call(List l) {
        TermConstant bodyTerm = (TermConstant) l.get(0);
        TermConstant fromTerm = (TermConstant)l.get(1);
        TermConstant toTerm = (TermConstant)l.get(2);
        TermConstant constraintTerm = (TermConstant)l.get(3);
        
        Loc fromLoc = representation.constantsToLocations.get(fromTerm.getIndex());
        Loc toLoc = representation.constantsToLocations.get(toTerm.getIndex());
        
        java.util.List<Loc> path = representation.fixedMapFloydWarshall.getPath(fromLoc, toLoc);        
        TermList pathTerm;
        //TODO: blocking by the other body???
        if(path == null){
            if(logger.isDebugEnabled()){
                logger.debug(body.getId() + ": Path from " + fromLoc + " to " + toLoc + " not found");
            }
            pathTerm = TermList.NIL;
        } else {
            if(logger.isDebugEnabled()){
                logger.debug(body.getId() + ": Found path from " + fromLoc + " to " + toLoc + " with cost " + representation.fixedMapFloydWarshall.getPathCost(fromLoc, toLoc));
            }
            
            path.add(toLoc); //the final location is not a part of the path returned from FloydWarshall
            
            pathTerm = TermList.NIL;
            
            //Construct the list term
            for(int i = path.size() - 1; i >= 0; i--){ 
                pathTerm = new TermList(representation.jshops.get(body).getConstant(representation.locationsToConstants.get(path.get(i))), pathTerm);
            }

        }
        return pathTerm;
    }
    
    
}
