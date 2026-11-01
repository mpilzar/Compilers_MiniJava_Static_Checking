class AmbiguousMultipleParameters {
    public static void main(String[] args) {
    }
}

class A {
}

class B extends A {
}

class C {
    public int foo(A a, int x) {
        return 0;
    }

    public int foo(B b, int x) { // ERROR
        return 1;
    }
}
