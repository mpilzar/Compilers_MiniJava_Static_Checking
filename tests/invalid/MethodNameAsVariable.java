class MethodNameAsVariable {
    public static void main(String[] args) {
    }
}

class A {
    public int foo() {
        foo = 1; // ERROR
        return 0;
    }
}
