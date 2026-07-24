package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.GatewayInfo;
import gen.Messages.GatewayListResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.GatewayDirection;
import org.camunda.bpm.model.bpmn.instance.Gateway;

import java.util.Map;

public class ListGateways {

    /**
     * Extract every gateway — Exclusive, Parallel, Inclusive, EventBased, or
     * Complex — from a BPMN 2.0 document, across every top-level process.
     * {@code type} names the specific BPMN element (e.g.
     * "exclusiveGateway"); {@code gateway_direction} is the BPMN
     * {@code gatewayDirection} attribute ("Unspecified", "Converging",
     * "Diverging", or "Mixed"). Blank input,
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
    public static GatewayListResult listGateways(AxiomContext ax, BpmnDocument input) {
        ax.log().info("listGateways handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return GatewayListResult.newBuilder().setError(f.getMessage()).build();
        }

        GatewayListResult.Builder result = GatewayListResult.newBuilder();
        // Gateway.class is queried ONCE — see ListTasks for why querying
        // subtypes too would double-count (getModelElementsByType is
        // polymorphic over the type's extending types).
        for (Gateway gateway : model.getModelElementsByType(Gateway.class)) {
            GatewayDirection direction = gateway.getGatewayDirection();
            result.addGateways(GatewayInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(gateway.getId()))
                    .setName(BpmnUtil.orEmpty(gateway.getName()))
                    .setType(gateway.getElementType().getTypeName())
                    .setGatewayDirection(direction == null ? "Unspecified" : direction.toString())
                    .setProcessId(BpmnUtil.enclosingProcessId(gateway))
                    .build());
        }
        return result.build();
    }
}
