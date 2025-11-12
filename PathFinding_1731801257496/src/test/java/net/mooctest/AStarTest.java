package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

public class AStarTest {

    private void assertPointEquals(int expectedX, int expectedY, long actualPoint) {
        assertEquals(expectedX, Point.getX(actualPoint));
        assertEquals(expectedY, Point.getY(actualPoint));
    }

    @Test
    public void testSearch_无障碍平滑路径得到起终点() {
        // 验证无障碍场景下开启平滑后路径仅包含起点与终点并清理搜索状态
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        Path smoothPath = astar.search(0, 0, 4, 4, grid, true);

        assertFalse(smoothPath.isEmpty());
        assertEquals(2, smoothPath.size());
        assertPointEquals(0, 0, smoothPath.get(0));
        assertPointEquals(4, 4, smoothPath.get(1));
        assertTrue(astar.isCLean(grid));
    }

    @Test
    public void testSearch_异常场景直接返回空路径() {
        // 验证起点终点不可走或相同位置时直接返回空路径且不污染网格
        Grid grid = new Grid(3, 3);
        AStar astar = new AStar();
        Path reusable = new Path();

        grid.setWalkable(0, 0, false);
        astar.search(0, 0, 2, 2, grid, reusable, false);
        assertTrue(reusable.isEmpty());
        assertTrue(astar.isCLean(grid));

        reusable.clear();
        grid.setWalkable(0, 0, true);
        grid.setWalkable(2, 2, false);
        astar.search(0, 0, 2, 2, grid, reusable, false);
        assertTrue(reusable.isEmpty());

        reusable.clear();
        grid.setWalkable(2, 2, true);
        astar.search(1, 1, 1, 1, grid, reusable, false);
        assertTrue(reusable.isEmpty());
    }

    @Test
    public void testOpen_方向和状态判断确保不重复入堆() {
        // 验证open方法在不同方向及节点状态下的剪枝逻辑和权重更新
        AStar astar = new AStar();
        Grid grid = new Grid(3, 3);
        astar.nodes.map = grid;

        grid.setWalkable(2, 1, false);
        astar.open(1, 1, 10, Grid.DIRECTION_RIGHT_DOWN, 2, 2, grid);
        assertEquals(0, astar.nodes.size);

        grid.setWalkable(2, 1, true);
        grid.setWalkable(1, 2, false);
        astar.open(1, 1, 10, Grid.DIRECTION_LEFT_UP, 2, 2, grid);
        assertEquals(0, astar.nodes.size);

        grid.setWalkable(1, 2, true);
        astar.open(1, 1, 10, Grid.DIRECTION_LEFT_UP, 2, 2, grid);
        assertEquals(1, astar.nodes.size);
        int info = grid.info(1, 1);
        int idx = Grid.openNodeIdx(info);
        long stored = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(stored));
        assertEquals(Grid.DIRECTION_LEFT_UP, grid.nodeParentDirection(1, 1));

        astar.open(1, 1, 20, Grid.DIRECTION_LEFT_UP, 2, 2, grid);
        assertEquals(10, Node.getG(astar.nodes.getOpenNode(idx)));

        astar.open(1, 1, 5, Grid.DIRECTION_LEFT_UP, 2, 2, grid);
        assertEquals(5, Node.getG(astar.nodes.getOpenNode(idx)));

        grid.nodeClosed(0, 1);
        astar.open(0, 1, 7, Grid.DIRECTION_LEFT, 2, 2, grid);
        assertEquals(1, astar.nodes.size);

        grid.setWalkable(0, 0, false);
        astar.open(0, 0, 3, Grid.DIRECTION_LEFT_UP, 2, 2, grid);
        assertEquals(1, astar.nodes.size);

        astar.nodes.clear();

        astar.nodes.size = Grid.MAX_OPEN_NODE_SIZE;
        try {
            astar.nodes.open(2, 2, 1, 1, Grid.DIRECTION_UP);
            fail("应当抛出TooLongPathException");
        } catch (TooLongPathException e) {
            assertTrue(e.getMessage().contains("TooManyOpenNodes"));
        }
        astar.nodes.size = 0;
    }

    @Test
    public void testFillPath_平滑与非平滑分支行为() {
        // 验证fillPath在平滑和非平滑模式下的入栈与裁剪逻辑
        AStar astar = new AStar();
        Grid grid = new Grid(4, 1);

        Path rawPath = new Path();
        astar.fillPath(0, 0, rawPath, grid, false);
        assertEquals(1, rawPath.size());
        assertPointEquals(0, 0, rawPath.get(0));

        Path smooth = new Path();
        astar.fillPath(0, 0, smooth, grid, true);
        astar.fillPath(1, 0, smooth, grid, true);
        assertEquals(2, smooth.size());

        grid.setWalkable(1, 0, false);
        astar.fillPath(2, 0, smooth, grid, true);
        assertEquals(3, smooth.size());
        assertPointEquals(2, 0, smooth.get(0));

        grid.setWalkable(1, 0, true);
        astar.fillPath(3, 0, smooth, grid, true);
        assertEquals(2, smooth.size());
        assertPointEquals(3, 0, smooth.get(0));
        assertPointEquals(0, 0, smooth.get(1));
    }

    @Test
    public void testNodes_堆扩容与上下滤操作() {
        // 验证Nodes的堆扩容、上滤和下滤行为以及close后的清理
        AStar astar = new AStar();
        Grid grid = new Grid(4, 4);
        astar.nodes.map = grid;
        astar.nodes.nodes = new long[1];

        astar.nodes.open(0, 0, 4, 4, Grid.DIRECTION_UP);
        int afterFirst = astar.nodes.nodes.length;
        astar.nodes.open(1, 0, 2, 3, Grid.DIRECTION_RIGHT);
        assertTrue(astar.nodes.nodes.length > afterFirst);

        astar.nodes.open(2, 0, 1, 1, Grid.DIRECTION_RIGHT);
        astar.nodes.open(3, 0, 4, 2, Grid.DIRECTION_RIGHT);
        astar.nodes.open(0, 1, 3, 2, Grid.DIRECTION_UP);
        assertEquals(5, astar.nodes.size);

        long first = astar.nodes.close();
        assertEquals(2, Node.getX(first));
        assertEquals(0, Node.getY(first));
        assertEquals(Grid.NODE_CLOSED, grid.info(2, 0));

        long second = astar.nodes.close();
        assertEquals(1, Node.getX(second));
        assertEquals(0, Node.getY(second));
        assertEquals(3, astar.nodes.size);

        assertFalse(astar.nodes.isClean());

        astar.nodes.clear();
        assertTrue(astar.nodes.isClean());
        assertTrue(grid.isClean());
    }

    @Test
    public void testNodeAndPath_编码与异常处理() {
        // 验证Node与Path的基础编码操作及越界异常
        long node = Node.toNode(3, 4, 7, 11);
        assertEquals(3, Node.getX(node));
        assertEquals(4, Node.getY(node));
        assertEquals(7, Node.getG(node));
        assertEquals(11, Node.getF(node));

        long updated = Node.setGF(node, 2, 5);
        assertEquals(2, Node.getG(updated));
        assertEquals(5, Node.getF(updated));

        try {
            Node.toNode(0, 0, 0, -1);
            fail("预期抛出TooLongPathException");
        } catch (TooLongPathException e) {
            assertEquals("TooBigF", e.getMessage());
        }

        Path path = new Path();
        assertTrue(path.isEmpty());
        path.add(1, 1);
        assertTrue(path.isEmpty());
        path.add(2, 2);
        assertFalse(path.isEmpty());

        for (int i = 3; i < 12; i++) {
            path.add(i, i);
        }
        assertTrue(path.ps.length >= 12);
        assertEquals(12, path.size());

        path.remove();
        assertEquals(11, path.size());
        assertPointEquals(11, 11, path.get(0));
        assertPointEquals(1, 1, path.get(path.size() - 1));
    }

    @Test
    public void testReachability_多场景判定最近可行点() {
        // 验证可达性工具在水平、竖直、对角及同格场景下的返回值
        Grid free = new Grid(5, 5);
        assertTrue(Reachability.isReachable(0, 0, 4, 4, free));

        Grid horizontal = new Grid(5, 5);
        horizontal.setWalkable(2, 2, false);
        long horizontalStop = Reachability.getClosestWalkablePointToTarget(0, 2, 4, 2, horizontal);
        assertPointEquals(1, 2, horizontalStop);

        Grid horizontalBlocked = new Grid(5, 5);
        horizontalBlocked.setWalkable(1, 2, false);
        long horizontalFail = Reachability.getClosestWalkablePointToTarget(0, 2, 4, 2, horizontalBlocked);
        assertPointEquals(0, 2, horizontalFail);
        assertFalse(Reachability.isReachable(0, 2, 4, 2, horizontalBlocked));

        Grid vertical = new Grid(5, 5);
        vertical.setWalkable(2, 3, false);
        long verticalStop = Reachability.getClosestWalkablePointToTarget(2, 0, 2, 4, vertical);
        assertPointEquals(2, 2, verticalStop);

        Grid sameCell = new Grid(5, 5);
        long sameCellResult = Reachability.getClosestWalkablePointToTarget(0, 0, 1, 1, 2, sameCell);
        assertPointEquals(1, 1, sameCellResult);

        Grid diagonalBlocked = new Grid(5, 5);
        diagonalBlocked.setWalkable(3, 3, false);
        long diagonalStop = Reachability.getClosestWalkablePointToTarget(0, 0, 4, 4, diagonalBlocked);
        assertPointEquals(2, 2, diagonalStop);
    }

    @Test
    public void testReachability_围栏与缩放异常() {
        // 验证围栏约束与非法缩放参数的异常处理
        Grid grid = new Grid(5, 5);
        try {
            Reachability.getClosestWalkablePointToTarget(0, 0, 1, 1, 0, grid);
            fail("缩放参数非法时应抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Illegal scale"));
        }

        Fence alwaysAllow = (sx, sy, ex, ey) -> true;
        long direct = Reachability.getClosestWalkablePointToTarget(0, 0, 4, 0, 1, grid, alwaysAllow);
        assertPointEquals(4, 0, direct);

        Fence limitFence = (sx, sy, ex, ey) -> Math.abs(ex - sx) + Math.abs(ey - sy) <= 2;
        long fenced = Reachability.getClosestWalkablePointToTarget(0, 0, 4, 0, 1, grid, limitFence);
        assertPointEquals(2, 0, fenced);

        assertEquals(1.5, Reachability.scaleDown(3, 2), 1e-9);
        assertEquals(6, Reachability.scaleUp(3, 2));
        assertPointEquals(3, 4, Reachability.scaleUpPoint(1.5, 2, 2));
    }

    @Test
    public void testUtils_校验与位掩码边界() {
        // 验证工具类的校验方法及掩码计算边界情况
        assertEquals(-1, Utils.mask(32));
        assertEquals(0b1111, Utils.mask(4));

        try {
            Utils.mask(0);
            fail("掩码位数非法时应抛出异常");
        } catch (RuntimeException e) {
            // expected
        }

        Utils.check(true);
        try {
            Utils.check(false, "错误信息");
            fail("应抛出带消息的异常");
        } catch (RuntimeException e) {
            assertEquals("错误信息", e.getMessage());
        }

        try {
            Utils.check(false, "数值 %d", 5);
            fail("应抛出格式化异常");
        } catch (RuntimeException e) {
            assertEquals("数值 5", e.getMessage());
        }
    }

    @Test
    public void testThreadLocalAStar_线程隔离() throws InterruptedException {
        // 验证ThreadLocal封装能确保不同线程持有的AStar实例互不干扰
        AStar main = ThreadLocalAStar.current();
        assertSame(main, ThreadLocalAStar.current());

        final AStar[] holder = new AStar[1];
        Thread thread = new Thread(() -> holder[0] = ThreadLocalAStar.current());
        thread.start();
        thread.join();

        assertNotNull(holder[0]);
        assertNotSame(main, holder[0]);
    }
}
