class UnknownParameterType {
    public static void main(String[] args) {
    }
}

class A {
    public int foo(int a, Missing b) { // ERROR
        return a;
    }
}
