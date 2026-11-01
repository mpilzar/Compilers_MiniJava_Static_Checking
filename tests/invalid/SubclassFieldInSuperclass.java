class SubclassFieldInSuperclass {
    public static void main(String[] args) {
    }
}

class A {
    public int get() {
        return onlyInB; // ERROR
    }
}

class B extends A {
    int onlyInB;
}
