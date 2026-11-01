class SyntaxVoidMethod {
    public static void main(String[] args) {
    }
}

class A {
    public void run() { // ERROR
        return 0;
    }
}
