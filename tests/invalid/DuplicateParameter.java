class DuplicateParameter {
    public static void main(String[] args) {
    }
}

class A {
    public int foo(int a, boolean a) { // ERROR
        return 0;
    }
}
