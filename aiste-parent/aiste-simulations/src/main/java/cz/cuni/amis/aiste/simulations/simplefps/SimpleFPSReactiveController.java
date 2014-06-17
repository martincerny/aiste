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
package cz.cuni.amis.aiste.simulations.simplefps;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractAgentController;
import cz.cuni.amis.aiste.simulations.simplefps.SimpleFPS;
import cz.cuni.amis.aiste.simulations.simplefps.SimpleFPSAction.ActionType;
import cz.cuni.amis.pathfinding.alg.astar.AStar;
import cz.cuni.amis.pathfinding.map.IPFGoal;
import cz.cuni.amis.pathfinding.map.IPFMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author 
 */
class Tuple<X, Y> 
{ 
  public final X first; 
  public final Y second; 
  public Tuple(X x, Y y) 
  { 
    this.first = x; 
    this.second = y; 
  } 
} 

public class SimpleFPSReactiveController extends AbstractAgentController<SimpleFPSAction, SimpleFPS> 
{
    int enemyID;
    String enemyPos;
    
    @Override
    public void init(IEnvironment<SimpleFPSAction> environment, SimpleFPS representation, AgentBody body, long stepDelay) 
    {
        super.init(environment, representation, body, stepDelay);
        //prepare everything you need in here
        enemyID = -1;
        enemyPos = "";
    }



    @Override
    public void onSimulationStep(double reward) 
    {
        super.onSimulationStep(reward);                                
        
        String ammo1status = representation.getAgentInfo(body.getId(), "ammo1");
        String ammo2status = representation.getAgentInfo(body.getId(), "ammo2");
        String ammo3status = representation.getAgentInfo(body.getId(), "ammo3");
        String healthStatus = representation.getAgentInfo(body.getId(), "health");
        String position = representation.getAgentInfo(body.getId(), "position");
        String travelingTo = representation.getAgentInfo(body.getId(), "traveling");        
        boolean enemyAlert = false;
        //zistim si, co sa nachadza v susednych izbach
        //Map< Map< toNode, itemThere>, Lenght>
        ArrayList<Tuple<String, Tuple<String, ItemType>>> neiborhood = new ArrayList<Tuple<String, Tuple<String, ItemType>>>();
        
        neiborhood = searchNeiborhood(position);                
        //v tejto chvili mame v neiborhood info o tom, kam vedu chodby a co je na druhej strane
        //touto fciou vieme v pripade potreby aj prehladavat a vytvarat dalsie pohlady (vlnovo)
        //je to pekne info, kedze zarhna to, kam ide chodba, co je na druhej strane a este aj dlzku chodby
        //dlzku chodby pouzije agent pri pohybe aj pri vybere zbrane pri strelbe
        
        enemyAlert = enemyInNeiborhood(neiborhood);     
        //skusim testovat, ci iny agenti nemaju position nastaveny na mojho suseda
        //ak ano, zapnem flag enemyAlert
        
        //agent ma nejake priority        
        //prezriem susedov a hladam 
        //PRIOTITY: 
        //1. if(dead) -> respawn();
        //2. ak robim nejaky MOVE, snazim sa ho dokoncit
        //3. if(nepriatel) -> shoot(target) toto bude zlozitejsie, lebo sa bude dalej rozhodovat aku zbran pouzije vzhladom k dlzke chodby a podobne        
        //4. if(health < HIGH) -> find(medikit)
        //5. if(weapon_n < HIGH) -> find(weapon_n)
        //ultimate stav (full HP, full ammo, no enemy) - MOVE
        
        //v pripade, ze napr. potrebujeme lekarnicku, ale susedia ziadnu neposkytuju, proste dame MOVE na nejakeho suseda
        //MOVE na random suseda bude posledna moznost
        
        //1. sme mrtvi - respawn
        if(healthStatus.equalsIgnoreCase("EMPTY"))
        {
            act(new SimpleFPSAction(ActionType.RESPAWN, ItemType.NOTHING, "", -1));
            //sme mrtvi - preto poziadame system o respawn;
            return;
        }
        //2. robime nejaky move - poziadame o posun dopredu
        if(!travelingTo.isEmpty()) //niekam cestujeme
        {
            act(new SimpleFPSAction(ActionType.MOVE, ItemType.NOTHING, travelingTo, -1));
            //agent si len poziada o posun !
            //to, ci je za polkou chodby a teda zmenu pozicie
            //bude riadit SimpleFPS - cize prostredie
            return;
        }
        //3. vidime nepriatela
        else
        if(enemyAlert) //vidime nepriatela, udaje o nom uz mame
        {
            //identifikujeme nepriatela
            //budeme blbo strielat na prveho na ktoreho nazime
            //v enemyID uz mame ID nepriatela, ktoreho sme zbadali
            //v enemyPos mame poziciu zbadaneho nepriatela
            
            //budeme strielat !
            //je nutne si zvolit zbran !
            //zbran si volime podla dlzky chodby
            if(chooseWeapon(neiborhood, ammo1status, ammo2status, ammo3status)) 
            {
                //tato metodka rozhodne o zbrani a zaroven odosle potrebnu ziadost o akciu                                                                      
                return;
            }
                
        }
        else
        //4. pozrieme sa na svoje zdravie
        //v podmienke je slusne otestovat aj to, ci nie sme mrtvi - co je ale zbytocne
        //ak by sme boli mrtvi, nedostaneme sa sem cez prvy if
        if(!healthStatus.equalsIgnoreCase("EMPTY") && !healthStatus.equalsIgnoreCase("HIGH"))
        {
            //sem sa dostaneme, ak zivoty nie su EMPTY a zaroven nie su HIGH (cize nie sme mrtvi, ale ani plne zdravi)
            //je nutne niekde najst lekarnicku
            //prv pozriem ci na nejakej nesedim
            if(findMedikit(neiborhood, position))
            {
                //metoda sa o vsetko postara a tiez poziada prostredie o potrebne akcie
                return;
            }
        } 
        else
        //5. pozrieme sa na zasobniky a ked nie su plne, skusime najst prvu neplnu zbran
        if(!ammo1status.equalsIgnoreCase("HIGH") || !ammo2status.equalsIgnoreCase("HIGH") || !ammo3status.equalsIgnoreCase("HIGH"))
        {
            //sem si vlezieme, ak aspon 1 zbran nema plne nabite
            //vyberieme si hlupo prvu nenabitu zbran a pozrieme, ci nesedime na municii, alebo ci v susedstve municiu nemame
            if(!ammo1status.equalsIgnoreCase("HIGH"))
            {
                //pistol nie je plne nabita
                //pozriem, ci nesedim na zbrani
                if(findWeapon(neiborhood, ItemType.WEAPON_1, position))
                {
                    //metoda sa postarala o najdenie zbrane a pripadne zadanie prikazov
                    //vsetko prebehlo v poriadku
                    return;
                }                
            }
            else
            if(!ammo2status.equalsIgnoreCase("HIGH"))
            {
                //brokovnia nie je plne nabita
                //pozriem, ci nesedim na zbrani
                if(findWeapon(neiborhood, ItemType.WEAPON_2, position))
                {
                    //metoda sa postarala o najdenie zbrane a pripadne zadanie prikazov
                    //vsetko prebehlo v poriadku
                    return;
                }  
            }
            else
            if(!ammo3status.equalsIgnoreCase("HIGH"))
            {
                //bazuka nie je plne nabita
                //pozriem, ci nesedim na zbrani
                if(findWeapon(neiborhood, ItemType.WEAPON_3, position))
                {
                    //metoda sa postarala o najdenie zbrane a pripadne zadanie prikazov
                    //vsetko prebehlo v poriadku
                    return;
                }  
            }        
        }
        //Final: vsetko je OK - nahodne zvolime suseda a poziadame o presun
        else
        {
            Random rand = new Random();
            int whichOne = rand.nextInt((neiborhood.size() - 1) + 1);
                        
            String target = neiborhood.get(whichOne).second.first;
            act(new SimpleFPSAction(ActionType.MOVE, ItemType.NOTHING, target, -1));
            //return; //tu uz return ani netreba volat. :)
        }                               
    }
    
    

    @Override
    public void shutdown() 
    {
        super.shutdown();
    }

    @Override
    public Class getRepresentationClass() 
    {
        return SimpleFPS.class;
    }

    @Override
    public String getLoggableRepresentation() 
    {
        return "SimpleFPSReactive";
                
    }   

    private ArrayList<Tuple<String, Tuple<String, ItemType>>> searchNeiborhood(String position) 
    {
        ArrayList<Tuple<String, Tuple<String, ItemType>>> neiborhood = new ArrayList<Tuple<String, Tuple<String, ItemType>>>();        
        
        
        for(int room = 0; room < representation.map.size(); ++room)
        {                                  
            String length = "";
            
            //nasli sme v mape miestnost, kde je prave agent
            if(position.equalsIgnoreCase(representation.map.get(room).ID))
            {                                
                for(int pass = 0; pass < representation.map.get(room).passages.size(); ++pass)
                {                    
                    String passageGoesTo = representation.map.get(room).passages.get(pass).toNode;
                    length = representation.map.get(room).passages.get(pass).length;
                    ItemType nextRoomItem = null;
                    //musime prehladat mapu a najst aktualneho suseda, aby sme ulozili jeho obsah
                    for(int i = 0; i < representation.map.size(); ++i)
                    {
                        if(representation.map.get(i).ID.equalsIgnoreCase(passageGoesTo))
                        {
                            nextRoomItem = representation.map.get(i).item;
                            break; //nasli sme, co potrebujeme - nema zmysel dalej iterovat cyklus
                        }
                    }
                    
                    Tuple<String, ItemType> nextRoom = new Tuple<String, ItemType>(passageGoesTo, nextRoomItem);   
                    Tuple<String, Tuple<String, ItemType>> completeNextRoom = new Tuple<String, Tuple<String, ItemType>>(length, nextRoom);  
                    neiborhood.add(completeNextRoom);
                }
                
                //nema zmysel dalej iterovat for cyklus
                break;
            }
        }
        return neiborhood;
    }        
    
    private boolean enemyInNeiborhood(ArrayList<Tuple<String, Tuple<String, ItemType>>> neiborhood)
    {
        for(int i = 0; i < neiborhood.size(); ++i)
        {
            for(int j = 0; j < SimpleFPS.players; ++j)
            {
                if(j == body.getId())
                {
                    //nema vyznam nieco robit so sebou samym
                    continue;
                }
                else
                {
                    String enemyPos = representation.getAgentInfo(j, "position");
                    if(neiborhood.get(i).second.first.equalsIgnoreCase(enemyPos))
                    {
                        //nepriatel !!!!!!
                        this.enemyID = j;
                        this.enemyPos = enemyPos;
                        return true;
                    }
                }                                
                        
            }
        }
        
        
        return false;
    }
    
    private boolean chooseWeapon(ArrayList<Tuple<String, Tuple<String, ItemType>>> neiborhood, String ammo1status, String ammo2status, String ammo3status)
    {
        for(int i = 0; i < neiborhood.size(); ++i)
        {
            if(neiborhood.get(i).second.first.equalsIgnoreCase(this.enemyPos))
            {
                if(neiborhood.get(i).first.equalsIgnoreCase("LOW"))
                {
                    if(!ammo2status.equalsIgnoreCase("EMPTY"))
                    {
                        act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_2, "", this.enemyID));
                        //strielam brokovnicou
                        return true;
                    }
                    else
                    {
                        //brokovnica ma malo nabojov
                        //vyberiem si prvu zbran s nabojmi a strielam
                        if(!ammo1status.equalsIgnoreCase("EMPTY"))
                        {
                            act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_1, "", this.enemyID));
                            return true;
                        }
                        else
                        if(!ammo3status.equalsIgnoreCase("EMPTY"))
                        {
                            act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_3, "", this.enemyID));
                            return true;
                        }
                    }                        
                }
                //podobne pre zvysne dlzky a zbranove kombinacie
                else
                if(neiborhood.get(i).first.equalsIgnoreCase("MED"))
                {
                    if(!ammo1status.equalsIgnoreCase("EMPTY"))
                    {
                        act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_1, "", this.enemyID));
                        //strielam pistolou
                        return true;
                    }
                    else
                    {
                        //pistol ma malo nabojov
                        //vyberiem si prvu zbran s nabojmi a strielam
                        if(!ammo2status.equalsIgnoreCase("EMPTY"))
                        {
                            act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_2, "", this.enemyID));
                            return true;
                        }
                        else
                        if(!ammo3status.equalsIgnoreCase("EMPTY"))
                        {
                            act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_3, "", this.enemyID));
                            return true;
                        }
                    }    
                }
                else
                if(neiborhood.get(i).first.equalsIgnoreCase("HIGH"))
                {
                    if(!ammo3status.equalsIgnoreCase("EMPTY"))
                    {
                        act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_3, "", this.enemyID));
                        //strielam bazukou
                        return true;
                    }
                    else
                    {
                        //bazuka ma malo nabojov
                        //vyberiem si prvu zbran s nabojmi a strielam
                        if(!ammo1status.equalsIgnoreCase("EMPTY"))
                        {
                            act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_1, "", this.enemyID));
                            return true;
                        }
                        else
                        if(!ammo2status.equalsIgnoreCase("EMPTY"))
                        {
                            act(new SimpleFPSAction(ActionType.SHOOT, ItemType.WEAPON_2, "", this.enemyID));
                            return true;
                        }
                    }    
                }

                break; //vsetko je nastavene, nema zmysel prezerat dalsie chodby
            }
        }  
        
        return false; //strielat by som chcel !... ale nemam nakoniec cim :(
    }
    
    private boolean findMedikit(ArrayList<Tuple<String, Tuple<String, ItemType>>> neiborhood, String position)
    {
        for(int room = 0; room < representation.map.size(); ++room)
        {                                                  
            //nasli sme v mape miestnost, kde je prave agent
            if(position.equalsIgnoreCase(representation.map.get(room).ID))
            {
               if(representation.map.get(room).item == ItemType.MEDIKIT)
               {
                   act(new SimpleFPSAction(ActionType.PICKUPITEM, ItemType.MEDIKIT, "", -1));
                   return true;
               }
            } 
        }
        //prehladame susedov kvoli lekarnicke            
        for(int room = 0; room < neiborhood.size(); ++room)
        {
            if(neiborhood.get(room).second.second == ItemType.MEDIKIT)
            {
                //sused ma lekarnicku !
                //hura za nou !
                act(new SimpleFPSAction(ActionType.MOVE, ItemType.NOTHING, neiborhood.get(room).second.first, -1));
                return true;
            }
        } 
        
        return false; //nenasla sa medicina
    }
    
    private boolean findWeapon(ArrayList<Tuple<String, Tuple<String, ItemType>>> neiborhood, ItemType weapon, String position)
    {
        for(int room = 0; room < representation.map.size(); ++room)
        {                                                  
            //nasli sme v mape miestnost, kde je prave agent
            if(position.equalsIgnoreCase(representation.map.get(room).ID))
            {
               if(representation.map.get(room).item == weapon)
               {
                   act(new SimpleFPSAction(ActionType.PICKUPITEM, weapon, "", -1));
                   return true;
               }
            } 
        }
        //pozriem, ci ju nemaju susedia         
        for(int room = 0; room < neiborhood.size(); ++room)
        {
            if(neiborhood.get(room).second.second == weapon)
            {
                //na susednom vrchole je ziadana zbran                
                act(new SimpleFPSAction(ActionType.MOVE, ItemType.NOTHING, neiborhood.get(room).second.first, -1));
                return true;
            }
        }
        
        return false;
    }
}
