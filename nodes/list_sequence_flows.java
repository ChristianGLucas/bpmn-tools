package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.SequenceFlowInfo;
import gen.Messages.SequenceFlowListResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.Map;

public class ListSequenceFlows {

    /**
     * Extract every sequence flow — source, target, and condition
     * expression (when present) — from a BPMN 2.0 document, across every
     * top-level process: the process's edge list. Blank input, malformed
     * XML, or input that fails BPMN 2.0 schema validation all return a
     * structured {@code error} instead of crashing.
     *
     * <p>{@code ax} is the AxiomContext (ADR-001): every platform capability is
     * reached through it — {@code ax.log()}, {@code ax.secrets()},
     * {@code ax.reflection()}, {@code ax.mutation()}. Node
     * code never talks to the platform directly.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The decoded BpmnDocument for this invocation.
     */
    public static SequenceFlowListResult listSequenceFlows(AxiomContext ax, BpmnDocument input) {
        ax.log().info("listSequenceFlows handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return SequenceFlowListResult.newBuilder().setError(f.getMessage()).build();
        }

        SequenceFlowListResult.Builder result = SequenceFlowListResult.newBuilder();
        for (SequenceFlow flow : model.getModelElementsByType(SequenceFlow.class)) {
            ConditionExpression condition = flow.getConditionExpression();
            result.addFlows(SequenceFlowInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(flow.getId()))
                    .setName(BpmnUtil.orEmpty(flow.getName()))
                    .setSourceRef(flow.getSource() == null ? "" : BpmnUtil.orEmpty(flow.getSource().getId()))
                    .setTargetRef(flow.getTarget() == null ? "" : BpmnUtil.orEmpty(flow.getTarget().getId()))
                    .setConditionExpression(condition == null ? "" : BpmnUtil.orEmpty(condition.getTextContent()))
                    .setProcessId(BpmnUtil.enclosingProcessId(flow))
                    .build());
        }
        return result.build();
    }
}
