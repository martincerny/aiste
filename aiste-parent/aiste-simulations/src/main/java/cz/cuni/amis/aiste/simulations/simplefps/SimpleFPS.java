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
import cz.cuni.amis.aiste.environment.IStateVariable;
import cz.cuni.amis.aiste.environment.impl.AbstractStateVariableRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.impl.EnumStateVariable;
import cz.cuni.amis.aiste.environment.impl.SimpleAgentType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

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
    // bazooka (high dmg, med acc, high range)
    String damage;
    String accuracy;
    String range;
    String ID;
    
    void Weapon(String dmg, String acc, String ran, String ID)
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
    
    void Medikit(String rest)
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
    
    Passage(String toNode, String length)
    {
        this.toNode = toNode;
        this.length = length;
    }
}


public class SimpleFPS extends AbstractSynchronizedEnvironment<SimpleFPSAction> 
{

    private int minPlayers;
    private int maxPlayers;
    private String mapLocation;    
    private ArrayList<Node1> map;        
    
    /**
     * Informace o jednotlivych agentech. Indexem je id {@link AgentBody#id}
     */
    private List<SimpleFPSBodyInfo> bodyInfos;
    
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
        
        
        //Map<SimpleFPSAgentType, AgentInstantiationDescriptor> volaco = (Map<SimpleFPSAgentType, AgentInstantiationDescriptor>) getInstantiationDescriptors();
        
        
         bodyInfos = new ArrayList<SimpleFPSBodyInfo>(); 
         int number = rand.nextInt((maxPlayers - minPlayers) + 1) + minPlayers;
         for(int i = 0; i < number; ++i)
         {
             SimpleFPSAgentType agentType = new SimpleFPSAgentType();
             
             AgentBody telo = createAgentBodyInternal(agentType);
         }
        
        
        //potom z List musime rozbabrat na state values
        //tu rozoberieme list(y) na state values a ulozime ako kazu nizsie
        
        //TODO: prostredi se musi umet reprezentovat pomoci sady state variables, to jsou vlastne pary jmeno-hodnota
        //protoze prostredi je obecne, budes je muset mit v nejakych kolekcich, asi listech nebo mapach, jak se ti to bude hodit
        //promenne pro veci jako zdravi nebo naboje je potreba pro tuhle reprezentaci diskretizovat (napr. DEAD, LOW, INJURED, HEALTHY)
        //itemsAtLocations = new ArrayList<IStateVariable>();
        //EnumStateVariable newVariable = new EnumStateVariable("ItemAtLocation1", ItemType.class); //jsou tez IntegerStateVariable, pripadne si muzes udelat i vlastni typy
        //itemsAtLocations.add(newVariable);
        
        //promennou je potreba zaregistrovat u parent tridy
        //addStateVariable(newVariable);
        //a nastavit ji (a v prubehu simulace udrzovat) hodnotu
        //setStateVariableValue(newVariable, ItemType.WEAPON_1);       
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
    
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, SimpleFPSAction> actionsToPerform) 
    {
        
        // informace o prostredi dostanu od agenta takto:
        //bodyInfos.get(body.getId())
        
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
        
        //cokoliv dalsiho potrebujes, dopis sem (a smaz tento bordel :-)
        /*if(true)
        {            
            throw new UnsupportedOperationException("Not supported yet.");
        }*/
        
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
}
