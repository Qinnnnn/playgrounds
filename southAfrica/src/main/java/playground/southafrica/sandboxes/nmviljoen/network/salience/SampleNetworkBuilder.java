package playground.southafrica.sandboxes.nmviljoen.network.salience;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import edu.uci.ics.jung.algorithms.generators.GraphGenerator;
import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphMLMetadata;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.io.graphml.EdgeMetadata;
import edu.uci.ics.jung.io.graphml.GraphMLReader2;
import edu.uci.ics.jung.io.graphml.GraphMetadata;
import edu.uci.ics.jung.io.graphml.HyperEdgeMetadata;
import edu.uci.ics.jung.io.graphml.NodeMetadata;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import playground.southafrica.sandboxes.nmviljoen.gridExamples.NmvLink;
import playground.southafrica.sandboxes.nmviljoen.gridExamples.NmvNode;
import playground.southafrica.utilities.Header;

public class SampleNetworkBuilder {
	final private static Logger LOG = Logger.getLogger(SampleNetworkBuilder.class);
	final private int Y_VARIATION = 5;
	final private int WEIGHT_VARIATION = 20;
	private Random random;

	public SampleNetworkBuilder(long seed) {
		this.random = new Random(seed);
	}

	private class GraphSupplier implements Supplier<Graph<NmvNode, NmvLink>>{
		@Override
		public Graph<NmvNode, NmvLink> get() {
			return new DirectedSparseGraph<NmvNode, NmvLink>();
		}
	}

	private class VertexSupplier implements Supplier<NmvNode>{
		int a = 0;
		@Override
		public NmvNode get() {
			a++;
			return new NmvNode(String.valueOf(a), String.valueOf(a), a, a+random.nextInt(Y_VARIATION));
		}
	}

	private class LinkSupplier implements Supplier<NmvLink>{
		int b = 0;
		@Override
		public NmvLink get() {
			return new NmvLink(String.valueOf(b++), Math.max(1,random.nextInt(WEIGHT_VARIATION)));
		}
	}

	
	public Graph<NmvNode, NmvLink> buildEppsteinGraph(int numVertices, int numEdges, int r){
		
		Supplier<Graph<NmvNode, NmvLink>> graphFactory = new GraphSupplier();
		Supplier<NmvNode> vertexFactory = new VertexSupplier();
		Supplier<NmvLink> edgeFactory = new LinkSupplier();
		GraphGenerator<NmvNode, NmvLink> generator = new EppsteinPowerLawGenerator<NmvNode, NmvLink>(graphFactory, vertexFactory, edgeFactory, numVertices, numEdges, r);
		return generator.get();
	}
	
	
	public static void writeGraphML(Graph<NmvNode, NmvLink> graph, String filename){
		/* Build all the node transformers. */
		Function<NmvNode, String> nodeIdT = new Function<NmvNode, String>() {
			@Override
			public String apply(NmvNode node) {
				return node.getId();
			}
		};
		Function<NmvNode, String> nodeXT = new Function<NmvNode, String>() {
			@Override
			public String apply(NmvNode node) {
				return node.getXAsString();
			}
		};
		Function<NmvNode, String> nodeYT = new Function<NmvNode, String>() {
			@Override
			public String apply(NmvNode node) {
				return node.getYAsString();
			}
		};
		GraphMLMetadata<NmvNode> nodeX = new GraphMLMetadata<NmvNode>("x", "0", nodeXT );
		GraphMLMetadata<NmvNode> nodeY = new GraphMLMetadata<NmvNode>("y", "0", nodeYT );

		Map<String, GraphMLMetadata<NmvNode>> vertexMap = new HashMap<String, GraphMLMetadata<NmvNode>>();
		vertexMap.put("x", nodeX);
		vertexMap.put("y", nodeY);
		
		/* Build all the edge transformers. */
		Function<NmvLink, String> linkIdT = new Function<NmvLink, String>() {
			@Override
			public String apply(NmvLink link) {
				return link.getId();
			}
		};
		Function<NmvLink, String> linkWeightT = new Function<NmvLink, String>() {
			@Override
			public String apply(NmvLink link) {
				return String.valueOf(link.getWeight());
			}
		};
		
		GraphMLMetadata<NmvLink> linkId = new GraphMLMetadata<NmvLink>("id", "0", linkIdT );
		GraphMLMetadata<NmvLink> linkWeight = new GraphMLMetadata<NmvLink>("id", "0", linkWeightT );
		
		Map<String, GraphMLMetadata<NmvLink>> linkMap = new HashMap<String, GraphMLMetadata<NmvLink>>();
		linkMap.put("weight", linkWeight);
		
		/* Set up the graph writer. */
		GraphMLWriter<NmvNode, NmvLink> writer = new GraphMLWriter<NmvNode, NmvLink>();
		writer.setVertexIDs(nodeIdT);
		writer.setVertexData(vertexMap);
		writer.setEdgeIDs(linkIdT);
		writer.setEdgeData(linkMap);

		/* Write the network to file. */
		try {
			writer.save(graph, IOUtils.getBufferedWriter(filename));
			System.out.println("GraphML file written " + filename);
		} catch (UncheckedIOException | IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not write graph to " + filename);
		}
	}
	
	public static Graph<NmvNode, NmvLink> readGraphML(String filename){
		LOG.info("Reading GraphML object from " + filename);
		
		Reader fileReader = IOUtils.getBufferedReader(filename);
		Function<GraphMetadata, DirectedSparseGraph<NmvNode, NmvLink>> graphTransformer = new Function<GraphMetadata, DirectedSparseGraph<NmvNode,NmvLink>>() {
			@Override
			public DirectedSparseGraph<NmvNode, NmvLink> apply(
					GraphMetadata arg0) {
				return new DirectedSparseGraph<NmvNode, NmvLink>();
			}
		};
		Function<NodeMetadata, NmvNode> vertexTransformer = new Function<NodeMetadata, NmvNode>() {
			@Override
			public NmvNode apply(NodeMetadata arg0) {
				NmvNode node = new NmvNode(arg0.getId(), arg0.getId(), Double.parseDouble(arg0.getProperty("x")), Double.parseDouble(arg0.getProperty("y")));
				return node;
			}
		};
		Function<EdgeMetadata, NmvLink> edgeTransformer = new Function<EdgeMetadata, NmvLink>() {
			@Override
			public NmvLink apply(EdgeMetadata arg0) {
				NmvLink link = new NmvLink(arg0.getId(), Double.parseDouble(arg0.getProperty("weight")));
				return link;
			}
		};
		Function<HyperEdgeMetadata, NmvLink> hyperEdgeTransformer = new Function<HyperEdgeMetadata, NmvLink>() {
			@Override
			public NmvLink apply(HyperEdgeMetadata arg0) {
				return null;
			}
		};
		GraphMLReader2<DirectedSparseGraph<NmvNode, NmvLink>, NmvNode, NmvLink> r2 = 
				new GraphMLReader2<DirectedSparseGraph<NmvNode, NmvLink>, NmvNode, NmvLink>(fileReader, graphTransformer, vertexTransformer, edgeTransformer, hyperEdgeTransformer );
		
		Graph<NmvNode, NmvLink> graph = null;
		try {
			graph = r2.readGraph();
		} catch (GraphIOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("Cannot read graph from " + filename);
		}
		LOG.info("Done reading GraphML object.");
		LOG.info("   |_ Nodes: " + graph.getVertexCount());
		LOG.info("   |_ Edges: " + graph.getEdgeCount());
		return graph;
	}

	public static void main(String[] args){
		Header.printHeader(SampleNetworkBuilder.class.toString(), args);

		long seed = Long.parseLong(args[0]);
		String filename = args[1];

		/* Build the sample network. */
		SampleNetworkBuilder snb = new SampleNetworkBuilder(seed);
		Graph<NmvNode, NmvLink> graph1 = snb.buildEppsteinGraph(2000, 200000, 20000);
		Graph<NmvNode, NmvLink> graph2 = snb.buildTestGraph();
		Graph<NmvNode, NmvLink> graph = graph1;
		
		
		snb.writeGraphML(graph, filename);

		/* Visualise the graph. */
//		snb.visualizeGraph(graph);
		
		/* Now read the graph back in. */
//		Graph<NmvNode, NmvLink> newGraph = snb.readGraphML(filename);
//		graph.equals(newGraph);
		
		Header.printFooter();
	}
	
	private void visualizeGraph(Graph<NmvNode, NmvLink> graph){
		Layout<NmvNode, NmvLink> layoutCircle = new CircleLayout<NmvNode, NmvLink>(graph);
		Function<NmvNode, Point2D> transformer = new Function<NmvNode, Point2D>() {
			@Override
			public Point2D apply(NmvNode node) {
				Point p = new Point();
				p.setLocation(node.X*100, node.Y*100);
				return p;
			}
		};
		Layout<NmvNode, NmvLink> layoutStatic = new StaticLayout<NmvNode, NmvLink>(graph, transformer );
		
		layoutStatic.setSize(new Dimension(400,400));
		BasicVisualizationServer<NmvNode, NmvLink> vv =
				new BasicVisualizationServer<NmvNode,NmvLink>(layoutCircle);
		vv.setPreferredSize(new Dimension(400,400));
		JFrame frame = new JFrame("Graph view");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true);
	}

	private Graph<NmvNode, NmvLink> buildTestGraph() {
		
		Graph<NmvNode, NmvLink> graph = new DirectedSparseGraph<NmvNode, NmvLink>();
		NmvNode n1 = new NmvNode("1", "1", 0.0, 10.0);
		NmvNode n2 = new NmvNode("2", "2", 10.0, 10.0);
		NmvNode n3 = new NmvNode("3", "3", 10.0, 0.0);
		NmvNode n4 = new NmvNode("4", "4", 0.0, 0.0);
		graph.addVertex(n1);
		graph.addVertex(n2);
		graph.addVertex(n3);
		graph.addVertex(n4);
		
		List<NmvNode> e1 = new ArrayList<NmvNode>(2);
		e1.add(n1);
		e1.add(n2);
		graph.addEdge(new NmvLink("12", 1), e1, EdgeType.DIRECTED);

		List<NmvNode> e2 = new ArrayList<NmvNode>(2);
		e2.add(n2);
		e2.add(n3);
		graph.addEdge(new NmvLink("23", 2), e2, EdgeType.DIRECTED);
		
		List<NmvNode> e3 = new ArrayList<NmvNode>(2);
		e3.add(n3);
		e3.add(n4);
		graph.addEdge(new NmvLink("34", 3), e3, EdgeType.DIRECTED);
		
		List<NmvNode> e4 = new ArrayList<NmvNode>(2);
		e4.add(n4);
		e4.add(n2);
		graph.addEdge(new NmvLink("42", 4), e4, EdgeType.DIRECTED);
		
		List<NmvNode> e5 = new ArrayList<NmvNode>(2);
		e5.add(n2);
		e5.add(n4);
		graph.addEdge(new NmvLink("24", 5), e5, EdgeType.DIRECTED);
		
		return graph;
	}

}
