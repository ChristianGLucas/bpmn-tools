package nodes;

import axiom.AxiomContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared test fixtures for christiangeorgelucas/bpmn-tools node tests:
 * a no-op AxiomContext, and BPMN 2.0 XML documents exercising every element
 * kind this package extracts. Centralized here so every node test asserts
 * against the SAME known structure instead of each inventing its own
 * (and drifting).
 *
 * <p>Not a node itself — no Axiom annotations, never registered as an
 * endpoint.
 */
final class TestSupport {

    private TestSupport() {}

    // A no-op AxiomContext a node author edits to drive a specific scenario.
    // Reflection exposes an empty graph, mutation is a sink. Implement only
    // what your assertions need.
    static final class TestContext implements AxiomContext {
        public Logger log() {
            return new Logger() {
                public void debug(String m, Map<String, String> a) {}
                public void info(String m, Map<String, String> a)  {}
                public void warn(String m, Map<String, String> a)  {}
                public void error(String m, Map<String, String> a) {}
            };
        }
        public Secrets secrets() { return name -> Optional.empty(); }
        public String executionId() { return "test-execution-id"; }
        public String flowId() { return "test-flow-id"; }
        public String tenantId() { return "test-tenant-id"; }
        public Reflection reflection() {
            return () -> new FlowReflection() {
                public List<ReflectionNode> nodes() { return List.of(); }
                public List<ReflectionEdge> edges() { return List.of(); }
                public List<ReflectionEdge> loopEdges() { return List.of(); }
                public FlowPosition position() { return new FlowPosition(0, 0, Map.of(), List.of()); }
                public String graphId() { return ""; }
            };
        }
        public Mutation mutation() {
            return () -> new FlowMutation() {
                public int addNode(String pkg, String ver, CanvasPosition pos) { return 0; }
                public void addEdge(int src, int dst, EdgeCondition cond) {}
            };
        }
    }

    static AxiomContext ax() {
        return new TestContext();
    }

    /**
     * A hand-built, schema-valid BPMN 2.0 document: an "OrderProcess" (one
     * pool "Order Department" with two lanes) —
     * StartEvent_1 -&gt; Task_CheckAvailability (userTask) -&gt;
     * Gateway_Available (exclusiveGateway, diverging) -&gt;
     * [Flow_3 "available == true"] -&gt; Task_ShipItems (serviceTask, with a
     * BoundaryEvent_Timeout timer boundary event attached) -&gt;
     * CallActivity_Invoice (calls InvoiceProcess) -&gt; EndEvent_Shipped, OR
     * [Flow_4 "available == false"] -&gt; EndEvent_Cancelled. Task_ShipItems
     * also contains a nested SubProcess_Fulfillment (StartEvent_Sub -&gt;
     * Task_Notify -&gt; EndEvent_Sub) — plus a second, non-executable
     * top-level process "InvoiceProcess" (InvoiceStart -&gt; InvoiceTask -&gt;
     * InvoiceEnd) with no pool/lanes of its own.
     *
     * <p>EVERY count below is a hand-computed independent oracle: read
     * directly off this literal XML text, not derived from running any of
     * this package's own code.
     *
     * <p>Tasks (7 total): Task_CheckAvailability (userTask),
     * Task_ShipItems (serviceTask), Task_Notify (task, inside the
     * sub-process), InvoiceTask (task, in the second process) = 4 in the
     * outer count used by ListTasks (which also includes the 3 nested
     * ones: Task_Notify is the only nested plain task; total distinct Task
     * interface instances = 4: Task_CheckAvailability, Task_ShipItems,
     * Task_Notify, InvoiceTask).
     *
     * <p>Gateways (1): Gateway_Available (exclusiveGateway, Diverging).
     *
     * <p>Events (9): StartEvent_1, BoundaryEvent_Timeout (timer),
     * EndEvent_Shipped, EndEvent_Cancelled, StartEvent_Sub, EndEvent_Sub,
     * InvoiceStart, InvoiceEnd = 8 total flow events (StartEvent_1 has no
     * event definition — a plain "none" start event).
     *
     * <p>Sequence flows (10): Flow_1..Flow_6 (OrderProcess),
     * Flow_Sub1, Flow_Sub2 (SubProcess_Fulfillment),
     * InvoiceFlow1, InvoiceFlow2 (InvoiceProcess) = 10 total.
     *
     * <p>Pools (1): Participant_Orders -&gt; OrderProcess, with 2 lanes
     * (Lane_Sales: 3 flow node refs; Lane_Warehouse: 5 flow node refs).
     *
     * <p>Sub-processes/call activities (2): SubProcess_Fulfillment
     * (subProcess), CallActivity_Invoice (callActivity, calledElement =
     * InvoiceProcess).
     */
    static final String VALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
            + "                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "                   id=\"Definitions_1\"\n"
            + "                   targetNamespace=\"http://bpmn-tools.axiom.dev/test\"\n"
            + "                   exporter=\"bpmn-tools-test-fixture\"\n"
            + "                   exporterVersion=\"1.0\">\n"
            + "  <bpmn:collaboration id=\"Collaboration_1\">\n"
            + "    <bpmn:participant id=\"Participant_Orders\" name=\"Order Department\" processRef=\"OrderProcess\"/>\n"
            + "  </bpmn:collaboration>\n"
            + "  <bpmn:process id=\"OrderProcess\" name=\"Order Process\" isExecutable=\"true\">\n"
            + "    <bpmn:laneSet id=\"LaneSet_1\">\n"
            + "      <bpmn:lane id=\"Lane_Sales\" name=\"Sales\">\n"
            + "        <bpmn:flowNodeRef>StartEvent_1</bpmn:flowNodeRef>\n"
            + "        <bpmn:flowNodeRef>Task_CheckAvailability</bpmn:flowNodeRef>\n"
            + "        <bpmn:flowNodeRef>Gateway_Available</bpmn:flowNodeRef>\n"
            + "      </bpmn:lane>\n"
            + "      <bpmn:lane id=\"Lane_Warehouse\" name=\"Warehouse\">\n"
            + "        <bpmn:flowNodeRef>Task_ShipItems</bpmn:flowNodeRef>\n"
            + "        <bpmn:flowNodeRef>BoundaryEvent_Timeout</bpmn:flowNodeRef>\n"
            + "        <bpmn:flowNodeRef>SubProcess_Fulfillment</bpmn:flowNodeRef>\n"
            + "        <bpmn:flowNodeRef>CallActivity_Invoice</bpmn:flowNodeRef>\n"
            + "        <bpmn:flowNodeRef>EndEvent_Shipped</bpmn:flowNodeRef>\n"
            + "      </bpmn:lane>\n"
            + "    </bpmn:laneSet>\n"
            + "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Order Received\"/>\n"
            + "    <bpmn:userTask id=\"Task_CheckAvailability\" name=\"Check Availability\"/>\n"
            + "    <bpmn:exclusiveGateway id=\"Gateway_Available\" name=\"Available?\" gatewayDirection=\"Diverging\"/>\n"
            + "    <bpmn:serviceTask id=\"Task_ShipItems\" name=\"Ship Items\"/>\n"
            + "    <bpmn:boundaryEvent id=\"BoundaryEvent_Timeout\" name=\"Shipping Timeout\" attachedToRef=\"Task_ShipItems\" cancelActivity=\"true\">\n"
            + "      <bpmn:timerEventDefinition id=\"TimerEventDefinition_1\"/>\n"
            + "    </bpmn:boundaryEvent>\n"
            + "    <bpmn:subProcess id=\"SubProcess_Fulfillment\" name=\"Fulfillment Follow-up\">\n"
            + "      <bpmn:startEvent id=\"StartEvent_Sub\" name=\"Fulfillment Started\"/>\n"
            + "      <bpmn:task id=\"Task_Notify\" name=\"Notify Customer\"/>\n"
            + "      <bpmn:endEvent id=\"EndEvent_Sub\" name=\"Fulfillment Done\"/>\n"
            + "      <bpmn:sequenceFlow id=\"Flow_Sub1\" sourceRef=\"StartEvent_Sub\" targetRef=\"Task_Notify\"/>\n"
            + "      <bpmn:sequenceFlow id=\"Flow_Sub2\" sourceRef=\"Task_Notify\" targetRef=\"EndEvent_Sub\"/>\n"
            + "    </bpmn:subProcess>\n"
            + "    <bpmn:callActivity id=\"CallActivity_Invoice\" name=\"Generate Invoice\" calledElement=\"InvoiceProcess\"/>\n"
            + "    <bpmn:endEvent id=\"EndEvent_Shipped\" name=\"Order Shipped\"/>\n"
            + "    <bpmn:endEvent id=\"EndEvent_Cancelled\" name=\"Order Cancelled\"/>\n"
            + "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_CheckAvailability\"/>\n"
            + "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_CheckAvailability\" targetRef=\"Gateway_Available\"/>\n"
            + "    <bpmn:sequenceFlow id=\"Flow_3\" name=\"Yes\" sourceRef=\"Gateway_Available\" targetRef=\"Task_ShipItems\">\n"
            + "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${available == true}</bpmn:conditionExpression>\n"
            + "    </bpmn:sequenceFlow>\n"
            + "    <bpmn:sequenceFlow id=\"Flow_4\" name=\"No\" sourceRef=\"Gateway_Available\" targetRef=\"EndEvent_Cancelled\">\n"
            + "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${available == false}</bpmn:conditionExpression>\n"
            + "    </bpmn:sequenceFlow>\n"
            + "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_ShipItems\" targetRef=\"CallActivity_Invoice\"/>\n"
            + "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"CallActivity_Invoice\" targetRef=\"EndEvent_Shipped\"/>\n"
            + "  </bpmn:process>\n"
            + "  <bpmn:process id=\"InvoiceProcess\" name=\"Invoice Process\" isExecutable=\"false\">\n"
            + "    <bpmn:startEvent id=\"InvoiceStart\"/>\n"
            + "    <bpmn:task id=\"InvoiceTask\" name=\"Create Invoice\"/>\n"
            + "    <bpmn:endEvent id=\"InvoiceEnd\"/>\n"
            + "    <bpmn:sequenceFlow id=\"InvoiceFlow1\" sourceRef=\"InvoiceStart\" targetRef=\"InvoiceTask\"/>\n"
            + "    <bpmn:sequenceFlow id=\"InvoiceFlow2\" sourceRef=\"InvoiceTask\" targetRef=\"InvoiceEnd\"/>\n"
            + "  </bpmn:process>\n"
            + "</bpmn:definitions>\n";

    /**
     * A minimal, schema-valid BPMN 2.0 document with a single process and no
     * {@code collaboration}/{@code participant} at all — the common,
     * legitimate "no pools" case.
     */
    static final String BARE_PROCESS_XML =
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
            + "                   id=\"Definitions_Bare\" targetNamespace=\"http://bpmn-tools.axiom.dev/test\">\n"
            + "  <bpmn:process id=\"BareProcess\" name=\"Bare Process\" isExecutable=\"true\">\n"
            + "    <bpmn:startEvent id=\"BareStart\"/>\n"
            + "    <bpmn:task id=\"BareTask\" name=\"Do Work\"/>\n"
            + "    <bpmn:endEvent id=\"BareEnd\"/>\n"
            + "    <bpmn:sequenceFlow id=\"BareFlow1\" sourceRef=\"BareStart\" targetRef=\"BareTask\"/>\n"
            + "    <bpmn:sequenceFlow id=\"BareFlow2\" sourceRef=\"BareTask\" targetRef=\"BareEnd\"/>\n"
            + "  </bpmn:process>\n"
            + "</bpmn:definitions>\n";

    /** Not even well-formed XML (an unclosed tag). */
    static final String MALFORMED_XML = "<bpmn:definitions><this-is-not-closed>";

    /**
     * Well-formed XML, but not valid BPMN 2.0: {@code sequenceFlow} omits
     * both {@code sourceRef} and {@code targetRef}, which the BPMN 2.0 XSD
     * declares as {@code use="required"} attributes of {@code tSequenceFlow}.
     */
    static final String SCHEMA_INVALID_XML =
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
            + "                   id=\"D1\" targetNamespace=\"http://bpmn-tools.axiom.dev/test\">\n"
            + "  <bpmn:process id=\"P1\" isExecutable=\"true\">\n"
            + "    <bpmn:startEvent id=\"S1\"/>\n"
            + "    <bpmn:endEvent id=\"E1\"/>\n"
            + "    <bpmn:sequenceFlow id=\"F1\"/>\n"
            + "  </bpmn:process>\n"
            + "</bpmn:definitions>\n";

    /** A syntactically well-formed but oversized document — bigger than the
     * package's 5 MiB input cap — used to prove the size check runs BEFORE
     * any parsing is attempted. */
    static String oversizedXml() {
        StringBuilder sb = new StringBuilder(BpmnUtil.MAX_XML_BYTES + 1024);
        sb.append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" id=\"D1\" targetNamespace=\"ns\">\n");
        sb.append("<!-- ");
        while (sb.length() < BpmnUtil.MAX_XML_BYTES + 512) {
            sb.append("padding-to-exceed-the-five-mebibyte-input-cap-");
        }
        sb.append(" -->\n</bpmn:definitions>\n");
        return sb.toString();
    }
}
