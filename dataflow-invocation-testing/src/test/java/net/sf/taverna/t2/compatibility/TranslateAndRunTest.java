/*******************************************************************************
 * Copyright (C) 2007 The University of Manchester   
 * 
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *    
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *    
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package net.sf.taverna.t2.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.sf.taverna.t2.facade.WorkflowInstanceFacade;
import net.sf.taverna.t2.facade.impl.WorkflowInstanceFacadeImpl;
import net.sf.taverna.t2.invocation.WorkflowDataToken;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.testing.CaptureResultsListener;
import net.sf.taverna.t2.testing.InvocationTestHelper;
import net.sf.taverna.t2.workflowmodel.Dataflow;
import net.sf.taverna.t2.workflowmodel.DataflowInputPort;
import net.sf.taverna.t2.workflowmodel.DataflowOutputPort;
import net.sf.taverna.t2.workflowmodel.DataflowValidationReport;
import net.sf.taverna.t2.workflowmodel.impl.DataflowImpl;
import net.sf.taverna.t2.workflowmodel.impl.EditsImpl;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author David Withers
 * @author Stuart Owen
 * 
 */
public class TranslateAndRunTest extends InvocationTestHelper {

	private static Logger logger = Logger.getLogger(TranslateAndRunTest.class);
	
	@Test
	@Ignore("Fails 50% of the time")
	public void translateAndValidateBiomartAndEMBOSSTest() throws Exception {
		DataflowImpl dataflow = (DataflowImpl) translateScuflFile("ModifiedBiomartAndEMBOSSAnalysis3.xml");
		DataflowValidationReport report = validateDataflow(dataflow);
		assertTrue("Unsatisfied processor found during validation",report.getUnsatisfiedEntities().isEmpty());
		assertTrue("Failed processors found during validation",report.getFailedEntities().isEmpty());
		assertTrue("Unresolved outputs found during validation",report.getUnresolvedOutputs().isEmpty());
		assertTrue("Validation failed",report.isValid());

		WorkflowInstanceFacade facade;
		facade = new EditsImpl().createWorkflowInstanceFacade(dataflow,context,"");
		CaptureResultsListener listener = new CaptureResultsListener(dataflow,context);
		facade.addResultListener(listener);
		
		facade.fire();
		
		waitForCompletion(listener,120);
		
		for (DataflowOutputPort outputPort : dataflow.getOutputPorts()) {
			logger.info("Values for port " + outputPort.getName());
			Object result = listener.getResult(outputPort.getName());
			if (result instanceof List) {
				for (Object element : (List<?>) result) {
					logger.info(element);
				}
			} else {
				logger.info(result);
			}
		}
	}
	
	/**
	 * Tests a simple workflow that contains unbound ports and a port with a default value.
	 * During translation it should remove the unbound ports, and add a String Constant activity upstream
	 * of the port with the default value.
	 */
	@Test
	public void testUnboundPortsAndADefaultValue() throws Exception {
		Dataflow dataflow = translateScuflFile("unbound_ports_with_default.xml");
		DataflowValidationReport report = validateDataflow(dataflow);
		assertTrue("Unsatisfied processor found during validation",report.getUnsatisfiedEntities().size() == 0);
		assertTrue("Failed processors found during validation",report.getFailedEntities().size() == 0);
		assertTrue("Unresolved outputs found during validation",report.getUnresolvedOutputs().size() == 0);
		assertTrue("Validation failed",report.isValid());
		
		WorkflowInstanceFacade facade;
		facade = new EditsImpl().createWorkflowInstanceFacade(dataflow,context,"");
		CaptureResultsListener listener = new CaptureResultsListener(dataflow,context);
		facade.addResultListener(listener);
		
		facade.fire();
		
		waitForCompletion(listener);
		
		assertEquals("The output was incorrect","Some Data",listener.getResult("out"));
	
	}
	
	@Ignore
	@Test
	public void testErrorPropogation() throws Exception {
		Dataflow dataflow = translateScuflFile("test_error_propagation.xml");
		DataflowValidationReport report = validateDataflow(dataflow);
		assertTrue("Unsatisfied processor found during validation",report.getUnsatisfiedEntities().size() == 0);
		assertTrue("Failed processors found during validation",report.getFailedEntities().size() == 0);
		assertTrue("Unresolved outputs found during validation",report.getUnresolvedOutputs().size() == 0);
		assertTrue("Validation failed",report.isValid());
		
		WorkflowInstanceFacade facade;
		facade = new EditsImpl().createWorkflowInstanceFacade(dataflow,context,"");
		CaptureResultsListener listener = new CaptureResultsListener(dataflow,context);
		facade.addResultListener(listener);
		
		facade.fire();
		
		waitForCompletion(listener,5);
		
		assertTrue("The result should be a list",listener.getResult("out") instanceof List);
		
		//TODO: test that the error is passed through.
	}
	
	@Test
	public void testWorkflowContainingWSDL() throws Exception {
		Dataflow dataflow = translateScuflFile("wsdl_test.xml");
		
		DataflowValidationReport report = validateDataflow(dataflow);
		assertTrue("Unsatisfied processor found during validation",report.getUnsatisfiedEntities().size() == 0);
		assertTrue("Failed processors found during validation",report.getFailedEntities().size() == 0);
		assertTrue("Unresolved outputs found during validation",report.getUnresolvedOutputs().size() == 0);
		assertTrue("Validation failed",report.isValid());
		
		WorkflowInstanceFacade facade;
		facade = new EditsImpl().createWorkflowInstanceFacade(dataflow,context,"");
		CaptureResultsListener listener = new CaptureResultsListener(dataflow,context);
		facade.addResultListener(listener);
		
		facade.fire();
		
		waitForCompletion(listener);
		
		assertTrue("The result should be a list",listener.getResult("out") instanceof List);
	}
	
	
	
	@Test
	public void testSimpleWorkflowWithInput() throws Exception {
		Dataflow dataflow = translateScuflFile("simple_workflow_with_input.xml");
		
		DataflowValidationReport report = validateDataflow(dataflow);
		assertTrue("Unsatisfied processor found during validation",report.getUnsatisfiedEntities().size() == 0);
		assertTrue("Failed processors found during validation",report.getFailedEntities().size() == 0);
		assertTrue("Unresolved outputs found during validation",report.getUnresolvedOutputs().size() == 0);
		assertTrue("Validation failed",report.isValid());
		
		List<String> inputs = new ArrayList<String>();
		inputs.add("one");
		inputs.add("two");
		inputs.add("three");
		
		for (String input : inputs) {
			
			WorkflowInstanceFacade facade;
			facade = new WorkflowInstanceFacadeImpl(dataflow,context,"");
			CaptureResultsListener listener = new CaptureResultsListener(dataflow,context);
			facade.addResultListener(listener);
			
			facade.fire();
			
			T2Reference entityId=context.getReferenceService().register(input, 0, true, context);
			for (DataflowInputPort port : dataflow.getInputPorts()) {
				WorkflowDataToken inputToken = new WorkflowDataToken("",new int[]{}, entityId, context);
				facade.pushData(inputToken, port.getName());
			}
			
			waitForCompletion(listener);
			
			assertEquals(input+"XXX", listener.getResult("output"));
		}
	}
	
	@Test
	//see TAV-721
	public void testTranslateAndValidateWithMerge() throws Exception {
		Dataflow dataflow = translateScuflFile("merge_lists_workflow.xml");
		
		DataflowValidationReport report = validateDataflow(dataflow);
		assertTrue("Unsatisfied processor found during validation",report.getUnsatisfiedEntities().size() == 0);
		assertTrue("Failed processors found during validation",report.getFailedEntities().size() == 0);
		assertTrue("Unresolved outputs found during validation",report.getUnresolvedOutputs().size() == 0);
		assertTrue("Validation failed",report.isValid());
	}
}


