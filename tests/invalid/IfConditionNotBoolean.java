class IfConditionNotBoolean {
    public static void main(String[] args) {
        if (1) // ERROR
            System.out.println(1);
        else
            System.out.println(0);
    }
}
