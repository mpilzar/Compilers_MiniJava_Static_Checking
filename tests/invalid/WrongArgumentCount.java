class WrongArgumentCount {
    public static void main(String[] args) {
        A a;
        int r;
        a = new A();
        r = a.add(1); // ERROR
    }
}

class A {
    public int add(int x, int y) {
        return x + y;
    }
}
