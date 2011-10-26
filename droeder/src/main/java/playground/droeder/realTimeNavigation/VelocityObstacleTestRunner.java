/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.droeder.realTimeNavigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.droeder.DRPaths;
import playground.droeder.Vector2D;
import playground.droeder.gis.DaShapeWriter;
import playground.droeder.realTimeNavigation.movingObjects.MovingAgentImpl;
import playground.droeder.realTimeNavigation.movingObjects.MovingObject;
import playground.droeder.realTimeNavigation.velocityObstacles.ReciprocalVelocityObstacleImpl;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;
import playground.gregor.sim2d_v2.events.XYVxVyEventImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class VelocityObstacleTestRunner {
	//Directory
	final static String DIR = DRPaths.VSP + "2D_Sim/";
	//parameters for run
	// (FACTORFORQUADTREE * MAXSPEED [m/s] * stepSize [s]) is distance in which the quadTree is looking for objects to build the VO 
	private static final double FACTORFORQUADTREE = 10.0;
	private static final int NUMOFOAGENTS = 10;
	private static final double MAXSPEED = 1.0;
	private static final double AGENTDIAMETER = 0.7;
	// ###############
	final static String OUTDIR = DIR + "output/" + "agents" + String.valueOf(NUMOFOAGENTS) + "_quadTreeR" + String.valueOf(FACTORFORQUADTREE) + "/";
	
	
	Map<Id, MovingObject> objects;
	private QuadTree<MovingObject> quadTree = null;
	private double minX;
	private double maxY;
	private double minY;
	private double maxX;
	private Collection<Id> finished;
	private boolean first = true;
	private int cnt = 0;
	private int nxtmsg = 1;
	private HashMap<String, Coord> agentPosition;
	private HashMap<String, SortedMap<String, String>> agentPositionAttrib;
	private HashMap<String, SortedMap<Integer, Coord>> vo;
	private HashMap<String, SortedMap<String, String>> voAttrib;
	private HashMap<String, List<Coord>> agentWay;
	private EventsManager manager;
	private String dir;
	private double time;
	
	private static final Logger log = Logger
			.getLogger(VelocityObstacleTestRunner.class);
	
	public static void main(String[] args){
		EventsManager manager = EventsUtils.createEventsManager();
		VelocityObstacleTestRunner runner = new VelocityObstacleTestRunner(manager, OUTDIR, 0.0);
		EventWriter writer = new EventWriterXML(OUTDIR + "events.xml.gz");
		manager.addHandler(writer);
		
		//create agentGeometry
		int numOfParts = 18;
		double agentRadius = AGENTDIAMETER / 2;
		
		Coordinate[] c = new Coordinate[numOfParts + 1];
		double angle = Math.PI * 2 / numOfParts;
		for(int i = 0; i <numOfParts; i++){
			c[i] = new Coordinate(agentRadius * Math.cos(angle * i), agentRadius * Math.sin(angle * i));
		}
		c[18] = c[0];
		Geometry g = new GeometryFactory().createLinearRing(c);

		// create Agents
		int numOfAgents = NUMOFOAGENTS;
		double agentDistributionRadius = 20;
		angle = Math.PI /numOfAgents;
		
		Vector2D position, goal;
		double maxSpeed = MAXSPEED;
		Id id;
		for(int i = 0 ; i < numOfAgents; i++){
			id = new IdImpl(i);
			position = new Vector2D(agentDistributionRadius * Math.cos(angle * i), agentDistributionRadius * Math.sin(angle * i));
			goal = new Vector2D(-1, position);
			runner.createAndAddMovingAgent(id, position, goal, maxSpeed, g);
		}
		
		runner.run(1, Math.pow(2, 12));
		runner.dump();
		writer.closeFile();
	}
	

	public VelocityObstacleTestRunner(EventsManager manager, String outDir, double startTime){
		this.manager = manager;
		this.objects = new HashMap<Id, MovingObject>();
		this.finished = new HashSet<Id>();
		this.dir = outDir;
		this.time = startTime;
		if(!new File(outDir).exists()){
			new File(outDir).mkdirs();
			log.info("Output-Directory " + outDir + " not found! Created: " + outDir +" ...");
		}
		this.initQuad();
	}
	
	public boolean createAndAddMovingAgent(Id id, Vector2D start, Vector2D goal, double maxSpeed, Geometry g){
		if(this.objects.containsKey(id)){
			log.error("can not create Agent: " + id.toString() + ". Id exists already!");
			return false;
		}
		this.objects.put(id, new MovingAgentImpl(start, goal, maxSpeed, id, g));
		this.manager.processEvent(new XYVxVyEventImpl(id, start.getX(), start.getY(), 
				this.objects.get(id).getSpeed().getX(), this.objects.get(id).getSpeed().getY(), this.time));
		findValuesForQuad(start);
		return true;
	}
	
	/**
	 * @param start
	 */
	private void findValuesForQuad(Vector2D start) {
		if(start.getX() < this.minX){
			minX = start.getX();
		}
		if(start.getX() > this.maxX){
			maxX = start.getX();
		}
		if(start.getY() < this.minY){
			this.minY = start.getY();
		}
		if(start.getY() > this.maxY){
			this.maxY = start.getY();
		}
	}

	public Map<Id, MovingObject> getObjects(){
		return this.objects;
	}
	
	
	public void run(double timeStep, double breakAfterSimStep){
		while(!this.finished()){
			this.doStep(timeStep);
			if(this.getStepCnt() > breakAfterSimStep){
				StringBuffer b = new StringBuffer();
				for(Id id: this.objects.keySet()){
					b.append(id.toString() + ", ");
					System.out.println("arrived?: " + objects.get(id).arrived() + " current: " + objects.get(id).getCurrentPosition().toString() + "\tgoal:" +  ((MovingAgentImpl) objects.get(id)).getGoal());
				}
				log.info("stopped run after " + this.getStepCnt() + "... Agents with id: " + b.toString() + " did not arrive at their goal...");
				break;
			}
		}
	}
	
	/**
	 * 
	 * @param timeStep in seconds
	 */
	public void doStep(double timeStep){
		if(quadTree == null){
			buildQuadTree();
		}
		Map<Id, Vector2D> desiredVelocity = selectDesiredVelocity();
		Map<Id, VelocityObstacle> vo= createVelocityObstacles(timeStep, desiredVelocity);
		walk(timeStep, vo);
	}

	/**
	 * 
	 */
	private void buildQuadTree() {
		this.quadTree = new QuadTree<MovingObject>(minX, minY, maxX, maxY);
		for(MovingObject o: objects.values()){
			this.quadTree.put(o.getCurrentPosition().getX(), o.getCurrentPosition().getY(), o);
		}
	}

	/**
	 * @return 
	 * 
	 */
	private Map<Id, Vector2D> selectDesiredVelocity() {
		Map<Id, Vector2D> speed = new HashMap<Id, Vector2D>();
		for(MovingObject o: this.objects.values()){
			Vector2D s;
			if(o instanceof MovingAgentImpl){
				s = ((MovingAgentImpl) o).getGoal().subtract(o.getCurrentPosition()).getUnitVector().addFactor(((MovingAgentImpl) o).getMaxSpeed());
			}else{
				s = o.getSpeed();
			}
			speed.put(o.getId(), s);
		}
		return speed;
	}

	/**
	 * @param desiredVelocity 
	 * @param stepSize 
	 * @return 
	 * 
	 */
	private Map<Id, VelocityObstacle> createVelocityObstacles(double stepSize, Map<Id, Vector2D> desiredVelocity) {
		Map<Id, VelocityObstacle> vos = new HashMap<Id, VelocityObstacle>();
		
		for(MovingObject o: this.objects.values()){
			VelocityObstacle vo = new ReciprocalVelocityObstacleImpl(o, this.quadTree.get(o.getCurrentPosition().getX(), 
					o.getCurrentPosition().getY(), VelocityObstacleTestRunner.FACTORFORQUADTREE * desiredVelocity.get(o.getId()).absolut()*stepSize));
			vos.put(o.getId(), vo);
			
		}
		return vos;
	}

	/**
	 * @param vo 
	 * @param timeStep 
	 * 
	 */
	private void walk(double timeStep, Map<Id, VelocityObstacle> vo) {
		// TODO find a better way using quadtree. actually this quadtree throws an stackoverflowexception if it is not initialised in every timestep
		this.initQuad();
		// store positions
		prepareForDump(vo);
		//increase time
		this.time += timeStep;
		// Agents walk
		for(MovingObject o: this.objects.values()){
			o.calculateNextStep(timeStep, vo.get(o.getId()));
			o.processNextStep();
			this.manager.processEvent(new XYVxVyEventImpl(o.getId(), o.getCurrentPosition().getX(), 
					o.getCurrentPosition().getY(), o.getSpeed().getX(), o.getSpeed().getY(), this.time));
			// TODO see above - problem with the quadtree
			this.findValuesForQuad(o.getCurrentPosition());
			if(o.arrived()){
				this.finishAgent(o);
			}
		}
		// remove finished from Map
		for(Id id : this.finished){
			if(this.objects.containsKey(id)){
				this.objects.remove(id);
			}
		}
		
		cnt++;
		if(cnt%nxtmsg == 0){
			log.info("processed " + cnt + " steps... " + this.getNumOfActiveAgents() + " agents still walk...");
			nxtmsg *= 2;
		}
	}
	
	private void finishAgent(MovingObject o){
		this.finished.add(o.getId());
		String id = o.getId().toString() + "_" + String.valueOf(cnt + 1);
		this.agentPosition.put(id, o.getCurrentPosition().getCoord());
		this.agentWay.get(o.getId().toString()).add(o.getCurrentPosition().getCoord());
		
		this.agentPositionAttrib.put(id, new TreeMap<String, String>());
		this.agentPositionAttrib.get(id).put("agent", o.getId().toString());
		this.agentPositionAttrib.get(id).put("step", String.valueOf(this.cnt + 1));
		this.agentPositionAttrib.get(id).put("time", String.valueOf(this.time));
	}
	
	/**
	 * @return
	 */
	private String getNumOfActiveAgents() {
		return String.valueOf(this.objects.size());
	}

	/**
	 * @return
	 */
	private boolean finished() {
		if(this.objects.isEmpty()){
			return true;
		}
		return false;
	}

	private void initQuad(){
		this.quadTree = null;
		minX = Double.MAX_VALUE;
		maxY = Double.MIN_VALUE;
		minY = Double.MAX_VALUE;
		maxX = Double.MIN_VALUE;
	}

	/**
	 * @param vo
	 */
	private void prepareForDump(Map<Id, VelocityObstacle> vo) {
		//init
		if(first){
			this.agentWay = new HashMap<String, List<Coord>>();
			this.agentPosition = new HashMap<String, Coord>();
			this.agentPositionAttrib = new HashMap<String, SortedMap<String, String>>();
			this.vo = new HashMap<String, SortedMap<Integer, Coord>>();
			this.voAttrib = new HashMap<String, SortedMap<String, String>>();
			first = false;
		}
		// prepare objects
		String id;
		for(MovingObject o : this.objects.values()){
			if(!agentWay.containsKey(o.getId().toString())){
				agentWay.put(o.getId().toString(), new ArrayList<Coord>());
			}
			agentWay.get(o.getId().toString()).add(o.getCurrentPosition().getCoord());
			id = o.getId().toString() + "_" + String.valueOf(cnt);
			this.agentPosition.put(id, o.getCurrentPosition().getCoord());
			
			this.agentPositionAttrib.put(id, new TreeMap<String, String>());
			this.agentPositionAttrib.get(id).put("agent", o.getId().toString());
			this.agentPositionAttrib.get(id).put("step", String.valueOf(this.cnt));
			this.agentPositionAttrib.get(id).put("time", String.valueOf(this.time));
		}
		
		//prepare obstacles
		for(Entry<Id, VelocityObstacle> e: vo.entrySet()){
			id = e.getKey().toString() + "_" + String.valueOf(cnt);
			if(!(e.getValue().getGeometry() == null)){
				for(int i = 0; i < e.getValue().getGeometry().getNumGeometries(); i++){
					int ii = 0;
					SortedMap<Integer, Coord> coords =  new TreeMap<Integer, Coord>();
					for(Coordinate c: e.getValue().getGeometry().getCoordinates()){
						coords.put(ii, MGC.coordinate2Coord(c));
						ii++;
					}
					id = e.getKey().toString() + "_" + String.valueOf(cnt) + "_" + Integer.valueOf(i);
					this.vo.put(id, coords);
					this.voAttrib.put(id, new TreeMap<String, String>());
					this.voAttrib.get(id).put("agent", e.getKey().toString());
					this.voAttrib.get(id).put("step", String.valueOf(cnt));
					this.voAttrib.get(id).put("time", String.valueOf(this.time));
				}
			}
		}
	}
	/**
	 * dumps the stored agent- and obstacledata to the out-directory, specified in the constructor
	 */
	public void dump(){
		DaShapeWriter.writeDefaultLineString2Shape(dir + "vo-Agents_"+ NUMOFOAGENTS + "-" + "QuadtreeFaktor_" + FACTORFORQUADTREE + ".shp", "vo", this.vo, this.voAttrib);
		DaShapeWriter.writeDefaultPoints2Shape(dir + "agentPosition-Agents_"+ NUMOFOAGENTS + "-" + "QuadtreeFaktor_" + FACTORFORQUADTREE + ".shp", "agentPosition", this.agentPosition, this.agentPositionAttrib);
		DaShapeWriter.writeDefaultLineStrings2Shape(dir + "agentWay-Agents_"+ NUMOFOAGENTS + "-" + "QuadtreeFaktor_" + FACTORFORQUADTREE + ".shp", "agentWay", this.agentWay);
	}
	
	public int getStepCnt(){
		return this.cnt;
	}
}
