package net.mooctest;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

public class AlgorithmFactoryTest {

    private final AlgorithmFactory factory = new AlgorithmFactory();

    // ========================================================
    // AlgorithmFactory 测试
    // ========================================================

    /**
     * 验证工厂能否返回优化快速排序的具体实现。
     */
    @Test
    public void testFactoryReturnsOptimizedQuickSort() {
        Algorithm algorithm = factory.getAlgorithm("quicksort");
        assertTrue("应返回 OptimizedQuickSort 实例", algorithm instanceof OptimizedQuickSort);
    }

    /**
     * 验证工厂能否返回冒泡排序的实现。
     */
    @Test
    public void testFactoryReturnsBubbleSort() {
        Algorithm algorithm = factory.getAlgorithm("bubblesort");
        assertTrue("应返回 BubbleSort 实例", algorithm instanceof BubbleSort);
    }

    /**
     * 验证工厂能否返回并行归并排序的实现。
     */
    @Test
    public void testFactoryReturnsParallelMergeSort() {
        Algorithm algorithm = factory.getAlgorithm("parallelmergesort");
        assertTrue("应返回 ParallelMergeSort 实例", algorithm instanceof ParallelMergeSort);
    }

    /**
     * 验证工厂对大小写不敏感的处理逻辑。
     */
    @Test
    public void testFactoryCaseInsensitive() {
        Algorithm algorithm = factory.getAlgorithm("QuickSort");
        assertTrue("大小写不敏感应返回 OptimizedQuickSort 实例", algorithm instanceof OptimizedQuickSort);
    }

    /**
     * 验证请求未知算法时能够抛出预期异常。
     */
    @Test(expected = AlgorithmNotFoundException.class)
    public void testFactoryUnknownAlgorithmThrows() {
        factory.getAlgorithm("does-not-exist");
    }

    // ========================================================
    // OptimizedQuickSort 测试
    // ========================================================

    /**
     * 验证优化快速排序在大规模数组上能够触发分治逻辑并正确排序。
     */
    @Test
    public void testOptimizedQuickSortHandlesLargeArray() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        ArrayDataStructure data = new ArrayDataStructure(20);
        data.fromArray(new int[]{20, 1, 18, 3, 16, 5, 14, 7, 12, 9, 10, 11, 8, 13, 6, 15, 4, 17, 2, 19});
        int[] original = data.toArray();
        sorter.sort(data);
        int[] actual = data.toArray();
        int[] normalized = Arrays.copyOf(actual, actual.length);
        Arrays.sort(normalized);
        assertArrayEquals("排序后的元素集合应与期望一致", createSequentialArray(1, 20), normalized);
        assertFalse("排序过程应改变原始顺序", Arrays.equals(original, actual));
    }

    /**
     * 验证优化快速排序在小数组上能够触发插入排序优化。
     */
    @Test
    public void testOptimizedQuickSortInsertionSortBranch() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        ArrayDataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{3, 1, 2, 5, 4});
        sorter.sort(data);
        assertArrayEquals("插入排序分支应保持正确顺序", new int[]{1, 2, 3, 4, 5}, data.toArray());
    }

    /**
     * 验证优化快速排序的二分查找逻辑。
     */
    @Test
    public void testOptimizedQuickSortBinarySearch() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        ArrayDataStructure data = new ArrayDataStructure(6);
        data.fromArray(new int[]{1, 2, 3, 4, 5, 6});
        assertEquals("目标存在时应返回正确索引", 3, sorter.search(data, 4));
        assertEquals("目标不存在时应返回 -1", -1, sorter.search(data, 100));
    }

    /**
     * 验证三数取中枢轴策略中的各个交换分支。
     */
    @Test
    public void testOptimizedQuickSortMedianOfThreeBranches() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        ArrayDataStructure data = new ArrayDataStructure(3);
        data.fromArray(new int[]{3, 2, 1});
        int pivot = sorter.medianOfThree(data, 0, 2);
        assertEquals("应选出中间的数作为枢轴", 2, pivot);
        assertArrayEquals("三数取中之后数组应被调整", new int[]{1, 2, 3}, data.toArray());
    }

    /**
     * 验证性能评估方法能够返回有效统计信息。
     */
    @Test
    public void testOptimizedQuickSortEvaluatePerformance() {
        OptimizedQuickSort sorter = new OptimizedQuickSort();
        ArrayDataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{5, 4, 3, 2, 1});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertNotNull("性能对象不应为空", performance);
        assertTrue("耗时应为非负值", performance.getTimeTaken() >= 0);
        assertEquals("线程数应固定为 1", 1, performance.getThreadCount());
    }

    // ========================================================
    // BubbleSort 测试
    // ========================================================

    /**
     * 验证冒泡排序能够对随机数组进行排序。
     */
    @Test
    public void testBubbleSortSortsData() {
        BubbleSort sorter = new BubbleSort();
        ArrayDataStructure data = new ArrayDataStructure(6);
        data.fromArray(new int[]{6, 5, 4, 3, 2, 1});
        sorter.sort(data);
        assertArrayEquals("冒泡排序结果应为升序", new int[]{1, 2, 3, 4, 5, 6}, data.toArray());
    }

    /**
     * 验证冒泡排序提供的线性查找能力。
     */
    @Test
    public void testBubbleSortLinearSearch() {
        BubbleSort sorter = new BubbleSort();
        ArrayDataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{4, 1, 3, 5, 2});
        assertEquals("找到目标值应返回正确位置", 2, sorter.search(data, 3));
        assertEquals("未找到目标值应返回 -1", -1, sorter.search(data, 9));
    }

    /**
     * 验证冒泡排序的性能评估逻辑。
     */
    @Test
    public void testBubbleSortEvaluatePerformanceMaintainsPositiveMetrics() {
        BubbleSort sorter = new BubbleSort();
        ArrayDataStructure data = new ArrayDataStructure(4);
        data.fromArray(new int[]{4, 3, 2, 1});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertNotNull("应返回性能统计对象", performance);
        assertTrue("比较次数应为非负", performance.getComparisons() >= 0);
        assertTrue("交换次数应为非负", performance.getSwaps() >= 0);
    }

    // ========================================================
    // ParallelMergeSort 测试
    // ========================================================

    /**
     * 验证并行归并排序的排序正确性。
     */
    @Test
    public void testParallelMergeSortSortsData() {
        ParallelMergeSort sorter = new ParallelMergeSort(4);
        ArrayDataStructure data = new ArrayDataStructure(8);
        data.fromArray(new int[]{8, 4, 6, 2, 7, 3, 5, 1});
        sorter.sort(data);
        assertArrayEquals("并行归并排序应输出有序数组", new int[]{1, 2, 3, 4, 5, 6, 7, 8}, data.toArray());
    }

    /**
     * 验证并行归并排序的二分查找能力。
     */
    @Test
    public void testParallelMergeSortBinarySearch() {
        ParallelMergeSort sorter = new ParallelMergeSort(2);
        ArrayDataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{1, 3, 5, 7, 9});
        assertEquals("存在的目标应返回索引", 4, sorter.search(data, 9));
        assertEquals("不存在的目标应返回 -1", -1, sorter.search(data, 0));
    }

    /**
     * 验证并行归并排序在空数组上的表现。
     */
    @Test
    public void testParallelMergeSortOnEmptyArray() {
        ParallelMergeSort sorter = new ParallelMergeSort(2);
        ArrayDataStructure data = new ArrayDataStructure(0);
        data.fromArray(new int[]{});
        sorter.sort(data);
        assertEquals("空数组排序后仍为空", 0, data.size());
    }

    /**
     * 验证并行归并排序在单元素数组上不会触发递归。
     */
    @Test
    public void testParallelMergeSortSingleElementNoWork() {
        ParallelMergeSort sorter = new ParallelMergeSort(2);
        ArrayDataStructure data = new ArrayDataStructure(1);
        data.fromArray(new int[]{42});
        sorter.sort(data);
        assertArrayEquals("单元素数组保持不变", new int[]{42}, data.toArray());
    }

    /**
     * 验证性能评估能够记录并行度信息。
     */
    @Test
    public void testParallelMergeSortEvaluatePerformanceRecordsParallelism() {
        ParallelMergeSort sorter = new ParallelMergeSort(3);
        ArrayDataStructure data = new ArrayDataStructure(6);
        data.fromArray(new int[]{6, 5, 4, 3, 2, 1});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertEquals("线程数应记录构造时的并行度", 3, performance.getThreadCount());
        assertTrue("比较次数应为非负", performance.getComparisons() >= 0);
    }

    // ========================================================
    // ArrayDataStructure 测试
    // ========================================================

    /**
     * 验证数组结构在扩容场景下的行为。
     */
    @Test
    public void testArrayDataStructureAddAndExpand() {
        ArrayDataStructure data = new ArrayDataStructure(2);
        data.add(1);
        data.add(2);
        data.add(3);
        assertEquals("扩容后应包含三个元素", 3, data.size());
        assertArrayEquals("数据内容应保持正确", new int[]{1, 2, 3}, data.toArray());
    }

    /**
     * 验证数组结构支持 fromArray 与 set 操作。
     */
    @Test
    public void testArrayDataStructureFromArrayAndMutate() {
        ArrayDataStructure data = new ArrayDataStructure(1);
        data.fromArray(new int[]{9, 8, 7});
        data.set(1, 100);
        assertEquals("设置后的值应被读取", 100, data.get(1));
        data.fromArray(new int[]{5});
        assertArrayEquals("重新加载后只保留新数据", new int[]{5}, data.toArray());
    }

    /**
     * 验证越界读取会抛出异常。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayDataStructureGetOutOfBounds() {
        ArrayDataStructure data = new ArrayDataStructure(1);
        data.add(1);
        data.get(1);
    }

    /**
     * 验证越界写入会抛出异常。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayDataStructureSetOutOfBounds() {
        ArrayDataStructure data = new ArrayDataStructure(1);
        data.add(1);
        data.set(1, 2);
    }

    // ========================================================
    // BSTDataStructure 测试
    // ========================================================

    /**
     * 验证二叉搜索树的中序遍历结果。
     */
    @Test
    public void testBSTDataStructureInorderTraversal() {
        BSTDataStructure bst = new BSTDataStructure();
        bst.fromArray(new int[]{5, 3, 7, 1, 4, 6, 8});
        assertEquals("树结构应统计正确大小", 7, bst.size());
        assertArrayEquals("中序遍历应为有序数组", new int[]{1, 3, 4, 5, 6, 7, 8}, bst.toArray());
    }

    /**
     * 验证 BST 不支持随机访问。
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testBSTDataStructureGetUnsupported() {
        new BSTDataStructure().get(0);
    }

    /**
     * 验证 BST 不支持按索引写入。
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testBSTDataStructureSetUnsupported() {
        new BSTDataStructure().set(0, 1);
    }

    /**
     * 验证 BST 的 fromArray 会清空旧数据并重建结构。
     */
    @Test
    public void testBSTDataStructureFromArrayResetsState() {
        BSTDataStructure bst = new BSTDataStructure();
        bst.add(100);
        bst.fromArray(new int[]{2, 1, 3});
        assertEquals("重新构建后大小应更新", 3, bst.size());
        assertArrayEquals("重新构建后应包含新数据", new int[]{1, 2, 3}, bst.toArray());
    }

    // ========================================================
    // HashTableDataStructure 测试
    // ========================================================

    /**
     * 验证哈希表结构的基本操作。
     */
    @Test
    public void testHashTableDataStructureOperations() {
        HashTableDataStructure table = new HashTableDataStructure();
        table.fromArray(new int[]{10, 20, 30});
        table.add(40);
        table.set(1, 200);
        assertEquals("读取应返回最新写入的值", 200, table.get(1));
        Set<Integer> values = new HashSet<>();
        for (int value : table.toArray()) {
            values.add(value);
        }
        assertEquals("集合中应包含四个不同值", 4, values.size());
        assertTrue("集合应包含自定义值", values.contains(200));
    }

    // ========================================================
    // HeapDataStructure 测试
    // ========================================================

    /**
     * 验证堆结构的入堆与 toArray 行为。
     */
    @Test
    public void testHeapDataStructureOperations() {
        HeapDataStructure heap = new HeapDataStructure();
        heap.fromArray(new int[]{5, 1, 3});
        heap.add(2);
        assertEquals("堆大小应正确统计", 4, heap.size());
        int[] array = heap.toArray();
        Arrays.sort(array);
        assertArrayEquals("堆转换后的数组包含所有元素", new int[]{1, 2, 3, 5}, array);
    }

    /**
     * 验证堆不支持按索引读取。
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testHeapDataStructureGetUnsupported() {
        new HeapDataStructure().get(0);
    }

    /**
     * 验证堆不支持按索引写入。
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testHeapDataStructureSetUnsupported() {
        new HeapDataStructure().set(0, 1);
    }

    // ========================================================
    // LinkedListDataStructure 测试
    // ========================================================

    /**
     * 验证链表结构的增删改查基础能力。
     */
    @Test
    public void testLinkedListDataStructureBasicOperations() {
        LinkedListDataStructure list = new LinkedListDataStructure();
        list.fromArray(new int[]{1, 2, 3});
        list.add(4);
        assertEquals("链表大小应更新", 4, list.size());
        assertEquals("读取中间元素应正确", 2, list.get(1));
        list.set(1, 99);
        assertEquals("修改后应返回新值", 99, list.get(1));
        assertArrayEquals("转换数组应体现所有更改", new int[]{1, 99, 3, 4}, list.toArray());
    }

    /**
     * 验证链表越界读取异常。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLinkedListDataStructureGetOutOfBounds() {
        LinkedListDataStructure list = new LinkedListDataStructure();
        list.add(1);
        list.get(1);
    }

    /**
     * 验证链表越界写入异常。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLinkedListDataStructureSetOutOfBounds() {
        LinkedListDataStructure list = new LinkedListDataStructure();
        list.add(1);
        list.set(1, 2);
    }

    // ========================================================
    // AlgorithmPerformance 测试
    // ========================================================

    /**
     * 验证性能对象的属性访问器。
     */
    @Test
    public void testAlgorithmPerformanceAccessors() {
        AlgorithmPerformance performance = new AlgorithmPerformance(100L, 10, 5, 256L, 3);
        assertEquals(100L, performance.getTimeTaken());
        assertEquals(10, performance.getComparisons());
        assertEquals(5, performance.getSwaps());
        assertEquals(256L, performance.getMemoryUsed());
        assertEquals(3, performance.getThreadCount());
    }

    /**
     * 验证性能报告打印流程不会抛出异常。
     */
    @Test
    public void testAlgorithmPerformanceReportDoesNotError() {
        AlgorithmPerformance performance = new AlgorithmPerformance(0L, 0, 0, 0L, 1);
        performance.report();
    }

    // ========================================================
    // AlgorithmManager 测试
    // ========================================================

    /**
     * 验证算法管理器能够添加、排序与搜索数据。
     */
    @Test
    public void testAlgorithmManagerSortAndSearch() throws Exception {
        AlgorithmManager manager = new AlgorithmManager();
        manager.addAlgorithm(new QuickSort());
        ArrayDataStructure data = new ArrayDataStructure(5);
        data.fromArray(new int[]{5, 4, 3, 2, 1});
        manager.sortData("Quick Sort", data);
        assertArrayEquals("管理器应驱动排序算法生效", new int[]{1, 2, 3, 4, 5}, data.toArray());
        assertEquals("排序后应支持二分查找", 2, manager.searchData("Quick Sort", data, 3));
    }

    /**
     * 验证未找到算法时 getAlgorithm 返回 null。
     */
    @Test
    public void testAlgorithmManagerGetAlgorithmMissing() {
        AlgorithmManager manager = new AlgorithmManager();
        assertNull("未注册的算法应返回 null", manager.getAlgorithm("Unknown"));
    }

    /**
     * 验证排序时缺少算法会抛出异常。
     */
    @Test(expected = AlgorithmNotFoundException.class)
    public void testAlgorithmManagerSortMissingThrows() throws Exception {
        AlgorithmManager manager = new AlgorithmManager();
        manager.sortData("Unknown", new ArrayDataStructure(1));
    }

    /**
     * 验证搜索时缺少算法会抛出异常。
     */
    @Test(expected = AlgorithmNotFoundException.class)
    public void testAlgorithmManagerSearchMissingThrows() throws Exception {
        AlgorithmManager manager = new AlgorithmManager();
        manager.searchData("Unknown", new ArrayDataStructure(1), 1);
    }

    // ========================================================
    // ConcurrentAlgorithmManager 测试
    // ========================================================

    /**
     * 验证并发算法管理器的排序与搜索流程。
     */
    @Test
    public void testConcurrentAlgorithmManagerParallelOperations() throws Exception {
        ConcurrentAlgorithmManager manager = new ConcurrentAlgorithmManager(2);
        manager.addAlgorithm(new BubbleSort());
        ArrayDataStructure data = new ArrayDataStructure(4);
        data.fromArray(new int[]{4, 3, 2, 1});
        try {
            Future<AlgorithmPerformance> sortFuture = manager.parallelSort("Bubble Sort", data);
            assertNotNull("应返回性能结果", sortFuture.get());
            Future<Integer> searchFuture = manager.parallelSearch("Bubble Sort", data, 2);
            assertEquals("应返回目标元素的新索引", Integer.valueOf(1), searchFuture.get());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * 验证并发搜索在未命中目标时的返回值。
     */
    @Test
    public void testConcurrentAlgorithmManagerParallelSearchNotFound() throws Exception {
        ConcurrentAlgorithmManager manager = new ConcurrentAlgorithmManager(2);
        manager.addAlgorithm(new BubbleSort());
        ArrayDataStructure data = new ArrayDataStructure(3);
        data.fromArray(new int[]{3, 2, 1});
        try {
            Future<Integer> future = manager.parallelSearch("Bubble Sort", data, 99);
            assertEquals("未命中应返回 -1", Integer.valueOf(-1), future.get());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * 验证请求不存在的算法时会抛出异常。
     */
    @Test(expected = AlgorithmNotFoundException.class)
    public void testConcurrentAlgorithmManagerMissingAlgorithmThrows() throws Throwable {
        ConcurrentAlgorithmManager manager = new ConcurrentAlgorithmManager(1);
        try {
            manager.parallelSort("Unknown", new ArrayDataStructure(0));
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            manager.shutdown();
        }
    }

    // ========================================================
    // DynamicAlgorithmManager 测试
    // ========================================================

    /**
     * 验证动态管理器的算法选择分支。
     */
    @Test
    public void testDynamicAlgorithmManagerSelectionLogic() {
        DynamicAlgorithmManager manager = new DynamicAlgorithmManager(new PerformanceTracker());
        ArrayDataStructure small = new ArrayDataStructure(3);
        small.fromArray(new int[]{3, 1, 2});
        assertEquals("小规模未排序应选择优化快排", "OptimizedQuickSort", manager.selectOptimalAlgorithm(small));

        ArrayDataStructure large = new ArrayDataStructure(1002);
        for (int i = 1001; i >= 0; i--) {
            large.add(i);
        }
        assertEquals("大规模数据应选择并行归并", "ParallelMergeSort", manager.selectOptimalAlgorithm(large));

        ArrayDataStructure sorted = new ArrayDataStructure(5);
        sorted.fromArray(new int[]{1, 2, 3, 4, 5});
        assertEquals("已排序数据应选择插入排序", "InsertionSort", manager.selectOptimalAlgorithm(sorted));
    }

    /**
     * 验证 isSorted 方法能正确识别有序与无序数据。
     */
    @Test
    public void testDynamicAlgorithmManagerSortedDetection() {
        DynamicAlgorithmManager manager = new DynamicAlgorithmManager(new PerformanceTracker());
        ArrayDataStructure sorted = new ArrayDataStructure(4);
        sorted.fromArray(new int[]{1, 2, 3, 4});
        assertTrue("升序数组应视为有序", manager.isSorted(sorted));

        ArrayDataStructure unsorted = new ArrayDataStructure(4);
        unsorted.fromArray(new int[]{4, 3, 2, 1});
        assertFalse("降序数组应视为无序", manager.isSorted(unsorted));
    }

    /**
     * 验证在未注册对应算法时自动排序会抛出异常。
     */
    @Test(expected = AlgorithmNotFoundException.class)
    public void testDynamicAlgorithmManagerAutoSelectSortFailsWithoutRegistration() throws Exception {
        DynamicAlgorithmManager manager = new DynamicAlgorithmManager(new PerformanceTracker());
        ArrayDataStructure data = new ArrayDataStructure(3);
        data.fromArray(new int[]{3, 2, 1});
        manager.autoSelectAndSort(data);
    }

    // ========================================================
    // QuickSort 测试
    // ========================================================

    /**
     * 验证基础快速排序的排序表现。
     */
    @Test
    public void testQuickSortSortsData() {
        QuickSort sorter = new QuickSort();
        ArrayDataStructure data = new ArrayDataStructure(6);
        data.fromArray(new int[]{6, 1, 5, 2, 4, 3});
        sorter.sort(data);
        assertArrayEquals("快速排序应输出有序序列", new int[]{1, 2, 3, 4, 5, 6}, data.toArray());
    }

    /**
     * 验证基础快速排序的二分查找功能。
     */
    @Test
    public void testQuickSortBinarySearch() {
        QuickSort sorter = new QuickSort();
        ArrayDataStructure data = new ArrayDataStructure(4);
        data.fromArray(new int[]{1, 3, 5, 7});
        assertEquals(2, sorter.search(data, 5));
        assertEquals(-1, sorter.search(data, 2));
    }

    /**
     * 验证基础快速排序的性能评估。
     */
    @Test
    public void testQuickSortEvaluatePerformance() {
        QuickSort sorter = new QuickSort();
        ArrayDataStructure data = new ArrayDataStructure(4);
        data.fromArray(new int[]{4, 3, 2, 1});
        AlgorithmPerformance performance = sorter.evaluatePerformance(data);
        assertTrue("消耗时间应为非负", performance.getTimeTaken() >= 0);
        assertTrue("比较次数应为非负", performance.getComparisons() >= 0);
    }

    // ========================================================
    // MultiThreadedSearch 测试
    // ========================================================

    /**
     * 验证多线程搜索能够找到目标元素。
     */
    @Test
    public void testMultiThreadedSearchFindsElement() throws Exception {
        MultiThreadedSearch search = new MultiThreadedSearch(4);
        prestartThreads(search);
        int[] data = {10, 20, 30, 40, 50, 60, 70, 80};
        try {
            assertEquals("目标存在应返回索引", 5, search.parallelSearch(data, 60));
        } finally {
            search.shutdown();
        }
    }

    /**
     * 验证多线程搜索在分块不均匀时仍能正确工作。
     */
    @Test
    public void testMultiThreadedSearchHandlesRemainderPartition() throws Exception {
        MultiThreadedSearch search = new MultiThreadedSearch(4);
        prestartThreads(search);
        int[] data = {1, 2, 3, 4, 5, 6, 7};
        try {
            assertEquals("目标在尾段仍应被找到", 6, search.parallelSearch(data, 7));
        } finally {
            search.shutdown();
        }
    }

    /**
     * 验证线性搜索独立使用时的未命中场景。
     */
    @Test
    public void testMultiThreadedSearchLinearSearchMiss() {
        MultiThreadedSearch search = new MultiThreadedSearch(2);
        try {
            assertEquals("未命中应返回 -1", -1, search.linearSearch(new int[]{1, 2, 3}, 9, 0, 3));
        } finally {
            search.shutdown();
        }
    }

    /**
     * 验证多线程搜索在未命中时返回 -1。
     */
    @Test
    public void testMultiThreadedSearchParallelSearchNotFound() throws Exception {
        MultiThreadedSearch search = new MultiThreadedSearch(3);
        prestartThreads(search);
        int[] data = {5, 4, 3, 2, 1};
        try {
            assertEquals("完全未命中应返回 -1", -1, search.parallelSearch(data, 10));
        } finally {
            search.shutdown();
        }
    }

    // ========================================================
    // PerformanceTracker 测试
    // ========================================================

    /**
     * 验证性能追踪器能找到耗时最短的算法。
     */
    @Test
    public void testPerformanceTrackerBestPerformance() {
        PerformanceTracker tracker = new PerformanceTracker();
        AlgorithmPerformance slow = new AlgorithmPerformance(100L, 0, 0, 0L, 1);
        AlgorithmPerformance fast = new AlgorithmPerformance(10L, 0, 0, 0L, 1);
        tracker.trackPerformance("slow", slow);
        tracker.trackPerformance("fast", fast);
        assertEquals("应返回耗时最短的记录", fast, tracker.getBestPerformance());
        tracker.generateReport();
    }

    /**
     * 验证性能追踪器在没有记录时返回 null。
     */
    @Test
    public void testPerformanceTrackerEmpty() {
        PerformanceTracker tracker = new PerformanceTracker();
        assertNull("无性能数据时应返回 null", tracker.getBestPerformance());
    }

    // ========================================================
    // 自定义异常测试
    // ========================================================

    /**
     * 验证无效数据异常的消息内容。
     */
    @Test
    public void testInvalidDataExceptionMessage() {
        InvalidDataException exception = new InvalidDataException("invalid");
        assertEquals("invalid", exception.getMessage());
    }

    /**
     * 验证并发异常的消息内容。
     */
    @Test
    public void testConcurrencyExceptionMessage() {
        ConcurrencyException exception = new ConcurrencyException("locked");
        assertEquals("locked", exception.getMessage());
    }

    // ========================================================
    // 工具方法
    // ========================================================

    /**
     * 工具方法：预启动多线程搜索中的线程池，避免除零错误。
     */
    private void prestartThreads(MultiThreadedSearch search) {
        try {
            Field executorField = MultiThreadedSearch.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) executorField.get(search);
            executor.prestartAllCoreThreads();
        } catch (Exception e) {
            throw new RuntimeException("无法预启动线程池", e);
        }
    }

    /**
     * 工具方法：判断数组是否为升序。
     */
    private boolean isAscending(int[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i - 1] > array[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 工具方法：生成 start 到 end（包含）的连续数组。
     */
    private int[] createSequentialArray(int start, int end) {
        int length = end - start + 1;
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = start + i;
        }
        return array;
    }
}
