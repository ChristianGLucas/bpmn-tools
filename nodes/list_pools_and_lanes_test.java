package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.CollaborationResult;
import gen.Messages.LaneInfo;
import gen.Messages.PoolInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListPoolsAndLanesTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML has exactly one pool
    // (Participant_Orders -> OrderProcess) with two lanes — Lane_Sales (3
    // flow node refs) and Lane_Warehouse (5 flow node refs) — read directly
    // off its literal XML text.
    @Test
    public void testListPoolsAndLanes_findsTheSinglePoolAndItsLanes() {
        AxiomContext ax = TestSupport.ax();
        CollaborationResult result = ListPoolsAndLanes.listPoolsAndLanes(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals(1, result.getPoolsCount());

        PoolInfo pool = result.getPools(0);
        assertEquals("Participant_Orders", pool.getId());
        assertEquals("Order Department", pool.getName());
        assertEquals("OrderProcess", pool.getProcessRef());
        assertEquals(2, pool.getLanesCount());

        LaneInfo sales = pool.getLanes(0);
        assertEquals("Lane_Sales", sales.getId());
        assertEquals("Sales", sales.getName());
        assertEquals(3, sales.getFlowNodeRefsCount());
        assertTrue(sales.getFlowNodeRefsList().contains("StartEvent_1"));
        assertTrue(sales.getFlowNodeRefsList().contains("Task_CheckAvailability"));
        assertTrue(sales.getFlowNodeRefsList().contains("Gateway_Available"));

        LaneInfo warehouse = pool.getLanes(1);
        assertEquals("Lane_Warehouse", warehouse.getId());
        assertEquals(5, warehouse.getFlowNodeRefsCount());
        assertTrue(warehouse.getFlowNodeRefsList().contains("Task_ShipItems"));
        assertTrue(warehouse.getFlowNodeRefsList().contains("SubProcess_Fulfillment"));
        assertTrue(warehouse.getFlowNodeRefsList().contains("CallActivity_Invoice"));
    }

    // A bare document with a process but no <collaboration>/<participant> at
    // all is a legitimate, common case — empty `pools`, not an error.
    @Test
    public void testListPoolsAndLanes_noCollaborationReturnsEmptyPoolsNotError() {
        AxiomContext ax = TestSupport.ax();
        CollaborationResult result = ListPoolsAndLanes.listPoolsAndLanes(ax, BpmnDocument.newBuilder().setXml(TestSupport.BARE_PROCESS_XML).build());
        assertEquals("", result.getError());
        assertEquals(0, result.getPoolsCount());
    }

    @Test
    public void testListPoolsAndLanes_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        CollaborationResult result = ListPoolsAndLanes.listPoolsAndLanes(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getPoolsCount());
    }
}
