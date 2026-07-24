package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.SubProcessInfo;
import gen.Messages.SubProcessListResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.SubProcess;

import java.util.Map;

public class ListSubProcesses {

    /**
     * Extract every embedded sub-process and call activity from a BPMN 2.0
     * document. {@code type} distinguishes a {@code subProcess} (expanded
     * inline) from a {@code callActivity} (a reference to a separately
     * defined, reusable process via {@code called_element}); {@code
     * parent_process_id} names the enclosing top-level process, even when
     * the sub-process is nested inside another sub-process. Blank input,
     * malformed XML, or input that fails BPMN
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
    public static SubProcessListResult listSubProcesses(AxiomContext ax, BpmnDocument input) {
        ax.log().info("listSubProcesses handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return SubProcessListResult.newBuilder().setError(f.getMessage()).build();
        }

        SubProcessListResult.Builder result = SubProcessListResult.newBuilder();

        // SubProcess and CallActivity are SIBLING interfaces (neither extends
        // the other), so two separate queries cannot double-count.
        for (SubProcess sp : model.getModelElementsByType(SubProcess.class)) {
            result.addSubProcesses(SubProcessInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(sp.getId()))
                    .setName(BpmnUtil.orEmpty(sp.getName()))
                    .setType("subProcess")
                    .setTriggeredByEvent(sp.triggeredByEvent())
                    .setParentProcessId(BpmnUtil.enclosingProcessId(sp))
                    .build());
        }
        for (CallActivity ca : model.getModelElementsByType(CallActivity.class)) {
            result.addSubProcesses(SubProcessInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(ca.getId()))
                    .setName(BpmnUtil.orEmpty(ca.getName()))
                    .setType("callActivity")
                    .setTriggeredByEvent(false)
                    .setCalledElement(BpmnUtil.orEmpty(ca.getCalledElement()))
                    .setParentProcessId(BpmnUtil.enclosingProcessId(ca))
                    .build());
        }
        return result.build();
    }
}
