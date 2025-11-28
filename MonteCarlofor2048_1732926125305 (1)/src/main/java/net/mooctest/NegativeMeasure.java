package net.mooctest;

import net.mooctest.Board;

public class NegativeMeasure implements Measure {
	Measure wrapped;

	public NegativeMeasure(Measure wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public double score(Board board) {
		return -wrapped.score(board);
	}
}
