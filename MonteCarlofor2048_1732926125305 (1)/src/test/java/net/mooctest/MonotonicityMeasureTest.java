package net.mooctest;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class MonotonicityMeasureTest {

    private Strategy identityStrategy;

    @Before
    public void resetRolloutDefaults() {
        identityStrategy = new Strategy() {
            @Override
            public Board play(Board board) {
                return board;
            }
        };
        UCTStrategy.rolloutMeasure = new SumMeasure();
        UCTStrategy.rolloutStrategy = identityStrategy;
    }

    // 测试：验证向左移动的合并逻辑以及拷贝后的棋盘不会影响原棋盘
    @Test
    public void testBoardMoveMergesAndCopy() {
        Board original = boardFromRows(new int[][] {
                {1, 1, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        Board moved = original.move(Board.LEFT);
        assertEquals(2, moved.grid()[Board.all[0]]);
        assertEquals(0, moved.grid()[Board.all[1]]);
        assertTrue(moved.changed);
        assertEquals(1, original.grid()[Board.all[0]]);
        assertEquals(1, original.grid()[Board.all[1]]);
    }

    // 测试：验证棋盘填满且无合并机会时的卡死判定与方向检测
    @Test
    public void testBoardFullStuckAndDirections() {
        Board board = createStuckBoard();
        assertTrue(board.isFull());
        assertTrue(board.isStuck());
        for (int move : Board.moves) {
            assertFalse(board.canDirection(move));
        }
    }

    // 测试：验证不安全生成随控随机数时能在指定位置出现新块
    @Test
    public void testBoardUnsafeSpawnDeterministic() {
        Board board = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        SequenceRandom stub = new SequenceRandom();
        stub.addInts(7, 0);
        setBoardRandom(board, stub);
        board.unsafe_spawn();
        assertEquals(2, board.grid()[Board.all[0]]);
        for (int i = 1; i < Board.all.length; i++) {
            assertEquals(0, board.grid()[Board.all[i]]);
        }
    }

    // 测试：验证安全生成会返回新对象且只新增一个非零方块
    @Test
    public void testBoardSpawnCreatesNewBoard() {
        Board board = boardFromRows(new int[][] {
                {1, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        int zerosBefore = countZeros(board);
        Board spawned = board.spawn();
        assertNotSame(board, spawned);
        assertEquals(zerosBefore, countZeros(board));
        assertEquals(zerosBefore - 1, countZeros(spawned));
    }

    // 测试：验证 equals、hashCode 与 toString 的一致性
    @Test
    public void testBoardEqualityHashToString() {
        Board a = boardFromRows(new int[][] {
                {1, 2, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        Board b = boardFromRows(new int[][] {
                {1, 2, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        Board c = boardFromRows(new int[][] {
                {0, 2, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("Board [grid="));
        assertTrue(a.toString().contains("."));
    }

    // 测试：验证多种评分函数在不同棋盘状态下的正确值
    @Test
    public void testMeasureImplementations() {
        Board mixed = boardFromRows(new int[][] {
                {1, 0, 0, 0},
                {0, 2, 0, 0},
                {0, 0, 3, 0},
                {0, 0, 0, 0}
        });
        SumMeasure sum = new SumMeasure();
        BestMeasure best = new BestMeasure();
        FreesMeasure frees = new FreesMeasure();
        NegativeMeasure negative = new NegativeMeasure(sum);
        ZeroMeasure zero = new ZeroMeasure();
        assertEquals((1 << 1) + (1 << 2) + (1 << 3), sum.score(mixed), 1e-9);
        assertEquals(1 << 3, best.score(mixed), 1e-9);
        assertEquals(13, frees.score(mixed), 1e-9);
        assertEquals(-sum.score(mixed), negative.score(mixed), 1e-9);
        assertEquals(0, zero.score(mixed), 1e-9);

        Board empty = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        assertEquals(0, best.score(empty), 1e-9);
    }

    // 测试：验证集成评分按权重聚合多个子评分
    @Test
    public void testEnsambleMeasureCombination() {
        Board board = boardFromRows(new int[][] {
                {1, 1, 0, 0},
                {0, 2, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        EnsambleMeasure ensamble = new EnsambleMeasure()
                .addMeasure(0.5, new SumMeasure())
                .addMeasure(2.0, new BestMeasure());
        double expected = 0.5 * new SumMeasure().score(board)
                + 2.0 * new BestMeasure().score(board);
        assertEquals(expected, ensamble.score(board), 1e-9);
    }

    // 测试：验证平滑度与单调性评分与手工计算一致
    @Test
    public void testSmoothAndMonotonicMeasuresConsistency() {
        Board board = boardFromRows(new int[][] {
                {1, 2, 0, 0},
                {0, 2, 3, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        double manual = manualDirectionalDifference(board, 0, 1);
        assertEquals(manual, new MonotonicityMeasure().score(board), 1e-9);
        assertEquals(manual, new SmoothMeasure().score(board), 1e-9);
    }

    // 测试：验证循环策略在无可行动作时会遍历自定义与默认队列后退出
    @Test
    public void testCyclicStrategyFallbackWhenNoMove() {
        Board original = createStuckBoard();
        Board copy = original.copy();
        CyclicStrategy strategy = new CyclicStrategy(Board.UP, Board.LEFT);
        Board result = strategy.play(original);
        assertEquals(copy, original);
        assertEquals(original, result);
    }

    // 测试：验证贪心策略在有动作时优先选择评分更高的方向
    @Test
    public void testGreedyStrategyPickMovePrefersHigherScore() {
        Measure leftBias = board -> {
            double score = 0;
            for (int i = 0; i < Board.all.length; i++) {
                int val = board.grid()[Board.all[i]];
                int col = i % 4;
                score += val * (4 - col);
            }
            return score;
        };
        GreedyStrategy strategy = new GreedyStrategy(leftBias);
        Board board = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 3, 0},
                {0, 0, 0, 0}
        });
        Board best = strategy.pickMove(board);
        assertEquals(board.move(Board.LEFT), best);
    }

    // 测试：验证贪心策略在评分相等时使用遍历顺序的最后一次动作并在卡死棋盘上立即停止
    @Test
    public void testGreedyStrategyTieBreakerAndPlayStops() {
        Measure constant = board -> 1.0;
        GreedyStrategy strategy = new GreedyStrategy(constant);
        Board board = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 0, 0}
        });
        Board selected = strategy.pickMove(board);
        assertEquals(board.move(Board.LEFT), selected);

        Board stuck = createStuckBoard();
        Board result = strategy.play(stuck);
        assertEquals(stuck, result);
    }

    // 测试：验证随机策略构造参数与选取动作的边界场景
    @Test
    public void testRandomStrategyConstructorAndPickMove() {
        try {
            new RandomStrategy(0.5, 0.5, 0.5);
            fail("应抛出参数异常");
        } catch (InvalidParameterException expected) {
            // expected
        }

        RandomStrategy strategy = new RandomStrategy(0.1, 0.2, 0.3, 0.4);
        SequenceRandom doubleStub = new SequenceRandom();
        doubleStub.addDoubles(0.65);
        strategy.rand = doubleStub;
        assertEquals(Board.LEFT, strategy.pickMove());

        RandomStrategy faulty = new RandomStrategy(0.25, 0.25, 0.0, 0.0);
        SequenceRandom badStub = new SequenceRandom();
        badStub.addDoubles(0.9);
        faulty.rand = badStub;
        try {
            faulty.pickMove();
            fail("概率和不足应抛出异常");
        } catch (InvalidParameterException expected) {
            // expected
        }
    }

    // 测试：验证随机策略在卡死棋盘上不会进入循环
    @Test
    public void testRandomStrategyPlayOnStuckBoard() {
        RandomStrategy strategy = new RandomStrategy();
        Board stuck = createStuckBoard();
        Board result = strategy.play(stuck.copy());
        assertEquals(stuck, result);
    }

    // 测试：验证平滑策略的内核函数、异常分支与最优动作选择
    @Test
    public void testSmoothStrategyKernelAndSelection() {
        SmoothStrategy idStrategy = new SmoothStrategy("id");
        assertEquals(Math.abs(5 - 3), idStrategy.kernel(5, 3));
        SmoothStrategy powStrategy = new SmoothStrategy("pow");
        assertEquals(Math.abs((1 << 2) - (1 << 1)), powStrategy.kernel(2, 1));
        try {
            new SmoothStrategy("bad").kernel(1, 2);
            fail("非法核函数应抛出错误");
        } catch (Error expected) {
            // expected
        }

        Board board = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 2, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 0}
        });
        Board picked = idStrategy.pickMove(board);
        Board expected = null;
        int best = Integer.MAX_VALUE;
        for (int move : Board.moves) {
            Board moved = board.move(move);
            if (moved.changed) {
                int smoothness = idStrategy.smoothness(moved);
                if (smoothness <= best) {
                    best = smoothness;
                    expected = moved;
                }
            }
        }
        assertEquals(expected, picked);
    }

    // 测试：验证平滑策略在无有效动作时立即结束
    @Test
    public void testSmoothStrategyPlayStopsWhenNoCandidate() {
        SmoothStrategy strategy = new SmoothStrategy("id");
        Board stuck = createStuckBoard();
        Board result = strategy.play(stuck);
        assertEquals(stuck, result);
    }

    // 测试：验证位棋盘的四个方向移动、转置与翻转等操作
    @Test
    public void testBitBoardsMovementAndTransforms() {
        Board board = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 1, 1, 0},
                {0, 0, 2, 2},
                {0, 0, 0, 0}
        });
        long bit = toBitBoard(board);
        assertEquals(board.move(Board.RIGHT), boardFromBitBoard(BitBoards.move_right(bit)));
        assertEquals(board.move(Board.LEFT), boardFromBitBoard(BitBoards.move_left(bit)));
        assertEquals(board.move(Board.UP), boardFromBitBoard(BitBoards.move_up(bit)));
        assertEquals(board.move(Board.DOWN), boardFromBitBoard(BitBoards.move_down(bit)));
        for (int move : BitBoards.moves) {
            assertEquals(board.move(move), boardFromBitBoard(BitBoards.move(bit, move)));
        }
        assertEquals(bit, BitBoards.trans(BitBoards.trans(bit)));
        assertEquals(bit, BitBoards.reverse(BitBoards.reverse(bit)));
    }

    // 测试：验证位棋盘的随机生成、自由格统计与方向判断
    @Test
    public void testBitBoardsSpawnPickAndUtilities() {
        Board board = boardFromRows(new int[][] {
                {1, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        long bit = toBitBoard(board);
        assertEquals(15, BitBoards.frees(bit));
        assertTrue(BitBoards.canDirection(bit, BitBoards.RIGHT));
        assertTrue(BitBoards.isStuck(bit));
        long stuckBit = toBitBoard(createStuckBoard());
        assertFalse(BitBoards.isStuck(stuckBit));

        SequenceRandom spawnStub = new SequenceRandom();
        spawnStub.addInts(0, 0);
        Random old = swapBitBoardsRandom(spawnStub);
        try {
            long spawned = BitBoards.spawn(bit);
            Board spawnBoard = boardFromBitBoard(spawned);
            assertEquals(14, BitBoards.frees(spawned));
            assertEquals(countZeros(board) - 1, countZeros(spawnBoard));
        } finally {
            swapBitBoardsRandom(old);
        }

        SequenceRandom pickStub = new SequenceRandom();
        pickStub.addInts(0, 5);
        old = swapBitBoardsRandom(pickStub);
        try {
            assertEquals(2, BitBoards.pickRandomly());
            assertEquals(1, BitBoards.pickRandomly());
        } finally {
            swapBitBoardsRandom(old);
        }
    }

    // 测试：验证 UCT 策略在卡死棋盘上立即返回
    @Test
    public void testUCTStrategyReturnsOriginalWhenStuck() {
        UCTStrategy strategy = new UCTStrategy(2, false, new SumMeasure(), identityStrategy);
        Board stuck = createStuckBoard();
        Board result = strategy.play(stuck.copy());
        assertEquals(stuck, result);
    }

    // 测试：验证选择叶子在不同棋盘状态下的展开与异常
    @Test
    public void testChoiceLeafExpansionAndSelection() {
        Board movable = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {1, 1, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        ChoiceLeaf leaf = new ChoiceLeaf(movable);
        Node expanded = leaf.expand();
        assertTrue(expanded instanceof ChoiceNode);

        ChoiceLeaf stuckLeaf = new ChoiceLeaf(createStuckBoard());
        Node exit = stuckLeaf.expand();
        assertTrue(exit instanceof ExitNode);

        try {
            leaf.select(true);
            fail("选择叶子不支持 select");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // 测试：验证选择节点在导入探索奖励与替换子节点时的逻辑
    @Test
    public void testChoiceNodeSelectAndExpand() {
        Board board = boardFromRows(new int[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        StubNode strong = new StubNode(board, 3, 30);
        StubNode weak = new StubNode(board, 1, 1);
        ChoiceNode choice = new ChoiceNode(board, 5.0, new ArrayList<>(Arrays.asList(weak, strong)));
        assertSame(strong, choice.select(true));
        assertSame(strong, choice.select(false));

        StubNode base = new StubNode(board, 0, 0);
        StubNode replacement = new StubNode(board, 0, 10);
        base.setExpandResult(replacement);
        ChoiceNode replaceChoice = new ChoiceNode(board, 1.0, new ArrayList<>(Arrays.asList(base)));
        replaceChoice.expand();
        assertTrue(replaceChoice.children.contains(replacement));
    }

    // 测试：验证终止节点的累计访问与异常分支
    @Test
    public void testExitNodeBehaviour() {
        Board board = new Board();
        ExitNode node = new ExitNode(board, 2.5);
        double initial = node.value();
        node.expand();
        assertEquals(initial + 2.5, node.value(), 1e-9);
        assertEquals(2, node.visits());
        try {
            node.select(true);
            fail("终止节点不支持 select");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // 测试：验证生成节点在新增子节点和复用旧子节点场景下的价值更新
    @Test
    public void testSpawnNodeHandlesNewAndExistingChildren() throws Exception {
        Board board = boardFromRows(new int[][] {
                {0, 1, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        });
        SpawnNode node = new SpawnNode(board);
        double baseValue = node.value();
        int baseVisits = node.visits();
        node.expand();
        assertTrue(node.value() >= baseValue);
        assertEquals(baseVisits + 1, node.visits());

        List<Node> children = getSpawnChildren(node);
        StubNode manual = new StubNode(node.board(), 0, 5);
        manual.setIncrement(4);
        children.add(manual);
        double before = node.value();
        node.expand();
        assertTrue(node.value() >= before + 4);
        assertTrue(children.contains(manual));
    }

    // ------------------ 辅助方法与测试桩 ------------------

    private Board boardFromRows(int[][] rows) {
        if (rows.length != 4) {
            throw new IllegalArgumentException("必须是 4x4 棋盘");
        }
        Board board = new Board();
        for (int r = 0; r < 4; r++) {
            if (rows[r].length != 4) {
                throw new IllegalArgumentException("必须是 4x4 棋盘");
            }
            for (int c = 0; c < 4; c++) {
                int idx = r * 4 + c;
                board.grid()[Board.all[idx]] = rows[r][c];
            }
        }
        return board;
    }

    private Board createStuckBoard() {
        return boardFromRows(new int[][] {
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 15, 16}
        });
    }

    private int countZeros(Board board) {
        int zeros = 0;
        for (int p : Board.all) {
            if (board.grid()[p] == 0) {
                zeros++;
            }
        }
        return zeros;
    }

    private double manualDirectionalDifference(Board board, int start, int end) {
        double res = 0;
        for (int m = start; m <= end; m++) {
            int dir = Board.dirs[m];
            for (int p : Board.orders[m]) {
                int a = board.grid()[p] == 0 ? 0 : 1 << board.grid()[p];
                int b = board.grid()[p + dir] == 0 ? 0 : 1 << board.grid()[p + dir];
                res += Math.abs(a - b);
            }
        }
        return res;
    }

    private void setBoardRandom(Board board, Random random) {
        try {
            Field field = Board.class.getDeclaredField("rand");
            field.setAccessible(true);
            field.set(board, random);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long toBitBoard(Board board) {
        long value = 0;
        for (int i = 0; i < Board.all.length; i++) {
            value |= (long) board.grid()[Board.all[i]] << (i * 4);
        }
        return value;
    }

    private Board boardFromBitBoard(long bit) {
        Board board = new Board();
        for (int i = 0; i < Board.all.length; i++) {
            board.grid()[Board.all[i]] = (int) ((bit >>> (i * 4)) & 0xf);
        }
        return board;
    }

    private Random swapBitBoardsRandom(Random replacement) {
        try {
            Field field = BitBoards.class.getDeclaredField("rand");
            field.setAccessible(true);
            Random old = (Random) field.get(null);
            field.set(null, replacement);
            return old;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Node> getSpawnChildren(SpawnNode node) throws Exception {
        Field field = SpawnNode.class.getDeclaredField("children");
        field.setAccessible(true);
        return (List<Node>) field.get(node);
    }

    private static class SequenceRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final Queue<Integer> ints = new ArrayDeque<>();
        private final Queue<Double> doubles = new ArrayDeque<>();

        SequenceRandom addInts(int... values) {
            for (int v : values) {
                ints.add(v);
            }
            return this;
        }

        SequenceRandom addDoubles(double... values) {
            for (double v : values) {
                doubles.add(v);
            }
            return this;
        }

        @Override
        public int nextInt(int bound) {
            if (ints.isEmpty()) {
                return 0;
            }
            int value = ints.poll();
            value = Math.abs(value);
            return bound == 0 ? 0 : value % bound;
        }

        @Override
        public double nextDouble() {
            if (doubles.isEmpty()) {
                return 0.0;
            }
            return doubles.poll();
        }
    }

    private static class StubNode extends Node {
        Node expandResult;
        double increment;

        StubNode(Board board, int visits, double value) {
            super(board);
            this.visits = visits;
            this.value = value;
        }

        void setExpandResult(Node expandResult) {
            this.expandResult = expandResult;
        }

        void setIncrement(double inc) {
            this.increment = inc;
        }

        @Override
        Node expand() {
            visits += 1;
            value += increment;
            return expandResult == null ? this : expandResult;
        }

        @Override
        Node select(boolean explore) {
            return this;
        }
    }
}
