class WhileConditionNotBoolean {
    public static void main(String[] args) {
        int i;
        i = 0;
        while (i) { // ERROR
            i = i + 1;
        }
    }
}
