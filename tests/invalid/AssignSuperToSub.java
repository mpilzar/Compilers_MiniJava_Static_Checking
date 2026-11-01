class AssignSuperToSub {
    public static void main(String[] args) {
        B b;
        b = new A(); // ERROR
    }
}

class A {
}

class B extends A {
}
