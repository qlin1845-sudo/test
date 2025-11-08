package net.mooctest;

import static org.junit.Assert.*;

import org.junit.Test;

public class LibraryTest {

	@Test
	public void test() {
		Book book = new Book("Effective Java", "Joshua Bloch", "0321356683", BookType.GENERAL, 5);
		
	}

}
