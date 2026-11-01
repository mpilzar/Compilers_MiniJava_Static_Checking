// The main class is an ordinary (empty) class: it can be extended and used as a type.
class ExtendsMainClass {
    public static void main(String[] args) {
        ExtendsMainClass p;
        p = new Sub();
        System.out.println(new Sub().go(p));
    }
}

class Sub extends ExtendsMainClass {
    int x;

    public int go(ExtendsMainClass p) {
        x = 1;
        return x;
    }
}
