package net.sf.taverna.t2.lineageService.capture.test;


import static org.junit.Assert.assertTrue;
import net.sf.taverna.t2.invocation.WorkflowDataToken;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.Dataflow;
import net.sf.taverna.t2.workflowmodel.DataflowInputPort;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Paolo Missier
 * 
 */
public class ProvenanceCaptureTest_test8 extends ProvenanceCaptureTestHelper {

	@Test
	public void testProvenanceCapture_test8() throws Exception {
		
		ProvenanceCaptureTestHelper helper = this;

		Dataflow dataflow = helper.setup("ProvenanceCaptureTestWithInput");

		String i1 = "a";
		T2Reference entityId1 = context.getReferenceService().register(i1, 0, true, context);

		// provide inputs if necessary
		for (DataflowInputPort port : dataflow.getInputPorts()) {

			if (port.getName().equals("I1")) {
				WorkflowDataToken inputToken1 = new WorkflowDataToken("",new int[]{}, entityId1, context);
				helper.getFacade().pushData(inputToken1, port.getName());
			}

		}

			helper.waitForCompletion();
		
		assertTrue("ok as long as we got this far", true);

	}
	
	
}


