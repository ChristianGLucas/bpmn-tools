package nodes;

import axiom.AxiomContext;
import gen.Messages.BpmnDocument;
import gen.Messages.GatewayInfo;
import gen.Messages.GatewayListResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ListGatewaysTest {

    // INDEPENDENT ORACLE: TestSupport.VALID_XML has exactly one gateway,
    // read directly off its literal XML text — Gateway_Available, an
    // exclusiveGateway with gatewayDirection="Diverging".
    @Test
    public void testListGateways_findsTheSingleGateway() {
        AxiomContext ax = TestSupport.ax();
        GatewayListResult result = ListGateways.listGateways(ax, BpmnDocument.newBuilder().setXml(TestSupport.VALID_XML).build());

        assertEquals("", result.getError());
        assertEquals(1, result.getGatewaysCount());

        GatewayInfo gw = result.getGateways(0);
        assertEquals("Gateway_Available", gw.getId());
        assertEquals("Available?", gw.getName());
        assertEquals("exclusiveGateway", gw.getType());
        assertEquals("Diverging", gw.getGatewayDirection());
        assertEquals("OrderProcess", gw.getProcessId());
    }

    @Test
    public void testListGateways_malformedXmlReturnsStructuredErrorNotCrash() {
        AxiomContext ax = TestSupport.ax();
        GatewayListResult result = ListGateways.listGateways(ax, BpmnDocument.newBuilder().setXml(TestSupport.MALFORMED_XML).build());
        assertFalse(result.getError().isEmpty());
        assertEquals(0, result.getGatewaysCount());
    }
}
