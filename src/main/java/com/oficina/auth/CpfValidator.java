package com.oficina.auth;

/** Regra portátil para a Function e para os testes locais, sem dependência cloud. */
public final class CpfValidator {
    private CpfValidator() { }

    public static boolean isValid(String cpf) {
        if (cpf == null) return false;
        String value = cpf.replaceAll("\\D", "");
        if (value.length() != 11 || value.chars().distinct().count() == 1) return false;
        return digit(value, 9, 10) == Character.getNumericValue(value.charAt(9))
                && digit(value, 10, 11) == Character.getNumericValue(value.charAt(10));
    }

    private static int digit(String value, int length, int weight) {
        int sum = 0;
        for (int i = 0; i < length; i++) sum += Character.getNumericValue(value.charAt(i)) * (weight - i);
        int result = 11 - sum % 11;
        return result >= 10 ? 0 : result;
    }
}
