package edu.utdallas.cs3345.project5;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.TagFilter.includeTags;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * A driver that runs a JUnit5 test class and produces a report that is written to stdout.  Contains a <code>
 * main</code> method, but ignores any input to stdin.
 * <p>
 * This driver is decently abstracted.  It can be used with a different test class (i.e. a different project)
 * simply by changing the {@link TestDriver#PACKAGE}, {@link TestDriver#CLASS}, and {@link TestDriver#TAGS}
 * constants.
 * 
 * @see {@link edu.utdallas.cs3345.projec5.TestDriver.TaggedTestExecutionSummary#getSummaryByTag(String)}
 * for the code that actually executes the tests
 * 
 * @ApiNote The only reason this driver is necessary is because the Eclipse plugin for JUnit5 does not support
 * Jupiter test suites yet.  That being said, it is still possible to run {@link LazyBinarySearchTreeTest}
 * directly in Eclipse (i.e. without this driver) using the plugin.  However, I want to use the JUnit5
 * {@link org.junit.jupiter.api.Tag} annotation to group test output by grading category, and I would not
 * be able to do that in the current plugin version unless I could use test suites.  I could theoretically
 * use the analogous <code>@Category</code> tag from JUnit4, and then use the JUnit4 plugin which does
 * support JUnit4 test suites, but I have already written the tests using several features that did not exist
 * before JUnit5 (e.g. <code>@Nested</code>), and so I settled on using this driver as a workaround.
 * 
 * @implNote This driver entails a few assumptions:
 * <ul>
 *   <li>There is exactly one top-level test class.
 *   <li>The top-level test class is located in a package other than the default package.
 *   <li>Every test method is tagged with exactly one tag.
 *   <li>The set of tags for all test methods is represented by the <code>TAGS</code> variable.
 * </ul>
 * The tag-related assumptions are in turn based on the assumption that the grade for a project will be
 * exactly represented by a rubric of exhaustive but disjoint / non-nested categories.
 * 
 * @implSpec Every test method in the test class must be annotated with exactly one tag.  It is intended
 * for each tag to represent the (single) category on the grading rubric to which a test method corresponds.
 * Test methods without a tag will not be run by this driver.  Test methods with multiple tags may result in
 * undefined behavior (e.g. incorrect aggregate statistics).
 * 
 * @implSpec The {@link TestDriver#TAGS} list constant should contain exactly one entry for each tag of
 * test methods to be executed.  If a tag is not contained in <code>TAGS</code>, then it will not be run
 * (assuming no test method is annotated with more than one tag).  If a tag is contained in <code>TAGS</code>
 * multiple times, then undefined behavior may result (e.g. incorrect aggregate statistics).  
 */
public class TestDriver
{
	private static final String PACKAGE = "edu.utdallas.cs3345.project5";
	private static final Class<?> CLASS = QuickSorterTest.class;
	private static final List<String> TAGS = QuickSorterTest.TAGS;
	
	public static void main(String[] args)
	{
		List<TaggedTestExecutionSummary> taggedTestExecutionSummaries = TAGS.stream()
				.map(tag -> TaggedTestExecutionSummary.from(tag))  // this line executes the tests for a given tag
				.collect(Collectors.toList());
		
		CharSequence formattedAggregateSummary = FormattedAggregateSummary.from(taggedTestExecutionSummaries);
		CharSequence formattedDetailsByTag = FormattedDetailsByTag.from(taggedTestExecutionSummaries);
		
		StringBuilder report = new StringBuilder();
		report.append(formattedAggregateSummary);
		report.append(SECTION_BREAK);
		report.append(formattedDetailsByTag);
		
		System.out.println(report);
	}
	
	private static class TaggedTestExecutionSummary
	{
		public String tag;
		public TestExecutionSummary testExecutionSummary;
		
		public TaggedTestExecutionSummary(String tag, TestExecutionSummary summary)
		{
			this.tag = tag;
			this.testExecutionSummary = summary;
		}
		
		public static TaggedTestExecutionSummary from(String tag)
		{
			return new TaggedTestExecutionSummary(tag, getSummaryByTag(tag));
		}
		
		/**
		 * This is the method that actually executes the tests for a given tag.
		 * @param tag
		 * @return
		 */
		private static TestExecutionSummary getSummaryByTag(String tag)
		{
			LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
					.selectors(
				    		selectPackage(PACKAGE),
				        selectClass(CLASS)
				    	)
				    .filters(
				    		includeClassNamePatterns(".*Test[s]?"),
				    		includeTags(tag)
				    	)
				    .build();

			Launcher launcher = LauncherFactory.create();
			SummaryGeneratingListener listener = new SummaryGeneratingListener();
			launcher.registerTestExecutionListeners(listener);
			launcher.execute(request);
			
			return listener.getSummary();
		}
	}
	
	private static class FormattedAggregateSummary
	{
		public AggregateSummary aggregateSummaryOverall;
		public Map<String,AggregateSummary> aggregateSummaryByTag = new TreeMap<>();  // TreeMap because ordered

		public static CharSequence from(List<TaggedTestExecutionSummary> taggedSummaries)
		{
			return new FormattedAggregateSummary(taggedSummaries).formatted();
		}
		
		private FormattedAggregateSummary(List<TaggedTestExecutionSummary> taggedSummaries)
		{
			aggregateSummaryOverall = taggedSummaries.stream()
					.map(taggedSummary -> {  // this is a workaround to effectively do a non-terminal "forEach" and a "map" at the same time
						AggregateSummary aggregateSummaryForTag = AggregateSummary.from(taggedSummary.testExecutionSummary);
						aggregateSummaryByTag.put(taggedSummary.tag, aggregateSummaryForTag);  // assumes only one per tag
						return aggregateSummaryForTag;
					})
					.reduce(AggregateSummary::reduce)
					.get();
		}
		
		private CharSequence formatted()
		{
			StringJoiner sj = new StringJoiner(ENDL + ENDL + ENDL);
			
			sj.add( overallHeader() + ENDL + ENDL + aggregateSummaryOverall.toString() );
			
			aggregateSummaryByTag.forEach( (tag,aggregateSummaryForTag) ->
					sj.add( tagHeaderFrom(tag) + ENDL + ENDL + aggregateSummaryForTag.toString() ) );
			
			return sj.toString();
		}
		
		private static CharSequence overallHeader()
		{
			String titleLine = "\\\\ Aggregate Summary Overall \\\\";
			String boxedLine = repeatedChar('\\', titleLine.length());
			return boxedLine + ENDL + titleLine + ENDL + boxedLine;
		}
		
		private static CharSequence tagHeaderFrom(String tag)
		{
			String titleLine = "\\\\ Aggregate Summary for Tag:  \"" + tag + "\" \\\\";
			String boxedLine = repeatedChar('\\', titleLine.length());
			return boxedLine + ENDL + titleLine + ENDL + boxedLine;
		}
		
		private static class AggregateSummary
		{
			public long testsAttempted;
			public long testsSucceeded;
			public long testsFailed;
			public long testsAborted;
			
			public static AggregateSummary from(TestExecutionSummary summary)
			{
				AggregateSummary temp = new AggregateSummary();
				temp.testsAttempted = summary.getTestsStartedCount();
				temp.testsSucceeded = summary.getTestsSucceededCount();
				temp.testsFailed = summary.getTestsFailedCount();
				temp.testsAborted = summary.getTestsAbortedCount();
				return temp;
			}
			
			private AggregateSummary() { }
			
			@Override
			public String toString()
			{
				StringJoiner sj = new StringJoiner(ENDL);
				sj.add("tests attempted: " + testsAttempted);
				sj.add("tests succeeded: " + testsSucceeded);
				sj.add("tests failed: " + testsFailed);
				sj.add("tests aborted: " + testsAborted);
				return sj.toString();
			}
			
			public static AggregateSummary reduce(AggregateSummary one, AggregateSummary two)
			{
				AggregateSummary temp = new AggregateSummary();
				temp.testsAttempted = one.testsAttempted + two.testsAttempted;
				temp.testsSucceeded = one.testsSucceeded + two.testsSucceeded;
				temp.testsFailed = one.testsFailed + two.testsFailed;
				temp.testsAborted = one.testsAborted + two.testsAborted;
				return temp;
			}
		}
	}
	
	private static class FormattedDetailsByTag
	{
		
		public static CharSequence from(List<TaggedTestExecutionSummary> taggedTestExecutionSummaries)
		{
			StringJoiner sj = new StringJoiner(ENDL + ENDL + ENDL + ENDL);  // three blank lines
			taggedTestExecutionSummaries.forEach( (ttes) ->  {
					if (ttes.testExecutionSummary.getTestsFailedCount() > 0)
						sj.add(FormattedDetailsOfTag.from(ttes.tag, ttes.testExecutionSummary));
			});
			return sj.toString();
		}
		
		private static class FormattedDetailsOfTag
		{
			private static final CharArrayWriter caw = new CharArrayWriter(); 
			
			public static CharSequence from(String tag, TestExecutionSummary testExectionSummary)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(headerFrom(tag));
				sb.append(ENDL).append(ENDL);
				sb.append(failuresOf(testExectionSummary));
				return sb;
			}
			
			private static CharSequence headerFrom(String tag)
			{
				String titleLine = "\\\\ Details for Tag:  \"" + tag + "\" \\\\";
				String boxedLine = repeatedChar('\\', titleLine.length());
				return boxedLine + ENDL + titleLine + ENDL + boxedLine;
			}
			
			private static CharSequence failuresOf(TestExecutionSummary testExecutionSummary)
			{
				caw.reset();
				PrintWriter pw = new PrintWriter(caw);
				testExecutionSummary.printFailuresTo(pw);
				return caw.toString();
			}
		}
	}
	
	
	///////////////
	// UTILITIES //
	///////////////
	
	private static final String ENDL = System.lineSeparator();
	private static final String EIGHTY_ASTERISKS = repeatedChar('*', 80);
	private static final String SECTION_BREAK = new StringBuilder()
			.append(ENDL).append(ENDL).append(ENDL).append(ENDL)  // three blank lines
			.append(EIGHTY_ASTERISKS).append(ENDL).append(EIGHTY_ASTERISKS)  // two lines of asterisks
			.append(ENDL).append(ENDL).append(ENDL).append(ENDL)  // three blank lines
			.toString();
	
	private static String repeatedChar(char character, int count)
	{
		char[] arr = new char[count];
		Arrays.fill(arr, character);
		return new String(arr);
	}
}
