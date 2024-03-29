/**
 * 
 */
package net.sf.taverna.t2.lineageService.analysis.test;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;

import net.sf.taverna.t2.lineageService.capture.test.propertiesReader;
import net.sf.taverna.t2.provenance.api.NativeAnswer;
import net.sf.taverna.t2.provenance.api.ProvenanceAccess;
import net.sf.taverna.t2.provenance.api.ProvenanceConnectorType;
import net.sf.taverna.t2.provenance.api.ProvenanceQueryParser;
import net.sf.taverna.t2.provenance.api.Query;
import net.sf.taverna.t2.provenance.api.QueryAnswer;
import net.sf.taverna.t2.provenance.lineageservice.Dependencies;
import net.sf.taverna.t2.provenance.lineageservice.LineageQueryResultRecord;
import net.sf.taverna.t2.provenance.lineageservice.ProvenanceQuery;
import net.sf.taverna.t2.provenance.lineageservice.utils.ProvenanceProcessor;
import net.sf.taverna.t2.provenance.lineageservice.utils.QueryVar;
import net.sf.taverna.t2.provenance.lineageservice.utils.Port;
import net.sf.taverna.t2.provenance.lineageservice.utils.Workflow;
import net.sf.taverna.t2.provenance.lineageservice.utils.WorkflowInstance;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author paolo
 *
 */
public class ProvenanceAPI_OLD_Test {

	private static final String DEFAULT_SELECTED_PROCESSORS = "ALL";
	private static final String TOP_PROCESSOR = "TOP";
	private static final String ALL_VARS = "ALL";
	private static final String DEFAULT_OPM_FILENAME = "src/test/resources/provenance-testing/OPMGraph.rdf";
	
	ProvenanceAccess pAccess = null;

	String DB_URL_LOCAL = propertiesReader.getString("dbhost");  // URL of database server //$NON-NLS-1$
	String DB_USER = propertiesReader.getString("dbuser");                        // database user id //$NON-NLS-1$
	String DB_PASSWD = propertiesReader.getString("dbpassword"); //$NON-NLS-1$
	String OPMGraphFilename = null;
	
	List<String> wfNames = null;
	Set<String> selectedProcessors = null;

	private static Logger logger = Logger.getLogger(ProvenanceAPI_OLD_Test.class);

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		setDataSource();
		System.setProperty("raven.eclipse","true");
		pAccess = new ProvenanceAccess(ProvenanceConnectorType.MYSQL);  // creates and initializes the provenance API
		configureInterface();              // sets user-defined preferences
	}


	public void setDataSource() {

		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,"org.osjava.sj.memory.MemoryContextFactory");
		System.setProperty("org.osjava.sj.jndi.shared", "true");

		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		ds.setMaxActive(50);
		ds.setMinIdle(10);
		ds.setMaxIdle(50);
		ds.setDefaultAutoCommit(true);
		ds.setUsername(DB_USER);
		ds.setPassword(DB_PASSWD);

		try {
			ds.setUrl("jdbc:mysql://"+DB_URL_LOCAL+"/T2Provenance");

			InitialContext context = new InitialContext();
			context.rebind("jdbc/taverna", ds);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testXMLQuery() {

		Query q = new Query();

		// get filename for XML query spec
		String querySpecFile = AnalysisTestFiles.getString("query.file");
		logger.info("testing query "+querySpecFile);

		ProvenanceQueryParser pqp = new ProvenanceQueryParser();
		pqp.setPAccess(pAccess);

		q = pqp.parseProvenanceQuery(querySpecFile);
		
		if (q == null) {
			logger.fatal("query processing failed. So sorry.");
			return;
		}
		logger.info("YOUR QUERY: "+q.toString());
		
		QueryAnswer answer=null;
		try {
			answer = pAccess.executeQuery (q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		reportAnswer(answer);
		saveOPMGraph(answer, OPMGraphFilename);
	}



	/**
	 * writes the RDF/XML OPM string to file
	 * @param opmFilename
	 */
	private void saveOPMGraph(QueryAnswer answer, String opmFilename) {
		
		if (answer.getOPMAnswer_AsRDF() == null) {
			logger.info("save OPM graph: OPM graph was NOT generated.");
			return;
		}
		
		try {
			FileWriter fw= new FileWriter(new File(opmFilename));
			fw.write(answer.getOPMAnswer_AsRDF());
			fw.close();
		} catch (IOException e) {
			logger.warn("saveOPMGraph: error saving graph to file "+opmFilename);
			logger.warn(e.getMessage());
		}
		logger.info("OPM graph saved to "+opmFilename);
	}


	/**
	 * Test method for {@link net.sf.taverna.t2.provenance.api.Query#executeQuery(java.util.List, java.lang.String, java.util.Set)}.
	 */
	@Test
	public final void testProvenanceQuery() {

		try {

			Query pq = composeQuery(); // creates a test query using a config file (AnalysisTestFiles.properties)
			QueryAnswer answer = pAccess.executeQuery (pq);
			reportAnswer(answer);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}



	/**
	 * acquire uer input query elements and compose a query
	 * @param selectedProcessors2 
	 * @param runID 
	 * @param targetVars 
	 */
	private Query composeQuery() {

		Query q = new Query();

		// THE OLD WAYS
		List<String>  runs = acquireRuns();  // currently contains the latest run
		q.setRunIDList(runs);

		List<ProvenanceProcessor> allProcessorsFlat = runsToProcessors(runs);

		List<ProvenanceProcessor> selectedProcessors = acquireSelectedProcessors(allProcessorsFlat);   
		q.setSelectedProcessors(selectedProcessors);

		List<QueryVar> selectedVars = acquireSelectedVars(runs.get(0), allProcessorsFlat);
		q.setTargetVars(selectedVars);

		return q;
	}


	private void reportAnswer(QueryAnswer answer) {

		NativeAnswer nAnswer = answer.getNativeAnswer();

		// nAnswer contains a Map of the form 
		// 	Map<QueryVar, Map<String, List<Dependencies>>>  answer;

		Map<QueryVar, Map<String, List<Dependencies>>>  dependenciesByVar = nAnswer.getAnswer();	
		for (QueryVar v:dependenciesByVar.keySet()) {
			logger.info("dependencies for port: "+v.getPname()+":"+v.getVname()+":"+v.getPath());

			Map<String, List<Dependencies>> deps = dependenciesByVar.get(v);
			for (String path:deps.keySet()) {
				logger.info("dependencies on path "+path);
				for (Dependencies dep:deps.get(path)) {

					for (LineageQueryResultRecord record: dep.getRecords()) {
						record.setPrintResolvedValue(false);
						logger.info(record.toString());
					}
				}
			}
		}		
	}



	private List<QueryVar> acquireSelectedVars(String runID, 
			List<ProvenanceProcessor> allProcessorsFlat) {

		//////////////
		// set the vars used as starting points
		//////////////
		List<QueryVar> targetVars = new ArrayList<QueryVar>();

		String queryVars = AnalysisTestFiles.getString("query.vars");

		// expect a sequence of the form pname��vname;pname��vname;... where the pnames are unqualified...

		String topLevelWorkflow = pAccess.getTopLevelWorkflowID(runID);

		if (queryVars.length() == 0 || queryVars.equals("ALL"))  { // default: TOP/ALL == all global OUTPUT vars, with fine granularity


			// look for the outputs of the top-level workflow	
			List<Port> dataflowPorts = pAccess.getPortsForDataflow(topLevelWorkflow);

			for (Port v:dataflowPorts) {
				if (!v.isInput()) {
					QueryVar qv = new QueryVar();
					qv.setWfName(topLevelWorkflow);
					qv.setWfInstanceId(runID);
					qv.setPname(v.getPName());
					qv.setVname(v.getVName());
					qv.setPath(null);  // all paths

					targetVars.add(qv);
				}
			}
		} else { // parse explicit user selection

			System.out.println("using user selection for initial ports");

			String[] queryVarsTokens = queryVars.split(";");

			// explicit query var
			for (String qvtoken:queryVarsTokens) {

				// one query var
				String[] qvComponents = qvtoken.split("��");

				QueryVar qv = new QueryVar();

				if (qvComponents.length>0)  qv.setPname(qvComponents[0].trim()); // pname
				else qv.setPname(TOP_PROCESSOR);  // default;

				if (qvComponents.length>1)  qv.setVname(qvComponents[1].trim()); // varname  
				else qv.setVname(ALL_VARS);  // default

				if (qvComponents.length>2)  qv.setPath(qvComponents[2].trim()); // path
				else qv.setPath(null);  // default  -- all paths

				//  <proc> == TOP: resolve immediately to the top level dataflow
				if (qv.getPname().equals(TOP_PROCESSOR)) {
					for (ProvenanceProcessor pp: allProcessorsFlat) {
						if (pp.getType() != null && pp.getType().equals(ProvenanceQuery.DATAFLOW_TYPE)) {
							qv.setPname(pp.getPname()); 
							qv.setWfName(pp.getWfInstanceRef());
							qv.setWfInstanceId(runID);
							break;
						}
					}
				}  else {  // need to qualify the pname with its workflow ID
					List<Workflow> workflows = pAccess.getContainingWorkflowsForProcessor(qv.getPname());

					// greedily pick the first workflow in the list
					qv.setPname(qv.getPname());
					qv.setWfName(workflows.get(0).getWfname());
					qv.setWfInstanceId(runID);

				}

				// <var> == ALL: unfold to include all variables for the proc
				if (qv.getVname().equals(ALL_VARS)) {

					List<Port> ports = pAccess.getPortsForProcessor(topLevelWorkflow, qv.getPname());

					for (int i=0; i<ports.size(); i++) {

						if (ports.get(i).isInput()) { continue; }

						QueryVar qv1 = new QueryVar();
						qv1.setPname(ports.get(i).getPName());
						qv1.setVname(ports.get(i).getVName());
						qv1.setPath(qv.getPath());
						qv.setWfName(topLevelWorkflow);
						qv.setWfInstanceId(runID);

						targetVars.add(qv);

					}
				} else {
					targetVars.add(qv);
				}
			}			
		}
		return targetVars;
	}



	/**
	 * set the run instances (scope)
	 in the future it will support selecting runs amongst those available.
	 at the moment it picks the latest run for testing purposes
	 * @return
	 */	
	private List<String> acquireRuns() {

		List<String> result = new ArrayList<String>();
		String runID = null;

		List<WorkflowInstance> wfInstances = pAccess.listRuns(null, null); // returns all available runs ordered by timestamp

		if (wfInstances.size()>0)  {  

			runID = wfInstances.get(0).getInstanceID();			
			result.add(runID);
			System.out.println("running query on instance "+runID);
		} else {
			assertFalse("FATAL: no wfinstances in DB -- terminating", wfInstances.size() == 0);
		}		
		return result;
	}


	private List<ProvenanceProcessor> runsToProcessors(List<String> runs) {

		List<String> topLevelWorkflows = new ArrayList<String>();

		// get top-level workflow for a run
		for (String run:runs) {
			topLevelWorkflows.add(pAccess.getTopLevelWorkflowID(run));
		}

		// we assume the current scenario where we only consider the latest run, for testing purposes
		String workflowID = topLevelWorkflows.get(0);
		Map<String, List<ProvenanceProcessor>> allProcessors = pAccess.getProcessorsInWorkflow(workflowID);

		// flatten all processors into one list. The context is contained within each processor object so it's not lost
		List<ProvenanceProcessor> allProcessorsFlat = new ArrayList<ProvenanceProcessor>();
		Collection<List<ProvenanceProcessor>> allProcs = allProcessors.values();
		for (List<ProvenanceProcessor> procList:allProcs) { allProcessorsFlat.addAll(procList); }

		return allProcessorsFlat;
	}


	/**
	 * parse user preferences for selected processors
	 * @param runs 
	 * @return
	 */
	private List<ProvenanceProcessor> acquireSelectedProcessors(List<ProvenanceProcessor> allProcessorsFlat) {

		List<ProvenanceProcessor> selectedProcessors = new ArrayList<ProvenanceProcessor>();

		// selected processors
		String selectedProcNames = AnalysisTestFiles.getString("query.processors");
		if (selectedProcNames == null) { selectedProcNames = DEFAULT_SELECTED_PROCESSORS; }

		// user selects all processors in the graph
		if (selectedProcNames.equals("ALL")) {
			selectedProcessors = allProcessorsFlat;
		}  else {
			// explicit user selection

			// parse user selection -- NB there is no workflow qualification to these names at the moment!!
			String[] selectedProcessorsSet  = selectedProcNames.split(",");

			// validate and qualify:
			// look for all processors with the same name (within any containing workflow) in allProcessors
			// and add each of them to the selected set
			// this involves a multiple full scan of the processor Map... needs optimizing 
			System.out.println("selected processors:");
			for (String sp: selectedProcessorsSet) {

				for (ProvenanceProcessor p:allProcessorsFlat) {
					if (p.getPname().equals(sp)) { // processor name found
						selectedProcessors.add(p);
					}
				}
			}
		}
		return selectedProcessors;
	}





	/**
	 * set user-defined values for toggles on the API
	 */
	private void configureInterface() {

		// do we need to return output processor values in addition to inputs?
		String returnOutputsPref = AnalysisTestFiles.getString("query.returnOutputs");
		if (returnOutputsPref != null) {
			pAccess.toggleIncludeProcessorOutputs(Boolean.parseBoolean(returnOutputsPref));	
		}

		// do we need to record actual values as part of the OPM graph?
		String recordArtifacValuesPref = AnalysisTestFiles.getString("OPM.recordArtifactValues");
		if (recordArtifacValuesPref != null) {			
			pAccess.toggleAttachOPMArtifactValues(Boolean.parseBoolean(recordArtifacValuesPref));
			System.out.println("OPM.recordArtifactValues: "+ pAccess.isAttachOPMArtifactValues());
		}

		String computeOPMGraph = AnalysisTestFiles.getString("OPM.computeGraph");
		if (computeOPMGraph != null) {
			pAccess.toggleOPMGeneration(Boolean.parseBoolean(computeOPMGraph));
			System.out.println("OPM.computeGraph: "+pAccess.isOPMGenerationActive());			
		}
		
		// user-selected file name for OPM graph?
		OPMGraphFilename = AnalysisTestFiles.getString("OPM.rdf.file");
		if (OPMGraphFilename == null) {
			OPMGraphFilename = DEFAULT_OPM_FILENAME;
			System.out.println("OPM.filename: "+OPMGraphFilename);			
		}
	}


}
