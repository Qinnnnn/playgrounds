/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.lib.tools.network.obj.RectangularArea;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingChoice.api.ParkingSelectionManager;
import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.apiDefImpl.ShortestWalkingDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;

public class ParkingManagerZH {

	private ParkingCostCalculator parkingCostCalculator;
	//key: parkingId
	private HashMap<Id,Parking> parkingsHashMap=new HashMap<Id, Parking>();
	private Network network;

	public ParkingManagerZH(HashMap<String, HashSet<Id>> parkingTypes,ParkingCostCalculator parkingCostCalculator, LinkedList<Parking> parkings, Network network) {
		this.parkingCostCalculator = parkingCostCalculator;
		this.network = network;
		this.parkingTypes=parkingTypes;
		initializeQuadTree(parkings);
		addParkings(parkings);
		
			occupiedParking = new IntegerValueHashMap<Id>();
			parkingFacilitiesOnLinkMapping = new HashMap<Id, List<Id>>();

			for (Parking parking:parkings){
				Id linkId= ((NetworkImpl) network).getNearestLink(parking.getCoord()).getId();
				assignFacilityToLink(linkId, parking.getId());
						
				Id oppositeDirectionLinkId = getOppositeDirectionLinkId(linkId,network);
				if (oppositeDirectionLinkId!=null){
					assignFacilityToLink(oppositeDirectionLinkId, parking.getId());
				}
			}
			
			this.initialParkingFacilityOfAgent=new HashMap<Id, Parking>();
			this.fullPublicParkingFacilities=new HashSet<Parking>();
			
			privateParkingFacilityIdMapping=new TwoHashMapsConcatenated<Id, String, Id>();
			for (Parking parking:parkings){
				if (parking.getId().toString().contains("pp")){
					PrivateParking privateParking=(PrivateParking) parking;
					nonFullPublicParkingFacilities.remove(parking.getCoord().getX(), parking.getCoord().getY(), parking);
				
					privateParkingFacilityIdMapping.put(privateParking.getActInfo().getFacilityId(), privateParking.getActInfo().getActType(), parking.getId());
				}
			}
			
	}

	public HashMap<Id, Parking> getParkingsHashMap() {
		return parkingsHashMap;
	}

	public void addParkings(Collection<Parking> parkingCollection) {
		RectangularArea rectangularArea=new RectangularArea(new CoordImpl(nonFullPublicParkingFacilities.getMinEasting(),nonFullPublicParkingFacilities.getMinNorthing()), new CoordImpl(nonFullPublicParkingFacilities.getMaxEasting(),nonFullPublicParkingFacilities.getMaxNorthing()));
		
		for (Parking parking : parkingCollection) {
			
			if (rectangularArea.isInArea(parking.getCoord())){
				nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
				parkingsHashMap.put(parking.getId(), parking);
			} else {
				DebugLib.emptyFunctionForSettingBreakPoint();
				DebugLib.stopSystemAndReportInconsistency("only add points, which are inside defined area.");
			}
			
		}
		
	}

	private void initializeQuadTree(Collection<Parking> parkingColl) {
		EnclosingRectangle rect=new EnclosingRectangle();
		
		for (Parking parking:parkingColl){
			rect.registerCoord(parking.getCoord());
		}
		nonFullPublicParkingFacilities=(new QuadTreeInitializer<Parking>()).getQuadTree(rect);
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	protected QuadTree<Parking> nonFullPublicParkingFacilities;
	protected final HashSet<Parking> fullPublicParkingFacilities;
	private final Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId, List<FacilityId>>
	//private final Map<Id, Id> facilityToLinkMapping;	// <FacilityId, LinkId>
	private IntegerValueHashMap<Id> occupiedParking;	// number of reserved parkings
	private final HashMap<String, HashSet<Id>> parkingTypes;
	private HashMap<Id,Parking> initialParkingFacilityOfAgent;
	
	
	public void resetParkingFacilityForNewIteration(){
		for (Parking parking:fullPublicParkingFacilities){
			nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		}
		occupiedParking=new IntegerValueHashMap<Id>();
	}
	
	public void setInitialParkingFacilityOfAgent(HashMap<Id, Parking> initialParkingFacilityOfAgent) {
		this.initialParkingFacilityOfAgent = initialParkingFacilityOfAgent;
	}

	private void assignFacilityToLink(Id linkId, Id facilityId) {
		List<Id> list = parkingFacilitiesOnLinkMapping.get(linkId);
		if (list == null) {
			list = new ArrayList<Id>();
			parkingFacilitiesOnLinkMapping.put(linkId, list);
		}
		list.add(facilityId);
	}

	private Id getOppositeDirectionLinkId(Id linkId, Network network) {
		Link link = network.getLinks().get(linkId);
		if (link == null)
			link = null;
		Node toNode = link.getToNode();
		Node fromNode = link.getFromNode();
		
		for (Link tmpLink: network.getNodes().get(fromNode.getId()).getInLinks().values()){
			if (tmpLink.getFromNode()==toNode){
				return tmpLink.getId();
			}
		}
		
		return null;
	}
	
	public int getFreeCapacity(Id facilityId) {
		
		int freeCapacity = parkingsHashMap.get(facilityId).getIntCapacity()-occupiedParking.get(facilityId);
		
		if (freeCapacity<0){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		if (freeCapacity>parkingsHashMap.get(facilityId).getIntCapacity()){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		return freeCapacity;
	}

	public void parkVehicle(Id facilityId) {
		occupiedParking.increment(facilityId);
		
		if (getFreeCapacity(facilityId)<0){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		if (getFreeCapacity(facilityId)==0){
			markFacilityAsFull(facilityId);
		}
	}

	private void markFacilityAsFull(Id facilityId) {
		Parking parking=parkingsHashMap.get(facilityId);
		nonFullPublicParkingFacilities.remove(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		fullPublicParkingFacilities.add(parking);
	}

	public void unParkVehicle(Id facilityId) {
		occupiedParking.decrement(facilityId);
		
		if (getFreeCapacity(facilityId)==1){
			markFacilityAsNonFull(facilityId);
		}
	}
	
	private void markFacilityAsNonFull(Id facilityId) {
		Parking parking=parkingsHashMap.get(facilityId);
		nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		fullPublicParkingFacilities.remove(parking);
	}

	public List<Id> getParkingsOnLink(Id linkId) {
		return parkingFacilitiesOnLinkMapping.get(linkId);
	}

	public Id getFreeParkingFacilityOnLink(Id linkId, String parkingType) {
		HashSet<Id> parkings=null;
		if (parkingTypes!=null){
			parkings = parkingTypes.get(parkingType);
		}
		
		List<Id> list = getParkingsOnLink(linkId);
		if (list == null) return null;
		else {
			int maxCapacity = 0;
			Id facilityId = null;
			for (Id id : list) {
				if (parkings!=null && !parkings.contains(id)){
					continue;
				}
				
				int capacity = parkingsHashMap.get(id).getIntCapacity();
				int occupied = occupiedParking.get(id);
				if ((capacity - occupied) > maxCapacity) facilityId = id;
			}
			return facilityId;
		}
	}
	
	public Parking getClosestFreeParking(Coord coord) {
		LinkedList<Parking> tmpList=new LinkedList<Parking>();
		Parking parking=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		
		// if parking full, try finding other free parkings in the quadtree
		
		while (getFreeCapacity(parking.getId())<=0){
			removeFullParkingFromQuadTree(tmpList, parking);
			parking=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return  parkingsHashMap.get(parking.getId());
	}
	
	public Id getClosestFreeParkingFacilityNotOnLink(Coord coord, Id linkId){
		LinkedList<Parking> tmpList=new LinkedList<Parking>();
		Parking parkingFacility=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		
		// if parking full or on specified link, try finding other free parkings in the quadtree
		while (getFreeCapacity(parkingFacility.getId())<=0 || parkingLinkMapping.get(parkingFacility.getId()).equals(linkId)){
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return parkingFacility.getId();
	}
	

	private void removeFullParkingFromQuadTree(LinkedList<Parking> tmpList, Parking parkingFacility) {
		tmpList.add(parkingFacility);
		nonFullPublicParkingFacilities.remove(parkingFacility.getCoord().getX(), parkingFacility.getCoord().getY(), parkingFacility);
	}

	private void resetParkingFacilitiesQuadTree(LinkedList<Parking> tmpList) {
		for (Parking parking:tmpList){
			nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		}
	}


	public ParkingCostCalculator getParkingCostCalculator() {
		return parkingCostCalculator;
	}

	//TODO: rename to include world free
	public Id getClosestParkingFacility(Coord coord) {
		if (nonFullPublicParkingFacilities.size()==0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		if (coord==null){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		return nonFullPublicParkingFacilities.get(coord.getX(), coord.getY()).getId();
	}
	
	// parkingId, linkId
	// TODO: init this!
	private HashMap<Id, Id> parkingLinkMapping=new HashMap<Id, Id>();
	
	public synchronized Id getClosestParkingFacilityNotOnLink(Coord coord, Id linkId) {		
		LinkedList<Parking> tmpList=new LinkedList<Parking>();
		Parking parkingFacility=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		
		// if parking full or on specified link, try finding other free parkings in the quadtree
		while (parkingLinkMapping.get(parkingFacility.getId()).equals(linkId)){
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return parkingFacility.getId();
	}
	
	public Collection<Parking> getAllFreeParkingWithinDistance(double distance,Coord coord){
		Collection<Parking> parkings = getAllParkingWithinDistance(distance, coord);
		
//		for (ActivityFacility parking:parkings){
//			if (getFreeCapacity(parking.getId())==0){
//				parkings.remove(parking.getId());
//			}
//		}
		
		return parkings;
	}

	private Collection<Parking> getAllParkingWithinDistance(double distance, Coord coord) {
		return nonFullPublicParkingFacilities.get(coord.getX(), coord.getY(),distance);
	}

	
	public Collection<Parking> getParkingFacilities(){
		return nonFullPublicParkingFacilities.values();
	}

	public HashMap<Id, Parking> getInitialParkingFacilityOfAgent() {
		return initialParkingFacilityOfAgent;
	}

	public Id getClosestFreeParkingFacilityId(Id linkId) {
		return getClosestParkingFacility(this.network.getLinks().get(linkId).getCoord());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// activity facility Id, activityType, parking facility id
		private TwoHashMapsConcatenated<Id, String, Id> privateParkingFacilityIdMapping;

			
		
		public Id getFreePrivateParking(Id actFacilityId, String actType){
			Id parkingFacilityId = privateParkingFacilityIdMapping.get(actFacilityId, actType);
		
			if (parkingFacilityId==null){
				return null;
			}
			
			if (getFreeCapacity(parkingFacilityId)>0){
				return parkingFacilityId;
			} else {
				return null;
			}
		}

		public void initFirstParkingOfDay(Population population) {
			for (Person person:population.getPersons().values()){
				ActivityImpl act = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(0);
				Id freePrivateParkingId = getFreePrivateParking(act.getFacilityId(),act.getType());
				
				if (freePrivateParkingId!=null){
					parkVehicle(freePrivateParkingId);
				} else {
					Id closestFreeParkingFacilityId = getClosestFreeParkingFacilityId(act.getLinkId());
					parkVehicle(closestFreeParkingFacilityId);
				}
			}
		}
	
	
	
	
	
	
}

