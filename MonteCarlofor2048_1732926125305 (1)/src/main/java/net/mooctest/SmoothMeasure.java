package net.mooctest;

import net.mooctest.Board;

public class SmoothMeasure implements Measure {
	@Override
	public double score(Board board) {
		int res = 0;
		for (int m = Board.UP; m <= Board.RIGHT; m++) {
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
