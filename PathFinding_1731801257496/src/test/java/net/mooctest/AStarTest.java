package net.mooctest;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class AStarTest {

    private Grid createGrid(int width, int height) {
        return new Grid(width, height);
    }

    private int[][] pathToArray(Path path) {
        int[][] coordinates = new int[path.size()][2];
        for (int i = 0; i < path.size(); i++) {
            long point = path.get(i);
            coordinates[i][0] = Point.getX(point);
            coordinates[i][1] = Point.getY(point);
        }
        return coordinates;
    }

    /**
     * 中文说明：验证没有障碍时A*能够生成包含起点终点的直线路径。
     */
    @Test
    public void testSearchWithoutObstaclesReturnsStraightPath() {
        AStar astar = new AStar();
        Grid grid = createGrid(5, 5);

        Path path = astar.search(0, 0, 4, 4, grid, false);

        assertFalse("路径不能为空", path.isEmpty());
        assertEquals(2, path.size());
        assertEquals(0, Point.getX(path.get(0)));
        assertEquals(0, Point.getY(path.get(0)));
        assertEquals(4, Point.getX(path.get(1)));
        assertEquals(4, Point.getY(path.get(1)));
        assertTrue("搜索后网格应保持干净", astar.isCLean(grid));
    }

    /**
     * 中文说明：验证起点被封锁时A*直接返回空路径。
     */
    @Test
    public void testSearchStartBlockedProducesEmptyPath() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        grid.setWalkable(0, 0, false);
        Path path = new Path();

        astar.search(0, 0, 2, 2, grid, path, false);

        assertTrue("起点不可行走时应返回空路径", path.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    /**
     * 中文说明：验证终点被封锁时A*返回空路径。
     */
    @Test
    public void testSearchEndBlockedProducesEmptyPath() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        grid.setWalkable(2, 2, false);
        Path path = new Path();

        astar.search(0, 0, 2, 2, grid, path, false);

        assertTrue("终点不可行走时应返回空路径", path.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    /**
     * 中文说明：验证起点和终点相同的情况会直接返回空路径。
     */
    @Test
    public void testSearchSameStartEndKeepsPathEmpty() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        Path path = new Path();

        astar.search(1, 1, 1, 1, grid, path, false);

        assertTrue("起终点相同不应生成路径", path.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    /**
     * 中文说明：验证平滑路径逻辑会去掉冗余点，只保留可直达的两端。
     */
    @Test
    public void testSearchWithSmoothingRemovesIntermediatePoints() {
        AStar astar = new AStar();
        Grid grid = createGrid(5, 5);
        Path raw = new Path();
        raw.add(1, 0); // 先添加中间点
        raw.add(0, 0); // 再添加起点

        astar.fillPath(2, 0, raw, grid, true);

        assertEquals("平滑后应只保留两个点", 2, raw.size());
        assertEquals(0, Point.getX(raw.get(0)));
        assertEquals(0, Point.getY(raw.get(0)));
        assertEquals(2, Point.getX(raw.get(1)));
        assertEquals(0, Point.getY(raw.get(1)));
    }

    /**
     * 中文说明：验证open遇到不可行走格子时不会加入节点。
     */
    @Test
    public void testOpenSkipsUnwalkableCells() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        astar.nodes.map = grid;
        grid.setWalkable(1, 1, false);

        astar.open(1, 1, 10, Grid.DIRECTION_UP, 2, 2, grid);

        assertEquals("不可行走点不应进入开放列表", 0, astar.nodes.size);
    }

    /**
     * 中文说明：验证open能正确加入新节点并写入父方向。
     */
    @Test
    public void testOpenRegistersNewNodeAndUpdatesDirection() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        astar.nodes.map = grid;

        astar.open(1, 0, 5, Grid.DIRECTION_RIGHT, 2, 2, grid);

        assertEquals(1, astar.nodes.size);
        assertEquals(Grid.DIRECTION_RIGHT, grid.nodeParentDirection(1, 0));
    }

    /**
     * 中文说明：验证已关闭的节点不会再次被添加。
     */
    @Test
    public void testOpenDoesNotReopenClosedNodes() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        astar.nodes.map = grid;
        grid.nodeClosed(1, 1);

        astar.open(1, 1, 5, Grid.DIRECTION_LEFT, 2, 2, grid);

        assertEquals("关闭节点不应被重新插入", 0, astar.nodes.size);
    }

    /**
     * 中文说明：验证open在已有节点上会更新更优路径并拒绝更差路径。
     */
    @Test
    public void testOpenUpdatesBetterPathAndRejectsWorse() {
        AStar astar = new AStar();
        Grid grid = createGrid(3, 3);
        astar.nodes.map = grid;

        astar.open(1, 0, 10, Grid.DIRECTION_LEFT, 2, 2, grid);
        int idx = Grid.openNodeIdx(grid.info(1, 0));
        long first = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(first));

        astar.open(1, 0, 5, Grid.DIRECTION_RIGHT, 2, 2, grid);
        long improved = astar.nodes.getOpenNode(idx);
        assertEquals(5, Node.getG(improved));
        assertEquals(Grid.DIRECTION_RIGHT, grid.nodeParentDirection(1, 0));

        astar.open(1, 0, 9, Grid.DIRECTION_UP, 2, 2, grid);
        long unchanged = astar.nodes.getOpenNode(idx);
        assertEquals("更差的路径不应覆盖原值", 5, Node.getG(unchanged));
        assertEquals(Grid.DIRECTION_RIGHT, grid.nodeParentDirection(1, 0));
    }

    /**
     * 中文说明：验证对角线移动在相邻格受阻时会被拒绝。
     */
    @Test
    public void testOpenSkipsDiagonalWhenAdjacentBlocked() {
        AStar astar = new AStar();
        Grid grid = createGrid(4, 4);
        astar.nodes.map = grid;

        grid.setWalkable(3, 2, false);
        astar.open(2, 2, 5, Grid.DIRECTION_RIGHT_DOWN, 3, 3, grid);
        assertEquals("受阻的右下对角不应加入", 0, astar.nodes.size);

        grid.setWalkable(2, 3, false);
        astar.open(2, 2, 5, Grid.DIRECTION_LEFT_UP, 3, 3, grid);
        assertEquals("受阻的左上对角不应加入", 0, astar.nodes.size);
    }

    private Path buildPathForDirection(int startX, int startY, int endX, int endY, int direction) {
        AStar astar = new AStar();
        Grid grid = createGrid(5, 5);
        grid.nodeParentDirectionUpdate(endX, endY, direction);
        Path path = new Path();
        astar.fillPath(endX, endY, startX, startY, path, grid, false);
        return path;
    }

    /**
     * 中文说明：验证四个正交方向的回溯填充逻辑正确。
     */
    @Test
    public void testFillPathHandlesOrthogonalDirections() {
        Path up = buildPathForDirection(1, 2, 1, 1, Grid.DIRECTION_UP);
        assertEquals(2, up.size());
        assertEquals(1, Point.getX(up.get(0)));
        assertEquals(2, Point.getY(up.get(0)));

        Path down = buildPathForDirection(1, 0, 1, 1, Grid.DIRECTION_DOWN);
        assertEquals(0, Point.getY(down.get(0)));

        Path left = buildPathForDirection(0, 1, 1, 1, Grid.DIRECTION_LEFT);
        assertEquals(0, Point.getX(left.get(0)));

        Path right = buildPathForDirection(2, 1, 1, 1, Grid.DIRECTION_RIGHT);
        assertEquals(2, Point.getX(right.get(0)));
    }

    /**
     * 中文说明：验证四个对角方向的回溯填充逻辑正确。
     */
    @Test
    public void testFillPathHandlesDiagonalDirections() {
        Path leftUp = buildPathForDirection(0, 2, 1, 1, Grid.DIRECTION_LEFT_UP);
        assertEquals(0, Point.getX(leftUp.get(0)));
        assertEquals(2, Point.getY(leftUp.get(0)));

        Path leftDown = buildPathForDirection(0, 0, 1, 1, Grid.DIRECTION_LEFT_DOWN);
        assertEquals(0, Point.getX(leftDown.get(0)));
        assertEquals(0, Point.getY(leftDown.get(0)));

        Path rightUp = buildPathForDirection(2, 2, 1, 1, Grid.DIRECTION_RIGHT_UP);
        assertEquals(2, Point.getX(rightUp.get(0)));
        assertEquals(2, Point.getY(rightUp.get(0)));

        Path rightDown = buildPathForDirection(2, 0, 1, 1, Grid.DIRECTION_RIGHT_DOWN);
        assertEquals(2, Point.getX(rightDown.get(0)));
        assertEquals(0, Point.getY(rightDown.get(0)));
    }

    /**
     * 中文说明：验证Path的增加、移除以及扩容行为。
     */
    @Test
    public void testPathGrowthAndAccess() {
        Path path = new Path();
        assertTrue(path.isEmpty());

        for (int i = 0; i < 12; i++) {
            path.add(i, i + 1);
        }
        assertEquals(12, path.size());
        assertFalse(path.isEmpty());

        long head = path.get(0);
        assertEquals(11, Point.getX(head));
        assertEquals(12, Point.getY(head));

        path.remove();
        assertEquals(11, path.size());
    }

    /**
     * 中文说明：验证Node的编码与解码逻辑。
     */
    @Test
    public void testNodeToNodeAndAccessors() {
        long node = Node.toNode(3, 4, 5, 15);
        assertEquals(3, Node.getX(node));
        assertEquals(4, Node.getY(node));
        assertEquals(5, Node.getG(node));
        assertEquals(15, Node.getF(node));

        long replaced = Node.setGF(node, 7, 13);
        assertEquals(7, Node.getG(replaced));
        assertEquals(13, Node.getF(replaced));
    }

    /**
     * 中文说明：验证Node在f为负数时会抛出异常，用于防止路径溢出。
     */
    @Test(expected = TooLongPathException.class)
    public void testNodeToNodeRejectsNegativeF() {
        Node.toNode(0, 0, 0, -1);
    }

    /**
     * 中文说明：验证Nodes结构的开闭节点顺序与堆操作正确。
     */
    @Test
    public void testNodesOpenCloseOrder() {
        Nodes nodes = new Nodes();
        Grid grid = createGrid(4, 4);
        nodes.map = grid;

        nodes.open(0, 0, 5, 5, Grid.DIRECTION_UP);
        nodes.open(1, 1, 2, 1, Grid.DIRECTION_DOWN);
        nodes.open(2, 2, 3, 0, Grid.DIRECTION_LEFT);

        long first = nodes.close();
        assertEquals(2, Node.getX(first));
        assertEquals(2, Node.getY(first));

        long second = nodes.close();
        assertEquals(1, Node.getX(second));
        assertEquals(1, Node.getY(second));

        long third = nodes.close();
        assertEquals(0, Node.getX(third));
        assertEquals(0, Node.getY(third));

        assertEquals(0, nodes.close());
    }

    /**
     * 中文说明：验证Nodes扩容与清理逻辑，以确保大量节点时仍能正常工作。
     */
    @Test
    public void testNodesGrowAndClear() {
        Nodes nodes = new Nodes();
        Grid grid = createGrid(40, 40);
        nodes.map = grid;

        for (int i = 0; i < 40; i++) {
            nodes.open(i, i, 1, i, Grid.DIRECTION_RIGHT);
        }
        assertTrue("堆容量应被扩展", nodes.nodes.length > 16);
        assertEquals(40, nodes.size);

        nodes.clear();
        assertTrue(nodes.isClean());
        assertNull(nodes.map);
        assertTrue(grid.isClean());
    }

    /**
     * 中文说明：验证Reachability在同格与围栏限制下的行为。
     */
    @Test
    public void testReachabilitySameCellAndFence() {
        Grid grid = createGrid(3, 3);
        Fence allow = (x1, y1, x2, y2) -> true;
        Fence deny = (x1, y1, x2, y2) -> false;

        long ok = Reachability.getClosestWalkablePointToTarget(0, 0, 0, 0, 1, grid, allow);
        assertEquals(Point.toPoint(0, 0), ok);

        long blocked = Reachability.getClosestWalkablePointToTarget(0, 0, 0, 0, 1, grid, deny);
        assertEquals("围栏阻止时应回到起点", Point.toPoint(0, 0), blocked);

        grid.setWalkable(0, 0, false);
        long startBlocked = Reachability.getClosestWalkablePointToTarget(0, 0, 1, 0, grid);
        assertEquals("起始格不可行走时应返回原点", Point.toPoint(0, 0), startBlocked);
        grid.setWalkable(0, 0, true);
    }

    /**
     * 中文说明：验证Reachability在水平路径受阻时返回最近可达点。
     */
    @Test
    public void testReachabilityHorizontalBlocking() {
        Grid grid = createGrid(5, 3);
        grid.setWalkable(2, 1, false);

        long result = Reachability.getClosestWalkablePointToTarget(0, 1, 4, 1, grid);
        assertEquals(Point.toPoint(1, 1), result);

        grid.setWalkable(1, 1, false);
        long start = Reachability.getClosestWalkablePointToTarget(0, 1, 4, 1, grid);
        assertEquals(Point.toPoint(0, 1), start);
    }

    /**
     * 中文说明：验证Reachability在垂直路径受阻时返回正确坐标。
     */
    @Test
    public void testReachabilityVerticalBlocking() {
        Grid grid = createGrid(3, 5);
        grid.setWalkable(1, 3, false);

        long result = Reachability.getClosestWalkablePointToTarget(1, 0, 1, 4, grid);
        assertEquals(Point.toPoint(1, 2), result);

        grid.setWalkable(1, 1, false);
        long start = Reachability.getClosestWalkablePointToTarget(1, 0, 1, 4, grid);
        assertEquals(Point.toPoint(1, 0), start);
    }

    /**
     * 中文说明：验证对角路径及缩放参数时的可达性判断。
     */
    @Test
    public void testReachabilityDiagonalAndScaling() {
        Grid grid = createGrid(6, 6);
        assertTrue(Reachability.isReachable(0, 0, 5, 5, grid));

        grid.setWalkable(3, 3, false);
        assertFalse(Reachability.isReachable(0, 0, 5, 5, grid));

        grid.setWalkable(3, 3, true);
        long scaled = Reachability.getClosestWalkablePointToTarget(0, 0, 8, 8, 2, grid);
        assertEquals(Point.toPoint(8, 8), scaled);
    }

    /**
     * 中文说明：验证非法缩放参数会抛出异常。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReachabilityIllegalScaleThrows() {
        Grid grid = createGrid(2, 2);
        Reachability.getClosestWalkablePointToTarget(0, 0, 1, 1, 0, grid);
    }

    /**
     * 中文说明：验证围栏阻止通行时返回最近安全点。
     */
    @Test
    public void testReachabilityFenceBlocksPath() {
        Grid grid = createGrid(4, 4);
        Fence fence = (x1, y1, x2, y2) -> x2 < 2; // 禁止通过x>=2

        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 3, 0, 1, grid, fence);
        assertEquals(Point.toPoint(1, 0), result);
    }

    /**
     * 中文说明：验证辅助缩放工具方法的正确性。
     */
    @Test
    public void testReachabilityScaleHelpers() {
        assertEquals(2.5, Reachability.scaleDown(5, 2), 1e-6);
        assertEquals(6, Reachability.scaleUp(3, 2));
        assertEquals(11, Reachability.scaleUp(3.7, 3));

        long point = Reachability.scaleUpPoint(1.2, 0.5, 4);
        assertEquals(Point.toPoint(4, 2), point);
    }

    /**
     * 中文说明：验证Utils的掩码与断言逻辑。
     */
    @Test
    public void testUtilsMaskAndCheck() {
        assertEquals(15, Utils.mask(4));
        assertEquals(-1, Utils.mask(32));

        try {
            Utils.mask(0);
            fail("掩码参数非法时应抛出异常");
        } catch (RuntimeException expected) {
            // ignore
        }

        Utils.check(true);
        try {
            Utils.check(false, "error");
            fail("断言失败时应抛出异常");
        } catch (RuntimeException expected) {
            // ignore
        }
    }

    /**
     * 中文说明：验证ThreadLocalAStar在不同线程返回不同实例。
     */
    @Test
    public void testThreadLocalAStarReturnsThreadSpecificInstance() throws InterruptedException {
        AStar main = ThreadLocalAStar.current();
        assertSame(main, ThreadLocalAStar.current());

        AtomicReference<AStar> otherRef = new AtomicReference<>();
        Thread t = new Thread(() -> otherRef.set(ThreadLocalAStar.current()));
        t.start();
        t.join();

        assertNotNull(otherRef.get());
        assertNotSame(main, otherRef.get());
    }

    /**
     * 中文说明：验证AStar在执行后能够被clear重置为干净状态。
     */
    @Test
    public void testAStarClearRestoresCleanState() {
        AStar astar = new AStar();
        Grid grid = createGrid(4, 4);
        astar.search(0, 0, 3, 3, grid, new Path(), false);
        assertTrue(astar.isCLean(grid));

        astar.nodes.map = grid;
        astar.open(1, 1, 3, Grid.DIRECTION_UP, 2, 2, grid);
        assertEquals(1, astar.nodes.size);

        astar.clear();
        assertTrue(astar.nodes.isClean());
        assertNull(astar.nodes.map);
        assertTrue(grid.isClean());
    }

    /**
     * 中文说明：验证当可行走区域被完全阻隔时A*会穷尽搜索并返回空路径。
     */
    @Test
    public void testSearchUnreachableBarrierReturnsEmpty() {
        AStar astar = new AStar();
        Grid grid = createGrid(5, 3);
        for (int x = 0; x < grid.getWidth(); x++) {
            grid.setWalkable(x, 1, false);
        }
        Path path = new Path();

        astar.search(0, 0, 4, 2, grid, path, false);

        assertTrue("阻隔导致无法到达时路径应为空", path.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    /**
     * 中文说明：验证存在障碍时路径会包含转折点。
     */
    @Test
    public void testSearchObstacleForcesTurnContainsPivot() {
        AStar astar = new AStar();
        Grid grid = createGrid(5, 5);
        grid.setWalkable(1, 0, false);
        grid.setWalkable(1, 1, false);
        Path path = new Path();

        astar.search(0, 0, 2, 2, grid, path, false);

        assertFalse("存在可行走路径时不应为空", path.isEmpty());
        assertTrue("路径应包含至少一个转折点", path.size() >= 3);

        boolean hasPivot = false;
        for (int[] coord : pathToArray(path)) {
            if (!((coord[0] == 0 && coord[1] == 0) || (coord[0] == 2 && coord[1] == 2))) {
                hasPivot = true;
            }
        }
        assertTrue("路径应包含转折点", hasPivot);
        assertTrue(astar.isCLean(grid));
    }

    /**
     * 中文说明：验证内部异常发生时路径会被清空并重新抛出。
     */
    @Test
    public void testSearchExceptionClearsPathAndRethrows() {
        AStar astar = new AStar();
        Path path = new Path();
        Grid grid = new Grid(3, 3) {
            boolean thrown;

            @Override
            void nodeParentDirectionUpdate(int x, int y, int d) {
                super.nodeParentDirectionUpdate(x, y, d);
                if (!thrown) {
                    thrown = true;
                    throw new RuntimeException("模拟异常");
                }
            }
        };

        try {
            astar.search(0, 0, 2, 2, grid, path, false);
            fail("触发异常后应重新抛出");
        } catch (RuntimeException expected) {
            assertTrue("异常后路径应被清空", path.isEmpty());
        }
        assertTrue(astar.isCLean(grid));
    }

    /**
     * 中文说明：验证对角线移动因围栏阻止时返回上一安全点。
     */
    @Test
    public void testReachabilityDiagonalInterruptedByFence() {
        Grid grid = createGrid(5, 5);
        Fence fence = (x1, y1, x2, y2) -> x2 + y2 < 2;

        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 4, 3, 1, grid, fence);

        assertEquals("围栏阻止后应退回安全点", Point.toPoint(0, 0), result);
    }

    /**
     * 中文说明：验证斜线行进时相邻格阻挡会终止前进。
     */
    @Test
    public void testReachabilityDiagonalBreaksWhenAdjacentCellBlocked() {
        Grid grid = createGrid(5, 5);
        grid.setWalkable(1, 0, false);

        long result = Reachability.getClosestWalkablePointToTarget(0, 0, 3, 2, grid);

        assertEquals("相邻格阻挡时应停留在起点", Point.toPoint(0, 0), result);
    }

    /**
     * 中文说明：验证格子越界时会被视为不可行走。
     */
    @Test
    public void testGridIsWalkableOutOfBounds() {
        Grid grid = createGrid(2, 2);
        assertFalse(grid.isWalkable(-1, 0));
        assertFalse(grid.isWalkable(0, -1));
        assertFalse(grid.isWalkable(2, 0));
        assertFalse(grid.isWalkable(0, 2));

        grid.setWalkable(1, 1, false);
        assertFalse(grid.isWalkable(1, 1));
        grid.setWalkable(1, 1, true);
        assertTrue(grid.isWalkable(1, 1));
    }

    /**
     * 中文说明：验证启发式成本计算具备对称性并与曼哈顿距离线性相关。
     */
    @Test
    public void testCostHeuristicSymmetry() {
        assertEquals(35, Cost.hCost(0, 0, 3, 4));
        assertEquals(35, Cost.hCost(3, 4, 0, 0));
        assertEquals(0, Cost.hCost(5, 5, 5, 5));
    }

    /**
     * 中文说明：验证多叉堆在多个子节点情形下仍可保持正确顺序。
     */
    @Test
    public void testNodesSiftDownHandlesMultipleChildren() {
        Nodes nodes = new Nodes();
        Grid grid = createGrid(6, 6);
        nodes.map = grid;

        nodes.open(0, 0, 3, 4, Grid.DIRECTION_UP);
        nodes.open(1, 0, 1, 2, Grid.DIRECTION_UP);
        nodes.open(2, 0, 2, 0, Grid.DIRECTION_UP);
        nodes.open(3, 0, 1, 5, Grid.DIRECTION_UP);
        nodes.open(4, 0, 0, 1, Grid.DIRECTION_UP);

        long first = nodes.close();
        assertEquals(4, Node.getX(first));
        assertEquals(0, Node.getY(first));

        long second = nodes.close();
        assertEquals(2, Node.getX(second));

        long third = nodes.close();
        assertEquals(1, Node.getX(third));

        long fourth = nodes.close();
        assertEquals(3, Node.getX(fourth));

        long fifth = nodes.close();
        assertEquals(0, Node.getX(fifth));

        assertEquals(0, nodes.size);

        nodes.clear();
        assertTrue(nodes.isClean());
        assertNull(nodes.map);
        assertTrue(grid.isClean());
    }
}
