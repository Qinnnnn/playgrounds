/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.lib.tools.tripReader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.boescpa.converters.visum.obj.Zone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Provides a representation of trips.
 *
 * @author boescpa
 */
public class Trip {

	// TODO-boescpa Write tests...

	private static Long tripIdCounter = (long)0;

	protected final long tripId;
	protected final Id agentId;
	protected final double startTime;
	protected final Id startLinkId;
	protected final double startXCoord;
	protected final double startYCoord;
	protected final double endTime;
	protected final Id endLinkId;
	protected final double endXCoord;
	protected final double endYCoord;
	protected final String mode;
	protected final String purpose;
	protected final double duration;
	protected final long distance;

	public Trip(Id agentId,
				double startTime, Id startLinkId, double startXCoord, double startYCoord,
				double endTime, Id endLinkId, double endXCoord, double endYCoord,
				String mode, String purpose, double duration, long distance) {

		tripIdCounter++;
		tripId = tripIdCounter;

		this.agentId = agentId;
		this.startTime = startTime;
		this.startLinkId = startLinkId;
		this.startXCoord = startXCoord;
		this.startYCoord = startYCoord;
		this.endTime = endTime;
		this.endLinkId = endLinkId;
		this.endXCoord = endXCoord;
		this.endYCoord = endYCoord;
		this.mode = mode;
		this.purpose = purpose;
		this.duration = duration;
		this.distance = distance;
	}



	public static String getHeader() {
		return "tripId\tagentId\tstartTime\tstartLink\tstartXCoord\tstartYCoord\tendTime\tendLink\tendXCoord\tendYCoord\tmode\tpurpose\tduration\tdistance";
	}

	@Override
	public String toString() {
		return tripId + "\t" + agentId + "\t" + startTime + "\t" + startLinkId + "\t" + startXCoord + "\t"
				+ startYCoord + "\t" + endTime + "\t" + endLinkId + "\t" + endXCoord + "\t" + endYCoord
				+ "\t" + mode + "\t" + purpose + "\t" + duration + "\t" + distance;
	}

	/**
	 * Reads a trip file into the memory and provides the trips in the form of a HashMap.
	 *
	 * @param sourceTripFile
	 * @return HashMap<Long,Trip> - Key: TripId, Value: Trip
	 */
	public static HashMap<Long,Trip> createTripCollection(String sourceTripFile) {
		HashMap<Long,Trip> tripCollection = new HashMap<Long, Trip>();
		Scenario scenarioUtils = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FileReader reader;
		try {
			reader = new FileReader(sourceTripFile);
			BufferedReader readsLines = new BufferedReader(reader);
			String newLine = readsLines.readLine(); // Header is read...
			newLine = readsLines.readLine();
			while (newLine != null) {
				String[] tripLine = newLine.split("\t");
				Trip trip = new Trip(
						scenarioUtils.createId(tripLine[1]), //agentId
						Double.parseDouble(tripLine[2]), // startTime
						scenarioUtils.createId(tripLine[3]), // startLinkId
						Double.parseDouble(tripLine[4]), // startXCoord
						Double.parseDouble(tripLine[5]), // startYCoord
						Double.parseDouble(tripLine[6]), // endTime
						scenarioUtils.createId(tripLine[7]), // endLinkId
						Double.parseDouble(tripLine[8]), // endXCoord
						Double.parseDouble(tripLine[9]), // endYCoord
						tripLine[10], // mode
						tripLine[11], // purpose
						Double.parseDouble(tripLine[12]), // duration
						Long.parseLong(tripLine[13])); // distance
				tripCollection.put(trip.tripId, trip);
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tripCollection;
	}
}
