package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.EventInfo;
import gen.Messages.EventListResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.util.Map;

public class ListEvents {

    /**
     * Extract every flow event — start, end, intermediate catch/throw, or
     * boundary — from a BPMN 2.0 document, across every top-level process.
     * {@code category} names the specific BPMN element (e.g. "startEvent");
     * {@code event_definition_type} names the FIRST attached event
     * definition's kind ("message", "timer", "signal", "error",
     * "escalation", "compensate", "conditional", "link", "terminate",
     * "cancel", or "" for a plain "none" event). {@code interrupting} is
     * meaningful only for a startEvent/boundaryEvent; {@code
     * attached_to_ref} is set only for a boundaryEvent. Blank input,
     * oversized input (over 5 MiB), malformed XML, or input that fails BPMN
     * 2.0 schema validation all return a structured {@code error} instead of
     * crashing.
     *
     * <p>{@code ax} is the AxiomContext (ADR-001): every platform capability is
     * reached through it — {@code ax.log()}, {@code ax.secrets()},
     * {@code ax.reflection()}, {@code ax.mutation()}. Node
     * code never talks to the platform directly.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The decoded BpmnDocument for this invocation.
     */
    public static EventListResult listEvents(AxiomContext ax, BpmnDocument input) {
        ax.log().info("listEvents handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return EventListResult.newBuilder().setError(f.getMessage()).build();
        }

        EventListResult.Builder result = EventListResult.newBuilder();
        // Event.class is queried ONCE — see ListTasks for why querying
        // subtypes too would double-count (getModelElementsByType is
        // polymorphic over the type's extending types: StartEvent, EndEvent,
        // IntermediateCatchEvent, IntermediateThrowEvent, and BoundaryEvent
        // all come back from this single query).
        for (Event event : model.getModelElementsByType(Event.class)) {
            boolean interrupting = true;
            String attachedToRef = "";
            if (event instanceof StartEvent) {
                interrupting = ((StartEvent) event).isInterrupting();
            } else if (event instanceof BoundaryEvent) {
                BoundaryEvent boundary = (BoundaryEvent) event;
                interrupting = boundary.cancelActivity();
                attachedToRef = boundary.getAttachedTo() == null ? "" : BpmnUtil.orEmpty(boundary.getAttachedTo().getId());
            } else {
                // End/intermediate events have no interrupting semantics.
                interrupting = false;
            }

            result.addEvents(EventInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(event.getId()))
                    .setName(BpmnUtil.orEmpty(event.getName()))
                    .setCategory(event.getElementType().getTypeName())
                    .setEventDefinitionType(BpmnUtil.eventDefinitionType(BpmnUtil.eventDefinitionsOf(event)))
                    .setInterrupting(interrupting)
                    .setAttachedToRef(attachedToRef)
                    .setProcessId(BpmnUtil.enclosingProcessId(event))
                    .build());
        }
        return result.build();
    }
}
