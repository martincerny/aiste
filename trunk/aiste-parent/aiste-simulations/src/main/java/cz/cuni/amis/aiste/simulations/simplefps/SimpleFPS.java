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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    String damage;
    String accuracy;
    String range;
    String ID;
    
    Weapon(String dmg, String acc, String ran, String ID)
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
    String restoresHP;
    
    Medikit(String rest)
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
        System.out.println("Creating weapons and medikit.");
        weapons = new ArrayList<Weapon>();
        
        Weapon weapon1 = new Weapon("LOW", "HIGH", "MED", "w1"); //pistol                        
        Weapon weapon2 = new Weapon("HIGH", "LOW", "LOW", "w2"); //shotgun
        Weapon weapon3 = new Weapon("HIGH", "LOW", "HIGH", "w3"); //bazooka
        medikit = new Medikit("HIGH"); //restores full health (health := HIGH)    
        weapons.add(weapon1);
        weapons.add(weapon2);
        weapons.add(weapon3);                       
        System.out.println("Items succesfully created.");
        System.out.println("");          
                        
         bodyInfos = new ArrayList<SimpleFPSBodyInfo>();                                   
         createAgentBodies();
        //v tomto bode mame vyrobene tela pre hracov/agentov                                                                         
        //este im nastavime pozicie podla mapy (spawnable roomz)
        initSpawnAgents();
         
        
        System.out.println("SAFE POINT REACHED !");
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
        //mam naprskanu mapu - poslem vystup ako vyzera
        for(int node = 0; node < parsedMap.size(); ++node)
        {
            System.out.println("Node " + parsedMap.get(node).ID + " looks like this:");
            System.out.println("There is " + parsedMap.get(node).item.name() + " in this room.");
            if(parsedMap.get(node).spawnable)
            {
                System.out.println("This room can be used as a spawn point.");
            }
            System.out.println("There are " + parsedMap.get(node).passages.size() + " passages from this room.");
            for(int pass = 0; pass < parsedMap.get(node).passages.size(); ++pass)
            {
                System.out.println("Passage number " + pass + " leads to room " + parsedMap.get(node).passages.get(pass).toNode );
                System.out.println("This passage has " + parsedMap.get(node).passages.get(pass).length + " length.");
            }
        }
        System.out.println("Simple map review terminated ~");
    }
    
    void createAgentBodies()
    {
        int number = rand.nextInt((maxPlayers - minPlayers) + 1) + minPlayers;  
        players = number;
        System.out.println("Creating agent bodies now ~");
         for(int i = 0; i < number; ++i)
         {                          
             AgentBody tempBody = createAgentBodyInternal(SimpleFPSAgentType.getInstance());
             System.out.println("Agent " + tempBody.getId() + " was succesfully created.");
         }
         System.out.println("Agent bodies created ~");
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
        System.out.println("All agents picked their spawn points ~");
    }
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, SimpleFPSAction> actionsToPerform) 
    {
        
        // informace o prostredi dostanu od agenta takto:
        //bodyInfos.get(body.getId());        
        
        //krok simulace vraci reward, ktery dostali agenti za provedene akce
        //v nasem pripade je reward +1 za zabiti oponenta, jinak 0 (tj. klasicky frag count)
        throw new UnsupportedOperationException("Not supported yet.");
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
        
        String speed;
        String ammo1;
        String ammo2;
        String ammo3;
        String health; 
        String position;
        
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
                    this.speed = "LOW";
                    break;
                case 2:
                    this.speed = "MED";
                    break;
                case 3:
                    this.speed = "HIGH";
                    break;
                default:
                    this.speed = "MED";
                    break;
            }            
            this.body = body;
            this.ammo1 = "EMPTY";
            this.ammo2 = "EMPTY";
            this.ammo3 = "EMPTY";
            this.health = "FULL";
            this.position = "";
            this.stepsToGoal = 0;
            this.stepsNeeded = 0;
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
    
    public void pickUpItem(ItemType item, int agentID)
    {
        switch(item)
        {
            case WEAPON_1:
                bodyInfos.get(agentID).ammo1 = "HIGH";
                break;
            case WEAPON_2:
                bodyInfos.get(agentID).ammo2 = "HIGH";
                break;
            case WEAPON_3:
                bodyInfos.get(agentID).ammo3 = "HIGH";
                break;
            case MEDIKIT:
                
                if(medikit.restoresHP.equalsIgnoreCase("HIGH"))
                {                    
                    bodyInfos.get(agentID).health = "HIGH";
                }
                else 
                if(medikit.restoresHP.equalsIgnoreCase("MED"))
                {
                    bodyInfos.get(agentID).health = "MED";   
                }
                else
                if(medikit.restoresHP.equalsIgnoreCase("LOW"))
                {
                    bodyInfos.get(agentID).health = "LOW"; 
                }
                break;
        }
    }

    public void respawn(int agentID)
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
    
    public void moveAgent(int agentID)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getAgentInfo(int agentID, String aspect)
    {
        if(aspect.equalsIgnoreCase("ammo1"))
        {
            return bodyInfos.get(agentID).ammo1;
        }
        else
        if(aspect.equalsIgnoreCase("ammo2"))
        {
            return bodyInfos.get(agentID).ammo2;
        }
        else
        if(aspect.equalsIgnoreCase("ammo3"))
        {
            return bodyInfos.get(agentID).ammo3;
        }
        else
        if(aspect.equalsIgnoreCase("health"))
        {
            return bodyInfos.get(agentID).health;
        }
        else
        if(aspect.equalsIgnoreCase("position"))
        {
            return bodyInfos.get(agentID).position;
        }
        else
        if(aspect.equalsIgnoreCase("speed"))
        {
            return bodyInfos.get(agentID).speed;
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
        
        return "Unknown input !";
    }
    
}


