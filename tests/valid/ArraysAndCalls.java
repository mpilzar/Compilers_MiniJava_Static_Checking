// int[] as parameter, return type, field and local.
class ArraysAndCalls {
    public static void main(String[] args) {
        Vec v;
        int[] a;
        v = new Vec();
        a = v.make(4);
        a[0] = v.sum(a);
        System.out.println(a[0]);
        System.out.println(a.length);
    }
}

class Vec {
    int[] last;

    public int[] make(int n) {
        int i;
        last = new int[n];
        i = 0;
        while (i < n) {
            last[i] = i;
            i = i + 1;
        }
        return last;
    }

    public int sum(int[] xs) {
        int i;
        int total;
        i = 0;
        total = 0;
        while (i < (xs.length)) {
            total = total + (xs[i]);
            i = i + 1;
        }
        return total;
    }
}
