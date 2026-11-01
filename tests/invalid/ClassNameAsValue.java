class ClassNameAsValue {
    public static void main(String[] args) {
        A a;
        a = A; // ERROR
    }
}

class A {
}
