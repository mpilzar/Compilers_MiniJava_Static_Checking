// The parameter classes are declared after the methods that use them; the overloads still clash
// because Q extends P.
class AmbiguousForwardTypes {
    public static void main(String[] args) {
    }
}

class Test {
    public int foo(P p) {
        return 0;
    }

    public int foo(Q q) { // ERROR
        return 1;
    }
}

class P {
}

class Q extends P {
}
