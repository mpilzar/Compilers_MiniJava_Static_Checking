class ThisInMainArgument {
    public static void main(String[] args) {
        A a;
        int r;
        a = new A();
        r = a.take(this); // ERROR
    }
}

class A {
    public int take(A other) {
        return 0;
    }
}
