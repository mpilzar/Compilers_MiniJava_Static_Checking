class MethodCallOnInt {
    public static void main(String[] args) {
        int x;
        x = 3;
        x = x.foo(); // ERROR
    }
}
