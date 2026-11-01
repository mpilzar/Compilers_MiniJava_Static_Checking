// Overrides reuse the parent's v-table slot; overloads in a subclass get new slots.
class OverrideAndOverload {
    public static void main(String[] args) {
        Base b;
        b = new MoreDerived();
        System.out.println(b.run(1));
    }
}

class Base {
    int counter;

    public int run(int x) {
        counter = x;
        return counter;
    }

    public int stop() {
        return 0;
    }
}

class Derived extends Base {
    boolean flag;

    public int run(int x) {
        counter = x + 1;
        return counter;
    }

    public int run(boolean x) {
        flag = x;
        return counter;
    }

    public int stop() {
        return 1;
    }

    public int resume() {
        return 2;
    }
}

class MoreDerived extends Derived {
    int extra;

    public int run(boolean x) {
        return 3;
    }

    public int resume(int times) {
        return times;
    }
}
