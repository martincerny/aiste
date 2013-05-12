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

import JSHOP2.JSHOP2;
import JSHOP2.Predicate;
import JSHOP2.Term;
import JSHOP2.TermConstant;
import JSHOP2.TermList;
import cz.cuni.amis.aiste.AisteException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class JShop2Utils {
    public static TermList createTermList(JSHOP2 jshop, int ... constantIndices){
        return createTermList(jshop, null, constantIndices);
    }
    
    public static TermList createTermList(JSHOP2 jshop, Term tail, int ... constantIndices){
        TermList ret;
        if(tail == null){
            ret = TermList.NIL;
        } else if(tail instanceof TermList){
            ret = (TermList) tail;
        } else {
            ret = new TermList(tail, TermList.NIL);
        }
        for(int i = constantIndices.length - 1; i >= 0; i--){
            ret = new TermList(jshop.getConstant(constantIndices[i]), ret);
        }
        return ret;
    }
    
    public static GroundActionInfo getGroundInfo(Predicate actionFromPlanner){
        int actionId = actionFromPlanner.getHead();
        java.util.List<Integer> actionParams = new ArrayList<Integer>();

        Term param = actionFromPlanner.getParam();
        if (param instanceof TermList) {
            for (Term listTerm = param; !listTerm.isNil(); listTerm = ((TermList) listTerm).getList().getTail()) {
                Term head = ((TermList) listTerm).getList().getHead();
                if (!(head instanceof TermConstant)) {
                    throw new AisteException("Cannot translate non-constant parameter");
                }
                actionParams.add(((TermConstant) head).getIndex());
            }
        } else //-- If the argument list is not a list term (which should not happen
        //-- usually, but there is no reason to assume that it will not happen.
        {
            if (!(param instanceof TermConstant)) {
                throw new AisteException("Cannot translate non-constant parameter");
            }
            actionParams.add(((TermConstant) param).getIndex());
        }
        
        return new GroundActionInfo(actionId, actionParams);
    }
    
    public static class GroundActionInfo {
        public int actionId;
        public List<Integer> params;

        public GroundActionInfo(int actionId, List<Integer> params) {
            this.actionId = actionId;
            this.params = params;
        }
        
        
    }
}
