package cz.bliksoft.javautils;

public class _<E> {
    E ref;
    public _( E e ){
        ref = e;
    }
    public E getValue() { return ref; }
    public void setValue( E e ){ this.ref = e; }

    public String toString() {
        return ref.toString();
    }
}
