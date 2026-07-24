package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ProcessGraphEdge;
import gen.Messages.ProcessGraphNode;
import gen.Messages.ProcessGraphResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.Map;

public class ToGraph {

    /**
     * Re-express a BPMN process's flow nodes (tasks, gateways, events,
     * sub-processes, call activities) and sequence flows as a directed
     * graph, across every top-level process in the document. The
     * {@code directed}/{@code nodes}/{@code edges} shape and the
     * {@code ProcessGraphNode}/{@code ProcessGraphEdge} field names
     * deliberately mirror christiangeorgelucas/graph-tools' {@code Graph}
     * envelope field-for-field, so mapping this result into
     * TopologicalSort (execution order), DetectCycle (find a loop),
     * Describe (summary stats), or Centrality (busiest step) inside a flow
     * is a mechanical field rename. {@code nodes} order is grouped by
     * concrete BPMN element type (a consequence of the underlying model
     * API's polymorphic type query), NOT document order — a graph has no
     * inherent node ordering, so this is never a correctness concern, but
     * don't rely on it matching {@code ListTasks}/{@code ListGateways}/
     * {@code ListEvents}'s own (also type-grouped, not document-order)
     * ordering either. Blank input, malformed
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
    public static ProcessGraphResult toGraph(AxiomContext ax, BpmnDocument input) {
        ax.log().info("toGraph handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return ProcessGraphResult.newBuilder().setError(f.getMessage()).build();
        }

        ProcessGraphResult.Builder result = ProcessGraphResult.newBuilder().setDirected(true);

        // FlowNode.class is queried ONCE: it is the common ancestor of every
        // task, gateway, event, sub-process, and call activity, and
        // getModelElementsByType is polymorphic (see ListTasks), so this
        // single query already covers every vertex kind with no
        // double-counting.
        for (FlowNode node : model.getModelElementsByType(FlowNode.class)) {
            String id = BpmnUtil.orEmpty(node.getId());
            String name = BpmnUtil.orEmpty(node.getName());
            result.addNodes(ProcessGraphNode.newBuilder()
                    .setId(id)
                    .setLabel(name.isEmpty() ? id : name)
                    .build());
        }

        for (SequenceFlow flow : model.getModelElementsByType(SequenceFlow.class)) {
            if (flow.getSource() == null || flow.getTarget() == null) {
                continue;
            }
            result.addEdges(ProcessGraphEdge.newBuilder()
                    .setFrom(BpmnUtil.orEmpty(flow.getSource().getId()))
                    .setTo(BpmnUtil.orEmpty(flow.getTarget().getId()))
                    .setWeight(1.0)
                    .setExplicitZeroWeight(false)
                    .build());
        }

        return result.build();
    }
}
