package net.mooctest;

import net.mooctest.Board;

public class ZeroMeasure implements Measure {
	@Override
	public double score(Board board) {
		return 0;
	}
}
