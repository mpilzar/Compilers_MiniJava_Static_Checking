// Every expression form and statement form of the language.
class Expressions {
    public static void main(String[] args) {
        Ops o;
        int r;
        o = new Ops();
        r = o.all(10, true);
        System.out.println(r);
    }
}

class Ops {
    int[] data;

    public int all(int n, boolean go) {
        int i;
        int sum;
        boolean done;
        data = new int[n];
        i = 0;
        sum = 0;
        done = !go;
        while (!done && (i < (data.length))) {
            data[i] = (i * 2) - 1;
            sum = sum + (data[i]);
            i = i + 1;
            done = !(i < n);
        }
        if (!!(sum < 0) && true) {
            sum = 0 - sum;
        } else {
            {
                System.out.println(sum);
            }
        }
        return this.twice(this.twice(sum));
    }

    public int twice(int x) {
        return x + x;
    }
}
