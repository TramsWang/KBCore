package utils.graph;

import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.util.*;

public class GraphView {
    public static class Edge {
        public Compound src;
        public Compound dst;

        public Edge(Compound src, Compound dst) {
            this.src = src;
            this.dst = dst;
        }
    }

    public void draw(Set<Compound> nodes, List<Edge> edges) {
        /* Connect to Neo4j */
        Driver driver = GraphDatabase.driver(
                "bolt://localhost:7687", AuthTokens.basic("neo4j", "kbcore")
        );

        /* Clear DB */
        Session session = driver.session();
        session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));
        session.writeTransaction(tx -> tx.run("CALL apoc.schema.assert({},{},true) YIELD label, key"));

        /* Create Nodes */
        for (Compound compound: nodes) {
            session.writeTransaction(tx -> tx.run(String.format("CREATE (e:`node`{name:\"%s\"})", toValidString(compound))));
        }

        /* Create Edges */
        for (Edge edge: edges) {
            session.writeTransaction(tx -> tx.run(
                    String.format(
                            "MATCH (s:`node`{name:\"%s\"}), (d:`node`{name:\"%s\"}) CREATE (s)-[r:`rel`]->(d)",
                            toValidString(edge.src), toValidString(edge.dst)
                    )
            ));
        }

        session.close();
        driver.close();
    }

    private String toValidString(Compound compound) {
        return compound.toString();
//        return compound.toString().replace('\\', '~').replace('\'', '_');
    }

    public static void main(String[] args) {
        Compound c1 = new Compound("\\src", new Term[]{new Atom("c"), new Atom("d")});
        Compound c2 = new Compound("parent", new Term[]{new Atom("c"), new Atom("d")});
        GraphView graphView = new GraphView();
        Set<Compound> nodes = new HashSet<>();
        nodes.add(c1);
        nodes.add(c2);
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(c1, c2));
        graphView.draw(nodes, edges);
    }
}
