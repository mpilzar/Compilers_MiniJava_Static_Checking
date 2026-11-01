class NoOverloadMatchesInherited {
    public static void main(String[] args) {
        B b;
        int r;
        b = new B();
        r = b.f(b); // ERROR
    }
}

class A {
    public int f(int x) {
        return x;
    }

    public int f(boolean x) {
        return 0;
    }
}

class B extends A {
    public int f(int x) {
        return 1;
    }

    public int f(int[] x) {
        return 2;
    }
}
