class NewUnknownClass {
    public static void main(String[] args) {
        int x;
        x = new Missing().foo(); // ERROR
    }
}
