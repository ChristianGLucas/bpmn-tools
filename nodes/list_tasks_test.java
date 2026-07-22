package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.TaskInfo;
import gen.Messages.TaskListResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListTasksTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML contains exactly four
    // task-family elements, read directly off its literal XML text —
    // Task_CheckAvailability (userTask), Task_ShipItems (serviceTask),
    // Task_Notify (plain task, nested inside SubProcess_Fulfillment but
    // whose enclosing TOP-LEVEL process is still OrderProcess), and
    // InvoiceTask (plain task, in the second process InvoiceProcess).
    // Looked up by id rather than positional index since
    // getModelElementsByType's cross-subtype ordering is not part of this
    // package's documented contract.
    @Test
    public void testListTasks_findsEveryTaskFamilyElementAcrossBothProcesses() {
        AxiomContext ax = TestSupport.ax();
        TaskListResult result = ListTasks.listTasks(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals(4, result.getTasksCount());

        Map<String, TaskInfo> byId = new HashMap<>();
        for (TaskInfo t : result.getTasksList()) {
            byId.put(t.getId(), t);
        }

        TaskInfo check = byId.get("Task_CheckAvailability");
        assertEquals("Check Availability", check.getName());
        assertEquals("userTask", check.getType());
        assertEquals("OrderProcess", check.getProcessId());

        TaskInfo ship = byId.get("Task_ShipItems");
        assertEquals("Ship Items", ship.getName());
        assertEquals("serviceTask", ship.getType());
        assertEquals("OrderProcess", ship.getProcessId());

        TaskInfo notify = byId.get("Task_Notify");
        assertEquals("Notify Customer", notify.getName());
        assertEquals("task", notify.getType());
        // Nested inside SubProcess_Fulfillment, but reports its TOP-LEVEL
        // enclosing process, not the sub-process's own id.
        assertEquals("OrderProcess", notify.getProcessId());

        TaskInfo invoice = byId.get("InvoiceTask");
        assertEquals("Create Invoice", invoice.getName());
        assertEquals("task", invoice.getType());
        assertEquals("InvoiceProcess", invoice.getProcessId());
    }

    @Test
    public void testListTasks_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        TaskListResult result = ListTasks.listTasks(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getTasksCount());
    }

    @Test
    public void testListTasks_isDeterministic() {
        AxiomContext ax = TestSupport.ax();
        BpmnDocument input = BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build();
        TaskListResult first = ListTasks.listTasks(ax, input);
        TaskListResult second = ListTasks.listTasks(ax, input);
        assertEquals(first.getTasksCount(), second.getTasksCount());
        assertTrue(first.getTasksCount() > 0);
    }
}
