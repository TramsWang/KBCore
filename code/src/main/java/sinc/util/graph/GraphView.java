package sinc.util.graph;

import org.jpl7.Compound;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import sinc.common.GraphNode4Compound;

import java.util.*;

public class GraphView {
    public static interface RedundancyChecker {
        boolean isRedundant(Compound c);
    }

    static public void draw(
            Iterator<Compound> nodeItr, Map<GraphNode4Compound, Set<GraphNode4Compound>> edges,
            RedundancyChecker checker
    ) {
        System.out.print("Printing Dependency Graph in Neo4j...");

        /* Connect to Neo4j */
        Driver driver = GraphDatabase.driver(
                "bolt://localhost:7687", AuthTokens.basic("neo4j", "kbcore")
        );

        /* Clear DB */
        Session session = driver.session();
        session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));
        session.writeTransaction(tx -> tx.run("CALL apoc.schema.assert({},{},true) YIELD label, key"));

        /* Create Nodes */
        while (nodeItr.hasNext()) {
            Compound node = nodeItr.next();
            if (checker.isRedundant(node)) {
                session.writeTransaction(tx -> tx.run(String.format("CREATE (e:`proved`{name:\"%s\"})", node)));
            } else {
                session.writeTransaction(tx -> tx.run(String.format("CREATE (e:`remained`{name:\"%s\"})", node)));
            }
        }

        /* Create Edges */
        for (Map.Entry<GraphNode4Compound, Set<GraphNode4Compound>> entry: edges.entrySet()) {
            GraphNode4Compound src = entry.getKey();
            for (GraphNode4Compound dst: entry.getValue()) {
                session.writeTransaction(tx -> tx.run(
                        String.format(
                                "MATCH (s{name:\"%s\"}), (d{name:\"%s\"}) CREATE (s)-[r:`rel`]->(d)",
                                src.compound, dst.compound
                        )
                ));
            }
        }

        session.close();
        driver.close();
        System.out.println("Done");
    }
}
