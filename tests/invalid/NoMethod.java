class NoMethod {

    public static void main(String[] args) {
        A a;
        a = new A ();

        a = a.foo(); // ERROR
    }

}

class A {

}
