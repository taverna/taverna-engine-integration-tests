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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.sf.taverna.t2.facade.WorkflowInstanceFacade;
import net.sf.taverna.t2.testing.CaptureResultsListener;
import net.sf.taverna.t2.testing.InvocationTestHelper;
import net.sf.taverna.t2.workflowmodel.Dataflow;
import net.sf.taverna.t2.workflowmodel.DataflowValidationReport;
import net.sf.taverna.t2.workflowmodel.impl.EditsImpl;

import org.junit.Ignore;
import org.junit.Test;

public class TranslateAndRunWSDLBasedTest extends InvocationTestHelper {

	@Ignore("eInfo service no longer available")
	@SuppressWarnings("unchecked")
	@Test
	//uses a dataflow based upon the eutils run_eInfo that contains both
	//xml input and output splitters. This involved wrapped doc/literal
	//with complex types.
	public void testRunInfo() throws Exception {
		Dataflow dataflow = translateScuflFile("run_eInfo.xml");
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
		
		assertNotNull("'fieldName' should be included in the results",listener.getResult("fieldName"));
		assertNotNull("'fieldList' should be included in the results",listener.getResult("fieldList"));
		assertNotNull("'DBDescription' should be included in the results",listener.getResult("DBDescription"));
		assertNotNull("'xmlOutput' should be included in the results",listener.getResult("xmlOutput"));
		
		assertTrue("'fieldName' should be a List", listener.getResult("fieldName") instanceof List);
		assertTrue("'fieldList' should be a List", listener.getResult("fieldList") instanceof List);
		assertTrue("'DBDescription' should be a String", listener.getResult("DBDescription") instanceof String);
		Object result = listener.getResult("xmlOutput");
		assertNotNull("Result should not be null",result);
		assertTrue("'xmlOutput' should be a ByteArrayInputStream but was:"+result.getClass(),result instanceof String);
		
		//some basic tests the the results were sane
		List fieldName = (List)listener.getResult("fieldName");
		assertTrue("fieldName should contain the String ALL",fieldName.contains("ALL"));
		assertTrue("fieldName should contain the String MHDA",fieldName.contains("MHDA"));
		
		List fieldList = (List)listener.getResult("fieldList");
		assertTrue("fieldList should contain some elements",fieldList.size()>0);
		
		String dbDescription = (String)listener.getResult("DBDescription");
		assertTrue("DBDescription should contain the word pubmed",dbDescription.toLowerCase().contains("pubmed"));
	
	}
	
	@SuppressWarnings("unchecked")
	@Test
	//a workflow invoking the KEGG operation list_organisms. Involves RPC/encoded, and chained output splitters actiing over a list.
	public void testKEGGListOrganisms() throws Exception {
		Dataflow dataflow = translateScuflFile("KEGG-list_organisms.xml");
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
		
		assertNotNull("There should be an output 'out'",listener.getResult("out"));
		assertTrue("the result should be a list",listener.getResult("out") instanceof List);
		List out = (List)listener.getResult("out");
		assertTrue("The list should contain more than 10 elements",out.size()>10);
		assertTrue("Homo sapien was an organism when I last checked",out.contains("Homo sapiens (human)"));
	}
}
