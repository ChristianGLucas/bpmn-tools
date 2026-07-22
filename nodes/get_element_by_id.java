package nodes;

import axiom.AxiomContext;
import gen.Messages.ElementDetail;
import gen.Messages.ElementLookupRequest;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.Map;

public class GetElementById {

    /**
     * Look up a single BPMN element by its {@code id} attribute and return
     * its type, name, and — for a flow node (task, gateway, or event) — the
     * ids of its incoming and outgoing sequence flows. Incoming/outgoing are
     * derived by matching every {@code sequenceFlow}'s {@code sourceRef}/
     * {@code targetRef} against the requested element, NOT by reading the
     * BPMN 2.0 schema's optional {@code <incoming>}/{@code <outgoing>}
     * bookkeeping child elements — those are frequently absent from
     * hand-written or programmatically generated BPMN (they are redundant
     * with sourceRef/targetRef and BPMN-schema-optional), and a lookup that
     * depended on them would silently under-report for any document that
     * omits them. {@code found = false} (with {@code error} empty) is a
     * legitimate result: no element with that id exists in the document.
     * Blank input, oversized input (over 5 MiB), malformed XML, input that
     * fails BPMN 2.0 schema validation, or a blank {@code element_id} all
     * return a structured {@code error} instead of crashing.
     *
     * <p>{@code ax} is the AxiomContext (ADR-001): every platform capability is
     * reached through it — {@code ax.log()}, {@code ax.secrets()},
     * {@code ax.reflection()}, {@code ax.mutation()}. Node
     * code never talks to the platform directly.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The decoded ElementLookupRequest for this invocation.
     */
    public static ElementDetail getElementById(AxiomContext ax, ElementLookupRequest input) {
        ax.log().info("getElementById handling", Map.of());

        String elementId = input.getElementId();
        if (elementId == null || elementId.isBlank()) {
            return ElementDetail.newBuilder().setError("element_id is required").build();
        }

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getDocument().getXml());
        } catch (BpmnUtil.Failure f) {
            return ElementDetail.newBuilder().setError(f.getMessage()).build();
        }

        ModelElementInstance element = model.getModelElementById(elementId);
        if (element == null) {
            return ElementDetail.newBuilder().setFound(false).build();
        }

        ElementDetail.Builder result = ElementDetail.newBuilder()
                .setFound(true)
                .setId(BpmnUtil.orEmpty(element.getAttributeValue("id")))
                .setName(BpmnUtil.orEmpty(element.getAttributeValue("name")))
                .setElementType(element.getElementType().getTypeName());

        if (element instanceof FlowNode) {
            for (SequenceFlow flow : model.getModelElementsByType(SequenceFlow.class)) {
                if (flow.getTarget() != null && elementId.equals(flow.getTarget().getId())) {
                    result.addIncoming(BpmnUtil.orEmpty(flow.getId()));
                }
                if (flow.getSource() != null && elementId.equals(flow.getSource().getId())) {
                    result.addOutgoing(BpmnUtil.orEmpty(flow.getId()));
                }
            }
        }

        return result.build();
    }
}
