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

import JSHOP2.Calculate;
import JSHOP2.List;
import JSHOP2.Term;
import JSHOP2.TermNumber;

/**
 *
 * @author Martin Cerny
 */
public class ReachableCalculate implements Calculate{
    private SpyVsSpyJShop2Representation representation;

    public ReachableCalculate(SpyVsSpyJShop2Representation representation) {
        this.representation = representation;
    }

    
    
    @Override
    public Term call(List l) {
        Term from = l.get(0);
        Term to = l.get(1);
        return new TermNumber(1);
    }
    
    
}
