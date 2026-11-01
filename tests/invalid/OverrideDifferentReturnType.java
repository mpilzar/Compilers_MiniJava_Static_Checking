class OverrideDifferentReturnType {
    public static void main(String[] args) {
    }
}

class A {
    public int foo(int x) {
        return x;
    }
}

class B extends A {
    public boolean foo(int x) { // ERROR
        return true;
    }
}
