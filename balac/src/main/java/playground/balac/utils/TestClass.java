package playground.balac.utils;

import java.util.Random;

public class TestClass {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Random r = new Random();
		
		while (true) {
			if (r.nextInt(2) < 0 )
				System.out.println("bla");
		}
		

	}

}