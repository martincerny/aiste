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

import JSHOP2.State;
import JSHOP2.TaskList;
import cz.cuni.amis.aiste.environment.IJShop2Problem;

/**
 *
 * @author Martin Cerny
 */
public class JShop2Problem implements IJShop2Problem{

    private String[] problemConstants;
    private State initialState;
    private TaskList taskList;

    public JShop2Problem(String[] problemConstants, State initialState, TaskList taskList) {
        this.problemConstants = problemConstants;
        this.initialState = initialState;
        this.taskList = taskList;
    }
    
    
    
    @Override
    public String[] getProblemConstants() {
        return problemConstants;
    }

    @Override
    public TaskList getTaskList() {
        return taskList;
    }

    @Override
    public State getInitialState() {
        return initialState;
    }

}
