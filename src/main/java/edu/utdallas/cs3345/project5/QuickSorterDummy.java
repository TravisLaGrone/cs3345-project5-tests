package edu.utdallas.cs3345.project5;

import java.time.Duration;
import java.util.ArrayList;

public class QuickSorterDummy
{

    /*
     * This private constructor is optional, but it does help to prevent accidental client instantiation of QuickSorter
     * via the default constructor.  (defining any constructor prevents the compiler from creating a default constructor)
     * This particular anti-instantiation technique is exactly what {@link java.util.Collections} does.
     */
    private QuickSorterDummy() { }

    public static <E extends Comparable<E>> Duration timedQuickSort(ArrayList<E> list, PivotStrategy strategy)
    {
        // TODO implement timedQuickSort(ArrayList<E>, PivotStrategy)
    		throw new UnsupportedOperationException("not implemented yet");
    }

    public static ArrayList<Integer> generateRandomList(int size)
    {
        // TODO implement generateRandomList(int)
    		throw new UnsupportedOperationException("not implemented yet");
    }

    public static enum PivotStrategy
    {
        FIRST_ELEMENT,
        RANDOM_ELEMENT,
        MEDIAN_OF_THREE_RANDOM_ELEMENTS,
        MEDIAN_OF_FIRST_AND_LAST_ELEMENTS
    }

}
