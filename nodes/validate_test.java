package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ValidateResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateTest {

    @Test
    public void testValidate_wellFormedSchemaConformantDocumentIsValid() {
        AxiomContext ax = TestSupport.ax();
        ValidateResult result = Validate.validate(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());
        assertTrue(result.getValid());
        assertEquals("", result.getError());
        assertEquals("", result.getViolation());
    }

    // INDEPENDENT ORACLE (verified directly against the library in a
    // standalone scratch run, not assumed): a sequenceFlow missing its
    // required sourceRef/targetRef fails Camunda's own parser with
    // "cvc-complex-type.4: Attribute 'sourceRef' must appear on element
    // 'bpmn:sequenceFlow'." — a legitimate schema-validation FINDING
    // (violation), distinct from a request-level FAILURE (error).
    @Test
    public void testValidate_schemaViolationSetsViolationNotError() {
        AxiomContext ax = TestSupport.ax();
        ValidateResult result = Validate.validate(ax, BpmnDocument.newBuilder().setXml(TestSupport.SCHEMA_INVALID_XML).build());
        assertFalse(result.getValid());
        assertFalse(result.getViolation().isEmpty());
        assertTrue(result.getViolation().contains("sourceRef"));
        assertEquals("", result.getError());
    }

    // Not well-formed XML is rejected by the SAME underlying parser call as
    // a schema violation (Camunda's parser does not expose these as
    // separable failure kinds) — also `violation`, not `error`.
    @Test
    public void testValidate_notWellFormedXmlAlsoSetsViolation() {
        AxiomContext ax = TestSupport.ax();
        ValidateResult result = Validate.validate(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getValid());
        assertFalse(result.getViolation().isEmpty());
        assertEquals("", result.getError());
    }

    @Test
    public void testValidate_blankXmlSetsError() {
        AxiomContext ax = TestSupport.ax();
        ValidateResult result = Validate.validate(ax, BpmnDocument.newBuilder().setXml("").build());
        assertFalse(result.getValid());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    public void testValidate_oversizedXmlSetsErrorBeforeParsing() {
        AxiomContext ax = TestSupport.ax();
        ValidateResult result = Validate.validate(ax, BpmnDocument.newBuilder().setXml(TestSupport.oversizedXml()).build());
        assertFalse(result.getValid());
        assertTrue(result.getError().toLowerCase().contains("cap"));
    }
}
