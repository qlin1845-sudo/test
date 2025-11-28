package net.mooctest;

import net.mooctest.Board;

public class FreesMeasure implements Measure {
	@Override
	public double score(Board board) {
		int frees = 0;
		for (int p : Board.all) {
			if (board.grid()[p] == 0)
				frees++;
		}
		return frees;
	}
}
