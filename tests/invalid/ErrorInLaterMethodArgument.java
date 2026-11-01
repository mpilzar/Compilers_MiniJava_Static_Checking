class ErrorInLaterMethodArgument {
    public static void main(String[] args) {
        A a;
        int r;
        a = new A();
        r = a.three(1, 2, true); // ERROR
    }
}

class A {
    public int three(int x, int y, int z) {
        return (x + y) + z;
    }
}
