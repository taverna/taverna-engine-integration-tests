package net.sf.taverna.t2.lineageService.capture.test;


import static org.junit.Assert.assertTrue;
import net.sf.taverna.t2.invocation.WorkflowDataToken;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.Dataflow;
import net.sf.taverna.t2.workflowmodel.DataflowInputPort;

import org.junit.Test;


/**
 * @author Paolo Missier
 * 
 */
public class ProvenanceCaptureTest_test9 extends ProvenanceCaptureTestHelper {

	@Test
	public void testProvenanceCapture_test9() throws Exception {

		ProvenanceCaptureTestHelper helper = this;

		Dataflow dataflow = helper.setup("ProvenanceCaptureTestWithInput");

		String i1 = "a";
		String i2 = "b";

		T2Reference entityId1 = context.getReferenceService().register(i1, 0, true, context);

		T2Reference entityId2 = context.getReferenceService().register(i2, 0, true, context);

		// provide inputs if necessary
		for (DataflowInputPort port : dataflow.getInputPorts()) {

			if (port.getName().equals("I1")) {
				WorkflowDataToken inputToken1 = new WorkflowDataToken("",new int[]{}, entityId1, context);
				helper.getFacade().pushData(inputToken1, port.getName());
			}

			else if (port.getName().equals("I2")) {
				WorkflowDataToken inputToken2 = new WorkflowDataToken("",new int[]{}, entityId2, context);
				helper.getFacade().pushData(inputToken2, port.getName());
			}
		}

		helper.waitForCompletion();

		assertTrue("ok as long as we got this far", true);

	}


}


