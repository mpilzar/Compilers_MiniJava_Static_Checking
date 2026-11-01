class ArrayIndexNotInt {
    public static void main(String[] args) {
        int[] a;
        int x;
        a = new int[2];
        x = a[false]; // ERROR
    }
}
