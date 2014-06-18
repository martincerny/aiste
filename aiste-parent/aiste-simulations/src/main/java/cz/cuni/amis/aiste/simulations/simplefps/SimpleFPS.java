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
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.aiste.simulations.simplefps.SimpleFPSAction.ActionType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author 
 */
enum ItemType 
{
    NOTHING, WEAPON_1, WEAPON_2, WEAPON_3, MEDIKIT
}

enum ValueType
{
    EMPTY, LOW, MED, HIGH
}

abstract class Item
{
    String name;      
}

class Weapon extends Item
{
    //low, med, high
    //3 druhy zbrani podla ID (W1, W2, W3)
    // pistol (low dmg, high acc, med range)
    // shotgun (high dmg, low acc, low range)
    // bazooka (high dmg, low acc, high range)
    ValueType damage;
    ValueType accuracy;
    ValueType range;
    String ID;
    
    Weapon(ValueType dmg, ValueType acc, ValueType ran, String ID)
    {
        this.damage = dmg;
        this.accuracy = acc;
        this.range = ran;
        this.ID = ID;
    }
}

class Medikit extends Item
{
    // LOW = 25hp, MED = 50hp, HIGH = 100hp
    ValueType restoresHP;
    
    Medikit(ValueType rest)
    {
        this.restoresHP = rest;
    }
}

class Node1
{       
    ArrayList<Passage> passages;
    ItemType item;
    boolean spawnable;
    String ID;
    
    Node1(String ID, ItemType item, boolean spawn, ArrayList<Passage> passs)
    {
        this.ID = ID;
        this.spawnable = spawn;
        this.item = item;
        this.passages = passs;
    }
    
}

class Passage
{
    //toNode - ID node (napr n1, n2,...)
    String toNode;
    //dlzka moze byt LOW, MED, HIGH - ak je napr LOW a strielam HIGH range zbranov, trafim cez chodbu
    //abstrakcia
    String length;    
    
    //doplnit ID chodby - do XML dokumentu a parsovat ho, cize doplnit kod do parsera vstupu
    
    Passage(String toNode, String length)
    {
        this.toNode = toNode;
        this.length = length;
    }
}


public class SimpleFPS extends AbstractSynchronizedEnvironment<SimpleFPSAction> implements IEnvironmentRepresentation
{

    private int minPlayers;
    private int maxPlayers;
    private String mapLocation;  
    public static int players;
    public ArrayList<Node1> map;        
    
    /**
     * Informace o jednotlivych agentech. Indexem je id {@link AgentBody#id}
     */
    private List<SimpleFPSBodyInfo> bodyInfos;
    ArrayList<Weapon> weapons;
    Medikit medikit;
    
    public SimpleFPS(int minP, int maxP, String mapLoc) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException 
    {
        super(SimpleFPSAction.class);
        registerRepresentation(this);
        
        //TODO doplnit a zmenit
        this.minPlayers = minP;
        this.maxPlayers = maxP;
        this.mapLocation = mapLoc;
              
        //dostaneme XML subor, musime ho prv parsovat do Listu
        System.out.println("Starting map parser - please wait.");
        map = XMLMapParser(mapLocation);
        System.out.println("Map " + mapLoc + " was succesfully loaded.");
        simpleMapReview(map); 
        
        //zapuzdrit vytvaranie predmetov fciou        
        System.out.println("");
        System.out.println("Creating weapons and medikits.");
        weapons = new ArrayList<Weapon>();
        
        Weapon weapon1 = new Weapon(ValueType.LOW, ValueType.HIGH, ValueType.MED, "w1"); //pistol                        
        Weapon weapon2 = new Weapon(ValueType.HIGH, ValueType.LOW, ValueType.LOW, "w2"); //shotgun
        Weapon weapon3 = new Weapon(ValueType.HIGH, ValueType.LOW, ValueType.HIGH, "w3"); //bazooka
        medikit = new Medikit(ValueType.HIGH); //restores full health (health := HIGH)    
        weapons.add(weapon1);
        weapons.add(weapon2);
        weapons.add(weapon3);                       
        System.out.println("Items succesfully created.");
        System.out.println("");   
        //tu konci buduca fcia
                        
         bodyInfos = new ArrayList<SimpleFPSBodyInfo>();                                                                    
    }
    
    ArrayList<Node1> XMLMapParser(String mapLocation) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException
    {
        ArrayList<Node1> ret = new ArrayList<Node1>();
        //InputStream in = new InputSteam(Test.class.getResourceAsStream(mapLocation));
        
        File file = new File("F:\\zapoctak Java\\aiste-parent\\aiste-simulations\\src\\main\\java\\cz\\cuni\\amis\\aiste\\simulations\\simplefps\\map1.txt");

        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
 
	Document doc = dBuilder.parse(file); 
        
        NodeList mapNodes = doc.getElementsByTagName("node");
        for(int i = 0; i < mapNodes.getLength(); ++i)
        {
            String newNodeID = "";
            String newNodeItem= "";
            ItemType item;
            boolean newNodeSpawn = false;
            ArrayList<Passage> newNodePasses = new ArrayList<Passage>();
            
            NamedNodeMap nodeMap = mapNodes.item(i).getAttributes();
            Node node = nodeMap.item(0);
            if(node.getNodeName().equalsIgnoreCase("id"))
            {
                newNodeID = node.getNodeValue();                
            }
            
            Element nodeNode = (Element) mapNodes.item(i);            
            NodeList nodeItem = nodeNode.getElementsByTagName("item");
            newNodeItem = nodeItem.item(0).getFirstChild().getNodeValue();
            if(newNodeItem.equalsIgnoreCase("w1"))
            {
                item = ItemType.WEAPON_1;                
            }
            else
            if(newNodeItem.equalsIgnoreCase("w2"))
            {
                item = ItemType.WEAPON_2;                
            }
            else
            if(newNodeItem.equalsIgnoreCase("w3"))
            {
                item = ItemType.WEAPON_3;                
            }
            else
            if(newNodeItem.equalsIgnoreCase("m"))
            {
                item = ItemType.MEDIKIT;                
            }  
            else
            {
                 item = ItemType.NOTHING;                 
            }                       
            
            NodeList nodeSpawn = nodeNode.getElementsByTagName("spawnable");
            if(nodeSpawn.getLength() == 1)
            {
                newNodeSpawn = true;                
            }
            
            NodeList nodePassages = nodeNode.getElementsByTagName("pass");
            for(int j = 0; j < nodePassages.getLength(); ++j)
            {
                String newPassToNode = "";
                String newPassLength= "";                                
                
                NamedNodeMap nodePass = nodePassages.item(j).getAttributes();
                Node singlePass = nodePass.item(0);
                if(singlePass.getNodeName().equalsIgnoreCase("tonode"))
                {
                    newPassToNode = singlePass.getNodeValue();                    
                }
                
                Element nodePassage = (Element) nodePassages.item(j);            
                NodeList passLength = nodePassage.getElementsByTagName("length");
                newPassLength = passLength.item(0).getFirstChild().getNodeValue();                
                
                //vyrobit class Passage a priradit do uz definovanej
                Passage pass = new Passage(newPassToNode, newPassLength);
                newNodePasses.add(pass);
                
            }
            
            //vyrobit class Node1 s tym, co mame doteraz narobene a priradit do listu vystupu
            Node1 newNode = new Node1(newNodeID, item, newNodeSpawn, newNodePasses);
            ret.add(newNode);
            
        }                
        
        return ret;
    }  
    
    void simpleMapReview(ArrayList<Node1> parsedMap)
    {
        System.out.println("Simple map review initialized ~");
        System.out.println("");
        //mam naprskanu mapu - poslem vystup ako vyzera
        for(int node = 0; node < parsedMap.size(); ++node)
        {
            System.out.println("Node " + parsedMap.get(node).ID + " looks like this:");
            System.out.println("");
            System.out.println("There is " + parsedMap.get(node).item.name() + " in this room.");            
            if(parsedMap.get(node).spawnable)
            {
                System.out.println("This room can be used as a spawn point.");
            }
            System.out.println("There are " + parsedMap.get(node).passages.size() + " passages from this room.");
            System.out.println("");
            for(int pass = 0; pass < parsedMap.get(node).passages.size(); ++pass)
            {
                System.out.println("Passage number " + pass + " leads to room " + parsedMap.get(node).passages.get(pass).toNode );
                System.out.println("This passage has " + parsedMap.get(node).passages.get(pass).length + " length.");
                System.out.println("");
            }
        }
        System.out.println("Simple map review terminated ~");
        System.out.println("");
    }       
    
    void initSpawnAgents()
    {
        System.out.println("Agents will now pick their spawn points.");
        for(int agent = 0; agent < bodyInfos.size(); ++agent)
        {
            for(int room = 0; room < map.size(); ++room)
            {
                boolean canSpawnHere = true;
                
                //pozrieme, ci je room spawnable
                if(map.get(room).spawnable)
                {
                    //toto ale nestaci !
                    //musime pozriet, ci predosli agenti uz neplanuju sem spawning !
                    for(int i = 0; i < agent; ++i)
                    {
                        //do tejto podmienky vlezieme, ak sme nasli agenta ktory sa sem chce spawnovat
                        if(bodyInfos.get(i).position.equals(map.get(room).ID))
                        {
                            //tento spawnpoint je obsadeny
                            canSpawnHere = false;
                        }
                    }
                    //zistil som, ci je spawn point obsadeny
                    if(canSpawnHere)
                    {
                        bodyInfos.get(agent).position = map.get(room).ID;
                        System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " will spawn in room " + map.get(room).ID);
                        break; //uz nema vyznam dalej iterovat forcyklus, pretoze sme nasli spawn
                    }                        
                    else
                    {
                        //nemozem sem spawnovat, preto len vynulujem flag
                        canSpawnHere = true;
                    }
                }
            }
        }
        this.players = bodyInfos.size();
        System.out.println("All agents picked their spawn points ~");
    }
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, SimpleFPSAction> actionsToPerform) 
    {        
        System.out.println("--------------------------------------------------------------------------");
        
        Map<AgentBody, Double> rewards = new HashMap<AgentBody, Double>();
        SimpleFPSAction tempAction = new SimpleFPSAction();
        
        if(actionsToPerform.isEmpty())
        {
            //prostredie pred agentmi
            //inicializujeme - initSpawn
            initSpawnAgents();
                        
            rewards.put(bodyInfos.get(0).body, 0d);
            rewards.put(bodyInfos.get(1).body, 0d);
            
            return rewards;
        }
        
        //agenti maju poziadavky na nejaku akciu
        //musime pre kadeho agenta vyhodnotit jeho ziadost
        //ak je mozne ju naplnit, urobime tak, inak podame spravu, ze ziadosti nie je mozne vyhoviet a agentova akcia prepada
        //akcie je nutne usporiadat podla priority
        //1. SHOOT, 2. MOVE, 3. RESPAWN, 4. PICKUPITEM
        for(int agent = 0; agent < players; ++agent)
        {
            
            tempAction = actionsToPerform.get(bodyInfos.get(agent).body);
            //prv si pozrieme, ci agent medzicasom nahodou neumrel
            //ak ano, automaticky zahodime jeho poziadavku
            //agent si v dalsom kroku pozrie zdravie a poziada o respawn
            if(tempAction.whatToDo != ActionType.RESPAWN)
            {   //toto je poistka, aby nam system nezrusil uz mrtveho agenta so ziadostou o respawn
                
                if(bodyInfos.get(agent).health == ValueType.EMPTY)
                {
                    System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " is dead ! His request expires.");
                    rewards.put(bodyInfos.get(agent).body, 0.0);
                    //pripojime odmenu mrtveho agenta
                    System.out.println("");
                    continue; //pokracujem dalsim agentom - tento uz nic neurobi. urcite. lebo je mrtvy.
                }
            }
            
            
            //poistka proti opakovanej strelbe do dlhej chodby musi byt vypnuta
            //inak hrozi, ze zabrani v dalsej strelbe agentovi
            bodyInfos.get(agent).failedToEngage = false;
            
            switch(tempAction.whatToDo)
            {
                case SHOOT:
                    System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " is shooting !");
                    System.out.println("Target is: agent " + tempAction.shootOnID);
                    System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " grabs out " + tempAction.weapon.toString());
                    
                    boolean killConfirmed = performAgentShooting(agent, tempAction);
                    //teraz zistime, cez aku chodbu to strielame vlastne
                    if(killConfirmed)
                    {
                        rewards.put(bodyInfos.get(agent).body, 1.0);
                    }  
                    else rewards.put(bodyInfos.get(agent).body, 0.0);                    
                    System.out.println("");
                    break;
                case MOVE:                    
                    //tu zalezi, ci agent uz doteraz ziadal o MOVE
                    //to zistime z atributov stepsToGoal a stepsNeeded;
                    performAgentMove(agent, tempAction);
                                        
                    rewards.put(bodyInfos.get(agent).body, 0.0);  
                    System.out.println("");
                    break;
                    
                case RESPAWN:
                    System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " is performing a respawn ritual on his soul.");
                    respawn(bodyInfos.get(agent).body.getId());
                    System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " have got a new body and can continue in the game.");
                    bodyInfos.get(agent).health = ValueType.HIGH;
                    bodyInfos.get(agent).stepsNeeded = 0;
                    bodyInfos.get(agent).stepsToGoal = 0;
                    bodyInfos.get(agent).travelingTo = "";
                    bodyInfos.get(agent).failedToEngage = false;                    
                    
                    rewards.put(bodyInfos.get(agent).body, 0.0);
                    System.out.println("");
                    break;
                case PICKUPITEM:
                    System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " is picking up an item.");
                    System.out.println("It is a " + tempAction.weapon);
                    pickUpItem(tempAction.weapon, bodyInfos.get(agent).body.getId(), bodyInfos.get(agent).position);
                    rewards.put(bodyInfos.get(agent).body, 0.0);
                    System.out.println("");
                    break;
                default:
                    System.out.println("Agent has an unknown request !");
                    System.out.println("");
                    break;
            }                        
        }                                                         
        
        return rewards;        
    }

    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) 
    {
        if(type != SimpleFPSAgentType.getInstance())
        {
            throw new AgentInstantiationException("Illegal agent type");
        }
        AgentBody newBody = new AgentBody(bodyInfos.size() /*nove id v rade*/, type);
        bodyInfos.add(new SimpleFPSBodyInfo(newBody));                
        
        return newBody;
    }

    public String getLoggableRepresentation() 
    {
        return "Default";
    }
    
    
    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() 
    {
        return Collections.singletonMap(SimpleFPSAgentType.getInstance(), new AgentInstantiationDescriptor(minPlayers, maxPlayers));
    }
    
    private static class SimpleFPSBodyInfo 
    {
        /**
         * Body, ktere reprezentuji.
         */
        AgentBody body;
        
        ValueType speed;
        ValueType ammo1;
        ValueType ammo2;
        ValueType ammo3;
        ValueType health; 
        String position;
        String travelingTo;
        boolean failedToEngage;
        
        int stepsToGoal;
        //indikuje, kolko tahov je nutne urobit pre nejaky goal
        //napriklad, agent s rychlostou LOW sa chce presunut cez chodbu MED
        //LOW < MED => stepsToGoal = 2 (LOW + LOW = MED)
        //ak by bola chodba HIGH, tak stepsToGoal = 4 (4*LOW = HIGH)
        int stepsNeeded;
        //tato premena uchova povodnu hodnotu
        //napriklad speed LOW a chodba MED = 2 steps
        //stepsToGoal indikuje, za kolko krokov tam budem
        //tato indikuje, kolko mi to celkovo trva
        //to preto, lebo v polke prehodim position na cielovu miestnost
        //a do polky som akoby v doterajsej miestnosti
        //potrebujem si teda nejako overit, ci uz nie som za polovickou
        //to overenie urobim porovnanim s touto hodnotou

        public SimpleFPSBodyInfo(AgentBody body) 
        {
            Random rand = new Random();
            int number = rand.nextInt((3 - 1) + 1) + 1;
            switch(number)
            {
                case 1:
                    this.speed = ValueType.LOW;
                    break;
                case 2:
                    this.speed = ValueType.MED;
                    break;
                case 3:
                    this.speed = ValueType.HIGH;
                    break;
                default:
                    this.speed = ValueType.MED;
                    break;
            }            
            this.body = body;
            this.ammo1 = ValueType.EMPTY;
            this.ammo2 = ValueType.EMPTY;
            this.ammo3 = ValueType.EMPTY;
            this.health = ValueType.HIGH;
            this.position = "";
            this.stepsToGoal = 0;
            this.stepsNeeded = 0;
            this.travelingTo = "";
            this.failedToEngage = false;
        }
        
        void setPosition(String pos)
        {
            this.position = pos;
        }
        
        String getPosition()
        {
            return this.position;
        }
    }
    
    public void pickUpItem(ItemType item, int agentID, String agentPos)
    {
        //prv doplnime potrebne parametre podla predmetu
        switch(item)
        {
            case WEAPON_1:
                bodyInfos.get(agentID).ammo1 = ValueType.HIGH;
                break;
            case WEAPON_2:
                bodyInfos.get(agentID).ammo2 = ValueType.HIGH;
                break;
            case WEAPON_3:
                bodyInfos.get(agentID).ammo3 = ValueType.HIGH;
                break;
            case MEDIKIT:                
                switch(medikit.restoresHP)
                {
                    case HIGH:
                        bodyInfos.get(agentID).health = ValueType.HIGH;
                        break;
                    case MED:
                        bodyInfos.get(agentID).health = ValueType.MED;
                        break;                        
                    case LOW:
                        bodyInfos.get(agentID).health = ValueType.LOW;
                        break;
                }                                
                break;
        }
        //potom zistime, aku vec agent zobral
        //a danemu uzlu v mape nastavime ItemType.NOTHING
        for(int room = 0; room < map.size(); ++room)
        {
            if(map.get(room).ID.equalsIgnoreCase(agentPos))
            {
                //nasli sme, kde je nas agent
                //map.get(room).item = ItemType.NOTHING;
                //vynulujeme obsah izby
                break; //nema vyznam dalej iterovat, co sme chceli je hotove
            }
        }
    }

    private void respawn(int agentID)
    {
        for(int room = 0; room < map.size(); ++room)
        {
            boolean canSpawnHere = true;

            //pozrieme, ci je room spawnable
            if(map.get(room).spawnable)
            {
                //toto ale nestaci !
                //musime pozriet, ci predosli agenti uz neplanuju sem spawning !
                for(int i = 0; i < agentID; ++i)
                {
                    //do tejto podmienky vlezieme, ak sme nasli agenta ktory sa sem chce spawnovat
                    if(bodyInfos.get(i).position.equals(map.get(room).ID))
                    {
                        //tento spawnpoint je obsadeny
                        canSpawnHere = false;
                    }
                }
                //zistil som, ci je spawn point obsadeny
                if(canSpawnHere)
                {
                    bodyInfos.get(agentID).position = map.get(room).ID;
                    System.out.println("Agent " + bodyInfos.get(agentID).body.getId() + " will respawn in room " + map.get(room).ID);
                    break; //uz nema vyznam dalej iterovat forcyklus, pretoze sme nasli spawn
                }                        
                else
                {
                    //nemozem sem spawnovat, preto len vynulujem flag
                    canSpawnHere = true;
                }
            }
        }
    }
    
    private void performAgentMove(int agent, SimpleFPSAction tempAction)
    {
        if(bodyInfos.get(agent).travelingTo.isEmpty())
        {
            //sem sa dostanem ak nebol vykonany pohyb
            System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " is moving to new location.");

            bodyInfos.get(agent).travelingTo = tempAction.moveTo;
            for(int room = 0; room < map.size(); ++room)
            {                           
                if(map.get(room).ID.equalsIgnoreCase(bodyInfos.get(agent).position))
                {
                    for(int j = 0; j < map.get(room).passages.size(); ++j)
                    {
                        if(map.get(room).passages.get(j).toNode.equalsIgnoreCase(tempAction.moveTo))
                        {
                            String passageLength = map.get(room).passages.get(j).length;
                            String agentSpeed = bodyInfos.get(agent).speed.toString();

                            int l = converter(passageLength);
                            int s = converter(agentSpeed);
                            bodyInfos.get(agent).stepsNeeded = l/s;
                            bodyInfos.get(agent).stepsToGoal = l/s;
                            if(bodyInfos.get(agent).stepsNeeded == 0)
                            {
                                bodyInfos.get(agent).stepsNeeded = 1;
                                bodyInfos.get(agent).stepsToGoal = 1;
                                //osetrenie pre pripady, ak je chodba kratka a agent rychly (podelenie da nulu)
                            }
                        }
                    }
                }
            }
        }
        else
        {
            //sem sa dostanem, ak v travelingTo je nejaka hodnota - niekam mam uz namierene

            if(bodyInfos.get(agent).stepsToGoal == 0)
            {
                //uz sme dorazili
                System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " reached his planed location.");
                bodyInfos.get(agent).stepsNeeded = 0;
                bodyInfos.get(agent).stepsToGoal = 0;
                bodyInfos.get(agent).travelingTo = "";
                //poziciu uz mame zmenenu, lebo sme ju zmenili uz v polke chodby
            }
            else
            {
                System.out.println("Agent " + bodyInfos.get(agent).body.getId() + " makes another step to reach new location.");
                --bodyInfos.get(agent).stepsToGoal;
                if(!bodyInfos.get(agent).position.equalsIgnoreCase(tempAction.moveTo))
                {
                    if(bodyInfos.get(agent).stepsToGoal == 0)
                    {
                        //velmi specialni pripad -_-
                        bodyInfos.get(agent).position = tempAction.moveTo;
                    }
                    else
                    //ma vyznam pozerat, len ak sme uz poziciu predtym neprehodili
                    if( (bodyInfos.get(agent).stepsNeeded / bodyInfos.get(agent).stepsToGoal) > 2 )
                    {
                       //menime po dostatocnej vzdialenosti - zhruba v polovici chodby 
                        bodyInfos.get(agent).position = tempAction.moveTo;
                    }
                }
            }

        }
    }
    
    private boolean performAgentShooting(int agent, SimpleFPSAction tempAction)
    {        
        ValueType value = null;
        int weaponRange = 0;
        int passageLength = 0;
        ItemType agentsWeapon = tempAction.weapon;
        switch(agentsWeapon)
        {
            case WEAPON_1:
                value = weapons.get(0).range;
                weaponRange = converter(value.toString());
                break;
            case WEAPON_2:
                value = weapons.get(1).range;
                weaponRange = converter(value.toString());
                break;
            case WEAPON_3:
                value = weapons.get(2).range;
                weaponRange = converter(value.toString());
                break;
            default:
                System.out.println("Unknown superweapon detected. Cheating is not tolerated.");
                break;
        }
        String fromNode = getAgentInfo(agent, "position");
        String toNodeID = getAgentInfo(tempAction.shootOnID, "position");
        if(fromNode.equalsIgnoreCase(toNodeID))
        {
            //v jednej miestnosti sme, nezaujima nas dlzka chodby
            passageLength = 0;
        }
        else
        {
            for(int room = 0; room < map.size(); ++room)
            {
                if(map.get(room).ID.equalsIgnoreCase(fromNode))
                {
                    for(int pass = 0; pass < map.get(room).passages.size(); ++pass)
                    {
                        if(map.get(room).passages.get(pass).toNode.equalsIgnoreCase(toNodeID))
                        {
                            String lenght = map.get(room).passages.get(pass).length;
                            passageLength = converter(lenght);
                        }
                    }
                }
            }
        }
        //mame dosah pouzitej zbrane aj dlzku chodby
        //teraz pozrieme, ci nahodou nestrielame zbranou s malym dosahom do dlhej chodby
        if(weaponRange < passageLength)
        {
            System.out.println("Agent " + agent + ": Target is too far for this weapon. Can't engage.");
            bodyInfos.get(agent).failedToEngage = true;
            return false;
        }
        else
        {
            bodyInfos.get(agent).failedToEngage = false;
            //zbran ma dostatocny dosah, preto poistku vynulujeme
            
            int passageFactor = 100 - passageLength;                        
            int weaponAccuracy = 0;
            agentsWeapon = tempAction.weapon;
            switch(agentsWeapon)
            {
                case WEAPON_1:
                    value = weapons.get(0).accuracy;
                    weaponAccuracy = converter(value.toString());
                    break;
                case WEAPON_2:
                    value = weapons.get(1).accuracy;
                    weaponAccuracy = converter(value.toString());
                    break;
                case WEAPON_3:
                    value = weapons.get(2).accuracy;
                    weaponAccuracy = converter(value.toString());
                    break;
                default:
                    System.out.println("Unknown superweapon detected. Cheating is not tolerated.");
                    break;
            }
            int chanceToHit = (weaponAccuracy + passageFactor)/2;
            
            boolean contact = shoot(agent, chanceToHit, tempAction);
            
            return contact;
            
        }
        
        //return false; //no kill
    }
    
    private boolean shoot(int agent, int chanceToHit, SimpleFPSAction tempAction)
    {
        Random rand = new Random();        
        int number = rand.nextInt((100 - 0) + 1);
        if(number > chanceToHit)
        {
            System.out.println("Agent " + agent + ": Shot failed to connect !");
            
            ItemType agentsWeapon = tempAction.weapon;                                 
            ValueType availableAmmo = null;
            
            switch(agentsWeapon)
            {
                case WEAPON_1:                    
                    availableAmmo = bodyInfos.get(agent).ammo1;
                    break;
                case WEAPON_2:                   
                    availableAmmo = bodyInfos.get(agent).ammo2;
                    break;
                case WEAPON_3:                   
                    availableAmmo = bodyInfos.get(agent).ammo3;
                    break;
                default:
                    System.out.println("Unknown superweapon detected. Cheating is not tolerated.");
                    break;
            }
            
            reloadWeapon(tempAction.weapon, agent, availableAmmo);
            return false;
        }
        else
        {
            //trafili sme nepriatela
            ItemType agentsWeapon = tempAction.weapon;
            ValueType weaponDamage = null;            
            ValueType targetHealth = null;
            ValueType availableAmmo = null;
            
            switch(agentsWeapon)
            {
                case WEAPON_1:
                    weaponDamage = weapons.get(0).damage;
                    targetHealth = bodyInfos.get(tempAction.shootOnID).health;
                    availableAmmo = bodyInfos.get(agent).ammo1;
                    break;
                case WEAPON_2:
                    weaponDamage = weapons.get(1).damage;
                    targetHealth = bodyInfos.get(tempAction.shootOnID).health;
                    availableAmmo = bodyInfos.get(agent).ammo2;
                    break;
                case WEAPON_3:
                    weaponDamage = weapons.get(2).damage;
                    targetHealth = bodyInfos.get(tempAction.shootOnID).health;
                    availableAmmo = bodyInfos.get(agent).ammo3;
                    break;
                default:
                    System.out.println("Unknown superweapon detected. Cheating is not tolerated.");
                    break;
            }
            
            //teraz musime podla zdravia a dmg nastavit udaje (naboje v agentovi, zdravie v nepriatelovi)
            //zacneme nabojmi
            reloadWeapon(agentsWeapon, agent, availableAmmo);                      
            //naboje nastavene
            
            //postrelime podla dmg nepriatela
            boolean killed = performInjury(weaponDamage, targetHealth, tempAction, agent);
            
            return killed;
        }                
    }
    
    private boolean performInjury(ValueType weaponDamage, ValueType targetHealth, SimpleFPSAction tempAction, int agent)
    {
        boolean killed = false;
        
        switch(weaponDamage)
        {
            case HIGH:
                switch(targetHealth)
                {
                    case HIGH:                                                         
                    case MED:                            
                    case LOW:
                        bodyInfos.get(tempAction.shootOnID).health = ValueType.EMPTY;
                        killed = true; //som zabil !
                        System.out.println("Agent " + agent + ": Target eliminated !");
                        //zbran ma vysoky dmg - zabije na jednu ranu (shotgun)
                        break;
                }

                break;
            case MED:
                switch(targetHealth)
                {
                    case HIGH:
                        bodyInfos.get(tempAction.shootOnID).health = ValueType.LOW;
                        System.out.println("Agent " + agent + ": Target criticaly injured !");
                        break;
                    case MED:                             
                    case LOW:
                        bodyInfos.get(tempAction.shootOnID).health = ValueType.EMPTY; 
                        System.out.println("Agent " + agent + ": Target eliminated !");
                        killed = true; //som zabil !
                        break;
                }
                break;
            case LOW:
                switch(targetHealth)
                {
                    case HIGH:
                        bodyInfos.get(tempAction.shootOnID).health = ValueType.MED;  
                        System.out.println("Agent " + agent + ": Target injured !");
                        break;
                    case MED: 
                        bodyInfos.get(tempAction.shootOnID).health = ValueType.LOW;  
                        System.out.println("Agent " + agent + ": Target criticaly injured !");
                        break;
                    case LOW:
                        bodyInfos.get(tempAction.shootOnID).health = ValueType.EMPTY;
                        System.out.println("Agent " + agent + ": Target eliminated !");
                        killed = true; //som zabil !
                        break;
                }
                break;
        }
        
        return killed;
    }
    
    private void reloadWeapon(ItemType agentsWeapon, int agent, ValueType availableAmmo)
    {
        switch(availableAmmo)
        {
            case HIGH:
                switch(agentsWeapon)
                {
                    case WEAPON_1:
                        bodyInfos.get(agent).ammo1 = ValueType.MED;
                        break;
                    case WEAPON_2:
                        bodyInfos.get(agent).ammo2 = ValueType.MED;
                        break;
                    case WEAPON_3:
                        bodyInfos.get(agent).ammo3 = ValueType.MED;
                        break;
                    default:                            
                        break;
                }

                break;
            case MED:
                switch(agentsWeapon)
                {
                    case WEAPON_1:
                        bodyInfos.get(agent).ammo1 = ValueType.LOW;
                        break;
                    case WEAPON_2:
                        bodyInfos.get(agent).ammo2 = ValueType.LOW;
                        break;
                    case WEAPON_3:
                        bodyInfos.get(agent).ammo3 = ValueType.LOW;
                        break;
                    default:                            
                        break;
                }
                break;
            case LOW:
                switch(agentsWeapon)
                {
                    case WEAPON_1:
                        bodyInfos.get(agent).ammo1 = ValueType.EMPTY;
                        break;
                    case WEAPON_2:
                        bodyInfos.get(agent).ammo2 = ValueType.EMPTY;
                        break;
                    case WEAPON_3:
                        bodyInfos.get(agent).ammo3 = ValueType.EMPTY;
                        break;
                    default:                            
                        break;
                }
                break;
            default:
                break;                        
        }
    }

    public String getAgentInfo(int agentID, String aspect)
    {
        if(aspect.equalsIgnoreCase("ammo1"))
        {
            return bodyInfos.get(agentID).ammo1.toString();
        }
        else
        if(aspect.equalsIgnoreCase("ammo2"))
        {
            return bodyInfos.get(agentID).ammo2.toString();
        }
        else
        if(aspect.equalsIgnoreCase("ammo3"))
        {
            return bodyInfos.get(agentID).ammo3.toString();
        }
        else
        if(aspect.equalsIgnoreCase("health"))
        {
            return bodyInfos.get(agentID).health.toString();
        }
        else
        if(aspect.equalsIgnoreCase("position"))
        {
            return bodyInfos.get(agentID).position;
        }
        else
        if(aspect.equalsIgnoreCase("speed"))
        {
            return bodyInfos.get(agentID).speed.toString();
        }
        else
        if(aspect.equalsIgnoreCase("stepsNeeded"))
        {
            return Integer.toString(bodyInfos.get(agentID).stepsNeeded);
        }
        else
        if(aspect.equalsIgnoreCase("stepsToGoal"))
        {
            return Integer.toString(bodyInfos.get(agentID).stepsToGoal);
        }
        else
        if(aspect.equalsIgnoreCase("travel"))
        {
            return bodyInfos.get(agentID).travelingTo;
        }
        else
        if(aspect.equalsIgnoreCase("failed"))
        {
            if(bodyInfos.get(agentID).failedToEngage)
                return "failed";
            else return "nofailed";            
        }
        
        return "Unknown input !";
    }
    
    public int converter(String toConv)
    {
        int res = 0;
        
        if(toConv.equalsIgnoreCase("HIGH"))
        {
           res = 100;
           return res;
        }
        if(toConv.equalsIgnoreCase("MED"))
        {
           res = 50;
           return res;
        }
        if(toConv.equalsIgnoreCase("LOW"))
        {
           res = 25;
           return res;
        }
        if(toConv.equalsIgnoreCase("EMPTY"))
        {
           res = 0;
           return res;
        }
        
        return res;
    }       
    
}


