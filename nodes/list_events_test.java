package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.EventInfo;
import gen.Messages.EventListResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListEventsTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML has exactly eight flow
    // events, read directly off its literal XML text — StartEvent_1,
    // BoundaryEvent_Timeout (timer), EndEvent_Shipped, EndEvent_Cancelled,
    // StartEvent_Sub, EndEvent_Sub (both inside the sub-process),
    // InvoiceStart, InvoiceEnd (in the second process).
    @Test
    public void testListEvents_findsEveryEventAcrossBothProcesses() {
        AxiomContext ax = TestSupport.ax();
        EventListResult result = ListEvents.listEvents(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals(8, result.getEventsCount());

        Map<String, EventInfo> byId = new HashMap<>();
        for (EventInfo e : result.getEventsList()) {
            byId.put(e.getId(), e);
        }

        EventInfo start = byId.get("StartEvent_1");
        assertEquals("startEvent", start.getCategory());
        assertEquals("", start.getEventDefinitionType());
        // BPMN 2.0's own schema default for a startEvent's isInterrupting is
        // true; this document never sets it explicitly.
        assertTrue(start.getInterrupting());
        assertEquals("OrderProcess", start.getProcessId());

        EventInfo boundary = byId.get("BoundaryEvent_Timeout");
        assertEquals("boundaryEvent", boundary.getCategory());
        assertEquals("timer", boundary.getEventDefinitionType());
        assertTrue(boundary.getInterrupting());
        assertEquals("Task_ShipItems", boundary.getAttachedToRef());
        assertEquals("OrderProcess", boundary.getProcessId());

        EventInfo shipped = byId.get("EndEvent_Shipped");
        assertEquals("endEvent", shipped.getCategory());
        assertEquals("", shipped.getEventDefinitionType());
        assertEquals("", shipped.getAttachedToRef());

        EventInfo invoiceStart = byId.get("InvoiceStart");
        assertEquals("startEvent", invoiceStart.getCategory());
        assertEquals("InvoiceProcess", invoiceStart.getProcessId());
    }

    @Test
    public void testListEvents_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        EventListResult result = ListEvents.listEvents(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getEventsCount());
    }
}
