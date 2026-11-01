class UnknownLocalType {
    public static void main(String[] args) {
    }
}

class A {
    public int foo() {
        int ok;
        Missing m; // ERROR
        return 0;
    }
}
