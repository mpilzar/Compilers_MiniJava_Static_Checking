class ArraySizeNotInt {
    public static void main(String[] args) {
        int[] a;
        a = new int[true]; // ERROR
    }
}
