class DoubleDeclaration6 {

    public static void main(String[] args){

    }

}


class A {

    public int foo(A a){
        return 1;
    }

    public int foo(A b){ // ERROR
        return 2;
    }


}
