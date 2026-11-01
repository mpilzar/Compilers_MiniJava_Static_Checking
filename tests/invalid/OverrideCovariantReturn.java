// MiniJava requires an override to keep exactly the same return type.
class OverrideCovariantReturn {
    public static void main(String[] args) {
    }
}

class A {
    public A self() {
        return this;
    }
}

class B extends A {
    public B self() { // ERROR
        return this;
    }
}
