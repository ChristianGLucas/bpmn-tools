package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ProcessGraphEdge;
import gen.Messages.ProcessGraphNode;
import gen.Messages.ProcessGraphResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToGraphTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML has exactly 15 flow nodes
    // (9 in OrderProcess directly + 3 nested inside SubProcess_Fulfillment +
    // 3 in InvoiceProcess) and 10 sequence flows (6 + 2 + 2) — every count
    // read directly off the literal XML text, matching ListTasks/
    // ListGateways/ListEvents/ListSequenceFlows's own independently
    // hand-computed counts for the same document.
    @Test
    public void testToGraph_producesADirectedGraphMatchingTheDocument() {
        AxiomContext ax = TestSupport.ax();
        ProcessGraphResult result = ToGraph.toGraph(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertTrue(result.getDirected());
        assertEquals(15, result.getNodesCount());
        assertEquals(10, result.getEdgesCount());

        Map<String, ProcessGraphNode> nodesById = new HashMap<>();
        for (ProcessGraphNode n : result.getNodesList()) {
            nodesById.put(n.getId(), n);
        }
        // Named node: label falls back to the BPMN `name`.
        assertEquals("Order Received", nodesById.get("StartEvent_1").getLabel());
        // Unnamed node (InvoiceStart has no `name` attribute in the fixture):
        // label falls back to the id itself, never blank.
        assertEquals("InvoiceStart", nodesById.get("InvoiceStart").getLabel());

        boolean foundGatewayToShip = false;
        for (ProcessGraphEdge e : result.getEdgesList()) {
            if (e.getFrom().equals("Gateway_Available") && e.getTo().equals("Task_ShipItems")) {
                foundGatewayToShip = true;
                assertEquals(1.0, e.getWeight(), 0.0);
                assertFalse(e.getExplicitZeroWeight());
            }
        }
        assertTrue(foundGatewayToShip, "expected an edge Gateway_Available -> Task_ShipItems");
    }

    @Test
    public void testToGraph_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        ProcessGraphResult result = ToGraph.toGraph(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getNodesCount());
        assertEquals(0, result.getEdgesCount());
    }
}
