package saleem.stockholmscenario.teleportation;

public class PTNetworkGenerator {

	public static void main(String[] args) {
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\transitSchedule.xml";
		String path1 = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PTNetworkTest.xml";
		XMLReaderWriter xml = new XMLReaderWriter();
		xml.writePTNetworkStopsAndLinks(xml.readFile(path), path1);//Read  transit schedule, create stops and write to ptStops.csv
	}
	

}
