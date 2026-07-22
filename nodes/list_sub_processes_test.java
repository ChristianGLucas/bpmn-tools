package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.SubProcessInfo;
import gen.Messages.SubProcessListResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ListSubProcessesTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML has exactly one embedded
    // sub-process (SubProcess_Fulfillment) and one call activity
    // (CallActivity_Invoice, calledElement="InvoiceProcess"), both direct
    // children of OrderProcess, read directly off the literal XML text.
    @Test
    public void testListSubProcesses_findsSubProcessAndCallActivity() {
        AxiomContext ax = TestSupport.ax();
        SubProcessListResult result = ListSubProcesses.listSubProcesses(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals(2, result.getSubProcessesCount());

        Map<String, SubProcessInfo> byId = new HashMap<>();
        for (SubProcessInfo sp : result.getSubProcessesList()) {
            byId.put(sp.getId(), sp);
        }

        SubProcessInfo sub = byId.get("SubProcess_Fulfillment");
        assertEquals("Fulfillment Follow-up", sub.getName());
        assertEquals("subProcess", sub.getType());
        assertEquals(false, sub.getTriggeredByEvent());
        assertEquals("", sub.getCalledElement());
        assertEquals("OrderProcess", sub.getParentProcessId());

        SubProcessInfo call = byId.get("CallActivity_Invoice");
        assertEquals("Generate Invoice", call.getName());
        assertEquals("callActivity", call.getType());
        assertEquals("InvoiceProcess", call.getCalledElement());
        assertEquals("OrderProcess", call.getParentProcessId());
    }

    @Test
    public void testListSubProcesses_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        SubProcessListResult result = ListSubProcesses.listSubProcesses(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getSubProcessesCount());
    }
}
