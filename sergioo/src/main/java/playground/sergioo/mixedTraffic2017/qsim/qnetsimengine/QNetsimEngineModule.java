/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * QNetsimEngineModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.sergioo.mixedTraffic2017.qsim.qnetsimengine;

import playground.sergioo.mixedTraffic2017.qsim.QSim;

public class QNetsimEngineModule {

    public static void configure(QSim qsim) {
        QNetsimEngine netsimEngine = new QNetsimEngine(qsim);
        qsim.addMobsimEngine(netsimEngine);
        qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
    }

}
