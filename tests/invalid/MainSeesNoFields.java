// main is static: fields of other classes (even subclasses of the main class) are not in scope.
class MainSeesNoFields {
    public static void main(String[] args) {
        x = 1; // ERROR
    }
}

class Sub extends MainSeesNoFields {
    int x;
}
