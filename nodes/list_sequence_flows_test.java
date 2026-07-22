package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.SequenceFlowInfo;
import gen.Messages.SequenceFlowListResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ListSequenceFlowsTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML has exactly ten sequence
    // flows, read directly off its literal XML text — Flow_1..Flow_6
    // (OrderProcess), Flow_Sub1/Flow_Sub2 (nested inside
    // SubProcess_Fulfillment), InvoiceFlow1/InvoiceFlow2 (InvoiceProcess).
    @Test
    public void testListSequenceFlows_findsEveryFlowAcrossBothProcesses() {
        AxiomContext ax = TestSupport.ax();
        SequenceFlowListResult result = ListSequenceFlows.listSequenceFlows(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals(10, result.getFlowsCount());

        Map<String, SequenceFlowInfo> byId = new HashMap<>();
        for (SequenceFlowInfo f : result.getFlowsList()) {
            byId.put(f.getId(), f);
        }

        SequenceFlowInfo flow1 = byId.get("Flow_1");
        assertEquals("StartEvent_1", flow1.getSourceRef());
        assertEquals("Task_CheckAvailability", flow1.getTargetRef());
        assertEquals("", flow1.getConditionExpression());
        assertEquals("OrderProcess", flow1.getProcessId());

        SequenceFlowInfo flow3 = byId.get("Flow_3");
        assertEquals("Gateway_Available", flow3.getSourceRef());
        assertEquals("Task_ShipItems", flow3.getTargetRef());
        assertEquals("${available == true}", flow3.getConditionExpression());

        SequenceFlowInfo flow4 = byId.get("Flow_4");
        assertEquals("${available == false}", flow4.getConditionExpression());

        SequenceFlowInfo sub1 = byId.get("Flow_Sub1");
        assertEquals("StartEvent_Sub", sub1.getSourceRef());
        assertEquals("Task_Notify", sub1.getTargetRef());
        // Nested inside the sub-process, but reports the TOP-LEVEL process.
        assertEquals("OrderProcess", sub1.getProcessId());

        SequenceFlowInfo invoiceFlow = byId.get("InvoiceFlow1");
        assertEquals("InvoiceProcess", invoiceFlow.getProcessId());
    }

    @Test
    public void testListSequenceFlows_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        SequenceFlowListResult result = ListSequenceFlows.listSequenceFlows(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getFlowsCount());
    }
}
