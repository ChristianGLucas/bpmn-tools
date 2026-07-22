package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.ParseResult;
import gen.Messages.ProcessInfo;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.util.Map;

public class Parse {

    /**
     * Parse a BPMN 2.0 XML document and extract its root {@code definitions}
     * element's metadata (id, target namespace, exporter, exporter version)
     * and the list of top-level {@code process} elements it defines (each
     * with its own id, name, and {@code isExecutable} flag) — the cheap
     * triage node to run before a more targeted extraction (ListTasks,
     * ListGateways, etc.). Parsing goes through Camunda's BPMN Model API,
     * which enforces the BPMN 2.0 XSD schema and is hardened against XXE.
     * Blank input, oversized input (over 5 MiB), input that is not
     * well-formed XML, or input that does not conform to the BPMN 2.0 schema
     * all return a structured {@code error} instead of crashing.
     *
     * <p>{@code ax} is the AxiomContext (ADR-001): every platform capability is
     * reached through it — {@code ax.log()}, {@code ax.secrets()},
     * {@code ax.reflection()}, {@code ax.mutation()}. Node
     * code never talks to the platform directly.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The decoded BpmnDocument for this invocation.
     */
    public static ParseResult parse(AxiomContext ax, BpmnDocument input) {
        ax.log().info("parse handling", Map.of());

        BpmnModelInstance model;
        try {
            model = BpmnUtil.parse(input.getXml());
        } catch (BpmnUtil.Failure f) {
            return ParseResult.newBuilder().setError(f.getMessage()).build();
        }

        Definitions defs = model.getDefinitions();
        ParseResult.Builder result = ParseResult.newBuilder()
                .setDefinitionsId(BpmnUtil.orEmpty(defs.getId()))
                .setTargetNamespace(BpmnUtil.orEmpty(defs.getTargetNamespace()))
                .setExporter(BpmnUtil.orEmpty(defs.getExporter()))
                .setExporterVersion(BpmnUtil.orEmpty(defs.getExporterVersion()));

        for (Process p : model.getModelElementsByType(Process.class)) {
            result.addProcesses(ProcessInfo.newBuilder()
                    .setId(BpmnUtil.orEmpty(p.getId()))
                    .setName(BpmnUtil.orEmpty(p.getName()))
                    .setIsExecutable(p.isExecutable())
                    .build());
        }

        return result.build();
    }
}
