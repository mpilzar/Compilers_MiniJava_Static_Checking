class ReturnSuperForSub {
    public static void main(String[] args) {
    }
}

class A {
    public B make() {
        return new A(); // ERROR
    }
}

class B extends A {
}
