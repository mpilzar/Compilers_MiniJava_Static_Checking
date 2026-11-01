class AmbiguousOverload {
    public static void main(String[] a){
        System.out.println(new Test().start());
    }
}

class A {}

class B extends A {}

class Test {
    public int start(){
        return 0;
    }

    public int foo(A arg){
        return 1;
    }

    // TYPE ERROR: Ambiguous overloading. B is a subtype of A.
    public int foo(B arg){ // ERROR
        return 2;
    }
}