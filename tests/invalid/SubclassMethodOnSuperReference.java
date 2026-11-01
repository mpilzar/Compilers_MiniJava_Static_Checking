class SubclassMethodOnSuperReference {
    public static void main(String[] args) {
        A a;
        int r;
        a = new B();
        r = a.onlyInB(); // ERROR
    }
}

class A {
}

class B extends A {
    public int onlyInB() {
        return 1;
    }
}
