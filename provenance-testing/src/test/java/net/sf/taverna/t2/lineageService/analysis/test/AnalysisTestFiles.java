package net.sf.taverna.t2.lineageService.analysis.test;


import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class AnalysisTestFiles {
	private static final String BUNDLE_NAME = "net.sf.taverna.t2.lineageService.analysis.test.AnalysisTestFiles"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private AnalysisTestFiles() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
