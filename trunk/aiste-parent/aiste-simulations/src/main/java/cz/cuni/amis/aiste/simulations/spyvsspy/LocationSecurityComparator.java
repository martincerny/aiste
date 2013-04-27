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

import JSHOP2.Term;
import JSHOP2.TermConstant;
import cz.cuni.amis.aiste.environment.AgentBody;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public class LocationSecurityComparator implements Comparator<Term> {

    SpyVsSpyJShop2Representation representation;
    AgentBody body;

    public LocationSecurityComparator(SpyVsSpyJShop2Representation representation, AgentBody body) {
        this.representation = representation;
        this.body = body;
    }

    protected int numHardTrapsAtLocation(SpyVsSpyMapNode node){
        Set<Integer> traps = new HashSet<Integer>(node.traps);
        int[] removersCarried = representation.environment.bodyInfos.get(body.getId()).numTrapRemoversCarried;
        for(int trapType = 0; trapType < representation.environment.defs.numTrapTypes; trapType++){
            if(traps.isEmpty()){
                return 0;
            }
            if(removersCarried[trapType] > 0){
                traps.remove(trapType);
            }
        }
        return traps.size();
    }
    
    @Override
    public int compare(Term o1, Term o2) {
        if(!(o1 instanceof TermConstant) || !(o2 instanceof TermConstant)){
            //cannot compare terms that are not constants - I cannot map those to locations
            return 0;
        }
        SpyVsSpyMapNode n1 = representation.environment.nodes.get(representation.constantsToLocationId.get(((TermConstant)o1).getIndex()));
        SpyVsSpyMapNode n2 = representation.environment.nodes.get(representation.constantsToLocationId.get(((TermConstant)o2).getIndex()));
        int hardTrapsDiff = numHardTrapsAtLocation(n1) - numHardTrapsAtLocation(n2);
        if(hardTrapsDiff != 0){
            return hardTrapsDiff;
        }
        
        return n1.traps.size() - n2.traps.size();
    }

    
}
