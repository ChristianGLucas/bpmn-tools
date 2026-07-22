package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.CollaborationResult;
import gen.Messages.LaneInfo;
import gen.Messages.PoolInfo;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.LaneSet;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.util.Map;

public class ListPoolsAndLanes {

    /**
     * Extract every pool (BPMN {@code participant}) and its lanes, with the
     * flow nodes assigned to each lane, from a BPMN 2.0 collaboration
     * diagram. An empty {@code pools} list is normal, not an error — a bare,
     * single-process BPMN document with no {@code collaboration} element has
     * no pools at all. Blank input, oversized input (over 5 MiB), malformed
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
    public static CollaborationResult listPoolsAndLanes(AxiomContext ax, BpmnDocument input) {
        ax.log().info("listPoolsAndLanes handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return CollaborationResult.newBuilder().setError(f.getMessage()).build();
        }

        CollaborationResult.Builder result = CollaborationResult.newBuilder();
        for (Participant participant : model.getModelElementsByType(Participant.class)) {
            PoolInfo.Builder pool = PoolInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(participant.getId()))
                    .setName(BpmnUtil.orEmpty(participant.getName()));

            Process process = participant.getProcess();
            if (process != null) {
                pool.setProcessRef(BpmnUtil.orEmpty(process.getId()));
                for (LaneSet laneSet : process.getLaneSets()) {
                    for (Lane lane : laneSet.getLanes()) {
                        LaneInfo.Builder laneInfo = LaneInfo.newBuilder()
                                .setId(BpmnUtil.orEmpty(lane.getId()))
                                .setName(BpmnUtil.orEmpty(lane.getName()));
                        for (FlowNode ref : lane.getFlowNodeRefs()) {
                            laneInfo.addFlowNodeRefs(BpmnUtil.orEmpty(ref.getId()));
                        }
                        pool.addLanes(laneInfo.build());
                    }
                }
            }
            result.addPools(pool.build());
        }
        return result.build();
    }
}
