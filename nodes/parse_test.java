package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ParseResult;
import gen.Messages.ProcessInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParseTest {

    // INDEPENDENT ORACLE: every value asserted here is read directly off
    // TestSupport.VALID_XML's literal text (id, targetNamespace, exporter,
    // exporterVersion attributes; the two <bpmn:process> elements and their
    // own id/name/isExecutable attributes), not derived from running this
    // node.
    @Test
    public void testParse_extractsDefinitionsAndProcesses() {
        AxiomContext ax = TestSupport.ax();
        ParseResult result = Parse.parse(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals("Definitions_1", result.getDefinitionsId());
        assertEquals("http://bpmn-tools.axiom.dev/test", result.getTargetNamespace());
        assertEquals("bpmn-tools-test-fixture", result.getExporter());
        assertEquals("1.0", result.getExporterVersion());

        assertEquals(2, result.getProcessesCount());
        ProcessInfo order = result.getProcesses(0);
        assertEquals("OrderProcess", order.getId());
        assertEquals("Order Process", order.getName());
        assertTrue(order.getIsExecutable());

        ProcessInfo invoice = result.getProcesses(1);
        assertEquals("InvoiceProcess", invoice.getId());
        assertEquals("Invoice Process", invoice.getName());
        assertFalse(invoice.getIsExecutable());
    }

    @Test
    public void testParse_blankXmlReturnsStructuredError() {
        AxiomContext ax = TestSupport.ax();
        ParseResult result = Parse.parse(ax, BpmnDocument.newBuilder().setXml("   ").build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getProcessesCount());
    }

    @Test
    public void testParse_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        ParseResult result = Parse.parse(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    public void testParse_schemaInvalidXmlReturnsStructuredError() {
        AxiomContext ax = TestSupport.ax();
        ParseResult result = Parse.parse(ax, BpmnDocument.newBuilder().setXml(TestSupport.SCHEMA_INVALID_XML).build());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    public void testParse_oversizedXmlReturnsStructuredError() {
        AxiomContext ax = TestSupport.ax();
        ParseResult result = Parse.parse(ax, BpmnDocument.newBuilder().setXml(TestSupport.oversizedXml()).build());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    public void testParse_isDeterministic() {
        AxiomContext ax = TestSupport.ax();
        BpmnDocument input = BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build();
        ParseResult first = Parse.parse(ax, input);
        ParseResult second = Parse.parse(ax, input);
        assertEquals(first.getDefinitionsId(), second.getDefinitionsId());
        assertEquals(first.getProcessesCount(), second.getProcessesCount());
    }
}
