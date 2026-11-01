class LengthOnNonArray {
    public static void main(String[] a){
        System.out.println(new A().foo());
    }
}

class A {
    public int foo(){
        int num;
        int len;
        num = 10;
        len = num.length; // length only an int[] array // ERROR
        return len;
    }
}