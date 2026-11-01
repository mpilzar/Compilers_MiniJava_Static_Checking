// Method calls on the result of other method calls, and recursion through fields.
class ChainedCalls {
    public static void main(String[] args) {
        System.out.println(((((new Node()).init(3)).next()).next()).value());
    }
}

class Node {
    Node nxt;
    int val;

    public Node init(int n) {
        val = n;
        if (0 < n) {
            nxt = new Node();
            nxt = nxt.init(n - 1);
        } else {
            nxt = this;
        }
        return this;
    }

    public Node next() {
        return nxt;
    }

    public int value() {
        return val;
    }
}
