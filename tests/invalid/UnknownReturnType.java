class UnknownReturnType {
    public static void main(String[] args) {
    }
}

class A {
    public Missing foo() { // ERROR
        return this;
    }
}
