class AmbiguousWithGrandparent {
    public static void main(String[] args) {
    }
}

class A {
    public int foo(int x, A a) {
        return 0;
    }
}

class B extends A {
}

class C extends B {
    public int foo(int x, C c) { // ERROR
        return 1;
    }
}
