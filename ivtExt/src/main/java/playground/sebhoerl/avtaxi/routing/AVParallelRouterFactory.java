package playground.sebhoerl.avtaxi.routing;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculatorFactory;

@Singleton
public class AVParallelRouterFactory implements ParallelLeastCostPathCalculatorFactory {
    @Inject @Named(AVModule.AV_MODE) TravelTime travelTime;
    @Inject Network network;

    @Override
    public LeastCostPathCalculator createRouter() {
        return new DijkstraFactory().createPathCalculator(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }
}
