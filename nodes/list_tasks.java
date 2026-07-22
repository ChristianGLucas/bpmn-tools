package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.TaskInfo;
import gen.Messages.TaskListResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Task;

import java.util.Map;

public class ListTasks {

    /**
     * Extract every task-family activity — {@code Task}, {@code UserTask},
     * {@code ServiceTask}, {@code ScriptTask}, {@code SendTask},
     * {@code ReceiveTask}, {@code ManualTask}, {@code BusinessRuleTask} — from
     * a BPMN 2.0 document, across every top-level process. {@code type}
     * names the specific BPMN element (e.g. "userTask"); {@code process_id}
     * names the enclosing top-level process, even for a task nested inside
     * an embedded sub-process. Blank input, oversized input (over 5 MiB),
     * malformed XML, or input that fails BPMN 2.0 schema validation all
     * return a structured {@code error} instead of crashing.
     *
     * <p>{@code ax} is the AxiomContext (ADR-001): every platform capability is
     * reached through it — {@code ax.log()}, {@code ax.secrets()},
     * {@code ax.reflection()}, {@code ax.mutation()}. Node
     * code never talks to the platform directly.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The decoded BpmnDocument for this invocation.
     */
    public static TaskListResult listTasks(AxiomContext ax, BpmnDocument input) {
        ax.log().info("listTasks handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return TaskListResult.newBuilder().setError(f.getMessage()).build();
        }

        TaskListResult.Builder result = TaskListResult.newBuilder();
        // Task.class is queried ONCE: Camunda's model type registry resolves
        // getModelElementsByType polymorphically (it walks every non-abstract
        // extending type), so this single query already returns every
        // UserTask/ServiceTask/ScriptTask/SendTask/ReceiveTask/ManualTask/
        // BusinessRuleTask/plain-Task instance — querying the subtypes too
        // would double-count them.
        for (Task task : model.getModelElementsByType(Task.class)) {
            result.addTasks(TaskInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(task.getId()))
                    .setName(BpmnUtil.orEmpty(task.getName()))
                    .setType(task.getElementType().getTypeName())
                    .setProcessId(BpmnUtil.enclosingProcessId(task))
                    .build());
        }
        return result.build();
    }
}
