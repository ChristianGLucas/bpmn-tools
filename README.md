# christiangeorgelucas/bpmn-tools

Composable [Axiom](https://axiomide.com) nodes for **BPMN 2.0** (business process
model) parsing, validation, and structural extraction — tasks, gateways,
events, sequence flows, pools/lanes, and sub-processes. Built for the Axiom
marketplace.

Wraps Camunda's [BPMN Model API](https://github.com/camunda/camunda-bpm-platform/tree/master/model-api/bpmn-model)
(`org.camunda.bpm.model:camunda-bpmn-model` + `camunda-xml-model`, both
Apache-2.0) — the standalone Java model layer Camunda's own process engine is
built on, and the authoritative reference implementation for programmatic
BPMN 2.0 XML handling in the JVM ecosystem. Offline, deterministic, stateless.

## Use it from your agent or app

Every node in this package is a **live, auto-scaling API endpoint** on the
[Axiom](https://axiomide.com) marketplace — call it from an AI agent or your own
code, with nothing to self-host.

**📦 See it on the marketplace:**
https://dev.axiomide.com/marketplace/christiangeorgelucas/bpmn-tools@0.1.0

**Hook it up to an AI agent (MCP).** Add Axiom's hosted MCP server to any MCP
client and every node becomes a typed tool your agent can call — search the
catalog, inspect a schema, and invoke it directly.

```bash
# Claude Code
claude mcp add --transport http axiom https://api.axiomide.com/mcp \
  --header "Authorization: Bearer $AXIOM_API_KEY"
```

Claude Desktop, Cursor, or any config-based client:

```json
{
  "mcpServers": {
    "axiom": {
      "type": "http",
      "url": "https://api.axiomide.com/mcp",
      "headers": { "Authorization": "Bearer YOUR_AXIOM_API_KEY" }
    }
  }
}
```

**Call it from the CLI.**

```bash
axiom invoke christiangeorgelucas/bpmn-tools/Parse --input '{ ... }'
```

**Call it over HTTP.**

```bash
curl -X POST https://api.axiomide.com/invocations/v1/nodes/christiangeorgelucas/bpmn-tools/0.1.0/Parse \
  -H "Authorization: Bearer $AXIOM_API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{ ... }'
```

> Input/output schema for each node is on the marketplace page above, or via
> `axiom inspect node christiangeorgelucas/bpmn-tools/Parse`.

### Get started free

Install the CLI:

```bash
# macOS / Linux — Homebrew
brew install axiomide/tap/axiom

# macOS / Linux — install script
curl -fsSL https://raw.githubusercontent.com/AxiomIDE/axiom-releases/main/install.sh | sh
```

**Windows:** download the `windows/amd64` `.zip` from the
[releases page](https://github.com/AxiomIDE/axiom-releases/releases), unzip it,
and put `axiom.exe` on your `PATH`.

Then `axiom version` to verify, `axiom login` (GitHub or Google) to authenticate,
and create an API key under **Console → API Keys**. Docs and sign-up at
**[axiomide.com](https://axiomide.com)**.

## Nodes

- **Parse** — root `definitions` metadata (id, target namespace, exporter)
  plus every top-level `process` (id, name, `isExecutable`). The cheap
  triage node to run first.
- **Validate** — checks a document against the official BPMN 2.0 XSD schema.
- **ListTasks** — every task-family activity (Task, UserTask, ServiceTask,
  ScriptTask, SendTask, ReceiveTask, ManualTask, BusinessRuleTask).
- **ListGateways** — every gateway (Exclusive, Parallel, Inclusive,
  EventBased, Complex), with its `gatewayDirection`.
- **ListEvents** — every start/end/intermediate/boundary event, including its
  event-definition kind (message/timer/signal/error/escalation/compensate/
  conditional/link/terminate/cancel).
- **ListSequenceFlows** — every sequence flow: source, target, condition
  expression.
- **ListPoolsAndLanes** — every pool (`participant`) and its lanes, with the
  flow nodes assigned to each lane.
- **ListSubProcesses** — every embedded sub-process and call activity.
- **GetElementById** — look up a single element by id; returns its type,
  name, and (for flow nodes) incoming/outgoing sequence flow ids.
- **ToGraph** — re-expresses a process's flow nodes and sequence flows as a
  directed graph, field-compatible with
  [`graph-tools`](https://github.com/ChristianGLucas/graph-tools)' `Graph`
  envelope, for downstream algorithms (topological sort, cycle detection,
  centrality, ...).

## Security

Parsing goes through Camunda's own parser, which is hardened against XXE by
default (verified directly against the upstream source,
`AbstractModelParser#protectAgainstXxeAttacks`): DOCTYPE declarations are
disallowed outright, external general/parameter entities are disabled, and
XInclude is disabled. Input is capped at 5 MiB (checked on the raw bytes
before any parsing is attempted).

## Bounds

`xml` is capped at 5 MiB. Oversized, blank, or malformed input returns a
structured `error` field rather than crashing.

## License

MIT — see [LICENSE](./LICENSE). Camunda's `camunda-bpmn-model` and
`camunda-xml-model` are Apache-2.0 with zero required runtime dependencies
beyond the JDK (verified by resolving their Maven POMs directly). None of it
is copyleft.
