package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ElementDetail;
import gen.Messages.ElementLookupRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetElementByIdTest {

    private static ElementLookupRequest lookup(String id) {
        return ElementLookupRequest.newBuilder()
                .setDocument(BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build())
                .setElementId(id)
                .build();
    }

    // INDEPENDENT ORACLE: Task_CheckAvailability has exactly one incoming
    // (Flow_1) and one outgoing (Flow_2) sequence flow, read directly off
    // TestSupport.VALID_XML's literal text.
    @Test
    public void testGetElementById_findsFlowNodeWithIncomingAndOutgoing() {
        AxiomContext ax = TestSupport.ax();
        ElementDetail result = GetElementById.getElementById(ax, lookup("Task_CheckAvailability"));

        assertEquals("", result.getError());
        assertTrue(result.getFound());
        assertEquals("Task_CheckAvailability", result.getId());
        assertEquals("Check Availability", result.getName());
        assertEquals("userTask", result.getElementType());
        assertEquals(1, result.getIncomingCount());
        assertEquals("Flow_1", result.getIncoming(0));
        assertEquals(1, result.getOutgoingCount());
        assertEquals("Flow_2", result.getOutgoing(0));
    }

    // The gateway has TWO outgoing flows (Flow_3, Flow_4) — order is not
    // part of this node's contract, so check membership, not position.
    @Test
    public void testGetElementById_gatewayHasTwoOutgoingFlows() {
        AxiomContext ax = TestSupport.ax();
        ElementDetail result = GetElementById.getElementById(ax, lookup("Gateway_Available"));

        assertTrue(result.getFound());
        assertEquals("exclusiveGateway", result.getElementType());
        assertEquals(1, result.getIncomingCount());
        assertEquals(2, result.getOutgoingCount());
        assertTrue(result.getOutgoingList().contains("Flow_3"));
        assertTrue(result.getOutgoingList().contains("Flow_4"));
    }

    // A non-flow-node element (a sequenceFlow itself) has empty
    // incoming/outgoing, not an error.
    @Test
    public void testGetElementById_nonFlowNodeElementHasEmptyIncomingOutgoing() {
        AxiomContext ax = TestSupport.ax();
        ElementDetail result = GetElementById.getElementById(ax, lookup("Flow_1"));

        assertTrue(result.getFound());
        assertEquals("sequenceFlow", result.getElementType());
        assertEquals(0, result.getIncomingCount());
        assertEquals(0, result.getOutgoingCount());
    }

    // A legitimate negative result: found=false, error empty.
    @Test
    public void testGetElementById_unknownIdReturnsFoundFalseNotError() {
        AxiomContext ax = TestSupport.ax();
        ElementDetail result = GetElementById.getElementById(ax, lookup("Does_Not_Exist"));

        assertFalse(result.getFound());
        assertEquals("", result.getError());
    }

    @Test
    public void testGetElementById_blankElementIdReturnsStructuredError() {
        AxiomContext ax = TestSupport.ax();
        ElementDetail result = GetElementById.getElementById(ax, lookup(""));
        assertFalse(result.getError().isEmpty());
        assertFalse(result.getFound());
    }

    @Test
    public void testGetElementById_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        ElementLookupRequest req = ElementLookupRequest.newBuilder()
                .setDocument(BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build())
                .setElementId("anything")
                .build();
        ElementDetail result = GetElementById.getElementById(ax, req);
        assertFalse(result.getError().isEmpty());
        assertFalse(result.getFound());
    }
}
