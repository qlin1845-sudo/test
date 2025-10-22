package net.mooctest;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

public class AlgorithmFactoryTest {

    private final AlgorithmFactory factory = new AlgorithmFactory();

    // =================================================================
    // AlgorithmFactory Tests
    // =================================================================

    /**
     * 测试工厂是否能正确创建 OptimizedQuickSort 实例。
     */
    @Test
    public void testGetQuickSortAlgorithm() {
        Algorithm algorithm = factory.getAlgorithm("quicksort");
        assertTrue("工厂应为 'quicksort' 返回 OptimizedQuickSort 实例", algorithm instanceof OptimizedQuickSort);
    }

    /**
     * 测试工厂是否能正确创建 BubbleSort 实例。
     */
    @Test
    public void testGetBubbleSortAlgorithm() {
        Algorithm algorithm = factory.getAlgorithm("bubblesort");
        assertTrue("工厂应为 'bubblesort' 返回 BubbleSort 实例", algorithm instanceof BubbleSort);
    }

    /**
     * 测试工厂是否能正确创建 ParallelMergeSort 实例。
     */
    @Test
    public void testGetParallelMergeSortAlgorithm() {
        Algorithm algorithm = factory.getAlgorithm("parallelmergesort");
        assertTrue("工厂应为 'parallelmergesort' 返回 ParallelMergeSort 实例", algorithm instanceof ParallelMergeSort);
    }

    /**
     * 测试工厂在输入未知算法名称时是否会抛出 AlgorithmNotFoundException。
     */
    @Test(expected = AlgorithmNotFoundException.class)
    public void testGetUnknownAlgorithm() {
        factory.getAlgorithm("unknownsort");
    }

    /**
     * 测试工厂对算法名称的大小写不敏感性。
     */
    @Test
    public void testGetAlgorithmCaseInsensitive() {
        Algorithm algorithm = factory.getAlgorithm("QuickSort");
        assertTrue("工厂应为 'QuickSort' 返回 OptimizedQuickSort 实例", algorithm instanceof OptimizedQuickSort);
    }

    // =================================================================
    // OptimizedQuickSort Tests
    // =================================================================

    /**
     * 测试 OptimizedQuickSort 的基本排序功能。
     */
    @Test
    public void testOptimizedQuickSort() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        sorter.sort(data);
        assertArrayEquals("数组应按升序排序", new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, data.toArray());
        assertEquals("获取算法名称", "Optimized Quick Sort", sorter.getName());
    }

    /**
     * 测试 OptimizedQuickSort 对小数组使用插入排序的优化。
     */
    @Test
    public void testOptimizedQuickSortWithInsertionSort() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        DataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{5, 1, 4, 2, 3});
        sorter.sort(data);
        assertArrayEquals("小数组应通过插入排序正确排序", new int[]{1, 2, 3, 4, 5}, data.toArray());
    }

    /**
     * 测试 OptimizedQuickSort 的二分查找功能。
     */
    @Test
    public void testOptimizedQuickSortBinarySearch() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertEquals("应找到元素 5 的索引", 4, sorter.search(data, 5));
        assertEquals("应返回 -1 如果元素未找到", -1, sorter.search(data, 10));
    }
    
    /**
     * 测试 OptimizedQuickSort 的性能评估功能。
     */
    @Test
    public void testOptimizedQuickSortEvaluatePerformance() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertNotNull("性能评估结果不应为 null", performance);
        assertTrue("执行时间应为正数", performance.getTime() >= 0);
    }


    // =================================================================
    // BubbleSort Tests
    // =================================================================

    /**
     * 测试 BubbleSort 的基本排序功能。
     */
    @Test
    public void testBubbleSort() {
        BubbleSort sorter = new BubbleSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        sorter.sort(data);
        assertArrayEquals("数组应按升序排序", new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, data.toArray());
        assertEquals("获取算法名称", "Bubble Sort", sorter.getName());
    }

    /**
     * 测试 BubbleSort 的线性查找功能。
     */
    @Test
    public void testBubbleSortSearch() {
        BubbleSort sorter = new BubbleSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        assertEquals("应找到元素 9 的索引", 2, sorter.search(data, 9));
        assertEquals("应返回 -1 如果元素未找到", -1, sorter.search(data, 10));
    }

    /**
     * 测试 BubbleSort 的性能评估功能。
     */
    @Test
    public void testBubbleSortEvaluatePerformance() {
        BubbleSort sorter = new BubbleSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertNotNull("性能评估结果不应为 null", performance);
        assertTrue("执行时间应为正数", performance.getTime() >= 0);
    }

    // =================================================================
    // ParallelMergeSort Tests
    // =================================================================

    /**
     * 测试 ParallelMergeSort 的基本排序功能。
     */
    @Test
    public void testParallelMergeSort() {
        ParallelMergeSort sorter = new ParallelMergeSort(4);
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2, 0});
        sorter.sort(data);
        assertArrayEquals("数组应按升序排序", new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, data.toArray());
        assertEquals("获取算法名称", "Parallel Merge Sort", sorter.getName());
    }
    
    /**
     * 测试 ParallelMergeSort 对空数组的排序。
     */
    @Test
    public void testParallelMergeSortEmptyArray() {
        ParallelMergeSort sorter = new ParallelMergeSort(4);
        DataStructure data = new ArrayDataStructure(0);
        data.fromArray(new int[]{});
        sorter.sort(data);
        assertArrayEquals("空数组排序后仍为空", new int[]{}, data.toArray());
    }


    /**
     * 测试 ParallelMergeSort 的二分查找功能。
     */
    @Test
    public void testParallelMergeSortBinarySearch() {
        ParallelMergeSort sorter = new ParallelMergeSort(4);
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertEquals("应找到元素 5 的索引", 4, sorter.search(data, 5));
        assertEquals("应返回 -1 如果元素未找到", -1, sorter.search(data, 10));
    }
    
    /**
     * 测试 ParallelMergeSort 的性能评估功能。
     */
    @Test
    public void testParallelMergeSortEvaluatePerformance() {
        ParallelMergeSort sorter = new ParallelMergeSort(4);
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertNotNull("性能评估结果不应为 null", performance);
        assertTrue("执行时间应为正数", performance.getTime() >= 0);
    }


    // =================================================================
    // ArrayDataStructure Tests
    // =================================================================

    /**
     * 测试 ArrayDataStructure 的基本功能。
     */
    @Test
    public void testArrayDataStructure() {
        DataStructure data = new ArrayDataStructure(2);
        data.add(10);
        data.add(20);
        assertEquals("大小应为 2", 2, data.size());
        assertEquals("索引 0 的值应为 10", 10, data.get(0));

        data.add(30); // 触发扩容
        assertEquals("扩容后大小应为 3", 3, data.size());
        assertEquals("索引 2 的值应为 30", 30, data.get(2));

        data.set(0, 5);
        assertEquals("设置后索引 0 的值应为 5", 5, data.get(0));
    }

    /**
     * 测试 ArrayDataStructure 在 get 时索引越界是否抛出异常。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayDataStructureGetOutOfBounds() {
        DataStructure data = new ArrayDataStructure(1);
        data.add(1);
        data.get(1); // 越界
    }


     /**
     * 测试 ArrayDataStructure 在 set 时索引越界是否抛出异常。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayDataStructureSetOutOfBounds() {
        DataStructure data = new ArrayDataStructure(1);
        data.add(1);
        data.set(1, 2); // 越界
    }

    // =================================================================
    // AlgorithmPerformance Tests
    // =================================================================

    /**
     * 测试 AlgorithmPerformance 类的构造函数和 getters。
     */
    @Test
    public void testAlgorithmPerformance() {
        AlgorithmPerformance performance = new AlgorithmPerformance(100, 50, 25, 1024, 4);
        assertEquals("时间应为 100", 100, performance.getTimeTaken());
        assertEquals("比较次数应为 50", 50, performance.getComparisons());
        assertEquals("交换次数应为 25", 25, performance.getSwaps());
        assertEquals("内存使用应为 1024", 1024, performance.getMemoryUsed());
        assertEquals("线程数应为 4", 4, performance.getThreadCount());
    }

    // =================================================================
    // Exception Tests
    // =================================================================

    /**
     * 测试 AlgorithmNotFoundException 的消息是否正确设置。
     */
    @Test
    public void testAlgorithmNotFoundException() {
        try {
            factory.getAlgorithm("nonexistent");
        } catch (AlgorithmNotFoundException e) {
            assertEquals("异常消息应匹配", "Algorithm not found: nonexistent", e.getMessage());
        }
    }
}
