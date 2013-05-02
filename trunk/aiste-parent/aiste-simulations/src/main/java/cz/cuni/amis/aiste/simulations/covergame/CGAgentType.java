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
package cz.cuni.amis.aiste.simulations.covergame;

import cz.cuni.amis.aiste.environment.impl.SimpleAgentType;

/**
 * Simple FPS has only one agent type, this one.
 * @author 
 */
public class CGAgentType extends SimpleAgentType {

    private static CGAgentType team0Instance = new CGAgentType(0);
    private static CGAgentType team1Instance = new CGAgentType(1);
    private int teamNo;
    
    private CGAgentType(int teamNo) {
        super("CGAgent_Team_" + teamNo);
        this.teamNo = teamNo;
    }

    public int getTeamNo() {
        return teamNo;
    }        

    public static CGAgentType getTeam0Instance() {
        return team0Instance;
    }

    public static CGAgentType getTeam1Instance() {
        return team1Instance;
    }

    
    
    
}
