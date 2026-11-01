// f(boolean) returns boolean, so its result cannot be stored in an int.
class OverloadResultTypeMismatch {
    public static void main(String[] args) {
        A a;
        int r;
        a = new A();
        r = a.f(1);
        r = a.f(true); // ERROR
    }
}

class A {
    public int f(int x) {
        return x;
    }

    public boolean f(boolean b) {
        return b;
    }
}
