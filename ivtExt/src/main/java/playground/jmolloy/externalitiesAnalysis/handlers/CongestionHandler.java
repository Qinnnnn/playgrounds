/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.jmolloy.externalitiesAnalysis.handlers;

import org.matsim.api.core.v01.events.handler.*;
import playground.vsp.congestion.handlers.CongestionInternalization;

/**
 * @author nagel
 *
 */
public interface CongestionHandler extends
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler,
VehicleEntersTrafficEventHandler,
PersonArrivalEventHandler,
        CongestionInternalization,
VehicleLeavesTrafficEventHandler {

}
