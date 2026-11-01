// Same-arity overloads that differ by unrelated types, with different return types.
class OverloadResolution {
    public static void main(String[] args) {
        Calc c;
        int i;
        boolean b;
        c = new Calc();
        i = c.pick(5);
        b = c.pick(true);
        i = c.pick(new A());
        i = c.pick(new B());
        i = c.pick(new int[3]);
        i = c.pair(new B(), 1);
        b = c.pair(new A(), true);
        i = c.pick();
        System.out.println(i);
    }
}

class A {
}

class B extends A {
}

class Calc {
    public int pick(int x) {
        return x;
    }

    public boolean pick(boolean x) {
        return x;
    }

    public int pick(A x) {
        return 1;
    }

    public int pick(int[] x) {
        return x.length;
    }

    public int pair(A a, int x) {
        return x;
    }

    public boolean pair(A a, boolean x) {
        return x;
    }

    public int pick() {
        return 0;
    }
}
