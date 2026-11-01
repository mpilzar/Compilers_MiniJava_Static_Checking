class ArgumentSuperForSub {
    public static void main(String[] args) {
        C c;
        int r;
        c = new C();
        r = c.take(new A()); // ERROR
    }
}

class A {
}

class B extends A {
}

class C {
    public int take(B b) {
        return 0;
    }
}
