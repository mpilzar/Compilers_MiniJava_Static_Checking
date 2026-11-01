class LengthOnObject {
    public static void main(String[] args) {
        A a;
        int n;
        a = new A();
        n = a.length; // ERROR
    }
}

class A {
}
