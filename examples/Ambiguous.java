class Ambiguous {
    public static void main(String[] args) {
        System.out.println(new Test().run());
    }
}

class Test {
    public int run() {
        return this.foo(new Q());
    }

    public int foo(P p) {
        return 0;
    }

    public int foo(Q q) {
        return 1;
    }
}

// P and Q are declared after Test, yet the clash above is still detected.
class P {
}

class Q extends P {
}
