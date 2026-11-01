class AmbiguousAcrossHierarchy {
    public static void main(String[] args) {
    }
}

class A {
    public int foo(A a) {
        return 0;
    }
}

class B extends A {
    public int foo(B b) { // ERROR
        return 1;
    }
}
