/**
 * 
 */
package net.sf.taverna.t2.lineageService.analysis.test;

import static org.junit.Assert.assertFalse;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.taverna.t2.lineageService.capture.test.propertiesReader;
import net.sf.taverna.t2.provenance.lineageservice.ProvenanceAnalysis;
import net.sf.taverna.t2.provenance.lineageservice.ProvenanceQuery;
import net.sf.taverna.t2.provenance.lineageservice.mysql.MySQLProvenanceQuery;
import net.sf.taverna.t2.provenance.lineageservice.mysql.NaiveProvenanceQuery;
import net.sf.taverna.t2.provenance.lineageservice.utils.ProvenanceProcessor;
import net.sf.taverna.t2.provenance.lineageservice.utils.QueryVar;
import net.sf.taverna.t2.provenance.lineageservice.utils.Port;
import net.sf.taverna.t2.provenance.lineageservice.utils.WorkflowInstance;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author paolo
 * @param <pathToDependencies>
 *
 */
public class ProvenanceAnalysisTest<pathToDependencies> {

	private static final String annotationsFileName = null;

	private static final String DEFAULT_QUERY_PATH = null;
	private static final String DEFAULT_SELECTED_PROCESSORS = "ALL";
	private static final String DEFAULT_SELECTED_INSTANCES = "LAST";
	private static final String DEFAULT_SELECTED_WF = "LAST";

	private static final String TOP_PROCESSOR = "TOP";
	private static final String ALL_VARS = "ALL";
	private static final String ALL_PATHS = "ALL";

	private static  String mySQLjdbcString = null;
	private static  String derbyjdbcString = null;

	private boolean returnOutputs = false;  // set through prefs. if true then we return output processor var bindings as well
	private boolean recordArtifactValues = false;  // set through prefs. if true then we record artifact values alongside names in OPM

	NaiveProvenanceQuery npq = null;
	ProvenanceAnalysis pa = null;
	ProvenanceQuery pq = null;		

	// shared by all tests -- these capture users configuration from AnalysisTestFiles.properties
	Set<String> selProcNames = new HashSet<String>();
	List<QueryVar>  qvList = new ArrayList<QueryVar>();

	String wfInstance = null; 

	private static Logger logger = Logger.getLogger(ProvenanceAnalysisTest.class);

	//////////////
	// set annotations
	//////////////

	String[] annotationFiles = 
	{ "webservice/src/test/resources/simple_workflow2_with_2_inputs.xml",  //0
			"webservice/src/test/resources/test_iterate_list_of_lists.xml",  //1
			"webservice/src/test/resources/test1.annot.xml",  //2
			"webservice/src/test/resources/test2.annot.xml",  //3
			"webservice/src/test/resources/test3.annot.xml",  //4
			"webservice/src/test/resources/test4.annot.xml",  //5
			"webservice/src/test/resources/test5.annot.xml",  //6
	"webservice/src/test/resources/test6.annot.xml"};  //7

	boolean useAnnotations = false;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		String DB_URL_LOCAL = propertiesReader.getString("dbhost");  // URL of database server //$NON-NLS-1$
		String DB_USER = propertiesReader.getString("dbuser");                        // database user id //$NON-NLS-1$
		String DB_PASSWD = propertiesReader.getString("dbpassword"); //$NON-NLS-1$

		derbyjdbcString = "jdbc:derby:/Users/paolo/Library/Application Support/taverna-2.1-SNAPSHOT-20090511/db";

		mySQLjdbcString = "jdbc:mysql://" + DB_URL_LOCAL + "/T2Provenance?user="
		+ DB_USER + "&password=" + DB_PASSWD;

		npq = new NaiveProvenanceQuery(mySQLjdbcString);		

		pq = new MySQLProvenanceQuery();	
		pa = new ProvenanceAnalysis(pq);

		acquireTestConfiguration();		
	}


	/**
	 * read from properties file, use defaults for the bits that are missing
	 * @throws SQLException
	 */
	private void acquireTestConfiguration() throws SQLException {

		if (useAnnotations)  pa.setAnnotationFile(annotationFiles[5]);
		else { System.out.println("WARNING: no annotations have been loaded"); }


		// selected processors
		String selectedProcessors = AnalysisTestFiles.getString("query.processors");
		if (selectedProcessors == null) {
			selectedProcessors = DEFAULT_SELECTED_PROCESSORS;			
		}

		// wf ID
		String selectedWF = AnalysisTestFiles.getString("query.workflow");
		if (selectedWF == null || selectedWF.contains("!")) {
			selectedWF = DEFAULT_SELECTED_WF;			
		}



		// wf instance(s)
		String selectedInstances = AnalysisTestFiles.getString("query.wfinstances");
		if (selectedInstances == null) {
			selectedInstances = DEFAULT_SELECTED_INSTANCES;			
		}

		// do we need to return output processor values in addition to inputs?
		String returnOutputsPref = AnalysisTestFiles.getString("query.returnOutputs");
		if (returnOutputsPref != null) {
			setReturnOutputs(Boolean.parseBoolean(returnOutputsPref));	
		}

		// do we need to record actual values as part of the OPM graph?
		String recordArtifacValuesPref = AnalysisTestFiles.getString("OPM.recordArtifactValues");
		if (recordArtifacValuesPref != null) {

			pa.setRecordArtifactValues(Boolean.parseBoolean(recordArtifacValuesPref));

			// are we recording artifact values along with their names?
			System.out.println("OPM.recordArtifactValues: "+ pa.isRecordArtifactValues());

		}

		// are we recording the actual (de-referenced) values at all?!
		String includeDataValuePref = AnalysisTestFiles.getString("query.returnDataValues");
		if (includeDataValuePref != null) {
			pa.setIncludeDataValue(Boolean.parseBoolean(includeDataValuePref));
			System.out.println("query.returnDataValues: "+pa.isIncludeDataValue());
		}

		String computeOPMGraph = AnalysisTestFiles.getString("OPM.computeGraph");
		if (computeOPMGraph != null) {
			pa.setGenerateOPMGraph(Boolean.parseBoolean(computeOPMGraph));
			logger.info("OPM.computeGraph: "+computeOPMGraph);			
		}

		//////////////
		// set the run instances (scope)
		//////////////

		List<WorkflowInstance> instances = null;

		if (selectedWF.equals("LAST")) { // default WF
			instances = pa.getWFInstanceIDs();  // ordered by timestamp
		} else  {
			instances = pa.getWFInstanceID(selectedWF);  // ordered by timestamp
		}

		if (! selectedInstances.equals("LAST")) {
			System.out.println("WARNING: only LAST wfinstance supported in this version");
		}

		if (instances.size()>0)  {  
			wfInstance = instances.get(0).getInstanceID();
			System.out.println("instance "+wfInstance);
		} else {
			assertFalse("FATAL: no wfinstances in DB -- terminating", instances.size() == 0);
		}

		logger.info("QUERY SCOPE: \nWF = "+selectedWF+" INSTANCE = "+wfInstance);



		//////////////
		// set the selected processors
		//////////////
		Map<String, String>  procConstraints = new HashMap<String, String>();
		procConstraints.put("W.instanceID", wfInstance);
		List<ProvenanceProcessor> allProcessors = pq.getProcessors(procConstraints);

		Set<String>  allProcessorsNames = new HashSet<String>();		
		for (ProvenanceProcessor pp: allProcessors) { allProcessorsNames.add(pp.getPname()); }

		if (selectedProcessors.equals("ALL")) {
			for (ProvenanceProcessor pp: allProcessors) { selProcNames.add(pp.getPname()); }			
		} else {

			String[] selectedProcessorsSet  = selectedProcessors.split(",");

			for (String sp: selectedProcessorsSet) {
				if (allProcessorsNames.contains(sp))  selProcNames.add(sp);
			}
		}

		System.out.println("selected processors:");
		for (String s:selProcNames) { System.out.println(s); }

		//////////////
		// set the vars used as starting points
		//////////////
		String proc = null;
		String queryVars = AnalysisTestFiles.getString("query.vars");

		if (queryVars.length() == 0)  { // default: TOP/ALL == all global OUTPUT vars, with fine granularity

			for (ProvenanceProcessor pp: allProcessors) {
				if (pp.getType() != null && pp.getType().equals(pq.DATAFLOW_TYPE)) {
					proc = pp.getPname(); 
					break;
				}
			}

			// proc should not be null here
			// get all output vars for proc
			Map<String, String>  varQueryConstraints = new HashMap<String, String>();

			varQueryConstraints.put("W.instanceID", wfInstance);
			varQueryConstraints.put("V.pnameRef", proc);  
			varQueryConstraints.put("V.inputOrOutput", "0");

			List<Port> outVars = pq.getVars(varQueryConstraints);

			for (int i=0; i<outVars.size(); i++) {
				QueryVar qv = new QueryVar();
				qv.setPname(outVars.get(i).getPName());
				qv.setVname(outVars.get(i).getVName());
				qv.setPath(ALL_PATHS);

				qvList.add(qv);
			}

		} else {  // parse user preferences

			System.out.println("using user selection for initial ports");

			String[] queryVarsTokens = queryVars.split(";");

			for (String qvtoken:queryVarsTokens) {

				// one query var
				String[] qvComponents = qvtoken.split("��");

				QueryVar qv = new QueryVar();

				if (qvComponents.length>0)   // pname 
					qv.setPname(qvComponents[0].trim());
				else 
					qv.setPname(TOP_PROCESSOR);  // default;

				if (qvComponents.length>1)  // varname
					qv.setVname(qvComponents[1].trim());  
				else 
					qv.setVname(ALL_VARS);  // default

				if (qvComponents.length>2)  // path
					qv.setPath(qvComponents[2].trim());
				else 
					qv.setPath(ALL_PATHS);  // default

				//  <proc> == TOP: resolve immediately to the top level dataflow
				if (qv.getPname().equals(TOP_PROCESSOR)) {
					for (ProvenanceProcessor pp: allProcessors) {
						if (pp.getType() != null && pp.getType().equals(pq.DATAFLOW_TYPE)) {
							qv.setPname(pp.getPname()); 
							break;
						}
					}
				}

				// <var> == ALL: unfold to all variables for the proc
				if (qv.getVname().equals(ALL_VARS)) {
					// get output vars for the proc
					Map<String, String>  varQueryConstraints = new HashMap<String, String>();

					varQueryConstraints.put("W.instanceID", wfInstance);
					varQueryConstraints.put("V.pnameRef", qv.getPname());  
					varQueryConstraints.put("V.inputOrOutput", "0");

					List<Port> outVars = pq.getVars(varQueryConstraints);

					QueryVar qv1;
					for (int i=0; i<outVars.size(); i++) {
						qv1 = new QueryVar();
						qv1.setPname(outVars.get(i).getPName());
						qv1.setVname(outVars.get(i).getVName());
						qv1.setPath(qv.getPath());

						qvList.add(qv1);
					}
				} else {
					qvList.add(qv);
				}
			}			
		}

		if (qvList.size() == 0) { assertFalse("no output vars to trace -- terminating", true); }

	}


	/**
	 * just intermediate results
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
//	@Test
//	public final void testSimpleLineageQuery() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
//
//		// simple first: test SimpleLineageQuery -- for the target variables
//		for (QueryVar qv:qvList) {
//
//			System.out.println("************\n Intermediate values on TARGET VAR ==> Simple lineage query: [instance, proc, port, path] = ["+
//					wfInstance+","+qv.getPname()+","+qv.getVname()+",["+qv.getPath()+"]]\n***********");
//
//			List<Dependencies> lqr = new ArrayList<Dependencies>();
//
//			if (qv.getPath().equals(pa.ALL_PATHS_KEYWORD)) {
//
//				Map<String, String> vbConstraints = new HashMap<String, String>();
//				vbConstraints.put("VB.PNameRef", qv.getPname());
//				vbConstraints.put("VB.varNameRef", qv.getVname());
//				vbConstraints.put("VB.wfInstanceRef", wfInstance);
//
//				List<PortBinding> vbList = pq.getVarBindings(vbConstraints); // DB
//
//				for (PortBinding vb:vbList) {
//
//					// path is of the form [x,y..]  we need it as x,y... 
//					String path = vb.getIteration().substring(1, vb.getIteration().length()-1);
//
//					System.out.println("simpleLineageQuery on path "+path);
//
//					if (!path.startsWith("["))  path = "["+path+"]";
//
//					LineageSQLQuery slq = pq.simpleLineageQuery(wfInstance, qv.getPname(), qv.getVname(), path);
//					lqr.add(pq.runLineageQuery(slq, pa.isIncludeDataValue()));
//
//				}
//			}  else {
//				LineageSQLQuery slq = pq.simpleLineageQuery(wfInstance, qv.getPname(), qv.getVname(), qv.getPath());
//				lqr.add(pq.runLineageQuery(slq, pa.isIncludeDataValue()));
//			}
//
//			System.out.println("******  intermediate values on TARGET VAR ["+
//					qv.getPname()+","+qv.getVname()+",["+qv.getPath()+"] query completed");
//
//			if (lqr != null) {
//
//				for (Dependencies res:lqr) {
//					res.setPrintResolvedValue(true);
//					System.out.println(res.toString());
//				}
//			}
//
//		}
//
//		for (String s:selProcNames) { 
//
//			System.out.println("************\n Intermediate values on SELECTED PROCESSOR ==> Simple lineage query: [instance, proc] = ["+
//					wfInstance+","+s+"]\n***********");
//
//			LineageSQLQuery slq = pq.simpleLineageQuery(wfInstance, s, null, null);
//
//			Dependencies runLineageQuery;
//			try {
//				runLineageQuery = pq.runLineageQuery(slq, pa.isIncludeDataValue());
//			} catch (SQLException e) {
//				throw e;
//			}			
//			System.out.println("******  intermediate values on SELECTED PROCESSOR ["+s+"] query completed");
//		}
//	}




	/**
	 * proper lineage query -- done our way
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JAXBException 
	 * @throws IOException 
	 * @throws OperatorException 
	 */
//	@Test
//	public final void testComputeLineagePaths() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, OperatorException, IOException, JAXBException {
//
//		// set return outputs pref
//		pa.setReturnOutputs(isReturnOutputs());
//
//		// TODO needs refactoring
//		QueryAnswer answer = pa.lineageQuery(qvList, wfInstance, selProcNames);
//
//		// display the native answer one var at the time
//		for (Map.Entry<QueryVar,Map<String, List<Dependencies>>> entry: answer.getNativeAnswer().getAnswer().entrySet()) {
//
//			QueryVar    v       = entry.getKey();
//			Map<String, List<Dependencies>> singleVarAnswer = entry.getValue();
//
//			logger.info("query answer for var "+v);
//
//			for (Map.Entry<String, List<Dependencies>> pathToDependencies: singleVarAnswer.entrySet()) {
//
//				String pathToValue       = pathToDependencies.getKey();
//				List<Dependencies>  deps = pathToDependencies.getValue();
//
//				logger.info("\t dependencies for path to value "+pathToValue);
//
//				// display results
//				for (Dependencies result:deps) {
//
//					//System.out.println("****** result: *****");
//					for (LineageQueryResultRecord r:result.getRecords()) {	
//						r.setPrintResolvedValue(false);					// unclutter visual output for testing
//						logger.info(r.toString());
//					}				
//				}
//			}
//		}
//
//		// display the OPM answer
//		String OPMAnswer_asXML = answer.getOPMAnswer_AsXML();
//		String OPMAnswer_asRDF = answer.getOPMAnswer_AsRDF();
//		
//		// this is a filename, not the string itself
//		logger.info("OPM / XML written to "+OPMAnswer_asXML);		
//		logger.info("OPM / RDF written to "+OPMAnswer_asRDF);
//		
//		assertTrue("lineage tree should have been printed above", true);
//	}
//






	/**
	 * uses the pre-computed paths to traverse DD recursively.<p/>
	 * Optimised to cache intermediate results and use them to avoid multiple path traversals in case
	 * of unfocused queries
	 * @throws SQLException
	 */
//	@Test
//	public final void testRecursiveNaiveQuery() throws SQLException {
//
//		Map<List<String>, List<DDRecord>> cachedResults = new HashMap<List<String>, List<DDRecord>>();
//
//		// for simplicity only consider one target var
//		String pname = qvList.get(0).getPname();
//		String vname = qvList.get(0).getVname();
//		String iteration = qvList.get(0).getPath();
//
//		//  need to add '[' ']' to path in this case to match what's in the DD for iterations...
//		if (iteration != null && iteration.length()>0)  iteration = new String("["+iteration+"]");
//
//		///////
//		// 1 - first traverse the graph to compute all paths to be queried -- use out own computeLineageQuery for this!!
//		///////
//		System.out.println("****  computing paths to selected processors for "+pname+" "+vname+" "+iteration);
//		long start = System.currentTimeMillis();
//		pa.searchDataflowGraph(wfInstance, vname, pname, iteration, selProcNames);
//		long stop = System.currentTimeMillis();
//
//		long pct = stop-start;
//
//		long qrtavg = 0;
//		int cnt =0;
//		// these are the paths we need to traverse
//		Map<String, List<List<String>>> allPaths = pa.getValidPaths();
//
//		///////
//		// 2 - sort paths by dec length -- rationale: shorter paths _may_ be subsumed by longer paths.
//		// (this is always true in our test dataflows!
//		///////
//		List<List<String>> sortedPaths = new ArrayList<List<String>>();
//
//		for (Map.Entry<String, List<List<String>>> entry:allPaths.entrySet()) {
//			for (List<String> aPath: entry.getValue()) {
//
//				boolean inserted = false;
//				for (int i=0; i<sortedPaths.size(); i++) {
//					if (aPath.size() > sortedPaths.get(i).size()) {
//						sortedPaths.add(i, aPath);
//						break;
//					}
//				}
//				if (!inserted) { sortedPaths.add(aPath); }
//			}
//		}		
//
//		///////
//		// 3 - then  call recursiveNaiveQuery once on each path, 
//		// but only if results are not already available through cache
//		///////
//		for (List<String> aPath:sortedPaths) {
////			for (Map.Entry<String, List<List<String>>> entry:allPaths.entrySet()) {
//
////			for (List<String> aPath: entry.getValue()) {
//
//			// is this path a prefix of a path for which we have already computed?
//			boolean found = (cachedResults.keySet().size()>0);
//			for (List<String> cachedPath:cachedResults.keySet()) {
//
//				found = true;
//				// prefix check
//				int i=0;
//				while (i< aPath.size() && i<cachedPath.size()) {
//
//					if (!aPath.get(i).equals(cachedPath.get(i))) { found = false; break; }
//					else i++;
//
//				}
//				if (found) break;
//			}
//
//			if (found) {
//				System.out.println("skipping computation on path: ");
//				for (String p1:aPath) { System.out.println(p1); }
//				continue; 
//			} else {
//				System.out.println("NOT skipping computation on path: ");
//				for (String p1:aPath) { System.out.println(p1); }					
//			}
//
//			start = System.currentTimeMillis();
//			List<DDRecord> result = npq.recursiveNaiveQuery(vname, pname, iteration, aPath);
//			stop = System.currentTimeMillis();
//
//			// cache this result
//			cachedResults.put(aPath , result);
//
//			long qrt = stop-start;
//
//			for (DDRecord r:result) {
//				System.out.println("pFrom = "+r.getPFrom()+" vFrom= "+r.getVFrom()+" valFrom= "+r.getValFrom());
//			}
//
//			System.out.println("lineage query response time: "+qrt+" ms");
//			System.out.println("total time: "+(pct+qrt)+"ms");
//
//			qrtavg += qrt;
//			cnt++;
////			}
////			}
//		}
//		System.out.println("path computation time: "+pct+"ms");
//		System.out.println("avg query response time: "+((float) qrtavg/cnt)+"ms");
//		System.out.println("avg total time: "+(((float) qrtavg/cnt)+pct)+"ms");
//	}




	/**
	 * - invokes searchDataflowGraph() to determine paths. timed execution<br/>
	 * - for each path: invokes generateNaiveQuery() to generate SQL query <br/>
	 * - executes query. timed execution
	 * @throws SQLException
	 */
//	@Test
//	public final void testGenerateNaiveQuery() throws SQLException {
//
//		// for simplicity only consider one target var
//		String pname = qvList.get(0).getPname();
//		String vname = qvList.get(0).getVname();
//		String iteration = qvList.get(0).getPath();
//
//		//  need to add '[' ']' to path in this case to match what's in the DD for iterations...
//		if (iteration != null && iteration.length()>0)  iteration = new String("["+iteration+"]");
//
//		///////
//		// 1 - first traverse the graph to compute all paths to be queried -- use our own computeLineageQuery for this!!
//		///////
//		System.out.println("****  computing paths to selected processors for "+pname+" "+vname+" "+iteration);
//
//		long start = System.currentTimeMillis();
//		pa.searchDataflowGraph(wfInstance, vname, pname, iteration, selProcNames);
//		long stop = System.currentTimeMillis();
//
//		long pct = stop - start;
//
//		// these are the paths we need to traverse
//		Map<String, List<List<String>>> allPaths = pa.getValidPaths();
//
//		long qrtavg = 0;
//		int cnt =0;
//		for (Map.Entry<String, List<List<String>>> entry:allPaths.entrySet()) {
//
//			for (List<String> aPath: entry.getValue()) {
//
//				System.out.println("generating lineage query "+(cnt+1));
//				//for (String p:aPath)  { System.out.println(p); }
//
//				start = System.currentTimeMillis();
////				String q = npq.generateNaiveQueryTwoTables(vname, pname, iteration, aPath);
//				String q = npq.generateNaiveQuery(vname, pname, iteration, aPath);
//				stop = System.currentTimeMillis();
//				long qgt = stop- start;
//
//				System.out.println("*** generated query:\n"+q+"\nin "+qgt+"ms");
//
//				Statement stmt = null;
//
//				// TIMER STARTS
//				start = System.currentTimeMillis();
//				try {
//					stmt = pq.getConnection().createStatement();
//				} catch (InstantiationException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalAccessException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (ClassNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				boolean success = stmt.execute(q);
//				stop = System.currentTimeMillis();
//				// TIMER STOPS
//
//				System.out.println("** results:  ***");
//				if (success) {
//					ResultSet rs = stmt.getResultSet();
//
//					while (rs.next()) {
//
//						String pFrom = rs.getString("pFrom");
//						String vFrom = rs.getString("vFrom");
//						String valFrom = rs.getString("valFrom");
////						String pTo = rs.getString("pTo");
////						String vTo = rs.getString("vTo");
////						String valTo = rs.getString("valTo");
//
//						System.out.println("pFrom = "+pFrom+" vFrom= "+vFrom+" valFrom= "+valFrom);
//					}
//				}
//				long qrt = stop-start;
//
//				System.out.println("query response time: "+qrt+"ms");			
//				System.out.println("total time: "+(pct+qrt)+"ms");
//
//				qrtavg += qrt;
//				cnt++;
//			}
//		}
//		System.out.println("path computation time: "+pct+"ms");
//		System.out.println("avg query response time: "+((float) qrtavg/cnt)+"ms");
//		System.out.println("avg total time: "+(((float) qrtavg/cnt)+pct)+"ms");
//
//		// report each time + avg / std.dev.  TODO
//
////		int blockLength = 4;
//
////		List<String> path = new ArrayList<String>();
//
////		for (int i=blockLength; i>=0; i--) {
////		path.add("LINEARBLOCK_"+i);
////		}
//
////		System.out.println("path processors: ");
////		for (String p:path)  { System.out.println(p); }
//
////		String q = npq.generateNaiveQuery("Y", "LINEARBLOCK_2", "[0]", path);
//
//	}




	/**
	 * OLD (obsolete??) this uses both Datalinks and DD and avoids (ignores??) prior path traversal
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	@Test
	public final void testcomputeLineageNaive() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		// compute once for each value of this var
		List<String> values = pq.getVarValues(wfInstance, qvList.get(0).getPname(), qvList.get(0).getVname());

		for (String value: values) {

			String pname = qvList.get(0).getPname();
			String vname = qvList.get(0).getVname();

			System.out.println("****  computeLineageNaive for "+pname+" "+vname+" "+value);

			boolean isInput = false;

			// add timers here
			Date start = new Date();
			npq.computeLineageNaive(wfInstance, pname, vname, value, isInput);
			Date stop  = new Date();

			System.out.println("lineage query response time: "+(stop.getTime() - start.getTime())+" ms");

		}
	}


	/**
	 * @return the returnOutputs
	 */
	public boolean isReturnOutputs() {
		return returnOutputs;
	}


	/**
	 * @param returnOutputs the returnOutputs to set
	 */
	public void setReturnOutputs(boolean returnOutputs) {
		this.returnOutputs = returnOutputs;
	}

}
