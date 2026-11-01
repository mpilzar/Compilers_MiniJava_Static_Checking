class SuperclassDeclaredLater {
    public static void main(String[] args) {
    }
}

class B extends A { // ERROR
}

class A {
}
