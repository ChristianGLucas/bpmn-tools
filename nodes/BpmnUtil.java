package nodes;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.CancelEventDefinition;
import org.camunda.bpm.model.bpmn.instance.CatchEvent;
import org.camunda.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ConditionalEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.camunda.bpm.model.bpmn.instance.EscalationEventDefinition;
import org.camunda.bpm.model.bpmn.instance.EventDefinition;
import org.camunda.bpm.model.bpmn.instance.LinkEventDefinition;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SignalEventDefinition;
import org.camunda.bpm.model.bpmn.instance.TerminateEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ThrowEvent;
import org.camunda.bpm.model.bpmn.instance.TimerEventDefinition;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Shared helpers for christiangeorgelucas/bpmn-tools nodes: input-size
 * enforcement, BPMN parsing through Camunda's BPMN Model API, and small
 * structural queries every node needs (enclosing process id, event
 * definition kind). Centralized here so every node enforces the same size
 * cap and error contract instead of drifting.
 *
 * <p>Not a node itself — no Axiom annotations, never registered as an
 * endpoint.
 */
final class BpmnUtil {

    private BpmnUtil() {}

    /**
     * A request-level failure: blank/oversized {@code xml}, XML that is not
     * even well-formed, or a BPMN 2.0 schema violation. Every node catches
     * this and converts it into its own structured {@code error} field
     * instead of letting it propagate as a crash.
     */
    static final class Failure extends RuntimeException {
        Failure(String message) {
            super(message);
        }
    }

    /**
     * Validates the blank contract on raw {@code xml} text and returns its
     * UTF-8 bytes, or throws {@link Failure}. Payload size is bounded by the
     * platform, not by node code.
     */
    static byte[] requireSizedXml(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new Failure("xml is required");
        }
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parses {@code xml} into a {@link BpmnModelInstance}, or throws
     * {@link Failure} with a human-readable message. Parsing goes through
     * Camunda's own parser, which is XXE-hardened (DOCTYPE disallowed,
     * external general/parameter entities disabled, XInclude disabled — see
     * {@code AbstractModelParser.protectAgainstXxeAttacks}, verified
     * directly against the upstream source) and enforces the BPMN 2.0 XSD
     * schema; any schema violation surfaces as a {@link Failure} here just
     * like a malformed-XML input does.
     */
    static BpmnModelInstance parse(String xml) {
        byte[] bytes = requireSizedXml(xml);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return Bpmn.readModelFromStream(in);
        } catch (Exception e) {
            throw new Failure(describe(e));
        }
    }

    /**
     * A human-readable message for any exception thrown while parsing or
     * validating. Prefers the CAUSE's message when present: Camunda's own
     * {@code ModelParseException} wraps the actual XML-syntax or
     * schema-violation detail (e.g. "cvc-complex-type.4: Attribute
     * 'sourceRef' must appear...") inside its cause, while the exception's
     * own top-level message is a generic, unhelpful "SAXException while
     * parsing input stream" (verified directly against the library in a
     * standalone scratch run). Falls back to the exception's own message,
     * then to its simple class name, for anything that carries neither.
     */
    static String describe(Throwable e) {
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        return e.getClass().getSimpleName();
    }

    static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Walks up the element tree to find the nearest enclosing top-level
     * {@code process} element — skipping over any intervening
     * subProcess/callActivity container, so a flow node nested inside a
     * sub-process still reports its top-level process's id, never the
     * sub-process's own id. Returns {@code ""} if the element is not inside
     * any process (e.g. a pool with no attached process).
     */
    static String enclosingProcessId(ModelElementInstance element) {
        ModelElementInstance cur = element.getParentElement();
        while (cur != null) {
            if (cur instanceof Process) {
                return orEmpty(((Process) cur).getId());
            }
            cur = cur.getParentElement();
        }
        return "";
    }

    /**
     * The event definitions attached to any {@code Event} (a {@code
     * CatchEvent} or {@code ThrowEvent}); empty for anything else, including
     * a plain ("none") event with no event definition.
     */
    static Collection<EventDefinition> eventDefinitionsOf(Object event) {
        if (event instanceof CatchEvent) {
            return ((CatchEvent) event).getEventDefinitions();
        }
        if (event instanceof ThrowEvent) {
            return ((ThrowEvent) event).getEventDefinitions();
        }
        return Collections.emptyList();
    }

    /**
     * Names the kind of the FIRST event definition in {@code defs}
     * ("message", "timer", "signal", "error", "escalation", "compensate",
     * "conditional", "link", "terminate", or "cancel"), or {@code ""} when
     * empty (a plain "none" event). BPMN 2.0 permits more than one
     * eventDefinition on a single event only for a narrow set of
     * combinations that are vanishingly rare in practice; this reports the
     * first, in document order.
     */
    static String eventDefinitionType(Collection<EventDefinition> defs) {
        if (defs == null || defs.isEmpty()) {
            return "";
        }
        Iterator<EventDefinition> it = defs.iterator();
        EventDefinition def = it.next();
        if (def instanceof MessageEventDefinition) return "message";
        if (def instanceof TimerEventDefinition) return "timer";
        if (def instanceof SignalEventDefinition) return "signal";
        if (def instanceof ErrorEventDefinition) return "error";
        if (def instanceof EscalationEventDefinition) return "escalation";
        if (def instanceof CompensateEventDefinition) return "compensate";
        if (def instanceof ConditionalEventDefinition) return "conditional";
        if (def instanceof LinkEventDefinition) return "link";
        if (def instanceof TerminateEventDefinition) return "terminate";
        if (def instanceof CancelEventDefinition) return "cancel";
        return "";
    }
}
