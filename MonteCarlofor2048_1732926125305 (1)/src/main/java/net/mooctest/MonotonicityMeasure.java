package net.mooctest;

import net.mooctest.Board;

public class MonotonicityMeasure implements Measure {
	@Override
	public double score(Board board) {
		int res = 0;
		for (int m = 0; m <= 1; m++) {
			int dir = Board.dirs[m];
			for (int p : Board.orders[m]) {
				int a = (board.grid()[p] == 0 ? 0 : 1 << board.grid()[p]);
				int b = (board.grid()[p+dir] == 0 ? 0 : 1 << board.grid()[p+dir]);
				res += Math.abs(a-b);
			}
		}
		return res;
	}
}
