class NoOverloadMatches {
    public static void main(String[] args) {
        A a;
        int r;
        a = new A();
        r = a.f(a); // ERROR
    }
}

class A {
    public int f(int x) {
        return x;
    }

    public int f(boolean b) {
        return 0;
    }
}
