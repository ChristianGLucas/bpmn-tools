package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ValidateResult;
import org.camunda.bpm.model.bpmn.Bpmn;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class Validate {

    /**
     * Validate a BPMN 2.0 XML document against the official BPMN 2.0 XSD
     * schema — the same schema Camunda's own BPMN Model API parser enforces
     * on every read. Returns {@code valid = true} only when the document is
     * well-formed XML AND schema-conformant.
     *
     * <p>Distinguishes two distinct failure kinds. Blank input sets
     * {@code error}: the request itself could not even be attempted.
     * Everything Camunda's parser itself rejects — a schema
     * violation (verified directly against the library: e.g. a
     * {@code sequenceFlow} missing its required {@code sourceRef} fails with
     * "cvc-complex-type.4: Attribute 'sourceRef' must appear...") OR XML
     * that is not even well-formed — sets {@code violation} instead: both
     * are legitimately "this is not valid BPMN 2.0", which is exactly what
     * this node answers, and Camunda's parser does not expose them as
     * separable failure kinds (both surface as the same exception type from
     * the same call). Check {@code error} first — {@code valid} is also
     * false when it is set, but for the different, request-level reason.
     *
     * <p>{@code ax} is the AxiomContext (ADR-001): every platform capability is
     * reached through it — {@code ax.log()}, {@code ax.secrets()},
     * {@code ax.reflection()}, {@code ax.mutation()}. Node
     * code never talks to the platform directly.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The decoded BpmnDocument for this invocation.
     */
    public static ValidateResult validate(AxiomContext ax, BpmnDocument input) {
        ax.log().info("validate handling", Map.of());

        byte[] bytes;
        try {
            bytes = BpmnUtil.requireSizedXml(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return ValidateResult.newBuilder().setValid(false).setError(f.getMessage()).build();
        }

        try (InputStream in = new ByteArrayInputStream(bytes)) {
            Bpmn.readModelFromStream(in);
            return ValidateResult.newBuilder().setValid(true).build();
        } catch (Exception e) {
            return ValidateResult.newBuilder().setValid(false).setViolation(BpmnUtil.describe(e)).build();
        }
    }
}
