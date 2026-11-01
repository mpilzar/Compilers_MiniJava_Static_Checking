// A method can only see fields of its own class and its superclasses.
class FieldOfUnrelatedClass {
    public static void main(String[] args) {
    }
}

class A {
    int secret;
}

class B {
    public int peek() {
        return secret; // ERROR
    }
}
