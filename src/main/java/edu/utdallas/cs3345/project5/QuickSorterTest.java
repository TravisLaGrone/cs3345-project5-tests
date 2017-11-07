package edu.utdallas.cs3345.project5;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.function.Executable;

import edu.utdallas.cs3345.project5.QuickSorter.PivotStrategy;
import junit.framework.AssertionFailedError;

@TestInstance(Lifecycle.PER_CLASS)
class QuickSorterTest
{
	static final String FIRST_ELEMENT_SORT_CORRECTNESS_TAG = "Sort.Correctness.FirstElement";
	static final String RANDOM_ELEMENT_SORT_CORRECTNESS_TAG = "Sort.Correctness.RandomElement";
	static final String MEDIAN_OF_THREE_SORT_CORRECTNESS_TAG = "Sort.Correctness.MedianThree";
	static final String MEDIAN_OF_THREE_RANDOM_SORT_CORRECTNESS_TAG = "Sort.Correctness.MedianThreeRandom";
	static final String TIME_MEASUREMENT_TAG = "Sort.TimeMeasurement";
	static final String RANDOM_LIST_GENERATION_TAG = "RandomListGeneration";
	static final String INPUT_VALIDATION_TAG = "InputValidation";
	static final List<String> TAGS;
	
	static final Duration SHORT_TIMEOUT = Duration.ofMillis(5);
	static final Duration MEDIUM_TIMEOUT = Duration.ofMillis(25);
	static final Duration LONG_TIMEOUT = Duration.ofMillis(125);
	static final Duration VERY_LONG_TIMEOUT = Duration.ofMillis(625);
	
	static final int NUM_LISTS = 3;
	static final int NUM_ELEMS = 20;
	static final ArrayList<Integer> EXPECTED_LIST = new ArrayList<>(
			IntStream.range(0,NUM_ELEMS).mapToObj(i -> i).collect(Collectors.toList()) );
	
	static {
			TAGS = Collections.unmodifiableList(Arrays.asList(
					FIRST_ELEMENT_SORT_CORRECTNESS_TAG,
					RANDOM_ELEMENT_SORT_CORRECTNESS_TAG,
					MEDIAN_OF_THREE_SORT_CORRECTNESS_TAG,
					MEDIAN_OF_THREE_RANDOM_SORT_CORRECTNESS_TAG,
					TIME_MEASUREMENT_TAG,
					RANDOM_LIST_GENERATION_TAG,
					INPUT_VALIDATION_TAG
			));
	}
	
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class InputValidationTest
	{
		@Tag(INPUT_VALIDATION_TAG)
		@Test
		void testTimedQuickSortWithNullList()
		{
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> assertThrows(NullPointerException.class,
							() -> QuickSorter.timedQuickSort( null, PivotStrategy.FIRST_ELEMENT ),
					"Suspected infinite loop"
			));
		}
		
		@Tag(INPUT_VALIDATION_TAG)
		@Test
		void testTimedQuickSortWithNullStrategy()
		{
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> assertThrows(NullPointerException.class,
							() -> QuickSorter.timedQuickSort( new ArrayList<>(Arrays.asList(1, 2, 3)), null ),
					"Suspected infinite loop"
					
			));
		}
		
		@Tag(INPUT_VALIDATION_TAG)
		@Test
		void testGenerateRandomListWithNegativeSize()
		{
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> assertThrows(IllegalArgumentException.class,
							() -> QuickSorter.generateRandomList( -1 ),
					"Suspected infinite loop"
			));
		}
	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class GenerateRandomlistTest
	{
		final int SIZE = 100;
		
		@Tag(RANDOM_LIST_GENERATION_TAG)
		@Test
		void testGenerateRandomList()
		{
			List<ArrayList<Integer>> lists = new LinkedList<>();
			for (int i = 0; i < 3; ++i) {
				assertTimeoutPreemptively(MEDIUM_TIMEOUT,
						() -> lists.add(QuickSorter.generateRandomList(SIZE)),
						"Suspected infinite loop"
				);
			}
			
			boolean noListNull = lists.stream().noneMatch(list -> list == null);
			assertTrue(noListNull, "returned a null list");
			
			boolean noElementNull = lists.stream().flatMap(list -> list.stream()).noneMatch(e -> e == null);
			assertTrue(noElementNull, "returned a list containing a null element");
			
			assert SIZE > -1;
			boolean allCorrectSize = lists.stream().allMatch(list -> list.size() == SIZE);
			assertTrue(allCorrectSize, "returned a list with a different size than specified");
			
			assert SIZE > 0;
			boolean notAllEqual = ! lists.stream().allMatch(list -> lists.get(0).equals(list));
			assertTrue(notAllEqual, "always returns the same list");
			
			assert SIZE > 1;
			boolean notAllSorted = ! lists.stream().allMatch(list -> isSorted(list));
			assertTrue(notAllSorted, "always returns a list that is already sorted");
		}
	}
	
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class QuickSortFirstElementStrategyTest
	{
		final String TAG = FIRST_ELEMENT_SORT_CORRECTNESS_TAG;
		PivotStrategy STRATEGY;
		
		@BeforeEach
		void setUp()
		{
			STRATEGY = PivotStrategy.FIRST_ELEMENT;  // reset each time in case student implement stateful enum
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithEmptyList()
		{
			List<Integer> elements = Arrays.asList();  // empty list
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithUnaryList()
		{
			List<Integer> elements = Arrays.asList( 1 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithBinaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithTernaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2, 3 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithObjectsOtherThanInteger()
		{
			List<String> elements = Arrays.asList( "a", "b", "c" );
			ArrayList<String> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithAllDuplicates()
		{
			List<Integer> elements = Arrays.asList( 2, 2, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY), // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
	
		@Tag(TAG)
		@Test
		void testQuickSortCorrectnessWithThreeLongLists()
		{
			List<ArrayList<Integer>> testLists = generateTestLists();
			
			testLists.forEach(list ->
					assertTimeoutPreemptively(LONG_TIMEOUT,
							() -> QuickSorter.timedQuickSort(list, STRATEGY),
							"Suspected infinite loop"));
			
			assertAll(testLists.stream().map(list -> () ->  // for each test list...
					assertEquals(EXPECTED_LIST, list)));
		}
	}
	
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class QuickSortRandomElementStrategyTest
	{
		final String TAG = RANDOM_ELEMENT_SORT_CORRECTNESS_TAG;
		PivotStrategy STRATEGY;
		
		@BeforeEach
		void setUp()
		{
			STRATEGY = PivotStrategy.RANDOM_ELEMENT;  // reset each time in case student implement stateful enum
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithEmptyList()
		{
			List<Integer> elements = Arrays.asList();  // empty list
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithUnaryList()
		{
			List<Integer> elements = Arrays.asList( 1 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithBinaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithTernaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2, 3 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithObjectsOtherThanInteger()
		{
			List<String> elements = Arrays.asList( "a", "b", "c" );
			ArrayList<String> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithAllDuplicates()
		{
			List<Integer> elements = Arrays.asList( 2, 2, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY), // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
	
		@Tag(TAG)
		@Test
		void testQuickSortCorrectnessWithThreeLongLists()
		{
			List<ArrayList<Integer>> testLists = generateTestLists();
			
			testLists.forEach(list ->
					assertTimeoutPreemptively(LONG_TIMEOUT,
							() -> QuickSorter.timedQuickSort(list, STRATEGY),
							"Suspected infinite loop"));
			
			assertAll(testLists.stream().map(list -> () ->  // for each test list...
					assertEquals(EXPECTED_LIST, list)));
		}
	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class QuickSortMedianOfThreeStrategyTest
	{
		final String TAG = MEDIAN_OF_THREE_SORT_CORRECTNESS_TAG;
		PivotStrategy STRATEGY;
		
		@BeforeEach
		void setUp()
		{
			STRATEGY = PivotStrategy.MEDIAN_OF_THREE_ELEMENTS;  // reset each time in case student implement stateful enum
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithEmptyList()
		{
			List<Integer> elements = Arrays.asList();  // empty list
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithUnaryList()
		{
			List<Integer> elements = Arrays.asList( 1 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithBinaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithTernaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2, 3 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithObjectsOtherThanInteger()
		{
			List<String> elements = Arrays.asList( "a", "b", "c" );
			ArrayList<String> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithAllDuplicates()
		{
			List<Integer> elements = Arrays.asList( 2, 2, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY), // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
	
		@Tag(TAG)
		@Test
		void testQuickSortCorrectnessWithThreeLongLists()
		{
			List<ArrayList<Integer>> testLists = generateTestLists();
			
			testLists.forEach(list ->
					assertTimeoutPreemptively(LONG_TIMEOUT,
							() -> QuickSorter.timedQuickSort(list, STRATEGY),
							"Suspected infinite loop"));
			
			assertAll(testLists.stream().map(list -> () ->  // for each test list...
					assertEquals(EXPECTED_LIST, list)));
		}
	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class QuickSortMedianOfThreeRandomStrategyTest
	{
		final String TAG = MEDIAN_OF_THREE_RANDOM_SORT_CORRECTNESS_TAG;
		PivotStrategy STRATEGY;
		
		@BeforeEach
		void setUp()
		{
			STRATEGY = PivotStrategy.MEDIAN_OF_THREE_RANDOM_ELEMENTS;  // reset each time in case student implement stateful enum
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithEmptyList()
		{
			List<Integer> elements = Arrays.asList();  // empty list
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithUnaryList()
		{
			List<Integer> elements = Arrays.asList( 1 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
			
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgrammaticStabilityWithBinaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithTernaryList()
		{
			List<Integer> elements = Arrays.asList( 1, 2, 3 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithObjectsOtherThanInteger()
		{
			List<String> elements = Arrays.asList( "a", "b", "c" );
			ArrayList<String> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY),  // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
		
		@Tag(TAG)
		@Test
		void testQuickSortProgammaticStabilityWithAllDuplicates()
		{
			List<Integer> elements = Arrays.asList( 2, 2, 2 );
			ArrayList<Integer> list = new ArrayList<>(elements);
			int expectedSize = list.size();
			
			assertTimeoutPreemptively(SHORT_TIMEOUT,
					() -> QuickSorter.timedQuickSort(list, STRATEGY), // fails test if throws an exception
					"Suspected infinite loop");
						
			assertNotNull(list, "returned list is null");
			assertAll(
					() -> assertEquals(expectedSize, list.size(),
							"list size incorrectly changed due to sorting"),
					() -> assertTrue(list.stream().noneMatch(e -> e == null),
							"some list element was incorrectly made null during sorting"));
			assertEquals(new HashSet<>(elements), new HashSet<>(list),
							"the value of one or more elements was incorrectly changed during sorting");
		}
	
		@Tag(TAG)
		@Test
		void testQuickSortCorrectnessWithThreeLongLists()
		{
			List<ArrayList<Integer>> testLists = generateTestLists();
			
			testLists.forEach(list ->
					assertTimeoutPreemptively(LONG_TIMEOUT,
							() -> QuickSorter.timedQuickSort(list, STRATEGY),
							"Suspected infinite loop"));
			
			assertAll(testLists.stream().map(list -> () ->  // for each test list...
					assertEquals(EXPECTED_LIST, list)));
		}
	}
		
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class QuickSortTimingTest
	{
		/**
		 * "first element" is chosen as the default pivot selection strategy since it is the simplest to
		 * implement correctly.
		 */
		PivotStrategy DEFAULT_STRATEGY = PivotStrategy.FIRST_ELEMENT;
		
		@BeforeEach
		void setUp()
		{
			DEFAULT_STRATEGY = PivotStrategy.FIRST_ELEMENT;  // reset each time in case student implement stateful enum
		}
		
		@Tag(TIME_MEASUREMENT_TAG)
		@Test
		void testTimedQuickSortReturnNonNull()
		{
			ArrayList<Integer> emptyList = new ArrayList<>();
			ArrayList<Integer> sortedList = new ArrayList<>(Arrays.asList(1, 2, 3));
			ArrayList<Integer> unsortedList = new ArrayList<>(Arrays.asList(3, 1, 2));
			
			assertAll(
					() -> assertTimeoutPreemptively(SHORT_TIMEOUT,
							() -> assertNotNull(
									QuickSorter.timedQuickSort(emptyList, DEFAULT_STRATEGY),
									"Returned null given an empty list"),
							"Suspected infinite loop"),
					() -> assertTimeoutPreemptively(SHORT_TIMEOUT, 
							() -> assertNotNull(
									QuickSorter.timedQuickSort(sortedList, DEFAULT_STRATEGY),
									"Returned null given a sorted ternary list"),
							"Suspected infinite loop"),
					() -> assertTimeoutPreemptively(SHORT_TIMEOUT,
							() -> assertNotNull(
									QuickSorter.timedQuickSort(unsortedList, DEFAULT_STRATEGY),
									"Returned null given an unsorted ternary list"),
							"Suspected infinite loop")
			);
		}
		
		@Tag(TIME_MEASUREMENT_TAG)
		@Test
		void testTimedQuickSortTimeMeasurementIsReasonable()
		{
			final int LARGE_SIZE = 500;
			
			ArrayList<Integer> largeList = new ArrayList<>(
					new Random().ints(LARGE_SIZE).mapToObj(i -> i).collect(Collectors.toList()));
			
			Duration lowerBound;
			Duration upperBound;
			Duration actualTime;
			Alias<Duration> aliasActualTime = new Alias<>();
			
			// sorting a list must take at least as long as verifying that the list is sorted
			lowerBound = time(() -> isSorted(largeList));
			
			// timing the timing of the sorting must take at least as long as timing the sorting
			upperBound = time(() -> assertTimeoutPreemptively(VERY_LONG_TIMEOUT,
					() -> {
						// the actual time is what the student says it takes
						Duration actualTimeLocal = QuickSorter.timedQuickSort(largeList, DEFAULT_STRATEGY);
						aliasActualTime.set(actualTimeLocal);  // necessary since Java doesn't support modifying environment references
					},
					"Suspected infinite loop"
			));
			
			actualTime = aliasActualTime.get();  // necessary since Java doesn't support modifying environment references
			
			assertTrue(lowerBound.compareTo(actualTime) <= 0, "Student's returned QuickSort duration is too low" + ENDL
					+ "Lower bound: " + lowerBound.toNanos() + " nanos" + " (approximated time to verify if sorted)" + ENDL
					+ "Student's claim: " + actualTime.toNanos() + " nanos"); 
			assertTrue(upperBound.compareTo(actualTime) >= 0, "Student's returned QuickSort duration is too high" + ENDL
					+ "Upper bound: " + upperBound.toNanos() + " nanos" + " (approximated time to time the student's timing)" + ENDL
					+ "Student's claim: " + actualTime.toNanos() + " nanos");
		}
	}

	
	///////////////
	// UTILITIES //
	///////////////
	
	static final String ENDL = System.lineSeparator();
	
	/**
	 * Determines whether a list is sorted according to the natural ordering of its elements.
	 * 
	 * @param list
	 * @return
	 */
	static <E extends Comparable<E>> boolean isSorted(List<E> list)
	{
		for (int i = 1; i < list.size(); ++i) {
			if (! (list.get(i-1).compareTo(list.get(i)) <= 0))
				return false;
		}
		return true;
	}

	/**
	 * Returns a shallow copy of a list, where the copy is sorted according to the natural ordering of its
	 * elements.
	 * 
	 * @param list
	 * @return
	 */
	static <T extends List<E>, E extends Comparable<E>> ArrayList<E> sorted(T list)
	{
		ArrayList<E> sortedList = new ArrayList<>(list);
		sortedList.sort(null);  // sorts by natural ordering
		return sortedList;
	}

	/**
	 * Executes the given executable, then returns the time in nanoseconds that it took to execute.
	 * 
	 * @param executable
	 * @return
	 */
	static Duration time(Executable executable)
	{
		try {
			long startTime = System.nanoTime();
			executable.execute();
			long finishTime = System.nanoTime();
			return Duration.ofNanos(finishTime - startTime);
		}
		catch (Throwable thr) {
			if (thr instanceof AssertionFailedError)
				throw (AssertionFailedError) thr;
			else
				throw new RuntimeException(thr);
		}
	}

	/**
     * A mutable reference to some variable.  Intended for use with lambdas that need to modify context
     * variables, since Java lambdas can only capture final or effectively final variables.  Originally
     * created for use with <code>assertTimeoutPreemptively()</code> to facilitate defending against
     * infinite loops in student code.
     * <p>
     * Use with caution!  Beware of situations where the lambda may be executed at later point in time than
     * is implied by its lexical location in the source code.
     *
     * @param <T>
     */
    static class Alias <T>
    {
    		private T value;

    		public Alias() { }
    		
    		public Alias(T value) { this.value = value; }

    		public T get() { return this.value; }

    		public void set(T value) { this.value = value; }
    }
	
    /**
     * @deprecated Redundant since {@link org.junit.jupiter.api.Assertions.assertEquals} already displays
     * the string representation of the expected and actual values.
     */
    @Deprecated
    static String stringifyLists(ArrayList<Integer> expectedList, ArrayList<Integer> actualList)
	{
		List<String> strExpectedElems = expectedList.stream()
				.map(i -> Objects.toString(i))
				.collect(Collectors.toList());
		List<String> strActualElems = actualList.stream()
				.map(i -> Objects.toString(i))
				.collect(Collectors.toList());
		
		final int maxElemLength = Stream.concat(strExpectedElems.stream(), strActualElems.stream())
				.mapToInt(s -> s.length())
				.max()
				.getAsInt();
		
		String strExpectedList = strExpectedElems.stream()
				.map(s -> leftPad(s, ' ', maxElemLength))
				.collect(Collectors.joining(", ", "[ ", " ]"));
		String strActualList = strActualElems.stream()
				.map(s -> leftPad(s, ' ', maxElemLength))
				.collect(Collectors.joining(", ", "[ ", " ]"));
		
		StringBuilder sb = new StringBuilder();
		sb.append("Expected: ").append(strExpectedList).append(System.lineSeparator());
		sb.append("Actual:   ").append(strActualList);
		
		return sb.toString();
	}

    static StringBuilder leftPad(CharSequence source, char character, int desiredMinTotalLen)
    {
    		StringBuilder sb = new StringBuilder();
    		
    		final int times = Math.max(0, desiredMinTotalLen - source.length());
    		for (int i = 0; i < times; ++i)
    			sb.append(character);
    		sb.append(source);
    				
    		return sb;
    }

    /**
     * Returns a list of {@link #NUM_LISTS} array lists that each comprise the elements of {@link
     * #EXPECTED_LIST} in different shuffled orders.
     * @return
     */
    static List<ArrayList<Integer>> generateTestLists()
    {
    		List<ArrayList<Integer>> lists = new ArrayList<>(NUM_LISTS);
    		for (int i = 0; i < NUM_LISTS; ++i) {
    			ArrayList<Integer> list = new ArrayList<>(EXPECTED_LIST);  // create shallow copy (ok since Integer immutable)
			Collections.shuffle(list);  // randomize order
			lists.add(list);
    		}
    		return lists;
    }
    
}
