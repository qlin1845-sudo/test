package net.mooctest;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/*
 * 测试代码基于JUnit 4，若eclipse提示未找到Junit 5的测试用例，请在Run Configurations中设置Test Runner为Junit 4。请不要使用Junit 5
 * 语法编写测试代码
 */

public class AStarTest {

    private Grid createGrid(int width, int height) {
        Grid grid = new Grid(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid.setWalkable(x, y, true);
            }
        }
        return grid;
    }

    private int countPathPoints(Path path) {
        return path.size();
    }

    private int extractX(Path path, int index) {
        return Point.getX(path.get(index));
    }

    private int extractY(Path path, int index) {
        return Point.getY(path.get(index));
    }

    // 测试：无障碍网格时应当找到直达路径
    @Test
    public void testSearch_StraightLinePath() {
        Grid grid = createGrid(5, 5);
        AStar astar = new AStar();

        Path path = astar.search(0, 0, 4, 4, grid);

        assertFalse("路径不能为空", path.isEmpty());
        assertEquals("起点应该正确", 0, extractX(path, 0));
        assertEquals("起点应该正确", 0, extractY(path, 0));
        assertEquals("终点应该正确", 4, extractX(path, countPathPoints(path) - 1));
        assertEquals("终点应该正确", 4, extractY(path, countPathPoints(path) - 1));
        assertTrue("搜索完成后内部状态应被清理", astar.isCLean(grid));
    }

    // 测试：存在障碍时算法应绕路
    @Test
    public void testSearch_WithObstacleDetour() {
        Grid grid = createGrid(6, 6);
        for (int y = 0; y < 5; y++) {
            grid.setWalkable(2, y, false);
        }
        grid.setWalkable(2, 5, true);

        AStar astar = new AStar();
        Path path = astar.search(0, 0, 5, 5, grid);

        assertFalse("路径依然应该存在", path.isEmpty());
        boolean detoured = false;
        for (int i = 1; i < path.size(); i++) {
            if (extractX(path, i) >= 3) {
                detoured = true;
            }
            assertTrue("路径不应穿越障碍", grid.isWalkable(extractX(path, i), extractY(path, i)));
        }
        assertTrue("路径应绕开障碍列", detoured);
        assertTrue(astar.isCLean(grid));
    }

    // 测试：起始点不可行走时直接返回空路径
    @Test
    public void testSearch_StartNotWalkable() {
        Grid grid = createGrid(3, 3);
        grid.setWalkable(0, 0, false);
        AStar astar = new AStar();
        Path path = new Path();
        astar.search(0, 0, 2, 2, grid, path, false);
        assertEquals("起点障碍应导致路径为空", 0, path.size());
        assertTrue(astar.isCLean(grid));
    }

    // 测试：终点不可行走时直接返回空路径
    @Test
    public void testSearch_EndNotWalkable() {
        Grid grid = createGrid(3, 3);
        grid.setWalkable(2, 2, false);
        AStar astar = new AStar();
        Path path = new Path();
        astar.search(0, 0, 2, 2, grid, path, false);
        assertEquals("终点障碍应导致路径为空", 0, path.size());
        assertTrue(astar.isCLean(grid));
    }

    // 测试：起点终点重合时无需搜索
    @Test
    public void testSearch_StartEqualsEnd() {
        Grid grid = createGrid(3, 3);
        AStar astar = new AStar();
        Path path = astar.search(1, 1, 1, 1, grid, true);
        assertEquals("同一点时应为空路径", 0, path.size());
        assertTrue(astar.isCLean(grid));
    }

    // 测试：无可达路径时应返回空
    @Test
    public void testSearch_UnreachableScenario() {
        Grid grid = createGrid(3, 3);
        grid.setWalkable(1, 0, false);
        grid.setWalkable(1, 1, false);
        grid.setWalkable(1, 2, false);
        grid.setWalkable(0, 1, false);
        grid.setWalkable(2, 1, false);
        AStar astar = new AStar();
        Path path = astar.search(0, 0, 2, 2, grid);
        assertEquals("被隔离时路径应为空", 0, path.size());
        assertTrue(astar.isCLean(grid));
    }

    // 测试：平滑功能应去掉冗余节点
    @Test
    public void testFillPath_SmoothRemovesRedundantPoints() {
        Grid grid = createGrid(6, 6);
        AStar astar = new AStar();
        Path path = new Path();
        path.add(0, 0);
        path.add(1, 1);
        astar.fillPath(5, 5, path, grid, true);
        assertEquals("平滑后应保留起点与新点", 2, path.size());
        assertEquals(0, extractX(path, 0));
        assertEquals(0, extractY(path, 0));
        assertEquals(5, extractX(path, 1));
        assertEquals(5, extractY(path, 1));
    }

    // 测试：不平滑时应直接追加节点
    @Test
    public void testFillPath_NoSmoothKeepsAllPoints() {
        Grid grid = createGrid(6, 6);
        AStar astar = new AStar();
        Path path = new Path();
        path.add(0, 0);
        astar.fillPath(2, 2, path, grid, false);
        assertEquals("未平滑时应保留所有节点", 2, path.size());
        assertEquals(2, extractX(path, 1));
        assertEquals(2, extractY(path, 1));
    }

    // 测试：异常发生时路径应清空且状态清理
    @Test
    public void testSearch_ExceptionClearsPath() {
        class FaultyGrid extends Grid {
            FaultyGrid() {
                super(3, 3);
            }

            @Override
            int info(int x, int y) {
                if (x == 1 && y == 0) {
                    throw new RuntimeException("fault");
                }
                return super.info(x, y);
            }
        }
        FaultyGrid grid = new FaultyGrid();
        AStar astar = new AStar();
        Path path = new Path();
        try {
            astar.search(0, 0, 2, 2, grid, path, false);
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            assertEquals("fault", e.getMessage());
        }
        assertEquals("异常后路径应被清空", 0, path.size());
        assertTrue(astar.isCLean(grid));
    }

    // 测试：open方法在对角障碍時不会打开节点
    @Test
    public void testOpen_DiagonalBlocked() {
        Grid grid = createGrid(3, 3);
        grid.setWalkable(1, 0, false);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        astar.open(0, 0, 10, Grid.DIRECTION_RIGHT_DOWN, 2, 2, grid);
        assertEquals("被禁止的对角节点不应加入", 0, astar.nodes.size);
    }

    // 测试：open方法在另一侧障碍时不会打开节点
    @Test
    public void testOpen_OtherDiagonalBlocked() {
        Grid grid = createGrid(3, 3);
        grid.setWalkable(0, 1, false);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        astar.open(0, 0, 10, Grid.DIRECTION_LEFT_UP, 2, 2, grid);
        assertEquals("被禁止的转角节点不应加入", 0, astar.nodes.size);
    }

    // 测试：open方法遇到已关闭节点直接返回
    @Test
    public void testOpen_WithClosedNode() {
        Grid grid = createGrid(3, 3);
        grid.grid[1][1] = (short) Grid.NODE_CLOSED;
        AStar astar = new AStar();
        astar.nodes.map = grid;
        astar.open(1, 1, 5, Grid.DIRECTION_UP, 2, 2, grid);
        assertEquals("关闭节点不能再次加入", 0, astar.nodes.size);
    }

    // 测试：open方法对已存在节点进行更优更新
    @Test
    public void testOpen_UpdateBetterPath() {
        Grid grid = createGrid(3, 3);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        astar.nodes.open(1, 1, 10, 10, Grid.DIRECTION_UP);
        long original = astar.nodes.getOpenNode(0);
        assertEquals(10, Node.getG(original));

        astar.open(1, 1, 5, Grid.DIRECTION_DOWN, 2, 2, grid);
        long updated = astar.nodes.getOpenNode(0);
        assertEquals("g值应被更新为更优解", 5, Node.getG(updated));
        assertEquals("父方向应更新", Grid.DIRECTION_DOWN, grid.nodeParentDirection(1, 1));
    }

    // 测试：open方法在不更优时保持不变
    @Test
    public void testOpen_NoBetterPath() {
        Grid grid = createGrid(3, 3);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        astar.nodes.open(1, 1, 5, 10, Grid.DIRECTION_UP);
        long original = astar.nodes.getOpenNode(0);
        astar.open(1, 1, 7, Grid.DIRECTION_DOWN, 2, 2, grid);
        assertEquals("g值不应被恶化", original, astar.nodes.getOpenNode(0));
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(1, 1));
    }

    // 测试：Nodes堆的基本开闭操作
    @Test
    public void testNodes_OpenCloseBehavior() {
        Grid grid = createGrid(4, 4);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        nodes.open(0, 0, 1, 4, Grid.DIRECTION_UP);
        nodes.open(1, 0, 0, 1, Grid.DIRECTION_UP);
        nodes.open(2, 0, 5, 0, Grid.DIRECTION_UP);
        assertEquals(3, nodes.size);
        long first = nodes.close();
        assertEquals("应弹出最小f节点", 0, Node.getG(first));
        assertEquals(2, nodes.size);
        nodes.clear();
        assertEquals(0, nodes.size);
        assertTrue(nodes.isClean());
    }

    // 测试：Nodes增长逻辑能扩容
    @Test
    public void testNodes_GrowCapacity() {
        Grid grid = createGrid(5, 5);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        int initialLength = nodes.nodes.length;
        for (int i = 0; i < initialLength + 1; i++) {
            int x = i % grid.getWidth();
            int y = i / grid.getWidth();
            nodes.open(x, y, i, i, Grid.DIRECTION_UP);
        }
        assertTrue("容量应扩展", nodes.nodes.length > initialLength);
    }

    // 测试：Nodes重新定位父节点能够上滤
    @Test
    public void testNodes_OpenNodeParentChanged() {
        Grid grid = createGrid(3, 3);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        nodes.open(0, 0, 10, 10, Grid.DIRECTION_UP);
        nodes.open(1, 0, 12, 5, Grid.DIRECTION_UP);
        int index = Grid.openNodeIdx(grid.info(1, 0));
        long node = nodes.getOpenNode(index);
        long adjusted = Node.setGF(node, 1, Node.getF(node) - Node.getG(node) + 1);
        nodes.openNodeParentChanged(adjusted, index, Grid.DIRECTION_RIGHT);
        assertEquals("节点应当上滤到堆顶", 1, Node.getG(nodes.getOpenNode(0)));
        assertEquals(Grid.DIRECTION_RIGHT, grid.nodeParentDirection(1, 0));
    }

    // 测试：Grid的walkable控制正确
    @Test
    public void testGrid_WalkableControls() {
        Grid grid = createGrid(2, 2);
        assertTrue(grid.isWalkable(0, 0));
        grid.setWalkable(0, 0, false);
        assertFalse(grid.isWalkable(0, 0));
        grid.setWalkable(0, 0, true);
        assertTrue(grid.isWalkable(0, 0));
        assertFalse(grid.isWalkable(-1, 0));
        assertFalse(grid.isWalkable(0, 2));
    }

    // 测试：Grid的父方向与索引维护
    @Test
    public void testGrid_ParentDirectionAndIndex() {
        Grid grid = createGrid(3, 3);
        grid.nodeParentDirectionUpdate(1, 1, Grid.DIRECTION_LEFT_DOWN);
        assertEquals(Grid.DIRECTION_LEFT_DOWN, grid.nodeParentDirection(1, 1));
        grid.openNodeIdxUpdate(1, 1, 5);
        assertEquals(5, Grid.openNodeIdx(grid.info(1, 1)));
        grid.nodeClosed(1, 1);
        assertTrue(Grid.isClosedNode(grid.info(1, 1)));
        grid.clear();
        assertTrue(grid.isClean());
    }

    // 测试：Node编码解码准确
    @Test
    public void testNode_EncodingAndDecoding() {
        long node = Node.toNode(123, 456, 789, 1000);
        assertEquals(123, Node.getX(node));
        assertEquals(456, Node.getY(node));
        assertEquals(789, Node.getG(node));
        assertEquals(1000, Node.getF(node));
        long updated = Node.setGF(node, 11, 22);
        assertEquals(11, Node.getG(updated));
        assertEquals(22, Node.getF(updated));
    }

    // 测试：Node在过大路径时抛出异常
    @Test(expected = TooLongPathException.class)
    public void testNode_ToNodeTooLong() {
        Node.toNode(0, 0, 0, -1);
    }

    // 测试：Path的容量增长与移除
    @Test
    public void testPath_GrowAndRemove() {
        Path path = new Path();
        for (int i = 0; i < 70; i++) {
            path.add(i, i);
        }
        assertEquals(70, path.size());
        path.remove();
        assertEquals(69, path.size());
        path.clear();
        assertEquals(0, path.size());
        assertTrue(path.isEmpty());
    }

    // 测试：Point编码与解码
    @Test
    public void testPoint_ToAndFromLong() {
        long point = Point.toPoint(100, 200);
        assertEquals(100, Point.getX(point));
        assertEquals(200, Point.getY(point));
    }

    // 测试：Reachability在同一格场景下工作
    @Test
    public void testReachability_SameCell() {
        Grid grid = createGrid(4, 4);
        assertTrue(Reachability.isReachable(1, 1, 1, 1, grid));
    }

    // 测试：Reachability在水平方向遇到障碍时返回最近点
    @Test
    public void testReachability_HorizontalBlock() {
        Grid grid = createGrid(6, 1);
        grid.setWalkable(3, 0, false);
        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 5, 0, grid);
        assertEquals(2, Point.getX(result));
        assertEquals(0, Point.getY(result));
    }

    // 测试：Reachability在竖直方向遇到障碍时返回最近点
    @Test
    public void testReachability_VerticalBlock() {
        Grid grid = createGrid(1, 6);
        grid.setWalkable(0, 3, false);
        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 0, 5, grid);
        assertEquals(0, Point.getX(result));
        assertEquals(2, Point.getY(result));
    }

    // 测试：Reachability处理不可达起点
    @Test
    public void testReachability_UnwalkableStart() {
        Grid grid = createGrid(3, 3);
        grid.setWalkable(0, 0, false);
        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 2, 2, grid);
        assertEquals(0, Point.getX(result));
        assertEquals(0, Point.getY(result));
    }

    // 测试：Reachability在斜线被Fence阻断时返回最近点
    @Test
    public void testReachability_WithFenceBlocking() {
        Grid grid = createGrid(5, 5);
        Fence fence = (x1, y1, x2, y2) -> !(x2 == 4 && y2 == 4);
        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 4, 4, 1, grid, fence);
        assertNotEquals("受Fence阻挡应无法抵达终点", Point.toPoint(4, 4), result);
    }

    // 测试：Reachability非法scale抛出异常
    @Test(expected = IllegalArgumentException.class)
    public void testReachability_IllegalScale() {
        Reachability.getClosestWalkablePointToTarget(0, 0, 1, 1, 0, createGrid(2, 2), null);
    }

    // 测试：Reachability缩放工具方法
    @Test
    public void testReachability_ScaleHelpers() {
        assertEquals(1.5, Reachability.scaleDown(3, 2), 1e-6);
        assertEquals(6, Reachability.scaleUp(2, 3));
        assertEquals(7, Reachability.scaleUp(2.5, 3));
        long point = Reachability.scaleUpPoint(1.5, 2.5, 2);
        assertEquals(3, Point.getX(point));
        assertEquals(5, Point.getY(point));
    }

    // 测试：ThreadLocalAStar每个线程持有独立实例
    @Test
    public void testThreadLocalAStar_Current() throws Exception {
        AStar mainThread = ThreadLocalAStar.current();
        final Set<AStar> set = new HashSet<>();
        Thread t = new Thread(() -> set.add(ThreadLocalAStar.current()));
        t.start();
        t.join();
        assertNotNull(mainThread);
        assertEquals(1, set.size());
        assertNotSame(mainThread, set.iterator().next());
    }

    // 测试：Utils的校验方法
    @Test
    public void testUtils_CheckAndMask() {
        Utils.check(true);
        Utils.check(true, "ok");
        Utils.check(true, "format %s", "ok");
        assertEquals(0x0F, Utils.mask(4));
        assertEquals(-1, Utils.mask(32));
    }

    // 测试：Utils校验失败抛出异常
    @Test(expected = RuntimeException.class)
    public void testUtils_CheckFail() {
        Utils.check(false);
    }

    // 测试：Utils mask非法参数抛出异常
    @Test(expected = RuntimeException.class)
    public void testUtils_MaskIllegal() {
        Utils.mask(0);
    }
}
