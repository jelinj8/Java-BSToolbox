package cz.bliksoft.javautils;

public class DoubleObject<P, Q> {
	P o1;
	Q o2;

	public DoubleObject(P o1, Q o2) {
		this.o1 = o1;
		this.o2 = o2;
	}

	public P getO1() {
		return o1;
	}

	public Q getO2() {
		return o2;
	}
}
