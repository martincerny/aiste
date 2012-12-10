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
package cz.cuni.amis.aiste.environment.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * A state variable whose range is integers from {@link #minimum} (inclusive) to {@link #maximum} (exclusive).
 * @author Martin Cerny
 */
public class IntegerStateVariable extends AbstractStateVariable{
    private int minimum;
    private int maximum;
    private List<Object> values;

    public IntegerStateVariable(String name, int minimum, int maximum) {
        super(name);
        this.minimum = minimum;
        this.maximum = maximum;
        values = new ArrayList<Object>(maximum - minimum);
        for(int i = minimum; i < maximum; i++){
            values.add(i);
        }
    }

    public int getMaximum() {
        return maximum;
    }

    public int getMinimum() {
        return minimum;
    }
    
    

    @Override
    public List<Object> getValues() {
        return values;
    }
    
    
    
    
}
