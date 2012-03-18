package basic;

public class Ordered<T> implements Comparable<Ordered<T>> {
	
	private double value;
	private T element;
	
	public Ordered(T element, double value) {
		this.element = element;
		this.value = value;
	}
	
	public T get() {
		return this.element;
	}
	
	public double value() {
		return this.value;
	}

	@Override
	public int compareTo(Ordered<T> arg0) {
		return Double.compare(arg0.value, this.value);
	}
}