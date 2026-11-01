// Classes may be used as field / parameter types before they are declared.
class ForwardReferences {
    public static void main(String[] args) {
        Owner o;
        o = new Owner();
        System.out.println(o.init());
    }
}

class Owner {
    Later later;
    Other other;

    public int init() {
        later = new Later();
        other = new Other();
        return later.use(other);
    }
}

class Later {
    int value;

    public int use(Other o) {
        value = o.get();
        return value;
    }

    // Distinguished from use(Other) because Unrelated (declared below) is not related to Other.
    public int use(Unrelated u) {
        return 0;
    }
}

class Other {
    public int get() {
        return 7;
    }
}

class Unrelated {
}
