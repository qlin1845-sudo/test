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
        assertTrue("执行时间应为正数", performance.getTimeTaken() >= 0);
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
        assertTrue("执行时间应为正数", performance.getTimeTaken() >= 0);
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
        assertTrue("执行时间应为正数", performance.getTimeTaken() >= 0);
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

    // =================================================================
    // New DataStructure Tests
    // =================================================================

    /**
     * 测试 BSTDataStructure 的功能。
     */
    @Test
    public void testBSTDataStructure() {
        DataStructure bst = new BSTDataStructure();
        bst.fromArray(new int[]{5, 3, 7, 1, 4, 6, 8});
        assertEquals("BST 大小应为 7", 7, bst.size());
        assertArrayEquals("toArray 应返回中序遍历结果", new int[]{1, 3, 4, 5, 6, 7, 8}, bst.toArray());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBSTDataStructureGet() {
        new BSTDataStructure().get(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBSTDataStructureSet() {
        new BSTDataStructure().set(0, 1);
    }
    
    /**
     * 测试 HashTableDataStructure 的功能。
     */
    @Test
    public void testHashTableDataStructure() {
        DataStructure ht = new HashTableDataStructure();
        ht.fromArray(new int[]{10, 20, 30});
        assertEquals("哈希表大小应为 3", 3, ht.size());
        ht.add(40);
        assertEquals("添加后大小应为 4", 4, ht.size());
        assertEquals("索引 1 的值应为 20", 20, ht.get(1));
        ht.set(1, 25);
        assertEquals("设置后索引 1 的值应为 25", 25, ht.get(1));
    }

    /**
     * 测试 HeapDataStructure 的功能。
     */
    @Test
    public void testHeapDataStructure() {
        DataStructure heap = new HeapDataStructure();
        heap.fromArray(new int[]{5, 1, 9});
        assertEquals("堆大小应为 3", 3, heap.size());
        heap.add(0);
        assertEquals("添加后大小应为 4", 4, heap.size());
        // toArray 不保证顺序，因此只检查大小
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testHeapDataStructureGet() {
        new HeapDataStructure().get(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHeapDataStructureSet() {
        new HeapDataStructure().set(0, 1);
    }
    
    /**
     * 测试 LinkedListDataStructure 的功能。
     */
    @Test
    public void testLinkedListDataStructure() {
        DataStructure ll = new LinkedListDataStructure();
        ll.fromArray(new int[]{1, 2, 3});
        assertEquals("链表大小应为 3", 3, ll.size());
        assertEquals("索引 1 的值应为 2", 2, ll.get(1));
        ll.set(1, 5);
        assertEquals("设置后索引 1 的值应为 5", 5, ll.get(1));
        assertArrayEquals(new int[]{1, 5, 3}, ll.toArray());
    }
    
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLinkedListDataStructureGetOutOfBounds() {
        DataStructure ll = new LinkedListDataStructure();
        ll.add(1);
        ll.get(1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLinkedListDataStructureSetOutOfBounds() {
        DataStructure ll = new LinkedListDataStructure();
        ll.add(1);
        ll.set(1, 2);
    }

    // =================================================================
    // AlgorithmManager Tests
    // =================================================================

    @Test
    public void testAlgorithmManager() throws Exception {
        AlgorithmManager manager = new AlgorithmManager();
        manager.addAlgorithm(new QuickSort());
        assertNotNull("应能获取 Quick Sort 算法", manager.getAlgorithm("Quick Sort"));
        
        DataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{5, 1, 4, 2, 3});
        manager.sortData("Quick Sort", data);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, data.toArray());
        
        assertEquals(2, manager.searchData("Quick Sort", data, 3));
    }
    
    @Test(expected = AlgorithmNotFoundException.class)
    public void testAlgorithmManagerSortNotFound() throws Exception {
        new AlgorithmManager().sortData("nonexistent", new ArrayDataStructure(1));
    }
    
    @Test(expected = AlgorithmNotFoundException.class)
    public void testAlgorithmManagerSearchNotFound() throws Exception {
        new AlgorithmManager().searchData("nonexistent", new ArrayDataStructure(1), 1);
    }

    // =================================================================
    // ConcurrentAlgorithmManager Tests
    // =================================================================

    @Test
    public void testConcurrentAlgorithmManager() throws Exception {
        ConcurrentAlgorithmManager manager = new ConcurrentAlgorithmManager(2);
        manager.addAlgorithm(new BubbleSort());
        
        DataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{5, 1, 4, 2, 3});

        Future<AlgorithmPerformance> perfFuture = manager.parallelSort("Bubble Sort", data);
        assertNotNull(perfFuture.get());
        
        Future<Integer> searchFuture = manager.parallelSearch("Bubble Sort", data, 4);
        assertEquals(Integer.valueOf(3), searchFuture.get());

        manager.shutdown();
    }

    // =================================================================
    // DynamicAlgorithmManager Tests
    // =================================================================

    @Test
    public void testDynamicAlgorithmManager() throws Exception {
        DynamicAlgorithmManager manager = new DynamicAlgorithmManager(new PerformanceTracker());
        
        // 测试选择逻辑
        DataStructure smallData = new ArrayDataStructure(5);
        smallData.fromArray(new int[]{5, 1, 4, 2, 3});
        assertEquals("OptimizedQuickSort", manager.selectOptimalAlgorithm(smallData));
        
        DataStructure largeData = new ArrayDataStructure(1001);
        for(int i = 1000; i >= 0; i--) largeData.add(i);
        assertEquals("ParallelMergeSort", manager.selectOptimalAlgorithm(largeData));

        DataStructure sortedData = new ArrayDataStructure(5);
        sortedData.fromArray(new int[]{1, 2, 3, 4, 5});
        assertEquals("InsertionSort", manager.selectOptimalAlgorithm(sortedData));
    }

    // =================================================================
    // QuickSort (non-optimized) Tests
    // =================================================================

    @Test
    public void testQuickSort() {
        QuickSort sorter = new QuickSort();
        DataStructure data = new ArrayDataStructure(10);
        data.fromArray(new int[]{5, 1, 9, 3, 7, 4, 8, 6, 2});
        sorter.sort(data);
        assertArrayEquals("数组应按升序排序", new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, data.toArray());
        assertEquals("Quick Sort", sorter.getName());
    }

    // =================================================================
    // MultiThreadedSearch Tests
    // =================================================================

    @Test
    public void testMultiThreadedSearch() throws Exception {
        MultiThreadedSearch searcher = new MultiThreadedSearch(4);
        int[] array = {1, 5, 9, 13, 17, 2, 6, 10, 14, 18, 3, 7, 11, 15, 19, 4, 8, 12, 16, 20};
        
        assertEquals(10, searcher.parallelSearch(array, 3));
        assertEquals(-1, searcher.parallelSearch(array, 100));
        
        searcher.shutdown();
    }

    // =================================================================
    // PerformanceTracker Tests
    // =================================================================

    @Test
    public void testPerformanceTracker() {
        PerformanceTracker tracker = new PerformanceTracker();
        AlgorithmPerformance p1 = new AlgorithmPerformance(100, 0, 0, 0, 1);
        AlgorithmPerformance p2 = new AlgorithmPerformance(50, 0, 0, 0, 1);
        
        tracker.trackPerformance("slow", p1);
        tracker.trackPerformance("fast", p2);
        
        assertEquals(p2, tracker.getBestPerformance());
        // 调用 report 仅为覆盖率
        tracker.generateReport();
    }
}
